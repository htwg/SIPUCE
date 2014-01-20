package de.fhkn.in.uce.socketswitch.simpleserver;

import java.net.Socket;

class ConnectionHandlerFactory {
	
	public static ConnectionHandlerFactory Instance = new ConnectionHandlerFactory();
	
	private ConnectionHandlerFactory() {

	}

	private boolean _makeSwitchable = false;

	public void setMakeSwitchable(boolean makeSwitchable) {
		_makeSwitchable = makeSwitchable;
	}

	public Runnable createConnectionHandler(Socket socket) {
		if (_makeSwitchable) {
			return new SwitchableSocketConnectionHandler(socket);
		}
		return new SocketConnectionHandler(socket);
	}
}
