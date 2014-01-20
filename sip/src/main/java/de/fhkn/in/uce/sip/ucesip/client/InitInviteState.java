package de.fhkn.in.uce.sip.ucesip.client;

import java.text.ParseException;

import javax.sdp.SdpException;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;

import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;
import de.fhkn.in.uce.sip.ucesip.message.IUCESipMessage;

class InitInviteState extends BaseInviteState {

	public InitInviteState(IInviteStateObserver stateObserver) {
		super(stateObserver);
	}

	@Override
	public void shutdown() {
		stateObserver.changeState(new FinishedInviteState(stateObserver));
	}

	@Override
	public InviteStates getState() {
		return InviteStates.Init;
	}
	
	@Override
	public void inviteUAS(IUCESipMessage message) throws UCESipException {
		try {
			stateObserver.getSipManager().sendInvite(stateObserver.getFromUser(), stateObserver.getToUser(), message.serialize());
		} catch (ParseException | InvalidArgumentException | SipException | SdpException e) {
			throw new UCESipException("Error on sending ivite", e);
		}
		stateObserver.changeState(new WaitForInviteResponseInviteState(stateObserver));
	}

}