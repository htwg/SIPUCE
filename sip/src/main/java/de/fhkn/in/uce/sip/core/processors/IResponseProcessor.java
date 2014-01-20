package de.fhkn.in.uce.sip.core.processors;

import javax.sip.ResponseEvent;

public interface IResponseProcessor {

	void processResponse(ResponseEvent responseEvent,
			String method);

}