package de.fhkn.in.uce.sip.ucesip.server.call;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.sip.core.message.IMessage;
import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;
import de.fhkn.in.uce.sip.ucesip.message.IUCESipMessage;

abstract class BaseServerCallState implements IServerCallState {

	static final Logger LOGGER = LoggerFactory.getLogger(BaseServerCallState.class);

	protected final ServerCallStateContext stateContext;

	public BaseServerCallState(ServerCallStateContext stateContext) {
		this.stateContext = stateContext;
	}

	@Override
	public void onBye(IMessage message) {
		LOGGER.info("Received bye in " + getCurrentState().toString() + " state");
	}

	@Override
	public void sendDecline() throws UCESipException {
		throw new IllegalStateException("No handler for sendDecline in state " + getCurrentState().toString());
	}

	@Override
	public void sendOK(IUCESipMessage okMessage) throws UCESipException {
		throw new IllegalStateException("No handler for sendOK in state " + getCurrentState().toString());
	}

	@Override
	public void sendQueued() throws UCESipException {
		throw new IllegalStateException("No handler for sendQueued in state " + getCurrentState().toString());
	}

	@Override
	public void sendRinging() throws UCESipException {
		throw new IllegalStateException("No handler for sendRinging in state " + getCurrentState().toString());
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
	public boolean isFinished() {
		return false;
	}
}