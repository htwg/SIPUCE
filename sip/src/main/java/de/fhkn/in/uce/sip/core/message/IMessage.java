package de.fhkn.in.uce.sip.core.message;

import de.fhkn.in.uce.sip.core.SipUser;

public interface IMessage {

	SipUser getFromUser();

	SipUser getToUser();

	String getMessage();


}