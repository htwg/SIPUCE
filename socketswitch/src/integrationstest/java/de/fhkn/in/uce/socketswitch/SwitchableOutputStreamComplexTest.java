package de.fhkn.in.uce.socketswitch;


import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import de.fhkn.in.uce.socketswitch.simpleserver.SimpleServer;

public abstract class SwitchableOutputStreamComplexTest {
	
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
	
	SwitchableOutputStream switchableOutputStream;
	
	Socket connection1;
	Socket connection2;
	
	OutputStream connection1OutputStream;
	OutputStream connection2OutputStream;
	
	ObjectOutputStream connection1ObjectOutputStream;
	ObjectOutputStream connection2ObjectOutputStream;
	
	abstract SwitchableOutputStream createSwitchableOutputStream(Socket socket) throws IOException;
	
	@Before
	public void setUp() throws Exception {
		connection1 = new Socket("localhost", 3141);
		connection2 = new Socket("localhost", 3141);
		
		connection1OutputStream = connection1.getOutputStream();
		connection2OutputStream = connection2.getOutputStream();
		
		connection1ObjectOutputStream = new ObjectOutputStream(connection1OutputStream);
		connection2ObjectOutputStream = new ObjectOutputStream(connection2OutputStream);
		
		switchableOutputStream = createSwitchableOutputStream(connection1);
	}

	@After
	public void tearDown() throws Exception {
		connection1.close();
		connection1 = null;
		
		connection2.close();
		connection2 = null;
	}

}
