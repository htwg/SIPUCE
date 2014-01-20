package de.fhkn.in.uce.api.socket;

import java.net.Socket;
import java.util.concurrent.TimeoutException;



interface IServerConnectionHandlerStrategy {
	void serverListen() throws UCEException;

	Socket serverAccept(int timeoutMillis) throws UCEException, TimeoutException;
	
	void serverClose();
}
