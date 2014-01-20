package de.fhkn.in.uce.sip.ucesip.server;

import java.text.ParseException;
import java.util.TooManyListenersException;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.sip.core.message.ErrorMessage;
import de.fhkn.in.uce.sip.core.message.IMessage;
import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;
import de.fhkn.in.uce.sip.ucesip.server.call.UCESipServerCall;

abstract class BaseServerState implements IServerState {

	static final Logger LOGGER = LoggerFactory.getLogger(BaseServerState.class);

	protected final IServerStateContext stateContext;

	public BaseServerState(IServerStateContext stateContext) {
		this.stateContext = stateContext;
	}

	@Override
	public void onBye(IMessage message) {
		LOGGER.info("Received bye in " + getCurrentState().toString() + " state");
	}

	@Override
	public void onFailure(ErrorMessage eMessage) {
		LOGGER.info("Received failure in " + getCurrentState().toString() + " state");
	}

	@Override
	public void onOk(IMessage okmessage) {
		LOGGER.info("Received ok in " + getCurrentState().toString() + " state");
	}

	@Override
	public void onTimeOut() {
		LOGGER.info("Received timeout in " + getCurrentState().toString() + " state");
	}

	@Override
	public void onInvite(IMessage inviteMessage) {
		LOGGER.info("Received invite in " + getCurrentState().toString() + " state");
	}
	
	@Override
	public void onInit() {
		// do nothing as default
	}

	@Override
	public int getStateTimeoutMillis() {
		// default timeout is infinity.
		return 0;
	}

	@Override
	public void onStateTimeout() {
		// default timeout is infinity.
		assert false : "must not happen, it was assumed that this state has a infinity timeout";
	}

	@Override
	public UCESipServerCall pollCall() {
		throw new IllegalStateException("No handler for takeCall in state " + getCurrentState().toString());
	}

	@Override
	public void startRegister() throws UCESipException {
		throw new IllegalStateException("No handler for startRegister in state " + getCurrentState().toString());
	}
	
	boolean tryToDerigister() {
		try {
			stateContext.getSipManager().deregister(stateContext.getServerUser());
		} catch (InvalidArgumentException | TooManyListenersException | ParseException | SipException | InterruptedException e) {
			LOGGER.info("deregister error", e);
			return false;
		}
		return true;
	}

}