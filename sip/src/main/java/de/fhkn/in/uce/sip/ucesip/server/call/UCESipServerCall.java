package de.fhkn.in.uce.sip.ucesip.server.call;

import de.fhkn.in.uce.sip.core.ISipManager;
import de.fhkn.in.uce.sip.core.SipUser;
import de.fhkn.in.uce.sip.core.message.IMessage;
import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;
import de.fhkn.in.uce.sip.ucesip.message.IUCESipMessage;
import de.fhkn.in.uce.sip.ucesip.settings.UCESipServerSettings;

public class UCESipServerCall {

	private final IUCESipMessage sipInviteMessage;
	private final SipUser clientUser;
	private final ServerCallStateContext serverCallStateContext;

	public UCESipServerCall(IUCESipMessage sipMessage, SipUser serverUser, SipUser clientUser, ISipManager sipManager,
			UCESipServerSettings sipSettings) {
		this.sipInviteMessage = sipMessage;
		this.clientUser = clientUser;
		serverCallStateContext = new ServerCallStateContext(sipManager, serverUser, clientUser, sipSettings);
	}

	public void onBye(IMessage message) {
		serverCallStateContext.onBye(message);
	}

	public void sendDecline() throws UCESipException {
		serverCallStateContext.sendDecline();
	}

	public void sendOK(final IUCESipMessage okMessage) throws UCESipException {
		serverCallStateContext.sendOK(okMessage);
	}

	public void sendQueued() throws UCESipException {
		serverCallStateContext.sendQueued();
	}

	public void sendRinging() throws UCESipException {
		serverCallStateContext.sendRinging();
	}

	public boolean isFinished() {
		return serverCallStateContext.isFinished();
	}

	public IUCESipMessage getSipInviteMessage() {
		return sipInviteMessage;
	}

	public SipUser getClientUser() {
		return clientUser;
	}

}