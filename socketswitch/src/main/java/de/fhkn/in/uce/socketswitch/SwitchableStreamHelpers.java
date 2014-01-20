package de.fhkn.in.uce.socketswitch;

import java.nio.ByteBuffer;

final class SwitchableStreamHelpers {
	
	private static final int LONG_BYTESZIE = 8;
	
	private SwitchableStreamHelpers() {
	}
	
	/**
	 * Calculates the difference between @param higherValue and @param lowerValue
	 * also if the @param higherValue overflowed and higherValue < lowerValue == true.
	 * The two values must not have a difference greater Long.MAX_VALUE.
	 * @param higherValue is the value, that overflows first.
	 * @param lowerValue
	 * @return the difference.
	 */
	public static long getLongDiff(long lowerValue, long higherValue) {
		long tRet;
		if (higherValue >= lowerValue) {
			tRet = higherValue - lowerValue;
		} else {
			tRet = (higherValue - Long.MIN_VALUE) + (Long.MAX_VALUE - lowerValue) + 1;
		}
		if (tRet < 0) {
			throw new ArithmeticException("The difference between the two long values exceeds Long.MAX_VALUE!");
		}
		return tRet;
	}

	public static byte[] longToBytes(long x) {
	    ByteBuffer buffer = ByteBuffer.allocate(LONG_BYTESZIE);
	    buffer.putLong(x);
	    return buffer.array();
	}

	public static long bytesToLong(byte[] bytes) {
	    ByteBuffer buffer = ByteBuffer.allocate(LONG_BYTESZIE);
	    buffer.put(bytes);
	    buffer.flip();
	    return buffer.getLong();
	}

}
