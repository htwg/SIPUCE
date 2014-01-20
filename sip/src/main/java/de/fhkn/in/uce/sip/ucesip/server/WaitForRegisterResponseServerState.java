package de.fhkn.in.uce.sip.ucesip.server;

import java.util.concurrent.TimeoutException;

import javax.sip.message.Request;

import de.fhkn.in.uce.sip.core.message.IMessage;
import de.fhkn.in.uce.sip.core.message.IResponseMessage;
import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;

class WaitForRegisterResponseServerState extends BaseAllowInviteHandleServerState {

	public WaitForRegisterResponseServerState(IServerStateContext stateContext) {
		super(stateContext);
	}

	@Override
	public ServerStates getCurrentState() {
		return ServerStates.WaitForRegisterResponse;
	}

	@Override
	public void shutdown() {
		// we come out here with a timeout
	}

	@Override
	public void onOk(IMessage okmessage) {
		IResponseMessage message = (IResponseMessage) okmessage;

		if (message.getRequestMethod().equals(Request.REGISTER) == false) {
			LOGGER.info("Received OK with wrong method in WaitForRegisterResponse state: " + okmessage.getMessage());
		}
		stateContext.changeState(new RegisteredServerState(stateContext));
	}

	@Override
	public int getStateTimeoutMillis() {
		return stateContext.getSipSettings().getWaitForRegisterResponseTimeout();
	}

	@Override
	public void onStateTimeout() {
		gotoErrorState(new TimeoutException("Wait for register response timed out (state timeout)"));
	}

	@Override
	public void onTimeOut() {
		gotoErrorState(new TimeoutException("Wait for register response timed out (server response timeout)"));
	}

	private void gotoErrorState(Exception reason) {
		declineCurrentCalls();
		super.tryToDerigister();
		stateContext.changeState(new ErrorServerState(stateContext, new UCESipException(reason)));
	}

}