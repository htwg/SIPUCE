package de.fhkn.in.uce.sip.ucesip.client;

import javax.sip.ObjectInUseException;
import javax.sip.message.Request;

import de.fhkn.in.uce.sip.core.message.ErrorMessage;
import de.fhkn.in.uce.sip.core.message.IMessage;
import de.fhkn.in.uce.sip.core.message.IResponseMessage;
import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;

class WaitForDerigisterResponseInviteState extends BaseInviteState {

	public WaitForDerigisterResponseInviteState(IInviteStateObserver stateObserver) {
		super(stateObserver);
	}

	@Override
	public void onOk(IMessage okmessage) {
		String method = ((IResponseMessage) okmessage).getRequestMethod();

		if (method.equals(Request.REGISTER) == false) {
			LOGGER.info("Received OK with wrong method in WaitForDerigisterResponseInviteState state: {} method {} ", okmessage.getMessage(), method);
			return;
		}
		tryToShutdown();
	}

	private void tryToShutdown() {
		try {
			stateObserver.getSipManager().shutdown();
		} catch (ObjectInUseException | InterruptedException e) {
			LOGGER.info("Error by receiving Ok in wait for derigister response ", e);
			stateObserver.changeState(new ErrorInviteState(stateObserver, new UCESipException("Error by derigister", e)));
		}
		stateObserver.changeState(new FinishedInviteState(stateObserver));
	}

	@Override
	public void shutdown() {
		// we are somewhere in the callflow and have only to wait until an
		// error comes or we are in the finished state.
	}

	@Override
	public InviteStates getState() {
		return InviteStates.WaitForDerigisterResponse;
	}

	@Override
	public void onDecline() {
		LOGGER.info("Received decline in wait for derigister response state");
		tryToShutdown();
	}

	@Override
	public void onTimeOut() {
		LOGGER.info("Received timeout in wait for derigister response state");
		tryToShutdown();
	}

	@Override
	public void onFailure(ErrorMessage eMessage) {
		LOGGER.info("Received failure in wait for derigister response state: " + eMessage.getMessage());
		tryToShutdown();
	}

	@Override
	public int getStateTimeoutMillis() {
		return stateObserver.getSipSettings().getWaitForDeregisterResponseTimeout();
	}

	@Override
	public void onStateTimeout() {
		LOGGER.info("Timeout in wait for derigister response state");
		tryToShutdown();
	}
}