package de.fhkn.in.uce.socketswitch;

import java.io.IOException;
import java.io.InputStream;

/**
 * A SwitchableInputStream object provides InputStream functionality
 * whereas the underlying SocketInputStreams can be exchanged.
 */
abstract class SwitchableInputStream extends InputStream {

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.InputStream#read()
	 * 
	 * Reads a single byte from the socket. Is only a wrapper for the {@link
	 * #read(byte[], int, int)} function.
	 */
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