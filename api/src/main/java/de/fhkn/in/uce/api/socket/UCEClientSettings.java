package de.fhkn.in.uce.api.socket;

import java.net.InetSocketAddress;

import de.fhkn.in.uce.sip.ucesip.settings.UCESipClientSettings;

/**
 * Settings for the UCE Client Socket. The settings of the different components
 * needed by the UCE Client Socket must be set here.
 * 
 */
public final class UCEClientSettings extends UCESettings {

	private final UCESipClientSettings clientSipSettings;

	/**
	 * Initializes an UCE Client Settings object with detailed settings of the
	 * components.
	 * 
	 * @param stunSettings
	 *            Settings for the STUN component.
	 * @param clientSipSettings
	 *            Settings for the SIP component.
	 */
	public UCEClientSettings(final UCEStunSettings stunSettings, final UCESipClientSettings clientSipSettings) {
		super(stunSettings);
		this.clientSipSettings = clientSipSettings;
	}

	protected UCEClientSettings(final UCEStunSettings stunSettings, final UCESipClientSettings clientSipSettings, final int socketSwitchTimeoutMillis,
			final boolean holepuncherEnabled) {
		super(stunSettings, socketSwitchTimeoutMillis, holepuncherEnabled);
		this.clientSipSettings = clientSipSettings;
	}

	/**
	 * Initializes an UCE Client Settings object with minimal settings of the
	 * components.
	 * 
	 * @param stunServerAddress
	 *            The address of the STUN server.
	 * @param sipProxyAddress
	 *            The address of the SIP Proxy server.
	 */
	public UCEClientSettings(final InetSocketAddress stunServerAddress, final InetSocketAddress sipProxyAddress) {
		super(stunServerAddress);
		clientSipSettings = new UCESipClientSettings(sipProxyAddress);
	}

	public UCESipClientSettings getSipSettings() {
		return clientSipSettings;
	}

}
