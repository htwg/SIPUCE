package de.fhkn.in.uce.sip.ucesip.server.call;

import java.text.ParseException;

import javax.sdp.SdpException;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;

import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;
import de.fhkn.in.uce.sip.ucesip.message.IUCESipMessage;

class InitServerCallState extends BaseServerCallState {

	public InitServerCallState(ServerCallStateContext stateContext) {
		super(stateContext);
	}

	@Override
	public ServerCallStates getCurrentState() {
		return ServerCallStates.Init;
	}

	@Override
	public void sendDecline() throws UCESipException {
		try {
			stateContext.changeState(new FinishedServerCallState(stateContext));
			stateContext.getSipManager().sendDecline(stateContext.getServerUser(), stateContext.getClientUser());
		} catch (SipException | ParseException | InvalidArgumentException | SdpException e) {
			throw new UCESipException("Unable to send decline", e);
		}
	}

	@Override
	public void sendOK(final IUCESipMessage okMessage) throws UCESipException {
		try {
			stateContext.changeState(new WaitForByeServerCallState(stateContext));
			stateContext.getSipManager().sendOk(stateContext.getServerUser(), stateContext.getClientUser(), okMessage.serialize());
		} catch (SipException | InvalidArgumentException | ParseException | SdpException e) {
			UCESipException ex = new UCESipException("Unable to send ok", e);
			stateContext.changeState(new ErrorServerCallState(stateContext, ex));
			throw ex;
		}
	}

	@Override
	public void sendQueued() throws UCESipException {
		try {
			stateContext.getSipManager().sendQueued(stateContext.getServerUser(), stateContext.getClientUser());
		} catch (ParseException | SipException | InvalidArgumentException | SdpException e) {
			UCESipException ex = new UCESipException("Unable to send queued", e);
			stateContext.changeState(new ErrorServerCallState(stateContext, ex));
			throw ex;
		}
	}

	@Override
	public void sendRinging() throws UCESipException {
		try {
			stateContext.getSipManager().sendRinging(stateContext.getServerUser(), stateContext.getClientUser());
		} catch (ParseException | SipException | InvalidArgumentException e) {
			UCESipException ex = new UCESipException("Unable to send queued", e);
			stateContext.changeState(new ErrorServerCallState(stateContext, ex));
			throw ex;
		}
	}

}