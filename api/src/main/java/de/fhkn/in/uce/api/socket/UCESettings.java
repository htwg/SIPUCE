package de.fhkn.in.uce.api.socket;

import java.net.InetSocketAddress;


/**
 * Common settings class for the UCE Client and UCE Server Socket.
 * 
 */
public abstract class UCESettings {
	
	private static final int DEFAULT_SOCKET_SWITCH_TIMEOUT = 10000;

	private final UCEStunSettings stunSettings;
	private final int socketSwitchTimeoutMillis;
	private final boolean holepuncherEnabled;

	/**
	 * Constructor to initialize and set the settings.
	 * Should only be visible to subclasses.
	 * 
	 */
	protected UCESettings(final UCEStunSettings stunSettings, final int socketSwitchTimeoutMillis, boolean holepuncherEnabled) {
		this.stunSettings = stunSettings;
		this.socketSwitchTimeoutMillis = socketSwitchTimeoutMillis;
		this.holepuncherEnabled = holepuncherEnabled;
	}
	
	/**
	 * Constructor to initialize and set the settings.
	 * Should only be visible to subclasses.
	 * 
	 */
	protected UCESettings(final UCEStunSettings stunSettings) {
		this.stunSettings = stunSettings;
		this.socketSwitchTimeoutMillis = DEFAULT_SOCKET_SWITCH_TIMEOUT;
		this.holepuncherEnabled = true;
	}

	/**
	 * Constructor to initialize and set the settings.
	 * Should only be visible to subclasses.
	 * 
	 */
	protected UCESettings(final InetSocketAddress stunServerAddress) {
		this.stunSettings = new UCEStunSettings(stunServerAddress);
		this.socketSwitchTimeoutMillis = DEFAULT_SOCKET_SWITCH_TIMEOUT;
		this.holepuncherEnabled = true;
	}

	public UCEStunSettings getStunSettings() {
		return stunSettings;
	}

	public int getSocketSwitchTimeoutMillis() {
		return socketSwitchTimeoutMillis;
	}

	public boolean isHolepuncherEnabled() {
		return holepuncherEnabled;
	}
}
