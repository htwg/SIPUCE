package de.fhkn.in.uce.sip.ucesip.server;

import java.text.ParseException;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;

import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;

class InitServerState extends BaseServerState {

	public InitServerState(IServerStateContext stateContext) {
		super(stateContext);
	}

	@Override
	public ServerStates getCurrentState() {
		return ServerStates.Init;
	}

	@Override
	public void shutdown() {
		stateContext.changeState(new FinishedServerState(stateContext));
	}

	@Override
	public void startRegister() throws UCESipException {
		try {
			stateContext.getSipManager().register(stateContext.getServerUser());
		} catch (InvalidArgumentException | ParseException | SipException e) {
			UCESipException ex = new UCESipException("Error by sip register", e);
			stateContext.changeState(new ErrorServerState(stateContext, ex));
			throw ex;
		}
		stateContext.changeState(new WaitForRegisterResponseServerState(stateContext));
	}

}