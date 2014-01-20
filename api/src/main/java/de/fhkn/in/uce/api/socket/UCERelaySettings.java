package de.fhkn.in.uce.api.socket;

import java.net.InetSocketAddress;

public class UCERelaySettings {

	private final InetSocketAddress relayAddress;
	
	public UCERelaySettings(final InetSocketAddress relayAddress) {
		this.relayAddress = relayAddress;
	}

	public InetSocketAddress getRelayAddress() {
		return relayAddress;
	}
}
