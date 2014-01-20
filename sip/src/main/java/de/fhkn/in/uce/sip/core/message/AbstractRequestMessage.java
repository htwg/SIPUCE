package de.fhkn.in.uce.sip.core.message;

import javax.sip.header.ContentLengthHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;

import de.fhkn.in.uce.sip.core.SipUser;

public abstract class AbstractRequestMessage extends AbstractMessage implements IRequestMessage {


	private final SipUser fromUser;
	private final SipUser toUser;
	private final Request request;

	protected AbstractRequestMessage(final Request request) {
		super(request);
		this.request = request;
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

	@Override
	public Request getRawRequest() {
		return request;
	}

	@Override
	public String getMessage() {
		ContentLengthHeader conlengthHeader =
				(ContentLengthHeader) request.getHeader(ContentLengthHeader.NAME);
		if (conlengthHeader != null
				&& conlengthHeader.getContentLength() > 0) {
			return new String(request.getRawContent());
		}
		return null;
	}

}
