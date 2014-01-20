package de.fhkn.in.uce.sip.ucesip.server;

import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;

class ErrorServerState extends BaseServerState {

	private final UCESipException error;

	public ErrorServerState(IServerStateContext stateContext, UCESipException error) {
		super(stateContext);
		this.error = error;
	}

	public UCESipException getError() {
		return error;
	}

	@Override
	public ServerStates getCurrentState() {
		return ServerStates.Error;
	}

	@Override
	public void shutdown() {

	}

}