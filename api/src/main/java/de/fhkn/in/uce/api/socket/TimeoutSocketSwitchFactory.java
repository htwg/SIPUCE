package de.fhkn.in.uce.api.socket;

import java.io.IOException;
import java.net.Socket;

import de.fhkn.in.uce.socketswitch.SwitchableSocket;
import de.fhkn.in.uce.socketswitch.TimeoutSwitchableSocket;

final class TimeoutSocketSwitchFactory implements ISocketSwitchFactory {

	@Override
	public SwitchableSocket create(Socket socket) throws IOException {
		return new TimeoutSwitchableSocket(socket);
	}

}
