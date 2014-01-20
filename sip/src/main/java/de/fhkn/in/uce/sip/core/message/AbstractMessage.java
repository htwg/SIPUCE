package de.fhkn.in.uce.sip.core.message;

import javax.sip.header.FromHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import de.fhkn.in.uce.sip.core.SipUser;

public abstract class AbstractMessage implements IMessage {

	private final SipUser fromUser;
	private final SipUser toUser;

	protected AbstractMessage(final Response response) {
		FromHeader from = (FromHeader) response.getHeader(FromHeader.NAME);
		ToHeader to = (ToHeader) response.getHeader(ToHeader.NAME);
		fromUser = SipUser.parseFromHeader(from);
		toUser = SipUser.parseFromHeader(to);
	}

	protected AbstractMessage(final Request request) {
		FromHeader from = (FromHeader) request.getHeader(FromHeader.NAME);
		ToHeader to = (ToHeader) request.getHeader(ToHeader.NAME);
		fromUser = SipUser.parseFromHeader(from);
		toUser = SipUser.parseFromHeader(to);
	}


	@Override
	public SipUser getFromUser() {
		return fromUser;
	}

	@Override
	public SipUser getToUser() {
		return toUser;
	}


}
