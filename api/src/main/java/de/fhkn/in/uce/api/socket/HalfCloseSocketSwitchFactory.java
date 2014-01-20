package de.fhkn.in.uce.api.socket;

import java.io.IOException;
import java.net.Socket;

import de.fhkn.in.uce.socketswitch.HalfCloseSwitchableSocket;
import de.fhkn.in.uce.socketswitch.SwitchableSocket;

final class HalfCloseSocketSwitchFactory implements ISocketSwitchFactory {

	@Override
	public SwitchableSocket create(Socket socket) throws IOException {
		return new HalfCloseSwitchableSocket(socket);
	}

}
