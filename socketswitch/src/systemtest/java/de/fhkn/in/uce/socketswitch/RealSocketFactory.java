package de.fhkn.in.uce.socketswitch;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class RealSocketFactory implements ISocketFactory {
	
	private final String server;
	private final int port;
	
	public RealSocketFactory(final String server, final int port) {
		this.server = server;
		this.port = port;
	}

	@Override
	public Socket createConnection() throws UnknownHostException, IOException {
		return new Socket(server, port);
	}

}
