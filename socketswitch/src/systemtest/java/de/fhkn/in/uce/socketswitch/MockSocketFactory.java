package de.fhkn.in.uce.socketswitch;


import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import de.fhkn.in.uce.socketswitch.test.mocksocket.MockSimpleServer;
import de.fhkn.in.uce.socketswitch.test.mocksocket.MockSocket;

public class MockSocketFactory implements ISocketFactory {
	
	private MockSimpleServer mockSimplServer;
	private int queueSize;
	
	public MockSocketFactory(int queueSize) {
		mockSimplServer = new MockSimpleServer();
		this.queueSize = queueSize;
	}

	@Override
	public Socket createConnection() throws UnknownHostException, IOException {
		MockSocket mockSock = new MockSocket(queueSize);
		mockSimplServer.startConnectionHandler(mockSock.getOppsositeSocket());
		
		return mockSock;
	}

}
