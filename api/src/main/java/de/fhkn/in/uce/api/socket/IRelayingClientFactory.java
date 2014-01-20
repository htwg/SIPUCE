package de.fhkn.in.uce.api.socket;

import java.net.InetSocketAddress;

import de.fhkn.in.uce.relaying.core.IRelayingClient;

interface IRelayingClientFactory {

	IRelayingClient create(InetSocketAddress relayServerAddress);

}