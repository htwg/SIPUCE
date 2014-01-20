package de.fhkn.in.uce.api.socket;

import java.net.InetSocketAddress;
import java.net.Socket;

import de.fhkn.in.uce.stun.ucestun.exception.UCEStunException;

import static org.mockito.Mockito.*;

public class StunEndpointMock implements IUCEStunEndpoint {

	@Override
	public InetSocketAddress getLocalEndpoint() {
		return new InetSocketAddress("localhost", 1008);
	}

	@Override
	public InetSocketAddress getPublicEnpoint() {
		return new InetSocketAddress("localhost", 1008);
	}

	@Override
	public Socket getStunEndpointSocket() {
		Socket tMockSock = mock(Socket.class);
		when(tMockSock.getLocalSocketAddress()).thenReturn(new InetSocketAddress("localhost", 1008));
		return tMockSock;
	}

	@Override
	public void evaluatePublicEndpoint() throws UCEStunException {
		
	}

}
