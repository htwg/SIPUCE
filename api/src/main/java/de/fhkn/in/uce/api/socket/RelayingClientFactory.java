package de.fhkn.in.uce.api.socket;

import java.net.InetSocketAddress;

import de.fhkn.in.uce.relaying.core.IRelayingClient;
import de.fhkn.in.uce.relaying.core.RelayingClient;

class RelayingClientFactory implements IRelayingClientFactory {
	
	@Override
	public IRelayingClient create(InetSocketAddress relayServerAddress) {
		return new RelayingClient(relayServerAddress);
	}

}
