package de.fhkn.in.uce.sip.core.processors;

import java.util.Map;

import javax.sip.ResponseEvent;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.sip.core.Call;
import de.fhkn.in.uce.sip.core.ISipManagerListener;
import de.fhkn.in.uce.sip.core.UserCallListNode;
import de.fhkn.in.uce.sip.core.message.OkMessage;


/**
 * Handles informational Responses.
 * UA MUST handle 180  RINGING and 100 TRYING response
 * <table>
 *  <tr> <th>200</th><td>OK</td></tr>
 *  <tr> <th>202</th><td>Accepted (non RFC 3261, deprecated)</td></tr>
 *  <tr> <th>204</th><td>No Notification (non RFC 3261, RFC 5839)</td></tr>
 * </table>
 * @author Felix Herz
 */
public class SuccessResponseProcessor  implements IResponseProcessor {

	private static final Logger LOGGER =
			LoggerFactory.getLogger(SuccessResponseProcessor.class);
	private final Map<String, UserCallListNode> listeners;

	public SuccessResponseProcessor(final Map<String, UserCallListNode> ml) {
		listeners = ml;
	}

	@Override
	public void processResponse(
			final ResponseEvent responseEvent,
			final String method) {

		Response response = responseEvent.getResponse();

		OkMessage okmessage = new OkMessage(response, method);
		UserCallListNode node = listeners.get(okmessage.getFromUser().toString());
		ISipManagerListener listener = node.getListener();
		if (method.equals(Request.INVITE)) {
			if (listener != null) {
				Call call = node.getCall(okmessage.getToUser());
				call.setDialog(responseEvent.getDialog());
			} else {
				LOGGER.warn("No listener registered for: " + okmessage.getFromUser());
			}
		}
		if (listener != null) {
			listener.onOk(okmessage);
		} else {
			LOGGER.warn("No listener registered for: " + okmessage.getFromUser());
		}
	}
}
