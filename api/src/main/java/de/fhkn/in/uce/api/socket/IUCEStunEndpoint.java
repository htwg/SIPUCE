package de.fhkn.in.uce.api.socket;

import java.net.InetSocketAddress;
import java.net.Socket;

import de.fhkn.in.uce.stun.ucestun.exception.UCEStunException;

public interface IUCEStunEndpoint {
	
	InetSocketAddress getLocalEndpoint();
	
	InetSocketAddress getPublicEnpoint();
	
	Socket getStunEndpointSocket();
	
	void evaluatePublicEndpoint() throws UCEStunException;
	
}
