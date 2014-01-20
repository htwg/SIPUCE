package de.fhkn.in.uce.sip.ucesip.client;

class FinishedInviteState extends BaseInviteState {

	public FinishedInviteState(IInviteStateObserver stateObserver) {
		super(stateObserver);
	}

	@Override
	public void shutdown() {
		// we are already in an end state.
	}

	@Override
	public InviteStates getState() {
		return InviteStates.Finished;
	}
}