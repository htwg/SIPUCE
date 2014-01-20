package de.fhkn.in.uce.sip.ucesip.server.call;

import java.text.ParseException;

import javax.sdp.SdpException;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;

import de.fhkn.in.uce.sip.core.message.IMessage;
import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;

class WaitForByeServerCallState extends BaseServerCallState {

	public WaitForByeServerCallState(ServerCallStateContext stateContext) {
		super(stateContext);
	}

	@Override
	public ServerCallStates getCurrentState() {
		return ServerCallStates.WaitForBye;
	}

	@Override
	public void onBye(IMessage message) {
		try {
			stateContext.getSipManager().sendOk(stateContext.getServerUser(), stateContext.getClientUser());
			stateContext.changeState(new FinishedServerCallState(stateContext));
		} catch (SipException | InvalidArgumentException | ParseException | SdpException e) {
			stateContext.changeState(new ErrorServerCallState(stateContext, new UCESipException("Error sending ok on received bye", e)));
		}
	}

	@Override
	public int getStateTimeoutMillis() {
		return stateContext.getSipSettings().getServerCallWaitForByeTimeout();
	}

	@Override
	public void onStateTimeout() {
		stateContext.changeState(new ErrorServerCallState(stateContext, new UCESipException("Waiting for bye timed out")));
	}

}