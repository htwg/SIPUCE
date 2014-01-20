package de.fhkn.in.uce.sip.ucesip.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.sip.core.message.ErrorMessage;
import de.fhkn.in.uce.sip.core.message.IMessage;
import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;
import de.fhkn.in.uce.sip.ucesip.message.IUCESipMessage;
import de.fhkn.in.uce.sip.ucesip.message.UCESipMessage;

abstract class BaseInviteState implements IInviteState {

	static final Logger LOGGER = LoggerFactory.getLogger(BaseInviteState.class);

	protected final IInviteStateObserver stateObserver;

	public BaseInviteState(IInviteStateObserver stateObserver) {
		this.stateObserver = stateObserver;
	}

	void setInviteResponseError(Exception error) {
		stateObserver.setInviteResponseError(error);
	}

	void setInviteResponseMessage(UCESipMessage msg) {
		stateObserver.setInviteResponseMessage(msg);
	}

	@Override
	public void onOk(IMessage okmessage) {
		LOGGER.info("Received ok in " + getState().toString() + " state: " + okmessage.getMessage());
	}

	@Override
	public void onDecline() {
		LOGGER.info("Received decline in " + getState().toString() + " state");
	}

	@Override
	public void onTimeOut() {
		LOGGER.info("Received timeout in " + getState().toString() + " state");
	}

	@Override
	public void onFailure(ErrorMessage eMessage) {
		LOGGER.info("Received failure in " + getState().toString() + " state: " + eMessage.getMessage());
	}
	
	@Override
	public void inviteUAS(IUCESipMessage message) throws UCESipException {
		throw new IllegalStateException("We want to start invite, why we are in state " + getState().toString());
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + getState().hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof BaseInviteState)) {
			return false;
		}
		BaseInviteState other = (BaseInviteState) obj;
		return other.getState().equals(this.getState());
	}
	
}