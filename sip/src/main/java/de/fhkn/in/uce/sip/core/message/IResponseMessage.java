package de.fhkn.in.uce.sip.core.message;

import javax.sip.message.Response;

public interface IResponseMessage extends IMessage {

	String getRequestMethod();

	Response getRawResponse();

}