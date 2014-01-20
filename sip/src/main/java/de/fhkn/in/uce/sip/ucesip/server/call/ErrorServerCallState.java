package de.fhkn.in.uce.sip.ucesip.server.call;

import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;

class ErrorServerCallState extends BaseServerCallState {

	public ErrorServerCallState(ServerCallStateContext stateContext) {
		super(stateContext);
	}

	public ErrorServerCallState(ServerCallStateContext stateContext, UCESipException uceSipException) {
		super(stateContext);
		LOGGER.info("Call state is in error client user: " + stateContext.getClientUser().toString(), uceSipException);
	}

	@Override
	public ServerCallStates getCurrentState() {
		return ServerCallStates.Error;
	}
	
	@Override
	public boolean isFinished() {
		return true;
	}
}