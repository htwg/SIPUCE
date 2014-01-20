package de.fhkn.in.uce.sip.ucesip.client;

import java.text.ParseException;
import java.util.TooManyListenersException;
import java.util.concurrent.TimeoutException;

import javax.sdp.SdpException;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.message.Request;

import de.fhkn.in.uce.sip.core.message.ErrorMessage;
import de.fhkn.in.uce.sip.core.message.IMessage;
import de.fhkn.in.uce.sip.core.message.IResponseMessage;
import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;
import de.fhkn.in.uce.sip.ucesip.message.UCESipMessage;

class WaitForInviteResponseInviteState extends BaseInviteState {

	public WaitForInviteResponseInviteState(IInviteStateObserver stateObserver) {
		super(stateObserver);
	}

	@Override
	public void onOk(IMessage okmessage) {

		String method = ((IResponseMessage) okmessage).getRequestMethod();

		if (method.equals(Request.INVITE) == false) {
			LOGGER.info("Received OK with wrong method in WaitForInviteResponse state: {} method: {}", okmessage.getMessage(),
					((IResponseMessage) okmessage).getRequestMethod());
			return;
		}

		// parse String to new OkMessage
		try {
			UCESipMessage okMsg = new UCESipMessage(okmessage.getMessage().substring(2));
			setInviteResponseMessage(okMsg);
		} catch (Exception e) {
			LOGGER.info("Error by receiving Ok in wait for invite response ", e);
			setInviteResponseError(e);
		}

		// from here we can only log errors, because on shutdown we try to
		// shutdown everything and throw no errors
		try {
			stateObserver.getSipManager().sendBye(stateObserver.getFromUser(), stateObserver.getToUser());
		} catch (SipException | InvalidArgumentException | ParseException | SdpException e) {
			LOGGER.info("Error by receiving Ok in wait for invite response ", e);
			stateObserver.changeState(new ErrorInviteState(stateObserver, new UCESipException("Error by derigister", e)));
		}
		stateObserver.changeState(new WaitForByeResponseInviteState(stateObserver));
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
		return InviteStates.WaitForInviteResponse;
	}

	@Override
	public void onDecline() {
		UCESipException e = new UCESipException("User Agent Server declined call");
		setInviteResponseError(e);
		tryToDeregister();
	}

	@Override
	public void onTimeOut() {
		UCESipException e = new UCESipException("Inviting User Agent Server timed out");
		setInviteResponseError(e);
		tryToDeregister();
	}

	@Override
	public void onFailure(ErrorMessage eMessage) {
		UCESipException e = new UCESipException("Inviting User Agent Server failed: " + eMessage.getMessage());
		setInviteResponseError(e);
		tryToDeregister();
	}

	@Override
	public int getStateTimeoutMillis() {
		return stateObserver.getSipSettings().getWaitForInviteResponseTimeout();
	}

	@Override
	public void onStateTimeout() {
		UCESipException e = new UCESipException("Inviting User Agent Server failed", new TimeoutException());
		setInviteResponseError(e);
		tryToDeregister();
	}
}