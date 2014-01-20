package de.fhkn.in.uce.socketswitch.test.mocksocket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;


class MockOutputStream extends OutputStream {

	private final ArrayBlockingQueue<Object> queue;
	private boolean shutOut = false;

	public MockOutputStream(final ArrayBlockingQueue<Object> queue) {
		this.queue = queue;
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (shutOut) {
			throw new RuntimeException("Mock error: not implemented");
		}
		if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
                   ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
		
		byte[] arr = new byte[len];
		System.arraycopy(b, off, arr, 0, len);
		try {
			queue.put(new ByteArrayInputStream(arr));
		} catch (InterruptedException e) {
			throw new RuntimeException("Mock error", e);
		}
	}

	@Override
	public void write(int b) throws IOException {
		byte[] temp = new byte[1];
		temp[0] = (byte) b;
		write(temp, 0, 1);
	}
	
	public void shutdownOutput() {
		shutOut = true;
		try {
			queue.put(MockSocketMessage.ShutdownOutput);
		} catch (InterruptedException e) {
			throw new RuntimeException("Mock error", e);
		}
	}

}