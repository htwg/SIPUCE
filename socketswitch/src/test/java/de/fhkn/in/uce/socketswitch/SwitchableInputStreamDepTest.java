package de.fhkn.in.uce.socketswitch;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;

import org.junit.Test;

public class SwitchableInputStreamDepTest {

	@Test
	public void testFunctionByteAsIntReturn() {
		assertEquals(7, byteReturn());
	}
	
	public int byteReturn() {
		byte a = 7;
		return a;
	}
	
	@Test
	public void testByteArrayInputStream() throws Exception {
		byte[] b = new byte[] { 1, 2, 3, 4, 5 };
		byte[] a = new byte[5];
		
		ByteArrayInputStream tInStr = new ByteArrayInputStream(b);
		
		assertEquals(5, tInStr.available());
		assertEquals(5, tInStr.read(a));
	}

}
