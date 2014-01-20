package de.fhkn.in.uce.api.socket;

import de.fhkn.in.uce.sip.ucesip.client.IUCESipUserAgentClient;
import de.fhkn.in.uce.sip.ucesip.server.IUCESipUserAgentServer;

final class InstantConnectionStrategyFactory implements IConnectionStrategyFactory {

	@Override
	public IClientConnectionHandlerStrategy createClientStrategy(final IUCESipUserAgentClient sipUAC, UCEClientSettings clientSettings,
			ISocketSwitchFactory socketSwitchFactory, IStunEndpointFactory stunEndpointFactory) {
		return new ClientConnectionHandlerStrategy(sipUAC, clientSettings, socketSwitchFactory, stunEndpointFactory);
	}

	@Override
	public IServerConnectionHandlerStrategy createServerStrategy(IUCESipUserAgentServer sipUAS, UCEServerSettings serverSettings,
			ISocketSwitchFactory socketSwitchFactory, IRelayingClientFactory relayingClientFactory, IStunEndpointFactory stunEndpointFactory) {
		return new InstantServerConnectionHandlerStrategy(sipUAS, serverSettings, socketSwitchFactory, relayingClientFactory, stunEndpointFactory);
	}
}
