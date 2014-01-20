package de.fhkn.in.uce.sip.ucesip.server;

class RegisteredServerState extends BaseAllowInviteHandleServerState {

	public RegisteredServerState(IServerStateContext stateContext) {
		super(stateContext);
	}

	@Override
	public void onInit() {
		if (stateContext.isShutdown()) {
			// we did not start the refresh thread
			shutdownStateChange();
			return;
		}
		stateContext.getRegisterRefreshThread().start();
	}

	@Override
	public ServerStates getCurrentState() {
		return ServerStates.Registered;
	}

	@Override
	public void shutdown() {
		stateContext.getRegisterRefreshThread().terminate();
		stateContext.getRegisterRefreshThread().interrupt();
		try {
			stateContext.getRegisterRefreshThread().join();
		} catch (InterruptedException e) {
			LOGGER.info("Joining register refresh thread interrupted", e);
		}
		shutdownStateChange();
	}
	
	private void shutdownStateChange() {
		declineCurrentCalls();
		stateContext.changeState(new WaitForCallsFinishedServerState(stateContext));
	}

}