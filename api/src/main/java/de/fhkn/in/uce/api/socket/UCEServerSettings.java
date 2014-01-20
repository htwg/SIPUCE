package de.fhkn.in.uce.api.socket;

import java.net.InetSocketAddress;

import de.fhkn.in.uce.sip.ucesip.settings.UCESipServerSettings;

/**
 * Settings for the UCE Server Socket. The settings of the different components
 * needed by the UCE Server Socket must be set here.
 * 
 */
public final class UCEServerSettings extends UCESettings {

	private static final int DEFAULT_RELAY_ALLOCATION_REFRESH_TIME = 5000;
	private static final int DEFAULT_STUN_ENDPOINT_REFRESH_TIME = 5000;

	private final UCERelaySettings relaySettings;
	private final int relayAllocationRefreshTimeMillis;
	private final int stunEndpointRefreshTimeMillis;
	private final UCESipServerSettings sipSettings;
	
	/**
	 * Initializes an UCE Server Settings object with detailed settings of the
	 * components.
	 * 
	 * @param stunSettings
	 *            Settings for the STUN component.
	 * @param sipSettings
	 *            Settings for the SIP component.
	 * @param relaySettings
	 *            Settings for the Relay component.
	 */
	public UCEServerSettings(final UCEStunSettings stunSettings, final UCESipServerSettings sipSettings, final UCERelaySettings relaySettings) {
		super(stunSettings);
		this.sipSettings = sipSettings;
		this.relaySettings = relaySettings;
		this.relayAllocationRefreshTimeMillis = DEFAULT_RELAY_ALLOCATION_REFRESH_TIME;
		this.stunEndpointRefreshTimeMillis = DEFAULT_STUN_ENDPOINT_REFRESH_TIME;
	}

	public UCEServerSettings(final UCEStunSettings stunSettings, final UCESipServerSettings sipSettings, final UCERelaySettings relaySettings,
			final int relayAllocationRefreshTimeMillis, final int stunEndpointRefreshTimeMillis, final int socketSwitchTimeoutMillis, final boolean holepuncherEnabled) {
		super(stunSettings, socketSwitchTimeoutMillis, holepuncherEnabled);
		this.sipSettings = sipSettings;
		this.relaySettings = relaySettings;
		this.relayAllocationRefreshTimeMillis = relayAllocationRefreshTimeMillis;
		this.stunEndpointRefreshTimeMillis = stunEndpointRefreshTimeMillis;
	}

	/**
	 * Initializes an UCE Server Settings object with minimal settings of the
	 * components.
	 * 
	 * @param stunServerAddress
	 *            The address of the STUN server.
	 * @param sipProxyAddress
	 *            The address of the SIP Proxy server.
	 * @param relayServerAddress
	 *            The address of the Relay server.
	 */
	public UCEServerSettings(final InetSocketAddress stunServerAddress, final InetSocketAddress sipProxyAddress,
			final InetSocketAddress relayServerAddress) {

		super(stunServerAddress);
		this.sipSettings = new UCESipServerSettings(sipProxyAddress);
		this.relaySettings = new UCERelaySettings(relayServerAddress);
		this.relayAllocationRefreshTimeMillis = DEFAULT_RELAY_ALLOCATION_REFRESH_TIME;
		this.stunEndpointRefreshTimeMillis = DEFAULT_STUN_ENDPOINT_REFRESH_TIME;
	}

	public UCERelaySettings getRelaySettings() {
		return relaySettings;
	}

	public int getStunEndpointRefreshTimeMillis() {
		return stunEndpointRefreshTimeMillis;
	}

	public int getRelayAllocationRefreshTimeMillis() {
		return relayAllocationRefreshTimeMillis;
	}
	
	public UCESipServerSettings getSipSettings() {
		return sipSettings;
	}
	
	
}
