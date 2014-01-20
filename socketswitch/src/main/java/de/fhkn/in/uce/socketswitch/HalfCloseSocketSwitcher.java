package de.fhkn.in.uce.socketswitch;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeoutException;

/**
 * Strategy which switches the input and output streams of the switchable
 * socket. At first new write calls to the output stream are blocked and the
 * input stream is prepared for the expected closing of the stream. Then the
 * switch message exchange on the new socket is started. After the switch
 * message validation shutdownOutput() is called on the old socket and writing
 * continues on the new OutputStream. Furthermore the input stream reads and
 * buffers the last bytes until the last byte is received (EOF) and switches to
 * the new input stream. The final closing of the old socket should be called
 * afterwards.
 * 
 */
final class HalfCloseSocketSwitcher {

	private static final long SOCKET_SWITCH_MESSAGE = -4669201387390518292L;
	private static final int SOCKET_SWITCH_MESSAGE_BYTESIZE = 8;

	private final HalfCloseSwitchableInputStream hcSwInputStream;
	private final HalfCloseSwitchableOutputStream hcSwOutputStream;

	public HalfCloseSocketSwitcher(
			HalfCloseSwitchableInputStream switchableInputStream,
			HalfCloseSwitchableOutputStream switchableOutputStream) {
		hcSwInputStream = switchableInputStream;
		hcSwOutputStream = switchableOutputStream;
	}

	public void switchSocket(Socket newSocket, int timeoutMillis)
			throws SwitchableException, TimeoutException, InterruptedException {

		final Socket newSock = newSocket;
		final int timeout = timeoutMillis;

		// halt new sending
		hcSwOutputStream.allocateSwitchLock();
		try {
			// halt new readings
			hcSwInputStream.allocateSwitchLock();
			try {
				internalSwitch(newSock, timeout);
			} catch (InterruptedException e) {
				throw e;
			} catch (TimeoutException e) {
				throw e;
			} catch (Exception e) {
				// Wraps the error into an SwitchableException
				throw new SwitchableException(e);
			} finally {
				hcSwInputStream.releaseSwitchLock();
			}
		} finally {
			hcSwOutputStream.releaseSwitchLock();
		}
	}

	private void internalSwitch(final Socket newSock, final int timeout)
			throws IOException, SwitchableException, InterruptedException,
			ExecutionException, TimeoutException {
		// get the old timeoutSetting
		int tOldTimeout = newSock.getSoTimeout();
		// the object stream reader reads the header first, so we have to
		// set the timeout here
		newSock.setSoTimeout(timeout);

		// send the switch message
		FutureTask<Exception> tWriteThreadExeption = new FutureTask<Exception>(
				new Callable<Exception>() {
					@Override
					public Exception call() throws Exception {
						try {
							newSock.getOutputStream()
									.write(SwitchableStreamHelpers
											.longToBytes(SOCKET_SWITCH_MESSAGE));
						} catch (Exception e) {
							return e;
						}
						return null;
					}
				});

		new Thread(tWriteThreadExeption).start();

		// Receive switch message
		byte[] tReceivedBytes = new byte[SOCKET_SWITCH_MESSAGE_BYTESIZE];
		readFinal(tReceivedBytes, newSock.getInputStream());

		Exception tWriteEx = tWriteThreadExeption.get();
		if (tWriteEx != null) {
			throw new SwitchableException(tWriteEx);
		}

		// Check received message.
		Long tReceivedMsg = SwitchableStreamHelpers.bytesToLong(tReceivedBytes);

		if (tReceivedMsg.equals(SOCKET_SWITCH_MESSAGE) == false) {
			throw new SwitchMessageCheckException(
					"Received switch socket message has a wrong version.");
		}

		// reset the socket timeout option
		newSock.setSoTimeout(tOldTimeout);

		// Everything OK, switch to new socket, we have to use another
		// thread because getting the write lock can block (write operation
		// blocks)
		// These operations are undoable
		FutureTask<Exception> tSwitchInputThreadExeption = new FutureTask<Exception>(
				new Callable<Exception>() {
					@Override
					public Exception call() throws Exception {
						try {
							// cannot manipulate switch lock (other thread).
							hcSwInputStream.switchStream(newSock, timeout);
						} catch (Exception e) {
							return e;
						}
						return null;
					}
				});

		new Thread(tSwitchInputThreadExeption).start();

		hcSwOutputStream.switchStream(newSock, timeout);

		Exception tSwitchEx = tSwitchInputThreadExeption.get();
		if (tSwitchEx != null) {
			throw new SwitchableException(tSwitchEx);
		}
	}

	private void readFinal(byte[] b, InputStream in) throws IOException {
		int tReadBytes = 0;
		while (tReadBytes < b.length) {
			int tCurrRead = in.read(b, tReadBytes, b.length - tReadBytes);
			if (tCurrRead <= 0) {
				throw new EOFException("Unexpected EOF during reading");
			}
			tReadBytes += tCurrRead;
		}
	}

}
