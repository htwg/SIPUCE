package de.fhkn.in.uce.sip.ucesip.settings;

import java.net.InetSocketAddress;

public class UCESipClientSettings extends UCESipSettings {
	
	private static final int DEFAULT_WAIT_FOR_BYE_RESPONSE_TIMEOUT = 1000;
	private static final int DEFAULT_WAIT_FOR_DERIGISTER_RESPONSE_TIMEOUT = 1000;
	private static final int DEFAULT_WAIT_FOR_INVITE_RESPONSE_TIMEOUT = 5000;
	
	private final int waitForByeResponseTimeout;
	private final int waitForDeregisterResponseTimeout;
	private final int waitForInviteResponseTimeout;

	public UCESipClientSettings(InetSocketAddress proxyAddress) {
		super(proxyAddress);
		waitForByeResponseTimeout = DEFAULT_WAIT_FOR_BYE_RESPONSE_TIMEOUT;
		waitForDeregisterResponseTimeout = DEFAULT_WAIT_FOR_DERIGISTER_RESPONSE_TIMEOUT;
		waitForInviteResponseTimeout = DEFAULT_WAIT_FOR_INVITE_RESPONSE_TIMEOUT;
	}

	public UCESipClientSettings(int expireTime, TransportProtocol transportProtocol, InetSocketAddress proxyAddress, int waitForInviteResponseTimeout, int waitForByeResponseTimeout, int waitForDeregisterResponseTimeout) {
		super(expireTime, transportProtocol, proxyAddress);
		this.waitForByeResponseTimeout = waitForByeResponseTimeout;
		this.waitForDeregisterResponseTimeout = waitForDeregisterResponseTimeout;
		this.waitForInviteResponseTimeout = waitForInviteResponseTimeout;
	}

	public int getWaitForDeregisterResponseTimeout() {
		return waitForDeregisterResponseTimeout;
	}

	public int getWaitForByeResponseTimeout() {
		return waitForByeResponseTimeout;
	}

	public int getWaitForInviteResponseTimeout() {
		return waitForInviteResponseTimeout;
	}

}
