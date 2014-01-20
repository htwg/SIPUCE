package de.fhkn.in.uce.api.socket;

import java.net.InetSocketAddress;

import de.fhkn.in.uce.api.socket.IRelayingClientFactory;
import de.fhkn.in.uce.relaying.core.IRelayingClient;

public class MockRelayingClientFactory implements IRelayingClientFactory {

	@Override
	public IRelayingClient create(InetSocketAddress relayServerAddress) {
		return new RelayingClientMock();
	}

}
