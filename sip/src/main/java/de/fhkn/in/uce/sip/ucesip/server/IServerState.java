package de.fhkn.in.uce.sip.ucesip.server;

import de.fhkn.in.uce.sip.core.message.ErrorMessage;
import de.fhkn.in.uce.sip.core.message.IMessage;
import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;
import de.fhkn.in.uce.sip.ucesip.server.call.UCESipServerCall;

interface IServerState {
	void onBye(final IMessage message);

	UCESipServerCall pollCall();

	void onOk(final IMessage okmessage);

	void onTimeOut();

	void onFailure(final ErrorMessage eMessage);

	void onInvite(final IMessage inviteMessage);

	void onStateTimeout();

	ServerStates getCurrentState();

	int getStateTimeoutMillis();

	void shutdown();

	void startRegister() throws UCESipException;
	
	void onInit();

}

enum ServerStates {
	Init, WaitForRegisterResponse, Registered, Finished, Error, WaitForDeregisterResponse, WaitForCallsFinished
}