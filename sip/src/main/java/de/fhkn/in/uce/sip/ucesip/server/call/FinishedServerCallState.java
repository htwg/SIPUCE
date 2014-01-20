package de.fhkn.in.uce.sip.ucesip.server.call;

class FinishedServerCallState extends BaseServerCallState {

	public FinishedServerCallState(ServerCallStateContext stateContext) {
		super(stateContext);
	}

	@Override
	public ServerCallStates getCurrentState() {
		return ServerCallStates.Finished;
	}
	
	@Override
	public boolean isFinished() {
		return true;
	}
}