package de.fhkn.in.uce.sip.ucesip.settings;

import java.net.InetSocketAddress;

public class UCESipSettings {

	private static final int DEFAULT_EXPIRE_TIME = 120;
	private static final TransportProtocol DEFAULT_TRANSPORT_PROTOCOL = TransportProtocol.Udp;
	
	private final int expireTime;
	private final TransportProtocol transportProtocol;
	private final InetSocketAddress proxyAddress;
	
	public UCESipSettings(final InetSocketAddress proxyAddress) {
		this(DEFAULT_EXPIRE_TIME, DEFAULT_TRANSPORT_PROTOCOL, proxyAddress);
	}
	
	public UCESipSettings(final int expireTime, final TransportProtocol transportProtocol, final InetSocketAddress proxyAddress) {
		this.expireTime = expireTime;
		this.transportProtocol = transportProtocol;
		this.proxyAddress = proxyAddress;
	}
	
	public InetSocketAddress getProxyAddress() {
		return proxyAddress;
	}

	public TransportProtocol getTransportProtocol() {
		return transportProtocol;
	}
	
	public String getTransportProtocolString() {
		switch (transportProtocol) {
		case Tcp:
			return "tcp";
		case Udp:
			return "udp";
		}
		assert false : "must not come here";
		return "";
	}

	public int getExpireTime() {
		return expireTime;
	}
	
	public enum TransportProtocol {
		Tcp,
		Udp
	}
}

