package de.fhkn.in.uce.sip.ucesip.client;

import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;
import de.fhkn.in.uce.sip.ucesip.message.IUCESipMessage;


public interface IUCESipUserAgentClient {

	IUCESipMessage inviteUCESipUAS(IUCESipMessage message) throws UCESipException, InterruptedException;

	void shutdown() throws UCESipException;
}
