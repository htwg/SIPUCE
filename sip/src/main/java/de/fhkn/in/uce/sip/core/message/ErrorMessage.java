package de.fhkn.in.uce.sip.core.message;

import javax.sip.message.Response;

public class ErrorMessage extends AbstractResponseMessage {

	public ErrorMessage(final Response response, final String method) {
		super(response, method);
	}

	public int getStatus() {
		return getRawResponse().getStatusCode();
	}

	@Override
	public String getMessage() {

		Response r = getRawResponse();
		return r.getReasonPhrase();
	}
}
