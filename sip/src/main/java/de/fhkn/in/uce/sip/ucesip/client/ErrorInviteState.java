package de.fhkn.in.uce.sip.ucesip.client;

import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;

class ErrorInviteState extends BaseInviteState {

	private final UCESipException error;
	
	public ErrorInviteState(IInviteStateObserver stateObserver, UCESipException error) {
		super(stateObserver);
		this.error = error;
	}
	
	@Override
	public void shutdown() {
		// we are already in an end state.
	}

	@Override
	public InviteStates getState() {
		return InviteStates.Error;
	}

	public Exception getError() {
		return error;
	}
}