package de.fhkn.in.uce.socketswitch;

import org.junit.Test;

public abstract class ByteCountSwitchableInputStreamTest extends
		SwitchableInputStreamTest {
	
	
	ByteCountSwitchableInputStream getByteCountSwitchableInputStream() {
		return (ByteCountSwitchableInputStream)switchableInputStream;
	}

		
	@Test(expected = SwitchableException.class)
	public final void testWrongReceivedBytesExceptionInteger() throws Exception {
		// more than positive integer value, less than positive long value
		getByteCountSwitchableInputStream().allocateSwitchLock(3000);
		getByteCountSwitchableInputStream().switchStream(socketMock2, 3000, ((long) Integer.MAX_VALUE) + 1);
	}
	
	@Test(expected = ArithmeticException.class)
	public final void testWrongReceivedBytesExceptionLong() throws Exception {
		// more than positive long value
		getByteCountSwitchableInputStream().allocateSwitchLock(3000);
		getByteCountSwitchableInputStream().switchStream(socketMock2, 3000, -1L);
	}
	
	@Test(expected = IllegalStateException.class)
	public final void testCancelSwitchStreamException() throws Exception {

		getByteCountSwitchableInputStream().releaseSwitchLock();
	}

	@Test(expected = IllegalStateException.class)
	public final void testSwitchStreamException() throws Exception {
		getByteCountSwitchableInputStream().switchStream(socketMock2, 3000, 0);
	}

}
