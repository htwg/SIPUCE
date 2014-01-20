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
 *  <tr> <th>500</th><td>Internal Server Error</td></tr>
 *  <tr> <th>501</th><td>Not Implemented</td></tr>
 *  <tr> <th>502</th><td>Bad Gateway</td></tr>
 *  <tr> <th>503</th><td>Service Unavailable</td></tr>
 *  <tr> <th>504</th><td>Server Time-out</td></tr>
 *  <tr> <th>505</th><td>SIP Version not supported</td></tr>
 *  <tr> <th>313</th><td>Message Too Large</td></tr>
 * </table>
 * @author Felix Herz
 */
public class ServerErrorResponseProcessor implements IResponseProcessor {

	static final Logger LOGGER =
			LoggerFactory.getLogger(ServerErrorResponseProcessor.class);
	private final Map<String, UserCallListNode> listeners;

	public ServerErrorResponseProcessor(final Map<String, UserCallListNode> ml) {
		listeners = ml;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processResponse(
			final ResponseEvent responseEvent,
			final String method) {

		Response response = responseEvent.getResponse();

		LOGGER.info("Server error response received.");
		String phrase = response.getReasonPhrase();
		LOGGER.info(phrase);
		ErrorMessage eMessage = new ErrorMessage(response, method);
		ISipManagerListener listener = listeners.get(eMessage.getToUser().toString()).getListener();
		if (listener != null) {
			listener.onFailure(eMessage);
		} else {
			LOGGER.warn("No listener registered for: " + eMessage.getFromUser());
		}
	}
}
