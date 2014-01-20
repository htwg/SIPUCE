package de.fhkn.in.uce.api.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.holepunching.core.ConnectionAuthenticator;
import de.fhkn.in.uce.relaying.core.IRelayingClient;
import de.fhkn.in.uce.socketswitch.SwitchableSocket;

final class ServerHolePuncherSwitcherThread extends HolePuncherSwitcherThread {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerHolePuncherSwitcherThread.class);

	private final IRelayingClient relayConnection;
	// access must be synchronized.
	private volatile Boolean relayIsDiscarded;

	public ServerHolePuncherSwitcherThread(InetSocketAddress privateEndpoint, InetSocketAddress publicEndpoint, InetSocketAddress localAddress,
			SocketAddress localSocketAddress, ConnectionAuthenticator auth, SwitchableSocket relaySwitchableSocket, IRelayingClient iRelayingClient,
			final int switchTimeoutMillis) {

		super(privateEndpoint, publicEndpoint, localAddress, localSocketAddress, auth, relaySwitchableSocket, switchTimeoutMillis);

		this.relayConnection = iRelayingClient;
		this.relayIsDiscarded = false;
	}

	protected void socketSwitched() {
		// if the socket is switched we can try to discard the allocation on the
		discardRelayAllocation();
	}

	@Override
	public void terminate() throws InterruptedException {
		super.terminate();
		discardRelayAllocation();
	}

	private synchronized void discardRelayAllocation() {
		if (relayIsDiscarded == false) {
			try {
				relayConnection.discardAllocation();
			} catch (IOException e) {
				LOGGER.info("Error discarding relay allocation", e);
			}
			this.relayIsDiscarded = true;
		}
	}

}
