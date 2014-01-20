package de.fhkn.in.uce.api.socket;

import java.io.IOException;
import java.util.UUID;

import de.fhkn.in.uce.api.socket.ConnectionUserData;
import de.fhkn.in.uce.api.socket.HalfCloseSocketSwitchFactory;
import de.fhkn.in.uce.api.socket.InstantConnectionStrategyFactory;
import de.fhkn.in.uce.api.socket.RelayingClientFactory;
import de.fhkn.in.uce.api.socket.StunEndpointFactory;
import de.fhkn.in.uce.api.socket.UCEClientSettings;
import de.fhkn.in.uce.api.socket.UCEException;
import de.fhkn.in.uce.api.socket.UCEServerSettings;
import de.fhkn.in.uce.api.socket.UCEServerSocket;
import de.fhkn.in.uce.api.socket.UCESocket;

public class InstantHalfCloseUCESystemTest extends UCESystemTest {

	@Override
	public UCEServerSocket createServerSocket(String sipUser, String sipServer,
			UCEServerSettings settings) throws IOException {
		return new UCEServerSocket(sipUser, sipServer, settings, new InstantConnectionStrategyFactory(), new HalfCloseSocketSwitchFactory(),
				new RelayingClientFactory(), new StunEndpointFactory());
	}

	@Override
	public UCESocket createClientSocket(String fromUserDomain, String toUser,
			String toUserDomain, UCEClientSettings settings) throws UCEException {
		ConnectionUserData tUsD = new ConnectionUserData(UUID.randomUUID().toString(), fromUserDomain, toUser, toUserDomain);
		return new UCESocket(tUsD, settings, new InstantConnectionStrategyFactory(), new HalfCloseSocketSwitchFactory(), new StunEndpointFactory());
	}
}