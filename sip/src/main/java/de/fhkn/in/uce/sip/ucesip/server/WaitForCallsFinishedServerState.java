package de.fhkn.in.uce.sip.ucesip.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import de.fhkn.in.uce.sip.core.SipUser;
import de.fhkn.in.uce.sip.core.message.IMessage;
import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;
import de.fhkn.in.uce.sip.ucesip.server.call.UCESipServerCall;

class WaitForCallsFinishedServerState extends BaseServerState {

	public WaitForCallsFinishedServerState(IServerStateContext stateContext) {
		super(stateContext);
	}

	@Override
	public void onInit() {
		checkRemainingCalls();
	}
	
	private void checkRemainingCalls() {
		List<String> tRemoveKeyList = new ArrayList<>();
		for (Entry<String, UCESipServerCall> i : stateContext.getServerCallMap().entrySet()) {
			if (i.getValue().isFinished()) {
				tRemoveKeyList.add(i.getKey());
			}
		}
		// delete in map
		for (String string : tRemoveKeyList) {
			stateContext.getServerCallMap().remove(string);
		}
		if(stateContext.getServerCallMap().isEmpty()) {
			changeDregister();
		}
	}

	@Override
	public ServerStates getCurrentState() {
		return ServerStates.WaitForCallsFinished;
	}

	@Override
	public void shutdown() {

	}

	@Override
	public void onBye(IMessage message) {
		SipUser fromUser = message.getFromUser();
		UCESipServerCall tCall = stateContext.getServerCallMap().get(fromUser.toString());
		if (tCall == null) {
			LOGGER.info("Received bye from unknown user " + message.getMessage());
			return;
		}
		tCall.onBye(message);
		checkRemainingCalls();
	}

	@Override
	public int getStateTimeoutMillis() {
		return stateContext.getSipSettings().getWaitForCallsFinishedTimeout();
	}

	@Override
	public void onStateTimeout() {
		LOGGER.info("waiting for all calls to finish timed out");
		changeDregister();
	}
	
	private void changeDregister() {
		if (super.tryToDerigister() == false) {
			stateContext.changeState(new ErrorServerState(stateContext, new UCESipException("Error send derigister")));
			return;
		}
		stateContext.changeState(new WaitForDeregisterResponseServerState(stateContext));
	}

}