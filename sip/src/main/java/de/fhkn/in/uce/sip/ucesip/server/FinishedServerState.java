package de.fhkn.in.uce.sip.ucesip.server;

class FinishedServerState extends BaseServerState {

	public FinishedServerState(IServerStateContext stateContext) {
		super(stateContext);
	}

	@Override
	public ServerStates getCurrentState() {
		return ServerStates.Finished;
	}

	@Override
	public void shutdown() {

	}

}