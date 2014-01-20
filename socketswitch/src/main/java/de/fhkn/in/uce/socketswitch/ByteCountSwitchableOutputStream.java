package de.fhkn.in.uce.socketswitch;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.fhkn.in.uce.socketswitch.SwitchableException;
import de.fhkn.in.uce.socketswitch.SwitchableOutputStream;

final class ByteCountSwitchableOutputStream extends SwitchableOutputStream {

	private final Lock writeLock;
	private final Lock switchLock;
	private volatile OutputStream currentSocketStream;
	private volatile boolean switchIsPrepared;
	// long variable must be able to overflow
	private volatile long sentBytesCount;
	private volatile long switchStart;
	private volatile long switchDuration;

	public ByteCountSwitchableOutputStream(Socket socket) throws IOException {
		switchIsPrepared = false;
		currentSocketStream = socket.getOutputStream();
		writeLock = new ReentrantLock(true);
		switchLock = new ReentrantLock(true);
		switchStart = 0;
		switchDuration = 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fhkn.in.uce.socketswitch.SwitchableOutputStream#write(byte[],
	 * int, int)
	 * 
	 * Cannot solve the write locking with one lock, because if the application
	 * wants to switch during sending data, the write function may block and the
	 * prepareToSwitchStream() function blocks, while the other connection side
	 * waits for a message on the new connection.
	 */
	@Override
	public void internalWrite(byte[] b, int off, int len) throws IOException {

		// Must block if a switch is active.
		switchLock.lock();

		sentBytesCount += len;

		// Must wait until the output stream is valid if a switch is active.
		writeLock.lock();
		switchLock.unlock();
		try {
			currentSocketStream.write(b, off, len);
			switchStart = System.nanoTime();
		} finally {
			writeLock.unlock();
		}
	}

	public void allocateSwitchLock() {
		if (switchIsPrepared == true) {
			throw new IllegalStateException(
					"allocateSwitchLock() called twice before a call of"
							+ "releaseSwitchLock() or switchStream()!");
		}
		// Ensure that the application will not reenter the write function.
		switchLock.lock();
		switchStart = System.nanoTime();
		switchIsPrepared = true;
	}

	public void releaseSwitchLock() {
		if (switchIsPrepared == false) {
			throw new IllegalStateException(
					"allocateSwitchLock() returned not successfully!");
		}
		switchIsPrepared = false;
		switchLock.unlock();
	}

	public void switchStream(Socket newSocket, int timeoutMillis)
			throws SwitchableException, TimeoutException, InterruptedException {

		if (switchIsPrepared == false) {
			throw new IllegalStateException(
					"allocateSwitchLock() returned not successfully!");
		}

		// now must get the write lock, to switch the output stream.
		boolean hasLock = false;
		hasLock = writeLock.tryLock(timeoutMillis, TimeUnit.MILLISECONDS);

		if (hasLock == false) {
			throw new TimeoutException("Timeout entering write lock");
		}

		try {
			currentSocketStream = newSocket.getOutputStream();
		} catch (IOException e) {
			throw new SwitchableException("Error switching to new stream!", e);
		} finally {
			switchDuration = System.nanoTime() - switchStart;
			writeLock.unlock();
		}

	}

	public long getWrittenBytesCountIndex() {
		return sentBytesCount;
	}

	@Override
	protected long getSwitchDuration() {
		return switchDuration;
	}
}
