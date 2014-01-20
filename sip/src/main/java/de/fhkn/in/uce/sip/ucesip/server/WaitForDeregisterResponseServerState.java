package de.fhkn.in.uce.sip.ucesip.server;

import java.text.ParseException;

import javax.sdp.SdpException;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.message.Request;

import de.fhkn.in.uce.sip.core.SipUser;
import de.fhkn.in.uce.sip.core.message.IMessage;
import de.fhkn.in.uce.sip.core.message.IResponseMessage;
import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;
import de.fhkn.in.uce.sip.ucesip.server.call.UCESipServerCall;

class WaitForDeregisterResponseServerState extends BaseServerState {

	public WaitForDeregisterResponseServerState(IServerStateContext stateContext) {
		super(stateContext);
	}

	@Override
	public ServerStates getCurrentState() {
		return ServerStates.WaitForDeregisterResponse;
	}

	@Override
	public void shutdown() {

	}

	@Override
	public void onOk(IMessage okmessage) {
		IResponseMessage message = (IResponseMessage) okmessage;

		if (message.getRequestMethod().equals(Request.REGISTER) == false) {
			LOGGER.info("Received OK with wrong method in WaitForRegisterResponse state: " + okmessage.getMessage());
			return;
		}
		stateContext.changeState(new FinishedServerState(stateContext));
	}

	@Override
	public void onInvite(IMessage inviteMessage) {
		// decline new invites until derigistered
		try {
			stateContext.getSipManager().sendDecline(stateContext.getServerUser(), inviteMessage.getFromUser());
		} catch (SipException | InvalidArgumentException | ParseException | SdpException e) {
			LOGGER.error("Unable to send decline on invite in wait for derigister state", e);
		}
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
	}

	@Override
	public int getStateTimeoutMillis() {
		return stateContext.getSipSettings().getWaitForDeregisterResponseTimeout();
	}

	@Override
	public void onStateTimeout() {
		stateContext.changeState(new ErrorServerState(stateContext, new UCESipException("Timeout on wait for derigister response")));
	}

}