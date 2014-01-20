package de.fhkn.in.uce.sip.core.processors;

import javax.sip.ResponseEvent;
import javax.sip.message.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles informational Responses.
 * UA MUST handle 180  RINGING and 100 TRYING response
 * <table>
 *  <tr> <th>100</th><td>Trying</td></tr>
 *  <tr> <th>180</th><td>Ringing</td></tr>
 *  <tr> <th>181</th><td>Call Is Being Forwarded</td></tr>
 *  <tr> <th>182</th><td>Queued</td></tr>
 *  <tr> <th>183</th><td>Session Progress</td></tr>
 *  <tr> <th>199</th><td>Early Dialog Terminated (non RFC 3261)</td></tr>
 * </table>
 * @author Felix Herz
 */
public class ProvisionalResponseProcessor  implements IResponseProcessor {

	static final Logger LOGGER =
			LoggerFactory.getLogger(ProvisionalResponseProcessor.class);


	public ProvisionalResponseProcessor() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processResponse(
			final ResponseEvent responseEvent,
			final String method) {

		Response response = responseEvent.getResponse();
		String reason = response.getReasonPhrase();
		LOGGER.info(reason);
	}
}
