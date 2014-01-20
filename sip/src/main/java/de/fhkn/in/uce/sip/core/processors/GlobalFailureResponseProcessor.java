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
 *  <tr> <th>600</th><td>Busy Everywhere</td></tr>
 *  <tr> <th>603</th><td>Decline</td></tr>
 *  <tr> <th>604</th><td>Does not exist anywhere</td></tr>
 *  <tr> <th>606</th><td>Not Acceptable</td></tr>
 * </table>
 * @author Felix Herz
 */
public class GlobalFailureResponseProcessor implements IResponseProcessor {

	private static final int DECLINED = 603;
	static final Logger LOGGER = LoggerFactory.getLogger(GlobalFailureResponseProcessor.class);
	private final Map<String, UserCallListNode> listeners;

	public GlobalFailureResponseProcessor(final Map<String, UserCallListNode> ml) {
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

		LOGGER.info("Global Failure response received.");
		ErrorMessage eMessage = new ErrorMessage(response, method);
		ISipManagerListener listener = listeners.get(eMessage.getFromUser().toString()).getListener();
		if (listener != null) {
			if (response.getStatusCode() == DECLINED) {
				listener.onDecline();
			} else {
				listener.onFailure(eMessage);
			}
		} else {
			LOGGER.warn("No listener registered for: " + eMessage.getFromUser());
		}
	}

}
