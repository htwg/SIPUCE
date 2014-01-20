package de.fhkn.in.uce.socketswitch;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fhkn.in.uce.socketswitch.SwitchableInputStream;
import de.fhkn.in.uce.socketswitch.simpleserver.ConnectionPurposeMessage;
import de.fhkn.in.uce.socketswitch.simpleserver.ServerFunction;
import de.fhkn.in.uce.socketswitch.simpleserver.SimpleServer;

public abstract class SwitchableInputStreamComplexTest {

	static SimpleServer simpleServer;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		simpleServer = new SimpleServer();
		simpleServer.startServer(10, 3141, 3142);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		simpleServer.shutdownServer();
		simpleServer = null;
	}
	
	SwitchableInputStream switchableInputStream;

	Socket connection1;
	Socket connection2;
	Socket connection3;

	OutputStream connection1OutputStream;
	OutputStream connection2OutputStream;
	OutputStream connection3OutputStream;

	ObjectOutputStream connection1ObjectOutputStream;
	ObjectOutputStream connection2ObjectOutputStream;
	ObjectOutputStream connection3ObjectOutputStream;

	abstract SwitchableInputStream createSwitchableInputStream(Socket socket) throws IOException;
	
	private SwitchableInputStream getSwitchableInputStream() {
		return switchableInputStream;
	}

	@Before
	public void setUp() throws Exception {
		connection1 = new Socket("localhost", 3141);
		connection2 = new Socket("localhost", 3141);
		connection3 = new Socket("localhost", 3141);

		connection1OutputStream = connection1.getOutputStream();
		connection2OutputStream = connection2.getOutputStream();
		connection3OutputStream = connection3.getOutputStream();

		connection1ObjectOutputStream = new ObjectOutputStream(
				connection1OutputStream);
		connection2ObjectOutputStream = new ObjectOutputStream(
				connection2OutputStream);
		connection3ObjectOutputStream = new ObjectOutputStream(
				connection3OutputStream);
		
		switchableInputStream = createSwitchableInputStream(connection1);
	}

	@After
	public void tearDown() throws Exception {
		connection1.close();
		connection1 = null;

		connection2.close();
		connection2 = null;

		connection3.close();
		connection3 = null;
	}

	@Test
	public final void testRead() throws IOException {
		byte[] b = new byte[5];
		byte[] a = new byte[] { 1, 2, 3, 4, 5 };

		selectServerFunction(ServerFunction.SendFiveBytesEverySecond, b.length);

		readFinish(b, getSwitchableInputStream());
		assertArrayEquals(a, b);
	}

	@Test
	public final void testReadByte() throws IOException {
		selectServerFunction(ServerFunction.SendFiveBytesEverySecond, 1);

		assertEquals(1, getSwitchableInputStream().read());
	}
	
	@Test
	public final void testReadToEOF() throws Exception {
		byte[] b = new byte[15];
		byte[] a = new byte[] { 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 0, 0, 0, 0, 0 };

		selectServerFunction(ServerFunction.SendXBytes, 10);
		shutDownServerOutput(1);
		getSwitchableInputStream().read(b, 0, 15);
		assertArrayEquals(a, b);
		int test = getSwitchableInputStream().read(b, 0, 15);
		assertEquals(-1, test);
	}

	@Test(expected = SocketException.class)
	public void testThrowSocketException() throws Exception {
		byte[] b = new byte[5];

		selectServerFunction(ServerFunction.SendFiveBytesEverySecond, b.length);
		connection1.close();
		readFinish(b, getSwitchableInputStream());

	}

	protected void readFinish(byte[] b, InputStream stream) throws IOException {
		int tRead = 0;
		int tOff = 0;
		int tLen = b.length;

		do {
			int tCurrRead = stream.read(b, tOff, tLen);
			tRead += tCurrRead;
			tOff += tCurrRead;
			tLen -= tCurrRead;
		} while (tRead < b.length);
	}

	protected void selectServerFunction(ServerFunction functionId, int max)
			throws IOException {
		ConnectionPurposeMessage tCp = new ConnectionPurposeMessage(functionId,
				UUID.randomUUID(), max, 0);
		connection1ObjectOutputStream.writeObject(tCp);
		connection2ObjectOutputStream.writeObject(tCp);
		connection3ObjectOutputStream.writeObject(tCp);
	}

	protected void shutDownServerOutput(int socket) throws IOException {
		ConnectionPurposeMessage tCp = new ConnectionPurposeMessage(
				ServerFunction.ShutdownOutput, UUID.randomUUID(), 0, 0);

		switch (socket) {
		case 1:
			connection1ObjectOutputStream.writeObject(tCp);
			break;
		case 2:
			connection2ObjectOutputStream.writeObject(tCp);
			break;
		case 3:
			connection3ObjectOutputStream.writeObject(tCp);
			break;
		default:
			break;
		}
	}

}
