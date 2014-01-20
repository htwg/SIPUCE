package de.fhkn.in.uce.socketswitch;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

interface ISocketFactory {
	Socket createConnection() throws UnknownHostException, IOException;
}
