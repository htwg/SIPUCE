package de.fhkn.in.uce.api.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import de.fhkn.in.uce.stun.attribute.Attribute;
import de.fhkn.in.uce.stun.attribute.XorMappedAddress;
import de.fhkn.in.uce.stun.header.STUNMessageClass;
import de.fhkn.in.uce.stun.header.STUNMessageMethod;
import de.fhkn.in.uce.stun.message.Message;
import de.fhkn.in.uce.stun.message.MessageReader;
import de.fhkn.in.uce.stun.message.MessageStaticFactory;
import de.fhkn.in.uce.stun.ucestun.exception.UCEStunException;

public class UCEStunEndpoint implements IUCEStunEndpoint {
	
	private Socket socketToStunServer = null;
	private InetSocketAddress stunAddress = null;
	private InetSocketAddress publicEndpoint = null;
	
	public UCEStunEndpoint(final UCEStunSettings settings) throws UCEStunException {
		this.stunAddress = settings.getStunAddress();
		this.establishConnection();
	}
	
	private void establishConnection() throws UCEStunException {

		this.socketToStunServer = new Socket();

		try {
			this.socketToStunServer.setReuseAddress(true);
			this.socketToStunServer.bind(null);
			this.socketToStunServer.connect(this.stunAddress);
		} catch (IOException e) {
			throw new UCEStunException(e);
		}
	}

	@Override
	public void evaluatePublicEndpoint() throws UCEStunException {
		this.sendStunRequest();
		Message response = this.receiveStunResponse();
		this.publicEndpoint = this.extractPublicEnpointFromResponse(response);
	}
	
	private void sendStunRequest() throws UCEStunException {

		final Message stunRequest = MessageStaticFactory.newSTUNMessageInstance(STUNMessageClass.REQUEST, STUNMessageMethod.BINDING);

		try {
			stunRequest.writeTo(this.socketToStunServer.getOutputStream());
		} catch (IOException e) {
			throw new UCEStunException(e);
		}
	}

	private Message receiveStunResponse() throws UCEStunException {

		final MessageReader messageReader = MessageReader.createMessageReader();

		Message stunResponse;

		try {
			stunResponse = messageReader.readSTUNMessage(this.socketToStunServer.getInputStream());
		} catch (IOException e) {
			throw new UCEStunException(e);
		}

		return stunResponse;
	}

	private InetSocketAddress extractPublicEnpointFromResponse(Message stunResponse) {
		Attribute publicEnpointAttribute = stunResponse.getAttribute(XorMappedAddress.class);
		XorMappedAddress publicXorMappedIP = (XorMappedAddress) publicEnpointAttribute;
		return publicXorMappedIP.getEndpoint();
	}

	@Override
	public InetSocketAddress getLocalEndpoint() {
		return new InetSocketAddress(socketToStunServer.getLocalAddress(), socketToStunServer.getLocalPort());
	}

	@Override
	public InetSocketAddress getPublicEnpoint() {
		return this.publicEndpoint;
	}

	@Override
	public Socket getStunEndpointSocket() {
		return this.socketToStunServer;
	}
}
