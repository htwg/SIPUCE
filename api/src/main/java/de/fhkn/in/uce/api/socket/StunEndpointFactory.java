package de.fhkn.in.uce.api.socket;

import de.fhkn.in.uce.stun.ucestun.exception.UCEStunException;

class StunEndpointFactory implements IStunEndpointFactory {
	
	@Override
	public IUCEStunEndpoint create(UCEStunSettings stunSettings) throws UCEStunException {
		return new UCEStunEndpoint(stunSettings);
	}

}
