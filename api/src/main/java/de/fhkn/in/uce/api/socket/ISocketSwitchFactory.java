package de.fhkn.in.uce.api.socket;

import java.io.IOException;
import java.net.Socket;

import de.fhkn.in.uce.socketswitch.SwitchableSocket;

interface ISocketSwitchFactory {
	SwitchableSocket create(Socket socket) throws IOException;
}
