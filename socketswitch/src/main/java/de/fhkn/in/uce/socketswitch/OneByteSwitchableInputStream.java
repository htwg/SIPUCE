package de.fhkn.in.uce.socketswitch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class OneByteSwitchableInputStream extends ByteCountSwitchableInputStream {

	private static final int PERMITTED_EXCEPTION_RETURN = -2;
	private final Lock streamLock;
	private final Lock switchLock;
	private final Lock closeLock;
	private InputStream currentSocketStream;
	private volatile ByteArrayInputStream byteBuffer;
	// long variable must be able to overflow
	private volatile long receivedBytesCount;
	private volatile boolean isSwitching;
	private volatile boolean switchIsPrepared;
	private volatile boolean socketExceptionPermitted;
	private Socket currentSocket;

	/**
	 * Create an instance of OneByteSwitchableInputStream.
	 * 
	 * @param socket
	 *            is needed to get the input stream
	 * @throws IOException
	 */
	public OneByteSwitchableInputStream(Socket socket) throws IOException {
		// need socket to set the read timeout
		currentSocket = socket;
		streamLock = new ReentrantLock(true);
		switchLock = new ReentrantLock(true);
		closeLock = new ReentrantLock(true);
		receivedBytesCount = 0;
		currentSocketStream = socket.getInputStream();
		switchIsPrepared = false;
		isSwitching = false;
		socketExceptionPermitted = false;
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

	
	/**
	 * Checks if some bytes are buffered from an old stream and returns them
	 * first. Must only be called from {@link #read(byte[], int, int)} inside
	 * the @see #readLock
	 */
	private int internalStreamRead(byte[] b, int off, int len)
			throws IOException {
		int tOff = off;
		int tLen = len;
		int tRead = 0;

		if (byteBuffer != null) {
			int tCanRead = Math.min(byteBuffer.available(), tLen);
			tRead = byteBuffer.read(b, tOff, tCanRead);

			if (byteBuffer.available() == 0) {
				// the bufferStream is no more needed
				byteBuffer.close();
				byteBuffer = null;

				if (tLen == tRead) {
					// the exact number of bytes to read were in the buffer.
					return tRead;
				}
			} else {
				// There must be some bytes left in the buffer stream. So we
				// read enough for the application and can return.
				return tRead;
			}
		}
		// update offset and length for next read.
		tLen -= tRead;
		tOff += tRead;
		int tReadFromStream = 0;

		try {
			tReadFromStream = currentSocketStream.read(b, tOff, tLen);
		} catch (SocketException e) {
			if (socketExceptionPermitted) {
				return PERMITTED_EXCEPTION_RETURN;
			}
			throw e;
		}
		if (tReadFromStream == -1) {
			if (tRead == 0) {
				return -1;
			} else {
				return tRead;
			}
		}

		tRead += tReadFromStream;
		receivedBytesCount += tReadFromStream;

		return tRead;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {

		int tOff = off;
		int tLen = len;
		int tReadBytes = 0;

		while (tLen > 0) {

			if (isSwitching) {
				switchLock.lock();
				switchLock.unlock();
			}

			try {
				streamLock.lock();

				// read one byte
				int tCurrReadBytes = internalStreamRead(b, tOff, 1);
				if (tCurrReadBytes == PERMITTED_EXCEPTION_RETURN) {
					continue;
				} else if (tCurrReadBytes == -1) {
					if (tReadBytes == 0) {
						return -1;
					} else {
						return tReadBytes;
					}
				}
				tOff += tCurrReadBytes;
				tReadBytes += tCurrReadBytes;
				tLen -= tCurrReadBytes;

				// after one byte was read, there will be checked how many
				// bytes there are available
				int bytesInBuffer = 0;
				if (byteBuffer != null) {
					bytesInBuffer = byteBuffer.available();
				}
				int tBytesToRead = Math.min(currentSocketStream.available()
						+ bytesInBuffer, tLen);

				tCurrReadBytes = internalStreamRead(b, tOff, tBytesToRead);
				if (tCurrReadBytes == PERMITTED_EXCEPTION_RETURN) {
					continue;
				} else if (tCurrReadBytes == -1) {
					if (tReadBytes == 0) {
						return -1;
					} else {
						return tReadBytes;
					}
				}

				tOff += tCurrReadBytes;
				tReadBytes += tCurrReadBytes;
				tLen -= tCurrReadBytes;

			} finally {
				streamLock.unlock();
			}
		}

		return tReadBytes;
	}

	@Override
	public void allocateSwitchLock(int timeoutMillis)
			throws SwitchableException, InterruptedException, TimeoutException {

		if (switchIsPrepared) {
			throw new IllegalStateException(
					"allocateSwitchLock() called twice before a call of"
							+ "cancelStreamSwitch() or switchStream()!");
		}
		boolean hasLock = false;
		hasLock = switchLock.tryLock(timeoutMillis, TimeUnit.MILLISECONDS);

		if (hasLock == false) {
			throw new TimeoutException("Timeout entering read lock");
		}
		closeLock.lock();
		switchIsPrepared = true;
		isSwitching = true;
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
			long mustReadCountIndex) throws SwitchableException,
			TimeoutException {
		if (switchIsPrepared == false) {
			throw new IllegalStateException(
					"allocateSwitchLock() returned not successfully!");
		}
		

		if (mustReadCountIndex == receivedBytesCount) {
			closeLock.lock();
			socketExceptionPermitted = true;
			try {
				currentSocketStream.close();
			} catch (IOException e) {
				throw new SwitchableException(
						"Cannot switch stream - error while closing old stream",
						e);
			}
			streamLock.lock();
			socketExceptionPermitted = false;
		} else {
			streamLock.lock();
			if (mustReadCountIndex != receivedBytesCount) {
				// now the application read method is blocked
				int oldSocketTimeout = 0;
				try {
					oldSocketTimeout = currentSocket.getSoTimeout();
					currentSocket.setSoTimeout(timeoutMillis);
					createNewBufferStreamAndReadLastBytes(mustReadCountIndex, timeoutMillis);
				} catch (Exception e) {
					// Unlock the lock at any known error
					isSwitching = false;
					throw new SwitchableException("Cannot switch stream!", e);
				} finally {
					try {
						currentSocket.setSoTimeout(oldSocketTimeout);
					} catch (SocketException e) {
						throw new SwitchableException(
								"Cannot switch stream - error while resetting SoTimeout",
								e);
					}
				}
			}
			closeLock.lock();
		}

		try {
			currentSocketStream = newSocket.getInputStream();
			currentSocket = newSocket;
		} catch (IOException e) {
			throw new SwitchableException("Error switching to new stream!", e);
		} finally {
			isSwitching = false;
			closeLock.unlock();
			streamLock.unlock();
		}
	}
	
	/**
	 * Returns the internal receivedBytesCountIndex. Is not thread safe and
	 * should only be used for debugging purpose.
	 * 
	 * @return
	 */
	@Override
	public long getReceivedBytesCount() {
		return receivedBytesCount;
	}
	
	public void acceptClosing()
	{
		closeLock.unlock();
	}
}
