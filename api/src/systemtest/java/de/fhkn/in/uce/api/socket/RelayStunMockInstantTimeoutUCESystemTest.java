package de.fhkn.in.uce.api.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;

import org.junit.Test;

import de.fhkn.in.uce.api.socket.ConnectionUserData;
import de.fhkn.in.uce.api.socket.InstantConnectionStrategyFactory;
import de.fhkn.in.uce.api.socket.MockRelayingClientFactory;
import de.fhkn.in.uce.api.socket.MockStunEndpointFactory;
import de.fhkn.in.uce.api.socket.TimeoutSocketSwitchFactory;
import de.fhkn.in.uce.api.socket.UCEClientSettings;
import de.fhkn.in.uce.api.socket.UCEException;
import de.fhkn.in.uce.api.socket.UCEServerSettings;
import de.fhkn.in.uce.api.socket.UCEServerSocket;
import de.fhkn.in.uce.api.socket.UCESocket;
import de.fhkn.in.uce.api.socket.UCETestSettings;
import de.fhkn.in.uce.sip.ucesip.settings.UCESipServerSettings;

public class RelayStunMockInstantTimeoutUCESystemTest extends UCESystemTest {

	private static final UCEServerSettings SERVERSETTINGS_LONG_TIMEOUT_WO_HOLE = new UCEServerSettings(new UCEStunSettings(new InetSocketAddress(
			"localhost", 3478)), new UCESipServerSettings(new InetSocketAddress("213.239.218.18", 5060)),
			new UCERelaySettings(new InetSocketAddress("localhost", 10300)), 5000, 5000, 10000, false);

	@Override
	public UCEServerSocket createServerSocket(String sipUser, String sipServer, UCEServerSettings settings) throws IOException {
		return new UCEServerSocket(sipUser, sipServer, SERVERSETTINGS_LONG_TIMEOUT_WO_HOLE, new InstantConnectionStrategyFactory(),
				new TimeoutSocketSwitchFactory(), new MockRelayingClientFactory(), new MockStunEndpointFactory());
	}

	@Override
	public UCESocket createClientSocket(String fromUserDomain, String toUser, String toUserDomain, UCEClientSettings settings) throws UCEException {
		ConnectionUserData tUsD = new ConnectionUserData(UUID.randomUUID().toString(), fromUserDomain, toUser, toUserDomain);
		return new UCESocket(tUsD, settings, new InstantConnectionStrategyFactory(), new TimeoutSocketSwitchFactory(), new MockStunEndpointFactory());
	}

	@Test
	public void testUce1000InvitesAcceptRelayStunMock() throws UCEException, IOException, InterruptedException {
		currentClientSettings = UCETestSettings.CLIENT_SETTINGS_WITHOUT_HOLEPUNCHER;

		testUceMultipleInvitesAccept(1000, 1000);
	}
}