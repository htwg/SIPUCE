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
import de.fhkn.in.uce.relaying.core.IRelayingClient;
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

final class OnDemandServerConnectionHandlerStrategy implements IServerConnectionHandlerStrategy {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(OnDemandServerConnectionHandlerStrategy.class);

	private final IUCESipUserAgentServer sipUAS;
	private final UCEServerSettings serverSettings;
	private final ISocketSwitchFactory socketSwitchFactory;
	private final IRelayingClientFactory relayingClientFactory;
	private final IStunEndpointFactory stunEndpointFactory;
	

	public OnDemandServerConnectionHandlerStrategy(final IUCESipUserAgentServer sipUAS, final UCEServerSettings serverSettings,
			final ISocketSwitchFactory socketSwitchFactory, IRelayingClientFactory relayingClientFactory, IStunEndpointFactory stunEndpointFactory) {
		this.sipUAS = sipUAS;
		this.serverSettings = serverSettings;
		this.socketSwitchFactory = socketSwitchFactory;
		this.relayingClientFactory = relayingClientFactory;
		this.stunEndpointFactory = stunEndpointFactory;
	}

	@Override
	public void serverListen() throws UCEException {
		// nothing to allocate..
	}

	@Override
	public Socket serverAccept(int timeoutMillis) throws UCEException, TimeoutException {
		SwitchableSocket serverSwitchableSocket;
		ServerHolePuncherSwitcherThread serverHolepuncherThread;
		IUCEStunEndpoint stunEndpoint;
		UCESipMessageDataStun stunClientData;
		UCESipMessageDataConnectionId connectionIdData;
		
		IUCESipMessage inviteMessage;
		UCESipServerCall tCall;
		try {
			tCall = sipUAS.takeCall(timeoutMillis);
			inviteMessage = tCall.getSipInviteMessage();
			connectionIdData = inviteMessage.getMessageData("ConnectionIdMessage", UCESipMessageDataConnectionId.class);
			stunClientData = inviteMessage.getMessageData("StunMessage", UCESipMessageDataStun.class);
			
			tCall.sendRinging();
			
			stunEndpoint = stunEndpointFactory.create(serverSettings.getStunSettings());
			stunEndpoint.evaluatePublicEndpoint();
			
		} catch (UCESipException | UCEStunException | InterruptedException e1) {
			throw new UCEException("Unable to accept an UCE Client Socket.", e1);
		}
		

		IRelayingClient relayingClient = relayingClientFactory.create(serverSettings.getRelaySettings().getRelayAddress());
		InetSocketAddress endpointAtRelay;
		try {
			endpointAtRelay = relayingClient.createAllocation();
		} catch (IOException e) {
			releaseStunEndpoint(stunEndpoint);
			throw new UCEException("Unable to ", e);
		}

		IUCESipMessage okMessage = new UCESipMessage();
		okMessage.addMessageData("StunMessage", new UCESipMessageDataStun(stunEndpoint.getLocalEndpoint(), stunEndpoint.getPublicEnpoint()));

		InetSocketAddress relayAddress = new InetSocketAddress(serverSettings.getRelaySettings().getRelayAddress().getAddress(),
				endpointAtRelay.getPort());
		
		okMessage.addMessageData("RelayMessage", new UCESipMessageDataRelay(relayAddress));

		Socket serverRelaySocket;
		try {
			tCall.sendOK(okMessage);
			serverRelaySocket = relayingClient.accept();
			
			try {
				serverSwitchableSocket = socketSwitchFactory.create(serverRelaySocket);
			} catch (IOException e) {
				tryCloseRelaySocket(serverRelaySocket);
				throw e;
			}
		} catch (UCESipException | IOException | InterruptedException e1) {
			releaseRelayAllocation(relayingClient);
			releaseStunEndpoint(stunEndpoint);
			throw new UCEException("Unable to establish connection", e1);
		}

		// relay connection is established, dont throw errors after, because our connection is up.
		
		InetSocketAddress privateEndpoint = stunClientData.getLocalEndpoint();
		InetSocketAddress publicEndpoint = stunClientData.getPublicEndpoint();
		InetSocketAddress localAddress = (InetSocketAddress) stunEndpoint.getStunEndpointSocket().getLocalSocketAddress();
		SocketAddress localSocketAddress = new InetSocketAddress(0);
		ConnectionAuthenticator targetAuth = new TargetConnectionAuthenticator(connectionIdData.getConnectionId());

		serverHolepuncherThread = new ServerHolePuncherSwitcherThread(privateEndpoint, publicEndpoint, localAddress, localSocketAddress, targetAuth,
				serverSwitchableSocket, relayingClient, serverSettings.getSocketSwitchTimeoutMillis());
		
		if (serverSettings.isHolepuncherEnabled()) {
			// can disable holepuncher for tests
			serverHolepuncherThread.start();
		}

		return new ServersideClientSocket(serverHolepuncherThread, serverSwitchableSocket, stunEndpoint);
	}
	
	private void tryCloseRelaySocket(Socket sock) {
		try {
			sock.close();
		} catch (IOException ex) {
			LOGGER.info("Unable to close relay connection", ex);
		}
	}

	private void releaseRelayAllocation(IRelayingClient relayingClient) {
		try {
			relayingClient.discardAllocation();
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
	public void serverClose() {
		// try to release all resources we have got
		// Cannot stunEndpoint socket because HolePuncher needs this opened
		// connection
		// and a holePuncher thread could also be in execution while calling
		// close
	}
}
