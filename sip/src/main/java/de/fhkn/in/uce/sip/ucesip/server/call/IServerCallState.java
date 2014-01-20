package de.fhkn.in.uce.sip.ucesip.server.call;

import de.fhkn.in.uce.sip.core.message.IMessage;
import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;
import de.fhkn.in.uce.sip.ucesip.message.IUCESipMessage;

interface IServerCallState {

	ServerCallStates getCurrentState();

	int getStateTimeoutMillis();

	void onStateTimeout();

	void onBye(IMessage message);

	void sendDecline() throws UCESipException;

	void sendOK(final IUCESipMessage okMessage) throws UCESipException;

	void sendQueued() throws UCESipException;

	void sendRinging() throws UCESipException;
	
	boolean isFinished();
}

enum ServerCallStates {
	Init, WaitForBye, Finished, Error
}