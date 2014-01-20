package de.fhkn.in.uce.api.socket;

import java.net.InetSocketAddress;

import de.fhkn.in.uce.relaying.core.IRelayingClient;

final class RelayAllocation {

		private final IRelayingClient relayingClient;
		private final InetSocketAddress endpointAddr;

		public RelayAllocation(IRelayingClient tRelCl, InetSocketAddress endpointAddr) {
			this.relayingClient = tRelCl;
			this.endpointAddr = endpointAddr;
		}

		public IRelayingClient getRelayingClient() {
			return relayingClient;
		}

		public InetSocketAddress getEndpointAddr() {
			return endpointAddr;
		}
	}