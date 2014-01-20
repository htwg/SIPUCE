package de.fhkn.in.uce.api.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.holepunching.core.ConnectionAuthenticator;
import de.fhkn.in.uce.holepunching.core.authentication.SourceConnectionAuthenticator;
import de.fhkn.in.uce.sip.ucesip.client.IUCESipUserAgentClient;
import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;
import de.fhkn.in.uce.sip.ucesip.message.IUCESipMessage;
import de.fhkn.in.uce.sip.ucesip.message.UCESipMessage;
import de.fhkn.in.uce.sip.ucesip.message.UCESipMessageDataConnectionId;
import de.fhkn.in.uce.sip.ucesip.message.UCESipMessageDataRelay;
import de.fhkn.in.uce.sip.ucesip.message.UCESipMessageDataStun;
import de.fhkn.in.uce.socketswitch.SwitchableSocket;
import de.fhkn.in.uce.stun.ucestun.exception.UCEStunException;

final class ClientConnectionHandlerStrategy implements IClientConnectionHandlerStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClientConnectionHandlerStrategy.class);

	private final IUCESipUserAgentClient sipUAC;
	private final ISocketSwitchFactory socketSwitchFactory;
	private final UCEClientSettings clientSettings;
	private final IStunEndpointFactory stunEndpointFactory;
 
	private volatile HolePuncherSwitcherThread clientHolepuncherThread;
	private volatile boolean isCon;
	private volatile IUCEStunEndpoint stunEndpoint;

	public ClientConnectionHandlerStrategy(final IUCESipUserAgentClient sipUAC, final UCEClientSettings clientSettings,
			final ISocketSwitchFactory socketSwitchFactory, IStunEndpointFactory stunEndpointFactory) {
		this.sipUAC = sipUAC;
		this.socketSwitchFactory = socketSwitchFactory;
		this.isCon = false;
		this.clientSettings = clientSettings;
		this.stunEndpointFactory = stunEndpointFactory;
	}

	@Override
	public synchronized Socket clientConnect() throws UCEException {

		InetSocketAddress ownLocalEndpoint;
		InetSocketAddress ownPublicEndpoint;

		UUID holepunchingToken = UUID.randomUUID();

		try {
			stunEndpoint = stunEndpointFactory.create(clientSettings.getStunSettings());
			stunEndpoint.evaluatePublicEndpoint();
		} catch (UCEStunException e1) {
			// no sip derigister here, sipUAC is injected. Upper level.
			throw new UCEException("Unable to evaluate public endpoint. Perhaps the stun server is not reachable.", e1);
		}
		ownLocalEndpoint = stunEndpoint.getLocalEndpoint();
		ownPublicEndpoint = stunEndpoint.getPublicEnpoint();

		IUCESipMessage inviteMessage = new UCESipMessage();
		inviteMessage.addMessageData("ConnectionIdMessage", new UCESipMessageDataConnectionId(holepunchingToken));
		inviteMessage.addMessageData("StunMessage", new UCESipMessageDataStun(ownLocalEndpoint, ownPublicEndpoint));

		IUCESipMessage okMessage;
		UCESipMessageDataRelay relayData;
		UCESipMessageDataStun stunData;
		SwitchableSocket clientSwitchableSocket;
		
		String relayServerErrorMsg = "";
		
		try {
			okMessage = sipUAC.inviteUCESipUAS(inviteMessage);

			relayData = okMessage.getMessageData("RelayMessage", UCESipMessageDataRelay.class);
			stunData = okMessage.getMessageData("StunMessage", UCESipMessageDataStun.class);
			
			relayServerErrorMsg = "Received relay server endpoint: " + relayData.getRelayEndpoint().toString();
			
			LOGGER.debug("client trying to connect to {}", relayData.getRelayEndpoint());
			Socket clientRelaySocket = new Socket();
			clientRelaySocket.connect(relayData.getRelayEndpoint());
			LOGGER.debug("client connected to {}", relayData.getRelayEndpoint());

			try {
				clientSwitchableSocket = socketSwitchFactory.create(clientRelaySocket);
			} catch (IOException ex) {
				tryCloseRelaySocket(clientRelaySocket);
				throw ex;
			}
		} catch (UCESipException | IOException | InterruptedException e1) {
			// no sip derigister here, sipUAC is injected. Upper level.
			closeStunEndpoint();
			throw new UCEException("Unable to establish connection. Perhaps the relay server is not reachable. " + relayServerErrorMsg, e1);
		}

		// now relay connection is established.

		InetSocketAddress remotePrivateEndpoint = stunData.getPublicEndpoint();
		InetSocketAddress remotePublicEndpoint = stunData.getLocalEndpoint();
		SocketAddress ownLocalSocketAddress = stunEndpoint.getStunEndpointSocket().getLocalSocketAddress();
		InetSocketAddress ownLocalAddress = (InetSocketAddress) stunEndpoint.getStunEndpointSocket().getLocalSocketAddress();
		ConnectionAuthenticator sourceAuth = new SourceConnectionAuthenticator(holepunchingToken);

		clientHolepuncherThread = new HolePuncherSwitcherThread(remotePrivateEndpoint, remotePublicEndpoint, ownLocalAddress, ownLocalSocketAddress,
				sourceAuth, clientSwitchableSocket, clientSettings.getSocketSwitchTimeoutMillis());

		if (clientSettings.isHolepuncherEnabled()) {
			// can disable holepuncher for tests
			clientHolepuncherThread.start();
		}

		// now close can be called.
		isCon = true;

		// do not! return ServersideClientSocket(). It is only for the server!
		// And on the client we don't need it, because we have only one socket
		// to handle with.
		return clientSwitchableSocket;
	}

	private void tryCloseRelaySocket(Socket sock) {
		try {
			sock.close();
		} catch (IOException ex) {
			LOGGER.info("Unable to close relay connection", ex);
		}
	}

	@Override
	public synchronized void clientClose() {
		if (isCon == false) {
			throw new IllegalStateException("Client is not connected");
		}

		// try to release everything we have allocated.

		try {
			// we terminate here because it is only one socket instance on the
			// client! and if we can close we were connected before.
			clientHolepuncherThread.terminate();
		} catch (Exception e) {
			LOGGER.info("Unable to terminate holepuncher switcher thread", e);
		}

		closeStunEndpoint();

		// no sip derigister here, sipUAC is injected! Upper level.
	}

	private void closeStunEndpoint() {
		try {
			stunEndpoint.getStunEndpointSocket().close();
		} catch (IOException e) {
			LOGGER.info("Unable to close socket to stun server", e);
		}
	}
}
