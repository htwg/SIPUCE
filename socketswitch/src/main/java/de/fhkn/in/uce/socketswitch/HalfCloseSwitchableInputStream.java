package de.fhkn.in.uce.socketswitch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.fhkn.in.uce.socketswitch.SwitchableException;
import de.fhkn.in.uce.socketswitch.SwitchableInputStream;

final class HalfCloseSwitchableInputStream extends SwitchableInputStream {

	private static final int SOCKT_READ_BUFFER_SIZE = 10000;

	private final Lock readLock;
	private final Lock switchLock;
	private volatile InputStream currentSocketStream;
	private volatile Socket currentSocket;
	private volatile boolean switchIsPrepared;
	private volatile boolean switchWasPrepared;
	private volatile boolean socketInvalid;

	private volatile ByteArrayInputStream byteBuffer;

	public HalfCloseSwitchableInputStream(Socket socket) throws IOException {
		readLock = new ReentrantLock(true);
		switchLock = new ReentrantLock(true);
		this.currentSocket = socket;
		this.currentSocketStream = this.currentSocket.getInputStream();
		switchIsPrepared = false;
		switchWasPrepared = false;
		socketInvalid = false;
	}

	public void allocateSwitchLock() throws SwitchableException {
		if (switchIsPrepared) {
			throw new IllegalStateException("allocateSwitchLock() called twice before a call of" + "releaseSwitchLock() or switchStream()!");
		}
		if (socketInvalid) {
			throw new SwitchableException("Previous switch failed unforgivable, socket is no more valid. Reading is allowed unitl EOF.");
		}
		switchLock.lock();
		switchIsPrepared = true;
		switchWasPrepared = true;
	}

	public void releaseSwitchLock() throws SwitchableException {
		if (switchIsPrepared == false) {
			throw new IllegalStateException("allocateSwitchLock() returned not successfully!");
		}
		switchIsPrepared = false;
		switchLock.unlock();
	}

	public void switchStream(Socket newSocket, int timeoutMillis) throws SwitchableException, TimeoutException, InterruptedException {
		if (switchIsPrepared == false) {
			throw new IllegalStateException("allocateSwitchLock() returned not successfully!");
		}

		if (socketInvalid) {
			throw new SwitchableException("Previous switch failed unforgivable, socket is no more valid. Reading is allowed unitl EOF.");
		}

		boolean hasLock = false;
		hasLock = readLock.tryLock(timeoutMillis, TimeUnit.MILLISECONDS);

		if (hasLock == false) {
			throw new TimeoutException("Timeout entering read lock");
		}

		try {
			readAndBufferLastBytes(timeoutMillis);
			currentSocket = newSocket;
			currentSocketStream = currentSocket.getInputStream();

		} catch (IOException e) {
			socketInvalid = true;
			throw new SwitchableException(e);
		} finally {
			readLock.unlock();
		}
	}

	private void readAndBufferLastBytes(int timeoutMillis) throws IOException {
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

		byte[] b = new byte[SOCKT_READ_BUFFER_SIZE];
		int tReadBytes;

		IOException readError = null;
		// buffer until EOF of old stream
		while (true) {
			try {
				tReadBytes = currentSocketStream.read(b);
			} catch (IOException e) {
				readError = e;
				break;
			}

			if (tReadBytes == -1) {
				break;
			}

			tbuff.write(b, 0, tReadBytes);
		}

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

		while (true) {

			int tCurrRead = 0;

			readLock.lock();
			try {
				// try to read the bytes for the application
				tCurrRead = internalStreamRead(b, off, len);
			} finally {
				// release the lock, so that the stream could be switched.
				readLock.unlock();
			}

			if (switchWasPrepared) {

				switchLock.lock();
				// must set with lock.
				switchWasPrepared = false;
				switchLock.unlock();

				if (tCurrRead == -1) {
					continue;
				}
			}

			return tCurrRead;

		}
	}

	/**
	 * Checks if some bytes are buffered from an old stream and returns them
	 * first. Must only be called from {@link #read(byte[], int, int)} inside
	 * the @see #readLock
	 */
	private int internalStreamRead(byte[] b, int off, int len) throws IOException {
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

		return currentSocketStream.read(b, off, len);
	}

}
