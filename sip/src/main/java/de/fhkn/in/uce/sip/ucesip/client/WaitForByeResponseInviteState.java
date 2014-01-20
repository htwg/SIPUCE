package de.fhkn.in.uce.sip.ucesip.client;

import java.text.ParseException;
import java.util.TooManyListenersException;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.message.Request;

import de.fhkn.in.uce.sip.core.message.ErrorMessage;
import de.fhkn.in.uce.sip.core.message.IMessage;
import de.fhkn.in.uce.sip.core.message.IResponseMessage;
import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;

class WaitForByeResponseInviteState extends BaseInviteState {

	public WaitForByeResponseInviteState(IInviteStateObserver stateObserver) {
		super(stateObserver);
	}

	@Override
	public void onOk(IMessage okmessage) {
		String method = ((IResponseMessage) okmessage).getRequestMethod();

		if (method.equals(Request.BYE) == false) {
			LOGGER.info("Received OK with wrong method in WaitForByeResponseInviteState state: {} method: {} ", okmessage.getMessage(), method);
			LOGGER.info("ClientUser: {} ToUser: {}", stateObserver.getFromUser(), ((IResponseMessage) okmessage).getFromUser());
			return;
		}
		tryToDeregister();
	}

	private void tryToDeregister() {
		try {
			stateObserver.getSipManager().deregister(stateObserver.getFromUser());
		} catch (InvalidArgumentException | TooManyListenersException | ParseException | SipException | InterruptedException e) {
			LOGGER.info("Error by derigister", e);
			stateObserver.changeState(new ErrorInviteState(stateObserver, new UCESipException("Error by derigister", e)));
		}
		stateObserver.changeState(new WaitForDerigisterResponseInviteState(stateObserver));
	}

	@Override
	public void shutdown() {
		// we are somewhere in the callflow and have only to wait until an
		// error comes or we are in the finished state.
	}

	@Override
	public InviteStates getState() {
		return InviteStates.WaitForByeResponse;
	}

	@Override
	public void onDecline() {
		LOGGER.info("Received decline in wait for bye response state");
		tryToDeregister();
	}

	@Override
	public void onTimeOut() {
		LOGGER.info("Received timeout in wait for bye response state");
		tryToDeregister();
	}

	@Override
	public void onFailure(ErrorMessage eMessage) {
		LOGGER.info("Received failure in wait for bye response state: " + eMessage.getMessage());
		tryToDeregister();
	}

	@Override
	public int getStateTimeoutMillis() {
		return stateObserver.getSipSettings().getWaitForByeResponseTimeout();
	}

	@Override
	public void onStateTimeout() {
		LOGGER.info("Timeout in wait for bye response state");
		tryToDeregister();
	}
}