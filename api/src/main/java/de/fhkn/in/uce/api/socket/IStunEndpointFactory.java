package de.fhkn.in.uce.api.socket;

import de.fhkn.in.uce.stun.ucestun.exception.UCEStunException;

interface IStunEndpointFactory {

	IUCEStunEndpoint create(UCEStunSettings stunSettings) throws UCEStunException;

}
