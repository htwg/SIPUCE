package de.fhkn.in.uce.socketswitch;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.net.Socket;

import org.junit.Test;

public class ByteCountSwitchableOutputStreamComplexTest extends
		SwitchableOutputStreamComplexTest {
	
	@Override
	SwitchableOutputStream createSwitchableOutputStream(Socket socket) throws IOException {
		return new ByteCountSwitchableOutputStream(socket);
	}

	ByteCountSwitchableOutputStream getSwitchableOutputStream() {
		return (ByteCountSwitchableOutputStream)switchableOutputStream;
	}
	
	@Test
	public void testWrite() throws IOException {
		byte[] b = new byte[] { 1, 2, 3, 4, 5 };
		
		getSwitchableOutputStream().write(b);
		
		assertEquals(5L, getSwitchableOutputStream().getWrittenBytesCountIndex());
	}
	
	@Test
	public void testWriteByte() throws IOException {
		getSwitchableOutputStream().write(1);
		
		assertEquals(1L, getSwitchableOutputStream().getWrittenBytesCountIndex());
	}
	
	@Test
	public void testWriteSwitchWrite() throws Exception {
		byte[] b = new byte[] { 1, 2, 3, 4, 5 };
		
		getSwitchableOutputStream().write(b);
		assertEquals(5L, getSwitchableOutputStream().getWrittenBytesCountIndex());
		
		getSwitchableOutputStream().allocateSwitchLock();
		
		assertEquals(5L, getSwitchableOutputStream().getWrittenBytesCountIndex());
		getSwitchableOutputStream().switchStream(connection2, 100);
		
		getSwitchableOutputStream().write(b);
		assertEquals(10L, getSwitchableOutputStream().getWrittenBytesCountIndex());
		
	}
	
	@Test
	public void testSwitchCancelWrite() throws Exception {
		byte[] b = new byte[] { 1, 2, 3, 4, 5 };
		
		getSwitchableOutputStream().allocateSwitchLock();
		assertEquals(0L, getSwitchableOutputStream().getWrittenBytesCountIndex());
		getSwitchableOutputStream().releaseSwitchLock();
		
		getSwitchableOutputStream().write(b);
		assertEquals(5L, getSwitchableOutputStream().getWrittenBytesCountIndex());
		 
	}
	
	@Test(expected = IllegalStateException.class)
	public void testSwitchSwitchException() throws Exception {	
		getSwitchableOutputStream().allocateSwitchLock();
		assertEquals(0L, getSwitchableOutputStream().getWrittenBytesCountIndex());	
		getSwitchableOutputStream().allocateSwitchLock();
	}
	
	@Test(expected = IllegalStateException.class)
	public void testCancelSwitchException() throws Exception {		
		getSwitchableOutputStream().releaseSwitchLock();		
	}
	
	@Test(expected = IllegalStateException.class)
	public void testSwitchException() throws Exception {		
		getSwitchableOutputStream().switchStream(connection2, 100);		
	}


	

}
