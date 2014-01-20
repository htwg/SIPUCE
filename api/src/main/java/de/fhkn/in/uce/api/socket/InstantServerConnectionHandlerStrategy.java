package de.fhkn.in.uce.api.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.holepunching.core.ConnectionAuthenticator;
import de.fhkn.in.uce.holepunching.core.authentication.TargetConnectionAuthenticator;
import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;
import de.fhkn.in.uce.sip.ucesip.message.IUCESipMessage;
import de.fhkn.in.uce.sip.ucesip.message.UCESipMessage;
import de.fhkn.in.uce.sip.ucesip.message.UCESipMessageDataConnectionId;
import de.fhkn.in.uce.sip.ucesip.message.UCESipMessageDataRelay;
import de.fhkn.in.uce.sip.ucesip.message.UCESipMessageDataStun;
import de.fhkn.in.uce.sip.ucesip.server.IUCESipUserAgentServer;
import de.fhkn.in.uce.sip.ucesip.server.call.UCESipServerCall;
import de.fhkn.in.uce.socketswitch.SwitchableSocket;
import de.fhkn.in.uce.stun.ucestun.exception.UCEStunException;

final class InstantServerConnectionHandlerStrategy implements IServerConnectionHandlerStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(InstantServerConnectionHandlerStrategy.class);

	private final IUCESipUserAgentServer sipUAS;
	private final UCEServerSettings serverSettings;
	private final ISocketSwitchFactory socketSwitchFactory;
	private final IRelayingClientFactory relayingClientFactory;
	private final IStunEndpointFactory stunEndpointFactory;

	private volatile RelayAllocationGeneratorThread relayAllocationGenerator;
	private volatile StunEndpointGeneratorThread stunEndpointGenerator;
	private volatile boolean isListening;

	public InstantServerConnectionHandlerStrategy(final IUCESipUserAgentServer sipUAS, final UCEServerSettings serverSettings,
			final ISocketSwitchFactory socketSwitchFactory, IRelayingClientFactory relayingClientFactory, IStunEndpointFactory stunEndpointFactory) {
		this.sipUAS = sipUAS;
		this.serverSettings = serverSettings;
		this.socketSwitchFactory = socketSwitchFactory;
		this.isListening = false;
		this.relayingClientFactory = relayingClientFactory;
		this.stunEndpointFactory = stunEndpointFactory;
	}

	@Override
	public synchronized void serverListen() throws UCEException {
		if (isListening) {
			throw new IllegalStateException("Already listening");
		}
		relayAllocationGenerator = new RelayAllocationGeneratorThread(serverSettings.getRelaySettings().getRelayAddress(),
				serverSettings.getRelayAllocationRefreshTimeMillis(), relayingClientFactory);
		stunEndpointGenerator = new StunEndpointGeneratorThread(serverSettings.getStunSettings(), serverSettings.getStunEndpointRefreshTimeMillis(),
				stunEndpointFactory);
		relayAllocationGenerator.start();
		stunEndpointGenerator.start();

		isListening = true;
	}

	@Override
	public synchronized Socket serverAccept(int timeoutMillis) throws UCEException, TimeoutException {
		if (isListening == false) {
			throw new IllegalStateException("Not listening");
		}

		SwitchableSocket switchableSocket;
		ServerHolePuncherSwitcherThread serverHolepuncherSwitcherThread;
		InetSocketAddress privateEndpoint;
		InetSocketAddress publicEndpoint;
		UCESipMessageDataConnectionId connectionIdData;

		RelayAllocation relayingAlloc;
		try {
			relayingAlloc = relayAllocationGenerator.takeRelayAllocation();
		} catch (InterruptedException | IOException e2) {
			throw new UCEException("Unable to get a new relay allocation. Perhaps the relay server is not reachable", e2);
		}

		IUCEStunEndpoint stunEndpoint;
		try {
			stunEndpoint = stunEndpointGenerator.takeStunEndpoint();
		} catch (UCEStunException | InterruptedException e1) {
			releaseRelayAllocation(relayingAlloc);
			throw new UCEException("Unable to get an evaluated public endpoint. Perhaps the stun server is not reachable", e1);
		}

		try {
			UCESipServerCall tCall = sipUAS.takeCall(timeoutMillis);
			IUCESipMessage inviteMessage = tCall.getSipInviteMessage();

			connectionIdData = inviteMessage.getMessageData("ConnectionIdMessage", UCESipMessageDataConnectionId.class);
			UCESipMessageDataStun stunClientData = inviteMessage.getMessageData("StunMessage", UCESipMessageDataStun.class);
			privateEndpoint = stunClientData.getLocalEndpoint();
			publicEndpoint = stunClientData.getPublicEndpoint();

			IUCESipMessage okMessage = new UCESipMessage();
			okMessage.addMessageData("StunMessage", new UCESipMessageDataStun(stunEndpoint.getLocalEndpoint(), stunEndpoint.getPublicEnpoint()));

			InetSocketAddress relayAddress = new InetSocketAddress(serverSettings.getRelaySettings().getRelayAddress().getAddress(), relayingAlloc
					.getEndpointAddr().getPort());

			okMessage.addMessageData("RelayMessage", new UCESipMessageDataRelay(relayAddress));

			tCall.sendOK(okMessage);

			Socket serverRelaySocket = relayingAlloc.getRelayingClient().accept();

			try {
				switchableSocket = socketSwitchFactory.create(serverRelaySocket);
			} catch (IOException e) {
				tryCloseRelaySocket(serverRelaySocket);
				throw e;
			}

		} catch (UCESipException | InterruptedException | IOException e1) {
			releaseRelayAllocation(relayingAlloc);
			releaseStunEndpoint(stunEndpoint);
			throw new UCEException("Unable to accept an UCE Client Socket.", e1);
		}

		// relay connection is established

		InetSocketAddress localAddress = (InetSocketAddress) stunEndpoint.getStunEndpointSocket().getLocalSocketAddress();
		SocketAddress localSocketAddress = new InetSocketAddress(0);
		ConnectionAuthenticator targetAuth = new TargetConnectionAuthenticator(connectionIdData.getConnectionId());

		serverHolepuncherSwitcherThread = new ServerHolePuncherSwitcherThread(privateEndpoint, publicEndpoint, localAddress, localSocketAddress,
				targetAuth, switchableSocket, relayingAlloc.getRelayingClient(), serverSettings.getSocketSwitchTimeoutMillis());

		if (serverSettings.isHolepuncherEnabled()) {
			// can disable holepuncher for tests
			serverHolepuncherSwitcherThread.start();
		}

		return new ServersideClientSocket(serverHolepuncherSwitcherThread, switchableSocket, stunEndpoint);
	}

	private void tryCloseRelaySocket(Socket sock) {
		try {
			sock.close();
		} catch (IOException ex) {
			LOGGER.info("Unable to close relay connection", ex);
		}
	}

	private void releaseRelayAllocation(RelayAllocation relayAllocation) {
		try {
			relayAllocation.getRelayingClient().discardAllocation();
		} catch (IOException e) {
			LOGGER.info("Unable to discard relay allocation", e);
		}
	}

	private void releaseStunEndpoint(IUCEStunEndpoint stunEndpoint) {
		try {
			stunEndpoint.getStunEndpointSocket().close();
		} catch (IOException e) {
			LOGGER.info("Unable to release stun endpoint", e);
		}
	}

	@Override
	public synchronized void serverClose() {
		if (isListening == false) {
			throw new IllegalStateException("Not listening");
		}
		// try to release all resources we have got
		// Cannot stunEndpoint socket because HolePuncher needs this opened
		// connection
		// and a holePuncher thread could also be in execution while calling
		// close
		try {
			stunEndpointGenerator.terminate();
		} catch (Exception e) {
			LOGGER.info("Unable to terminate stun endpoint generator", e);
		}

		try {
			relayAllocationGenerator.terminate();
		} catch (Exception e) {
			LOGGER.info("Unable to terminate relay allocation generator", e);
		}

		// do not shutdown sipUAS, it is injected, so upper level has to handle.
	}
}
