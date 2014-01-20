package de.fhkn.in.uce.socketswitch.test.mocksocket;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MockSocketTest {

	MockSocket asd;
	Socket asdOpposite;
	
	@Before
	public void setUp() {
		asd = new MockSocket(10);
		asdOpposite = asd.getOppsositeSocket();
	}
	
	@After
	public void tearDown() throws IOException {
		asd.close();
		asdOpposite.close();
	}
	
	
	@Test
	public void testExhangeByte() throws IOException {
		asd.getOutputStream().write(10);
		
		assertEquals(10, asdOpposite.getInputStream().read());
	}
	
	@Test
	public void testExhangeByteArrays() throws IOException {
		asd.getOutputStream().write(new byte[] { 1, 2, 3, 4, 5, 6 });
		asdOpposite.getOutputStream().write(new byte[] { 7, 8, 9, 10 });
		
		byte[] compArr1 = new byte[6];
		asdOpposite.getInputStream().read(compArr1);
		
		assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, compArr1);
		
		byte[] compArr2 = new byte[4];
		asd.getInputStream().read(compArr2);
		
		assertArrayEquals(new byte[] { 7, 8, 9, 10 }, compArr2);
	}
	
	@Test
	public void testShutdownOutput() throws IOException {
		asd.getOutputStream().write(10);
		assertEquals(10, asdOpposite.getInputStream().read());
		
		asd.shutdownOutput();
		assertEquals(-1, asdOpposite.getInputStream().read());
		
	}
	
	@Test
	public void testExchangeObjects() throws IOException, ClassNotFoundException {
		ObjectOutputStream out = new ObjectOutputStream(asd.getOutputStream());		
		ObjectInputStream in = new ObjectInputStream(asdOpposite.getInputStream());
		
		out.writeObject(new TestMessage(0, 0));
		assertEquals(new TestMessage(0, 0), in.readObject());
		
		out.writeObject(new TestMessage(1, 2));
		assertEquals(new TestMessage(1, 2), in.readObject());
		
		out.writeObject(new Long(1234));
		assertEquals(new Long(1234), in.readObject());
		
		
		
	}

}
