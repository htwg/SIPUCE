package de.fhkn.in.uce.sip.core.processors;

import java.util.Map;

import javax.sip.ResponseEvent;
import javax.sip.header.UnsupportedHeader;
import javax.sip.message.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.sip.core.ISipManagerListener;
import de.fhkn.in.uce.sip.core.UserCallListNode;
import de.fhkn.in.uce.sip.core.message.ErrorMessage;

/**
 * Handles redirect Responses.
 * <table>
 *  <tr> <th>400</th><td>Bad Request</td></tr>
 *  <tr> <th>401</th><td>Unauthorized</td></tr>
 *  <tr> <th>402</th><td>Payment Required</td></tr>
 *  <tr> <th>403</th><td>Forbidden</td></tr>
 *  <tr> <th>404</th><td>Not Found</td></tr>
 *  <tr> <th>405</th><td>Method Not Allowed</td></tr>
 *  <tr> <th>406</th><td>Not Acceptable</td></tr>
 *  <tr> <th>407</th><td>Proxy Authentication Required</td></tr>
 *  <tr> <th>408</th><td>Request Timeout</td></tr>
 *  <tr> <th>410</th><td>Gone</td></tr>
 *  <tr> <th>413</th><td>Request Entity Too Large</td></tr>
 *  <tr> <th>414</th><td>Request-URI Too Large</td></tr>
 *  <tr> <th>415</th><td>Unsupported Media Type</td></tr>
 *  <tr> <th>416</th><td>Unsupported URI Scheme</td></tr>
 *  <tr> <th>420</th><td>Bad Extension</td></tr>
 *  <tr> <th>421</th><td>Extension Required</td></tr>
 *  <tr> <th>423</th><td>Interval Too Brief</td></tr>
 *  <tr> <th>480</th><td>Temporarily not available</td></tr>
 *  <tr> <th>481</th><td>Call Leg/Transaction Does Not Exist</td></tr>
 *  <tr> <th>482</th><td>Loop Detected</td></tr>
 *  <tr> <th>483</th><td>Too Many Hops</td></tr>
 *  <tr> <th>484</th><td>Address Incomplete</td></tr>
 *  <tr> <th>485</th><td>Ambiguous</td></tr>
 *  <tr> <th>486</th><td>Busy Here</td></tr>
 *  <tr> <th>487</th><td>Request Terminated</td></tr>
 *  <tr> <th>488</th><td>Not Acceptable Here</td></tr>
 *  <tr> <th>491</th><td>Request Pending</td></tr>
 *  <tr> <th>493</th><td>Undecipherable</td></tr>
 * </table>
 * @author Felix Herz
 */
public class ClientErrorResponseProcessor implements IResponseProcessor {

	private static final int REQUEST_TIMED_OUT = 408;
	private static final int UNSUPPORTED_EXTENSION = 420;
	static final Logger LOGGER = LoggerFactory.getLogger(ClientErrorResponseProcessor.class);

	private final Map<String, UserCallListNode> listeners;

	public ClientErrorResponseProcessor(final Map<String, UserCallListNode> ml) {
		listeners = ml;
	}

	@Override
	public void processResponse(
			final ResponseEvent responseEvent,
			final String method) {
		LOGGER.info("Client error response recieved");

		Response response = responseEvent.getResponse();
		int status = response.getStatusCode();
		String phrase = response.getReasonPhrase();
		LOGGER.info(phrase);
		if (status == UNSUPPORTED_EXTENSION) {
			UnsupportedHeader uheader = (UnsupportedHeader)response.getHeader(UnsupportedHeader.NAME);
			LOGGER.info("Unsupported extension: " + uheader.getOptionTag());
			LOGGER.info("Retry without extensions");
		}

		ErrorMessage eMessage = new ErrorMessage(response, method);
		ISipManagerListener listener = listeners.get(eMessage.getFromUser().toString()).getListener();
		if (listener != null) {
			if (status != REQUEST_TIMED_OUT) {
				listener.onFailure(eMessage);
			} else {
				listener.onTimeOut();
			}
		} else {
			LOGGER.warn("No listener registered for: " + eMessage.getFromUser());
		}
	}
}
