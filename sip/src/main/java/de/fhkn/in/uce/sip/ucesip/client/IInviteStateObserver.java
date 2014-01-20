package de.fhkn.in.uce.sip.ucesip.client;

import de.fhkn.in.uce.sip.core.ISipManager;
import de.fhkn.in.uce.sip.core.SipUser;
import de.fhkn.in.uce.sip.ucesip.message.UCESipMessage;
import de.fhkn.in.uce.sip.ucesip.settings.UCESipClientSettings;

interface IInviteStateObserver {
	void setInviteResponseError(Exception error);
	void setInviteResponseMessage(UCESipMessage msg);
	SipUser getToUser();
	SipUser getFromUser();
	UCESipClientSettings getSipSettings();
	ISipManager getSipManager();
	void changeState(IInviteState state);
}