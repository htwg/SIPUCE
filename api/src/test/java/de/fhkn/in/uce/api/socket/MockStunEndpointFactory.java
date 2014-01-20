package de.fhkn.in.uce.api.socket;

import de.fhkn.in.uce.api.socket.IStunEndpointFactory;
import de.fhkn.in.uce.stun.ucestun.exception.UCEStunException;

public class MockStunEndpointFactory implements IStunEndpointFactory {

	@Override
	public IUCEStunEndpoint create(UCEStunSettings stunSettings) throws UCEStunException {
		return new StunEndpointMock();
	}

}
