package de.fhkn.in.uce.sip.core.message;

import javax.sip.header.ContentLengthHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Response;

import de.fhkn.in.uce.sip.core.SipUser;

public abstract class AbstractResponseMessage extends AbstractMessage implements IResponseMessage {


	private final SipUser fromUser;
	private final SipUser toUser;
	private final Response response;
	private final String requestMethod;

	protected AbstractResponseMessage(final Response response, final String method) {
		super(response);
		this.response = response;
		this.requestMethod = method;
		FromHeader from = (FromHeader) response.getHeader(FromHeader.NAME);
		ToHeader to = (ToHeader) response.getHeader(ToHeader.NAME);
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
	public Response getRawResponse() {
		return response;
	}

	@Override
	public String getMessage() {
		ContentLengthHeader conlengthHeader =
				(ContentLengthHeader) response.getHeader(ContentLengthHeader.NAME);
		if (conlengthHeader != null
				&& conlengthHeader.getContentLength() > 0) {
			return new String(response.getRawContent());
		}
		return null;
	}

	@Override
	public String getRequestMethod() {
		return requestMethod;
	}
}
