package de.fhkn.in.uce.sip.core.message;

import javax.sip.message.Request;


public interface IRequestMessage extends IMessage {

	Request getRawRequest();
}