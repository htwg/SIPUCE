package de.fhkn.in.uce.socketswitch;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;


/**
 * Dependencies test for TimeoutSwitchableInputStream, which don't need
 * any setups.
 *
 */
public class TimeoutSwitchableInputStreamDepTest {

	@Test
	public void testArrayInputStreamSequence() throws IOException {
		byte[] b = new byte[] { 1, 2, 3 };
		InputStream str = new ByteArrayInputStream(b);
		byte[] c = new byte[3];
		
		assertEquals(3, str.read(c));
		assertArrayEquals(b, c);
	}

}
