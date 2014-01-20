package de.fhkn.in.uce.sip.ucesip.client;

import de.fhkn.in.uce.sip.core.message.ErrorMessage;
import de.fhkn.in.uce.sip.core.message.IMessage;
import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;
import de.fhkn.in.uce.sip.ucesip.message.IUCESipMessage;

interface IInviteState {
	void onOk(IMessage okmessage);

	void onDecline();

	void onTimeOut();

	void onFailure(ErrorMessage eMessage);

	void shutdown();

	InviteStates getState();

	int getStateTimeoutMillis();

	void onStateTimeout();
	
	void inviteUAS(IUCESipMessage message) throws UCESipException;

}

enum InviteStates {
	Init, WaitForInviteResponse, WaitForByeResponse, WaitForDerigisterResponse, Error, Finished
}