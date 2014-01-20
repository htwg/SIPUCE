package de.fhkn.in.uce.socketswitch;

import java.io.IOException;
import java.io.OutputStream;

abstract class SwitchableOutputStream extends OutputStream {

	public SwitchableOutputStream() {
		super();
	}

	protected abstract void internalWrite(byte[] b, int off, int len) throws IOException;

	protected abstract long getSwitchDuration();

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	public final void write(byte[] b, int off, int len) throws IOException {
		if (b == null) {
			throw new IllegalArgumentException("The buffer must not be null");
		} else if (off < 0) {
			throw new IndexOutOfBoundsException();
		} else if (off > b.length) {
			throw new IndexOutOfBoundsException();
		} else if (len < 0) {
			throw new IndexOutOfBoundsException();
		} else if ((off + len) > b.length) {
			throw new IndexOutOfBoundsException();
		} else if ((off + len) < 0) {
			throw new IndexOutOfBoundsException();
		} else if (len == 0) {
			return;
		}
		internalWrite(b, off, len);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	@Override
	public final void write(int b) throws IOException {
		byte[] temp = new byte[1];
		temp[0] = (byte) b;
		write(temp, 0, 1);
	}

}