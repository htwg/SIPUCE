package de.fhkn.in.uce.sip.demo.controller;

import java.net.InetSocketAddress;
import java.util.Observer;

import de.fhkn.in.uce.sip.core.SipUser;

public interface IController {
	/**
	 * Register on SIP-Registrar and start update thread.
	 */
	void register();

	/**
	 * Unregister from SIP-Registrar.
	 */
	void unregister();

	/** Connect to another SIP User.<br/><br/>
	 * <ul>
	 * <li>First get the public IP address using a STUN or a TURN Server.</li>
	 * <li>Second get the address of the partner using SIP.</li>
	 * <li>Then connect via Relay Server and initiate hole punching.</li>
	 * </ul>
	 * @param to
	 */
	void connect(String to);

	void disconnect();

	void addObserver(Observer observer);

	void deleteObserver(Observer observer);

	void initSipStack();

	void stop();

	void printInfo();




	InetSocketAddress getPrivateAddress();

	void setPrivateAddress(InetSocketAddress privateAddress);

	InetSocketAddress getPublicAddress();

	void setPublicAddress(InetSocketAddress publicAddress);

	InetSocketAddress getSipProxyAddress();

	void setSipProxyAddress(InetSocketAddress sipProxyAddress);

	void setUser(SipUser user);

	SipUser getUser();

	void acceptCall();

	void declineCall();
}
