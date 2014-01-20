package de.fhkn.in.uce.socketswitch.test.mocksocket;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

class MockInputStream extends InputStream {

	private final ArrayBlockingQueue<Object> queue;
	private volatile int readTimeout;
	private volatile InputStream currentInputStream;
	private boolean inShut = false;

	public MockInputStream(final ArrayBlockingQueue<Object> queue) {
		this.queue = queue;
		readTimeout = 0;
	}

	public void setReadTimeout(int timeout) {
		readTimeout = timeout;
	}

	public int getReadTimeout() {
		return readTimeout;
	}

	public void shutdownInput() {
		inShut = true;
	}

	public boolean isInputShutdown() {
		return inShut;
	}

	@Override
	public synchronized int read(byte[] b, int off, int len) throws IOException {
		if (inShut) {
			return -1;
		}

		Object tObj;

		if (currentInputStream == null) {
			try {
				if (readTimeout == 0) {
					tObj = queue.take();
				} else {
					tObj = queue.poll(readTimeout, TimeUnit.MILLISECONDS);
				}
			} catch (InterruptedException e) {
				throw new RuntimeException("Mock error", e);
			}
			
			if (tObj == null) {
				throw new SocketTimeoutException("Mock socket read timeout");
			}

			if (tObj instanceof MockSocketMessage) {
				switch ((MockSocketMessage) tObj) {
				case ShutdownOutput:
					inShut = true;
					return -1;
				}
			}

			if ((tObj instanceof InputStream) == false) {
				throw new RuntimeException("Mock error: strange object");
			}

			currentInputStream = (InputStream) tObj;
		}

		int readAvail = currentInputStream.available();
		if (readAvail <= len) {
			currentInputStream.read(b, off, readAvail);
			currentInputStream = null;
			return readAvail;
		} else {
			currentInputStream.read(b, off, len);
			return len;
		}
	}

	@Override
	public int read() throws IOException {
		byte temp[] = new byte[1];
		int n = read(temp, 0, 1);
		if (n <= 0) {
			return -1;
		}
		return temp[0];
	}
}