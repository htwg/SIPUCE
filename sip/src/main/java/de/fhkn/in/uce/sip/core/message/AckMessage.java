package de.fhkn.in.uce.sip.core.message;

import javax.sip.message.Request;

public class AckMessage extends AbstractRequestMessage implements IRequestMessage {

	public AckMessage(final Request request) {
		super(request);
	}

}
