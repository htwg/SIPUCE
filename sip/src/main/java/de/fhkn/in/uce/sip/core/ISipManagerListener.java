package de.fhkn.in.uce.sip.core;

import de.fhkn.in.uce.sip.core.message.ErrorMessage;
import de.fhkn.in.uce.sip.core.message.IMessage;


public interface ISipManagerListener {

	void onInvite(IMessage inviteMessage);

	void onRinging(IMessage ringingMessage);

	void onOk(IMessage okmessage);

	void onAck(IMessage message);

	void onBye(IMessage message);

	void onTimeOut();

	void onDecline();

	void onFailure(ErrorMessage eMessage);
}
