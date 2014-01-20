package de.fhkn.in.uce.socketswitch;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

public class SwitchableStreamHelpersTest {

	@Test
	public void testGetLongDiff() {
		
		assertEquals(Long.MAX_VALUE, SwitchableStreamHelpers.getLongDiff(0L, Long.MAX_VALUE));
		assertEquals(Long.MAX_VALUE - 1000L, SwitchableStreamHelpers.getLongDiff(1000L, Long.MAX_VALUE));
		assertEquals(1, SwitchableStreamHelpers.getLongDiff(Long.MAX_VALUE, Long.MAX_VALUE + 1));
		assertEquals(1000, SwitchableStreamHelpers.getLongDiff(Long.MAX_VALUE, Long.MAX_VALUE + 1000));
		assertEquals(1000, SwitchableStreamHelpers.getLongDiff(Long.MAX_VALUE + 1, Long.MAX_VALUE + 1001));
	}
	
	@Test(expected = ArithmeticException.class)
	public void testGetLongDiffException() {
		SwitchableStreamHelpers.getLongDiff(0L, -1L);
	}
	
	@Test
	public void generateRandomLong() {
		System.out.println("Random Long: " + (new Random()).nextLong());
	}
	
	@Test
	public void longConversion() {
		long tl = -4669201387390518292L;
		byte[] tb = SwitchableStreamHelpers.longToBytes(tl);
		long tz = SwitchableStreamHelpers.bytesToLong(tb);
		assertEquals(tl, tz);
	}

}
