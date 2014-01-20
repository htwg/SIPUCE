package de.fhkn.in.uce.sip.ucesip.server;

import de.fhkn.in.uce.sip.core.SipUser;
import de.fhkn.in.uce.sip.core.message.IMessage;
import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;
import de.fhkn.in.uce.sip.ucesip.message.IUCESipMessage;
import de.fhkn.in.uce.sip.ucesip.message.UCESipMessage;
import de.fhkn.in.uce.sip.ucesip.server.call.UCESipServerCall;

abstract class BaseAllowInviteHandleServerState extends BaseServerState {

	public BaseAllowInviteHandleServerState(IServerStateContext stateContext) {
		super(stateContext);
	}

	@Override
	public void onInvite(IMessage inviteMessage) {
		SipUser clientUser = inviteMessage.getFromUser();

		if (stateContext.getServerCallMap().containsKey(clientUser.toString())) {
			LOGGER.info("Ivite already received from user: " + clientUser.toString());
			return;
		}

		IUCESipMessage inviteMsg = new UCESipMessage(inviteMessage.getMessage().substring(2));

		LOGGER.trace("BaseAllowInviteHandleServerState onInvite creating UCESipServerCall");
		UCESipServerCall tServerCall = new UCESipServerCall(inviteMsg, stateContext.getServerUser(), clientUser, stateContext.getSipManager(),
				stateContext.getSipSettings());
		
		stateContext.getServerCallMap().put(clientUser.toString(), tServerCall);

		boolean tInserted = stateContext.getCallQueue().offer(tServerCall);
		if (tInserted == false) {
			// call queue is full, send decline
			try {
				LOGGER.info("Server declining call because callQueue is full");
				tServerCall.sendDecline();
			} catch (UCESipException e) {
				LOGGER.error("Cannot send decline on new received invite", e);
				return;
			}
			return;
		}
		stateContext.onNewCallReceived(tServerCall, stateContext.getCallQueue().size());
	}

	@Override
	public void onBye(IMessage message) {
		SipUser fromUser = message.getFromUser();
		UCESipServerCall tCall = stateContext.getServerCallMap().get(fromUser.toString());
		if (tCall == null) {
			LOGGER.info("Received bye from not invited user " + message.getMessage());
			return;
		}
		tCall.onBye(message);
	}

	@Override
	public UCESipServerCall pollCall() {
		return stateContext.getCallQueue().poll();
	}

	void declineCurrentCalls() {
		for (UCESipServerCall iCall : stateContext.getCallQueue()) {
			try {
				LOGGER.trace("sending decline because of shutdown request to " + iCall.getClientUser().toString());
				iCall.sendDecline();
			} catch (UCESipException e) {
				LOGGER.info("Sending decline on element of call queue failed", e);
			}
		}
	}
	

}
