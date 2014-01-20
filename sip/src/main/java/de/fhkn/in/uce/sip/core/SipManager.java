package de.fhkn.in.uce.sip.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.ParseException;
import java.util.Map;
import java.util.TooManyListenersException;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.sdp.SdpException;
import javax.sdp.SdpParseException;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.SipListener;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.TransportNotSupportedException;
import javax.sip.header.CSeqHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.sip.core.message.AckMessage;
import de.fhkn.in.uce.sip.core.message.ByeMessage;
import de.fhkn.in.uce.sip.core.message.InviteMessage;
import de.fhkn.in.uce.sip.core.processors.ClientErrorResponseProcessor;
import de.fhkn.in.uce.sip.core.processors.GlobalFailureResponseProcessor;
import de.fhkn.in.uce.sip.core.processors.IResponseProcessor;
import de.fhkn.in.uce.sip.core.processors.ProvisionalResponseProcessor;
import de.fhkn.in.uce.sip.core.processors.RedirectResponseProcessor;
import de.fhkn.in.uce.sip.core.processors.ServerErrorResponseProcessor;
import de.fhkn.in.uce.sip.core.processors.SuccessResponseProcessor;

/**
 * 
 * @author feherz
 * 
 */
public class SipManager implements SipListener, ISipManager {

	private static final int RESPONSE_CLASSES = 100;

	private static final int GLOBAL_FAILURE = 6;

	private static final int SERVER_ERROR = 5;

	private static final int CLIENT_ERROR = 4;

	private static final int REDIRECT = 3;

	private static final int SUCCESS = 2;

	private static final int PROVISIONAL = 1;

	private SipMessageHandler sipMessageHandler = null;

	private final ConcurrentMap<String, UserCallListNode> listeners = new ConcurrentHashMap<>();

	static final Logger LOGGER = LoggerFactory.getLogger(SipManager.class);

	private final Map<Integer, IResponseProcessor> responseProcessors;

	private volatile boolean isShutdown;

	//To detect last request
	private Request lastRequest;

	/**
	 * Creates and starts the SipStack.
	 * 
	 * @param privateAddress
	 *            - The <b>local IP:Port</b> combination
	 * @param preferences
	 * @param instance
	 * @throws PeerUnavailableException
	 * @throws TooManyListenersException
	 * @throws ObjectInUseException
	 * @throws TransportNotSupportedException
	 * @throws InvalidArgumentException
	 * @throws IOException
	 */
	public SipManager(final SipPreferences preferences,
			final SipUAInstance instance) throws PeerUnavailableException,
			TransportNotSupportedException, InvalidArgumentException,
			ObjectInUseException, TooManyListenersException, IOException {

		if (preferences.getPrivateAddress() == null) {
			@SuppressWarnings("resource")
			Socket s = new Socket();
			s.connect(preferences.getSipProxyAddress());

			InetSocketAddress privateAddress = new InetSocketAddress(
					s.getLocalAddress(),
					s.getLocalPort());
			preferences.setPrivateAddress(privateAddress);
			LOGGER.debug(privateAddress.toString());
		}

		sipMessageHandler = SipMessageHandler.createInstance(preferences,
				instance);
		sipMessageHandler.getSipProvider().addSipListener(this);

		responseProcessors = new TreeMap<>();
		responseProcessors.put(PROVISIONAL, new ProvisionalResponseProcessor());
		responseProcessors.put(SUCCESS, new SuccessResponseProcessor(listeners));
		responseProcessors.put(REDIRECT, new RedirectResponseProcessor(listeners));
		responseProcessors.put(CLIENT_ERROR, new ClientErrorResponseProcessor(listeners));
		responseProcessors.put(SERVER_ERROR, new ServerErrorResponseProcessor(listeners));
		responseProcessors.put(GLOBAL_FAILURE, new GlobalFailureResponseProcessor(listeners));
	}

	/** {@inheritDoc} */
	@Override
	public void processRequest(final RequestEvent requestEvent) {
		try {
			Request request = requestEvent.getRequest();
			String method = ((CSeqHeader) request.getHeader(CSeqHeader.NAME))
					.getMethod();

			printRequestReceived(request);

			if (lastRequest != null && request.equals(lastRequest)) {
				LOGGER.warn("DUPLICATED REQUEST");
				return;
			}
			lastRequest = request;


			if (method.equals(Request.INVITE)) {
				try {
					processInvite(requestEvent);
				} catch (SdpParseException | ParseException | SipException
						| InvalidArgumentException e) {
					LOGGER.error(e.toString());
				}
			} else if (request.getMethod().equals(Request.ACK)) {
				processAck(requestEvent);
			} else if (request.getMethod().equals(Request.BYE)) {
				try {
					processBye(requestEvent);
				} catch (ParseException | InvalidArgumentException | SipException e) {
					LOGGER.error(e.toString());
				}
				reset();
			} else if (request.getMethod().equals(Request.CANCEL)) {
				processCancel(requestEvent);
				reset();
			} else {
				// TODO: not implemented request processing: I think we should
				// response with somewhat....
				LOGGER.warn("Unkown Request: {}", method);
			}
		} catch (Exception ex) {
			LOGGER.error(ex.toString());
		}
	}

	@Override
	public void processResponse(final ResponseEvent responseEvent) {
		try {
			Response response = responseEvent.getResponse();
			int status = response.getStatusCode();
			LOGGER.info("\n------\nRESPONSE RECEIVED\n" + "Reason Phrase:\t"
					+ response.getReasonPhrase() + "\n------");
			LOGGER.trace("\n" + response + "\n");

			String method = ((CSeqHeader) response.getHeader(CSeqHeader.NAME)).getMethod();

			LOGGER.debug("Status Code:\t\t\t" + status);
			LOGGER.debug("Related Request of Response:\t" + method);

			ViaHeader viaheader = (ViaHeader) response.getHeader(ViaHeader.NAME);
			int port = viaheader.getRPort();
			String ip = viaheader.getReceived();
			if (ip != null && port != -1) {
				LOGGER.debug("Got Endpoint to Sip Server from ViaHeader: " + ip
						+ ":" + port);
				InetSocketAddress sipServerEndpoint = new InetSocketAddress(ip,
						port);
				sipMessageHandler.getPreferences().setPublicAddress(
						sipServerEndpoint);
			}

			/*
			 * TODO If more than one Via header field value is present in a
			 * response, the UAC SHOULD discard the message.
			 */

			IResponseProcessor processor = responseProcessors.get(status / RESPONSE_CLASSES);
			processor.processResponse(responseEvent, method);

		} catch (Exception ex) {
			LOGGER.error(ex.toString());
		}
	}

	@Override
	public void sendInvite(final SipUser from, final SipUser to)
			throws ParseException, InvalidArgumentException, SipException, SdpException {
		Call call = sipMessageHandler.sendInvite(from, to, null);
		listeners.get(from.toString()).addCall(to, call);
	}

	@Override
	public void sendInvite(final SipUser from, final SipUser to, final String message)
			throws ParseException, InvalidArgumentException, SipException,
			SdpException {
		Call call = sipMessageHandler.sendInvite(from, to, message);
		listeners.get(from.toString()).addCall(to, call);
	}

	private void processCancel(final RequestEvent requestEvent) {

	}

	private void processBye(final RequestEvent requestEvent)
			throws ParseException, InvalidArgumentException, SipException {
		ByeMessage message = new ByeMessage(requestEvent.getRequest());

		UserCallListNode node = listeners.get(message.getToUser().toString());
		if (node == null) {
			return;
		}

		ISipManagerListener listener = node.getListener();
		if (listener != null) {
			Call call = node.getCall(message.getFromUser());
			call.setServerTransaction(requestEvent.getServerTransaction());
			call.setCurrentRequest(message);
			listener.onBye(message);
		} else {
			LOGGER.warn("No listener registered for: {}", message.getFromUser());
			//TODO if there is no listener found, there can't be a call -> sendnot found direct on the serveTransaction
		}
	}

	private void processAck(final RequestEvent requestEvent) {
		AckMessage message = new AckMessage(requestEvent.getRequest());

		UserCallListNode node = listeners.get(message.getToUser().toString());
		ISipManagerListener listener = node.getListener();

		if (listener != null) {
			Call call = node.getCall(message.getFromUser());
			call.setDialog(requestEvent.getDialog());
			listener.onAck(message);
		} else {
			LOGGER.warn("No listener registered for: {}", message.getFromUser());
		}
	}

	private void processInvite(final RequestEvent requestEvent)
			throws SdpParseException, ParseException, SipException, InvalidArgumentException {
		/*
		 * Establish a new connection and send RINGING as a temporary response
		 * If there is already a session, send a BUSY
		 */
		InviteMessage inviteMessage = new InviteMessage(requestEvent.getRequest());

		Call call = new Call(inviteMessage.getFromUser(), inviteMessage.getToUser());
		call.setServerTransaction(requestEvent.getServerTransaction());
		call.setCurrentRequest(inviteMessage);

		UserCallListNode node = listeners.get(inviteMessage.getToUser().toString());
		ISipManagerListener listener = node.getListener();
		node.addCall(inviteMessage.getFromUser(), call);
		if (listener != null) {
			listener.onInvite(inviteMessage);
		} else {
			LOGGER.warn("No listener registered for: " + inviteMessage.getFromUser());
			sipMessageHandler.sendNotFound(call);
		}
	}

	/** {@inheritDoc}
	 * @throws SipException
	 * @throws InvalidArgumentException
	 * @throws ParseException */
	@Override
	public void register(final SipUser user)
			throws ParseException, InvalidArgumentException,
			SipException {
		if (isShutdown) {
			throw new IllegalStateException("Sip core is shut down");
		}
		sipMessageHandler.sendRegister(user);
	}

	/** {@inheritDoc} */
	@Override
	public void deregister(final SipUser user)
			throws InvalidArgumentException,
			TooManyListenersException, ParseException, SipException,
			InterruptedException {
		sipMessageHandler.sendUnregister(user);
	}

	/** {@inheritDoc} */
	@Override
	public void processTransactionTerminated(
			final TransactionTerminatedEvent arg0) {

		if (arg0.isServerTransaction()) {
			Request r = arg0.getServerTransaction().getRequest();
			ToHeader to = (ToHeader) r.getHeader(ToHeader.NAME);
			SipUser toUser = SipUser.parseFromHeader(to);

			FromHeader from = (FromHeader) r.getHeader(FromHeader.NAME);
			SipUser fromUser = SipUser.parseFromHeader(from);

			UserCallListNode node = listeners.get(toUser.toString());
			Call call = node.getCall(fromUser);
			call.setServerTransaction(null);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void processIOException(final IOExceptionEvent exceptionEvent) {
		LOGGER.info("IOException happened for " + exceptionEvent.getHost()
				+ " port = " + exceptionEvent.getPort());

	}

	/** {@inheritDoc} */
	@Override
	public void processDialogTerminated(
			final DialogTerminatedEvent dialogTerminatedEvent) {
	}

	/** {@inheritDoc} */
	@Override
	public void processTimeout(final TimeoutEvent timeoutEvent) {
		LOGGER.warn("Transaction Time out");
		SipUser u;
		if (timeoutEvent.isServerTransaction()) {
			LOGGER.warn("ServerTransaction");
			Request r = timeoutEvent.getServerTransaction().getRequest();
			u = SipUser.parseFromHeader((ToHeader)r.getHeader(ToHeader.NAME));
		} else {
			LOGGER.warn("ClientTransaction");
			Request r = timeoutEvent.getClientTransaction().getRequest();
			u = SipUser.parseFromHeader((FromHeader)r.getHeader(FromHeader.NAME));
		}
		ISipManagerListener listener = listeners.get(u.toString()).getListener();
		if (listener != null) {
			listener.onTimeOut();
		} else {
			LOGGER.warn("No listener registereeed for: {}" + u);
		}
	}

	/**
	 * Reset all connections.
	 */
	private void reset() {
		sipMessageHandler.reset();
	}



	/** {@inheritDoc} */
	@Override
	public void addListener(final SipUser user, final ISipManagerListener listener) {
		listeners.put(user.toString(), new UserCallListNode(listener));
	}





	/** {@inheritDoc} */
	@Override
	public void sendAck(final SipUser from, final SipUser to)
			throws InvalidArgumentException, SipException {
		// Hopefully there is a dialog set.. :S
		Call call = listeners.get(from.toString()).getCall(to);
		sipMessageHandler.sendAck(call);
	}

	/** {@inheritDoc} */
	@Override
	public void sendBye(final SipUser from, final SipUser to)
			throws SipException, InvalidArgumentException, ParseException, SdpException {
		Call call = listeners.get(from.toString()).getCall(to);
		sipMessageHandler.sendBye(call);
	}

	/*
	 * Print Methods
	 */

	private void printRequestReceived(final Request request) {
		LOGGER.info("\n------\nREQUEST RECEIVED " + "Method:\t"
				+ request.getMethod() + "\n------");
		LOGGER.trace("\n" + request + "\n");
	}



	@Override
	public SipPreferences getPreferences() {
		return sipMessageHandler.getPreferences();
	}

	public void sendCancel(final Call call) {
		sipMessageHandler.sendCancel(call);
	}






	/*
	 * 
	 * 
	 * 
	 * Serverside Methods
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 */

	@Override
	public void sendQueued(final SipUser from, final SipUser to) throws ParseException, SipException, InvalidArgumentException {
		Call call = listeners.get(from.toString()).getCall(to);
		sipMessageHandler.sendQueued(call);

	}

	@Override
	public void sendDecline(final SipUser from, final SipUser to) throws SipException, ParseException, InvalidArgumentException {
		Call call = listeners.get(from.toString()).getCall(to);
		sipMessageHandler.sendDecline(call);
	}

	/** {@inheritDoc} */
	@Override
	public void sendRinging(final SipUser from, final SipUser to) throws ParseException,
	SipException, InvalidArgumentException {
		Call call = listeners.get(from.toString()).getCall(to);
		sipMessageHandler.sendRinging(call);
	}


	/** {@inheritDoc} */
	@Override
	public void sendOk(final SipUser from, final SipUser to) throws SipException,
	InvalidArgumentException, ParseException, SdpException {
		sendOk(from, to, null);
	}

	/** {@inheritDoc} */
	@Override
	public void sendOk(final SipUser from, final SipUser to, final String message)
			throws SipException, InvalidArgumentException, ParseException,
			SdpException {
		Call call = listeners.get(from.toString()).getCall(to);
		if (call != null) {
			sipMessageHandler.sendOk(call, message);
			Request request = call.getRawRequest();
			if (request != null
					&& request.getMethod().equals(Request.BYE)) {
				SipUser u = SipUser.parseFromHeader((ToHeader)request.getHeader(ToHeader.NAME));
				listeners.get(u.toString()).removeCall(u);
			}
		} else {
			LOGGER.error("No Request to Acknowledge");
		}

	}







	/** {@inheritDoc}
	 * @throws InterruptedException */
	@Override
	public final synchronized void shutdown()
			throws ObjectInUseException {
		reset();

		if (isShutdown) {
			return;
		}
		sipMessageHandler.getSipProvider().removeSipListener(this);
		sipMessageHandler.shutdown();
		isShutdown = true;
		LOGGER.debug("SipStack stopped.");
	}

	@Override
	public void removeListener(final SipUser user) {
		listeners.remove(user.toString());
	}

}


