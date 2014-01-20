package de.fhkn.in.uce.sip.core;

import java.net.InetSocketAddress;

public class SipPreferences {

	private static final String DEFAULT_PROTOCOL = "udp";
	
	// Expire time in seconds - 5 minutes by default
	private static final int DEFAULT_EXPIRE_TIME = 300;

	private String sipTransportProtocol;
	private InetSocketAddress sipProxyAddress;
	private InetSocketAddress privateAddress;
	private InetSocketAddress publicAddress;
	private int expireTime = DEFAULT_EXPIRE_TIME;

	/**
	 * 
	 * @param proxyAddress
	 */
	public SipPreferences(final InetSocketAddress proxyAddress) {
		super();
		sipTransportProtocol = DEFAULT_PROTOCOL;
		sipProxyAddress = proxyAddress;
	}

	/**
	 * @param sipTransportProtocol
	 * @param sipProxyAddress
	 * @param privateAddress
	 * @param publicAddress
	 */
	public SipPreferences(final String sipTransportProtocol,
			final InetSocketAddress sipProxyAddress,
			final InetSocketAddress privateAddress,
			final InetSocketAddress publicAddress) {
		super();
		this.sipTransportProtocol = sipTransportProtocol;
		this.sipProxyAddress = sipProxyAddress;
		this.privateAddress = privateAddress;
		this.publicAddress = publicAddress;
	}

	/**
	 * @return the sipTransportProtocol
	 */
	public final String getSipTransportProtocol() {
		return sipTransportProtocol;
	}

	/**
	 * @param sipTransportProtocol the sipTransportProtocol to set
	 */
	public final void setSipTransportProtocol(final String sipTransportProtocol) {
		this.sipTransportProtocol = sipTransportProtocol;
	}

	/**
	 * @return the sipProxyAddress
	 */
	public final InetSocketAddress getSipProxyAddress() {
		return sipProxyAddress;
	}

	/**
	 * @param sipProxyAddress the sipProxyAddress to set
	 */
	public final void setSipProxyAddress(final InetSocketAddress sipProxyAddress) {
		this.sipProxyAddress = sipProxyAddress;
	}

	/**
	 * @return the privateAddress
	 */
	public final InetSocketAddress getPrivateAddress() {
		return privateAddress;
	}

	/**
	 * @param privateAddress the privateAddress to set
	 */
	public final void setPrivateAddress(final InetSocketAddress privateAddress) {
		this.privateAddress = privateAddress;
	}

	/**
	 * @return the publicAddress
	 */
	public final InetSocketAddress getPublicAddress() {
		return publicAddress;
	}

	/**
	 * @param publicAddress the publicAddress to set
	 */
	public final void setPublicAddress(final InetSocketAddress publicAddress) {
		this.publicAddress = publicAddress;
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();

		sb.append("Private IP = ").append(getPrivateAddressString()).append("%n");
		sb.append("Public IP  = ").append(getPublicAddressString()).append("%n");
		sb.append("Proxy      = ").append(getProxyAddressString()).append("%n");
		sb.append("Protocol   = ").append(sipTransportProtocol).append("%n");

		return sb.toString();
	}

	public String getPublicAddressString() {
		return getAddressString(publicAddress);
	}

	public String getPrivateAddressString() {
		return getAddressString(privateAddress);
	}

	public String getProxyAddressString() {
		return getAddressString(sipProxyAddress);
	}


	private String getAddressString(final InetSocketAddress addr) {
		if (addr == null) {
			return "null";
		}
		return String.format("%s:%d",
				addr.getAddress().getHostAddress(),
				addr.getPort());
	}

	public int getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(final int expireTime) {
		this.expireTime = expireTime;
	}

	public String getPopulateString() {
		if (publicAddress != null) {
			return getPublicAddressString();
		}
		return getPrivateAddressString();
	}

	public int getPopulatePort() {
		if (publicAddress != null) {
			return publicAddress.getPort();
		}
		return  privateAddress.getPort();
	}

	public String getPopulateAddress() {
		if (publicAddress != null) {
			return publicAddress.getAddress().getHostAddress();
		}
		return  privateAddress.getAddress().getHostAddress();
	}
}