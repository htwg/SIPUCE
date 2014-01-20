package de.fhkn.in.uce.socketswitch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.fhkn.in.uce.socketswitch.SwitchableException;

final class TimeoutSwitchableInputStream extends ByteCountSwitchableInputStream {

	private static final int DEFAUT_PERIODIC_READ_TIMEOUT = 5;
	private volatile int periodicReadTimeout = DEFAUT_PERIODIC_READ_TIMEOUT;
	private volatile int readTimeout;

	private final Lock switchLock;
	private volatile InputStream currentSocketStream;
	private volatile Socket currentSocket;
	private volatile boolean switchIsPrepared;
	private volatile boolean socketInvalid;
	// long variable must be able to overflow
	private volatile long receivedBytesCount;

	private volatile ByteArrayInputStream byteBuffer;

	public TimeoutSwitchableInputStream(Socket socket) throws IOException {
		switchLock = new ReentrantLock(true);
		this.currentSocket = socket;
		// save the old "real" timeout
		readTimeout = currentSocket.getSoTimeout();
		socket.setSoTimeout(periodicReadTimeout);
		this.currentSocketStream = this.currentSocket.getInputStream();
		switchIsPrepared = false;
		socketInvalid = false;
		receivedBytesCount = 0;
	}

	@Override
	public void allocateSwitchLock(int timeoutMillis) throws SwitchableException, InterruptedException, TimeoutException {
		if (switchIsPrepared) {
			throw new IllegalStateException(
					"allocateSwitchLock() called twice before a call of"
							+ "releaseSwitchLock() or switchStream()!");
		}
		if (socketInvalid) {
			throw new SwitchableException(
					"Previous switch failed unforgivable, socket is no more valid. Reading is allowed until EOF.");
		}
		boolean hasLock = false;
		hasLock = switchLock.tryLock(timeoutMillis, TimeUnit.MILLISECONDS);

		if (hasLock == false) {
			throw new TimeoutException("Timeout entering read lock");
		}
		switchIsPrepared = true;
	}

	@Override
	public void releaseSwitchLock() {
		if (switchIsPrepared == false) {
			throw new IllegalStateException(
					"allocateSwitchLock() returned not successfully!");
		}
		switchIsPrepared = false;
		switchLock.unlock();
	}

	@Override
	public void switchStream(Socket newSocket, int timeoutMillis,
			long mustReadCountIndex) throws SwitchableException, TimeoutException {
		if (switchIsPrepared == false) {
			throw new IllegalStateException(
					"allocateSwitchLock() returned not successfully!");
		}

		if (socketInvalid) {
			throw new SwitchableException(
					"Previous switch failed unforgivable, socket is no more valid. Reading is allowed until EOF.");
		}

		try {
			createNewBufferStreamAndReadLastBytes(mustReadCountIndex,
					timeoutMillis);

			// need new socket to set the read timeout
			newSocket.setSoTimeout(periodicReadTimeout);
			currentSocket = newSocket;
			currentSocketStream = currentSocket.getInputStream();

		} catch(SocketTimeoutException ex) {
			throw new TimeoutException("Read timed out during readding last bytes from old socket");
		} catch (IOException e) {
			socketInvalid = true;
			throw new SwitchableException(e);
		} 
	}

	private void createNewBufferStreamAndReadLastBytes(
			long bytesMustReceiveIndex, int waitForLastBytesTimeoutMillis)
			throws IOException, SwitchableException {

		// Get the number of bytes we have to buffer.
		long tMustSocketReadLong = SwitchableStreamHelpers.getLongDiff(
				receivedBytesCount, bytesMustReceiveIndex);
		if (tMustSocketReadLong > (long) Integer.MAX_VALUE) {
			throw new SwitchableException(
					"The difference between the sent bytes and received bytes exceeds "
							+ "Integer.MAX_VALUE, perhaps more bytes were received than specified!");
		}
		int tMustSocketRead = (int) tMustSocketReadLong;
		
		// if no bytes must be received, we have nothing to do
		if (tMustSocketRead == 0) {
			return;
		}

		readAndBufferLastBytes(waitForLastBytesTimeoutMillis, tMustSocketRead);
	}

	private void readAndBufferLastBytes(int timeoutMillis, int mustSocketRead)
			throws IOException {
		int oldReadTimeout = currentSocket.getSoTimeout();
		// set receive timeout
		currentSocket.setSoTimeout(timeoutMillis);

		ByteArrayOutputStream tbuff = new ByteArrayOutputStream();

		// read bytes from old buffer
		if (byteBuffer != null) {
			byte[] ttbuff = new byte[byteBuffer.available()];
			byteBuffer.read(ttbuff);
			tbuff.write(ttbuff);
		}

		byte[] b = new byte[mustSocketRead];

		IOException readError = null;
		int tReadBytes = 0;

		try {
			while (tReadBytes < mustSocketRead) {
				int tCurrReadBytes = 0;
				tCurrReadBytes = currentSocketStream.read(b, tReadBytes,
						mustSocketRead - tReadBytes);
				if (tCurrReadBytes == -1) {
					throw new EOFException(
							"Unexpected EOF read during reading last bytes");
				}
				tReadBytes += tCurrReadBytes;
			}
		} catch (IOException e) {
			// we have to safe the buffered bytes for reading
			readError = e;
		}

		receivedBytesCount += tReadBytes;
		tbuff.write(b, 0, tReadBytes);

		if (tbuff.size() != 0) {
			byteBuffer = new ByteArrayInputStream(tbuff.toByteArray());
		}

		if (readError != null) {
			throw readError;
		}

		// reset socket timeout
		currentSocket.setSoTimeout(oldReadTimeout);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int tWaitedMillis = 0;

		// we loop until some bytes are read, or an exception occurred,
		// or a read timeout was specified and is reached.
		do {
			
			// We need to get some times out of the read method
			// (here done with the timeout exception),
			// so that we can switch the socket, if needed.
			switchLock.lock();
			try {
				// try to read the bytes for the application
				return internalStreamRead(b, off, len);
			} catch (SocketTimeoutException stex) {
				// we have to track the waited time for the application timeout
				tWaitedMillis += periodicReadTimeout;
			} finally {
				// release the lock, so that the stream could be switched.
				switchLock.unlock();
			}

			// if a timeout was set we have to check if the
			// already waited time exceeds the timeout.
			// readTimeout == 0 : no timeout limit
			if ((readTimeout != 0) && (readTimeout <= tWaitedMillis)) {
				throw new SocketTimeoutException("Read timed out");
			}
		} while (true);

	}

	/**
	 * Checks if some bytes are buffered from an old stream and returns them
	 * first. Must only be called from {@link #read(byte[], int, int)} inside
	 * the @see #readLock
	 */
	private int internalStreamRead(byte[] b, int off, int len)
			throws IOException {
		int tRead = 0;

		if (byteBuffer != null) {
			// Thread safe call of ByteArrayInputStream.available() returns the
			// right value.
			int tCanRead = Math.min(byteBuffer.available(), len);
			tRead = byteBuffer.read(b, off, tCanRead);
			if (byteBuffer.available() == 0) {
				// the bufferStream is no more needed
				byteBuffer.close();
				byteBuffer = null;
			}
			// we return here because a socket read can throw exceptions.
			return tRead;
		}

		// if socket is invalid, we can read all bytes in the buffer, but no
		// more from the socket
		if (socketInvalid) {
			return -1;
		}
		tRead = currentSocketStream.read(b, off, len);
		if (tRead > 0) {
			receivedBytesCount += tRead;
		}
		return tRead;
	}

	public void setReadTimeout(int timeoutMillis) throws SocketException {
		if (timeoutMillis < 0) {
			throw new IllegalArgumentException("timeoutMillis must be positive or 0");
		}
		switchLock.lock();
		try {
			readTimeout = timeoutMillis;
			if (timeoutMillis < periodicReadTimeout) {
				internalSetPeriodicReadTimeout(timeoutMillis);
			}
		} finally {
			switchLock.unlock();
		}
	}

	public void setPeriodicReadTimeout(int timeoutMillis) throws SocketException {
		if (timeoutMillis < 0) {
			throw new IllegalArgumentException("timeoutMillis must be positive or 0");
		}
		switchLock.lock();
		try {
		internalSetPeriodicReadTimeout(timeoutMillis);
		} finally {
			switchLock.unlock();
		}
	}
	
	private void internalSetPeriodicReadTimeout(int timeoutMillis) throws SocketException {
		periodicReadTimeout = timeoutMillis;
		currentSocket.setSoTimeout(timeoutMillis);
	}

	@Override
	public long getReceivedBytesCount() {
		return receivedBytesCount;
	}

}
