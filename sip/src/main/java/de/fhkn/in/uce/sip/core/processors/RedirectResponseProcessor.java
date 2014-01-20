package de.fhkn.in.uce.sip.core.processors;

import java.util.Map;

import javax.sip.ResponseEvent;
import javax.sip.message.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.sip.core.ISipManagerListener;
import de.fhkn.in.uce.sip.core.UserCallListNode;
import de.fhkn.in.uce.sip.core.message.ErrorMessage;

/**
 * Handles redirect Responses.
 * <table>
 *  <tr> <th>300</th><td>Multiple Choices</td></tr>
 *  <tr> <th>301</th><td>Moved Permanently</td></tr>
 *  <tr> <th>302</th><td>Moved Temporarily</td></tr>
 *  <tr> <th>305</th><td>Use Proxy</td></tr>
 *  <tr> <th>380</th><td>Alternative Service</td></tr>
 * </table>
 * @author Felix Herz
 */
public class RedirectResponseProcessor implements IResponseProcessor {

	static final Logger LOGGER =
			LoggerFactory.getLogger(ClientErrorResponseProcessor.class);
	private final Map<String, UserCallListNode> listeners;

	public RedirectResponseProcessor(final Map<String, UserCallListNode> ml) {
		listeners = ml;
	}

	@Override
	public void processResponse(
			final ResponseEvent responseEvent,
			final String method) {

		Response response = responseEvent.getResponse();

		//TODO UA MUST handle 300 response

		/* TODO Upon receipt of a redirection response (for example, a 301 response
		status code), clients SHOULD use the URI(s) in the Contact header
		field to formulate one or more new requests based on the redirected
		request SEE 8.1.3.4*/

		ErrorMessage eMessage = new ErrorMessage(response, method);
		ISipManagerListener listener = listeners.get(eMessage.getToUser().toString()).getListener();
		if (listener != null) {
			listener.onFailure(eMessage);
		}  else {
			LOGGER.warn("No listener registered for: " + eMessage.getFromUser());
		}
	}

}
