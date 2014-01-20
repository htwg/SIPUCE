package de.fhkn.in.uce.api.socket;

import java.net.InetSocketAddress;

public class UCEStunSettings {

	private final InetSocketAddress stunAddress;

	public UCEStunSettings(final InetSocketAddress stunAddress) {
		this.stunAddress = stunAddress;
	}
	
	public InetSocketAddress getStunAddress() {
		return stunAddress;
	}
}
