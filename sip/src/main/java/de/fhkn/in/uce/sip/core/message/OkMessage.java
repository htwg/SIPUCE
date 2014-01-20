package de.fhkn.in.uce.sip.core.message;

import javax.sip.message.Response;

public class OkMessage extends AbstractResponseMessage {

	public OkMessage(final Response response, final String method) {
		super(response, method);
	}
}
