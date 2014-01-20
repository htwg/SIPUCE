package de.fhkn.in.uce.api.socket;

import de.fhkn.in.uce.sip.ucesip.client.IUCESipUserAgentClient;
import de.fhkn.in.uce.sip.ucesip.server.IUCESipUserAgentServer;

interface IConnectionStrategyFactory {
	IClientConnectionHandlerStrategy createClientStrategy(final IUCESipUserAgentClient sipUAC, final UCEClientSettings clientSettings,
			ISocketSwitchFactory socketSwitchFactory, IStunEndpointFactory stunEndpointFactory);

	IServerConnectionHandlerStrategy createServerStrategy(final IUCESipUserAgentServer sipUAS, final UCEServerSettings serverSettings,
			ISocketSwitchFactory socketSwitchFactory, IRelayingClientFactory relayingClientFactory, IStunEndpointFactory stunEndpointFactory);
}
