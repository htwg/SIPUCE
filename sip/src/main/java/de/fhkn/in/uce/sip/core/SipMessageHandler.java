package de.fhkn.in.uce.sip.core;

import gov.nist.javax.sdp.MediaDescriptionImpl;
import gov.nist.javax.sdp.fields.InformationField;
import gov.nist.javax.sip.ListeningPointExt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import javax.sdp.Info;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TransportNotSupportedException;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.AllowHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.RequireHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.SupportedHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.sip.core.message.InviteMessage;

/**
 * 
 * @author feherz
 *
 */
public final class SipMessageHandler {

	private static final int SECONDS = 1000;

	private static final int UDP_HEARTBEAT_TIME = 25;

	private static final Logger LOGGER =
			LoggerFactory.getLogger(SipMessageHandler.class);

	/*
	 * Sip stack variables
	 */
	private final AddressFactory addressFactory;
	private final MessageFactory messageFactory;
	private final HeaderFactory headerFactory;
	private final SipStack sipStack;
	private final SipProvider sipProvider;

	/*
	 * Constants
	 */
	private static final String VIA_BRANCH_PARAM = "z9hG4bK";
	private static final String PARAM_TRANSPORT = "transport";
	private static final String PARAM_REG_ID = "reg-id";
	private static final String PARAM_SIP_INSTANCE = "+sip.instance";
	private static final String PARAM_SIP_INSTANCE_FORMAT = "\"<urn:uuid:%s>\"";
	private static final int MAX_FORWARDS = 70;


	private final SipUAInstance instance;

	private final SipPreferences preferences;


	//Derived, often used attributes from preferences
	private final String sipTransportProtocol;

	/*
	 * Contextual attributes
	 */
	//The Call-ID SHOULD be the same in each registration from a UA
	private final CallIdHeader registerCallIdHeader;

	private final AtomicLong registerCalls = new AtomicLong(1);

	// The flow number (see RFC 5626). We have only one flow
	private static final long REG_ID = 1L;


	//indicates whether the stack is already shut down
	private volatile boolean shutdown = false;

	private final ListeningPoint listeningPoint;


	private SipMessageHandler(final SipPreferences preferences, final SipUAInstance instance)
			throws PeerUnavailableException, TransportNotSupportedException,
			InvalidArgumentException, ObjectInUseException {

		this.preferences = preferences;
		this.instance = instance;
		InetSocketAddress sipProxyAddress = preferences.getSipProxyAddress();
		sipTransportProtocol = preferences.getSipTransportProtocol();
		InetSocketAddress privateAddress = preferences.getPrivateAddress();

		Properties properties = new Properties();

		// STACK_NAME is required but can be chosen free
		properties.setProperty("javax.sip.STACK_NAME", preferences.getPopulateString());
		properties.setProperty("javax.sip.IP_ADRESS", preferences.getPopulateAddress());
		properties.setProperty("javax.sip.OUTBOUND_PROXY",
				sipProxyAddress.getAddress().getHostAddress() + ":"
						+ sipProxyAddress.getPort() + "/"
						+ sipTransportProtocol);
		properties.setProperty("javax.sip.DEBUG_LOG", "debugLog.txt");
		properties.setProperty("javax.sip.SERVER_LOG", "serverLog.txt");
		properties.setProperty("javax.sip.TRACE_LEVEL", "32");

		SipFactory sipFactory = SipFactory.getInstance();

		// name of the implementation. NIST = National Institute of Standards
		// and Technology
		sipFactory.setPathName("gov.nist");
		sipStack = sipFactory.createSipStack(properties);

		headerFactory = sipFactory.createHeaderFactory();
		addressFactory = sipFactory.createAddressFactory();
		messageFactory = sipFactory.createMessageFactory();
		listeningPoint = sipStack.createListeningPoint(
				privateAddress.getAddress().getHostAddress(),
				privateAddress.getPort(),
				sipTransportProtocol);
		sipProvider = sipStack.createSipProvider(listeningPoint);
		registerCallIdHeader = sipProvider.getNewCallId();
		keepAliveThread.start();
	}


	private String getNewTag() {
		return String.valueOf(Math.random() * Long.MAX_VALUE);
	}



	/**
	 * Registers a user on the registrar.
	 * @param user
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 * @throws SipException
	 */
	public void sendRegister(final SipUser user)
			throws ParseException, InvalidArgumentException, SipException {
		sendRegister(user, preferences.getExpireTime());
	}

	/**
	 * Unregisters a user on the registrar.
	 * @param user
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 * @throws SipException
	 */
	public void sendUnregister(final SipUser user)
			throws ParseException, InvalidArgumentException, SipException {
		sendRegister(user, 0);
	}

	/**
	 * Sends out a register request with a given Address to the SIP proxy. <br/>
	 * It sets the user for current messages.
	 * 
	 * @param address
	 *            The public IP:Port combination
	 * @param expires
	 *            The time how long the binding will last or zero to unregister
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 * @throws SipException
	 */
	private void sendRegister(final SipUser user, final int expires)
			throws ParseException, InvalidArgumentException, SipException {

		String sipUsername = user.getUsername();

		FromHeader fromHeader = user.getFromHeader(addressFactory,
				headerFactory, null);
		ToHeader toHeader = user.getToHeader(addressFactory, headerFactory);

		CSeqHeader cSeqHeader =
				headerFactory.createCSeqHeader(registerCalls.getAndIncrement(), Request.REGISTER);

		MaxForwardsHeader maxForwardsHeader =
				headerFactory.createMaxForwardsHeader(MAX_FORWARDS);
		ExpiresHeader expiresHeader =
				headerFactory.createExpiresHeader(expires);

		SipURI contactUri = addressFactory.createSipURI(sipUsername, preferences.getPopulateAddress());
		contactUri.setPort(preferences.getPopulatePort());
		Address contactAddress = addressFactory.createAddress(contactUri);
		ContactHeader contactHeader;
		if (expires != 0) {
			contactHeader = headerFactory.createContactHeader(contactAddress);
		} else {
			contactHeader = headerFactory.createContactHeader();
		}
		if (sipTransportProtocol.equalsIgnoreCase(ListeningPoint.TCP)) {
			contactUri.setParameter(PARAM_TRANSPORT, ListeningPoint.TCP);
		}
		contactAddress.setDisplayName(sipUsername);

		//TODO Should be the same than the uri in the To Header
		SipURI requestURI = addressFactory.createSipURI(
				sipUsername,
				user.getDomain());

		// randomized Branch ids param
		ViaHeader viaHeader =
				headerFactory.createViaHeader(
						preferences.getPopulateAddress(), preferences.getPopulatePort(),
						sipTransportProtocol, generateBranchId());
		// send the response back to the source IP address and port from which
		// the request originated
		ArrayList<ViaHeader> viaHeaders = new ArrayList<>();
		viaHeaders.add(viaHeader);

		/*
		 * RFC 6314 - NAT Traversal Techniques for SIP
		 */
		// Symmetric Response - Server answers on the IP:Port from package
		if (sipTransportProtocol.equalsIgnoreCase(ListeningPoint.UDP)) {
			viaHeader.setRPort();
		}

		contactHeader.setParameter(PARAM_REG_ID, "" + REG_ID);
		contactHeader.setParameter(PARAM_SIP_INSTANCE,
				String.format(PARAM_SIP_INSTANCE_FORMAT,
						instance.getInstanceId().toString()));

		SupportedHeader supportedHeader = headerFactory
				.createSupportedHeader("path, outbound");

		RequireHeader requireHeader = headerFactory.createRequireHeader("path, outbound");
		// End of NAT Traversal Techniques

		Request request = messageFactory.createRequest(requestURI,
				Request.REGISTER, registerCallIdHeader, cSeqHeader, fromHeader,
				toHeader, viaHeaders, maxForwardsHeader);
		request.setExpires(expiresHeader);
		request.addHeader(contactHeader);
		request.addHeader(supportedHeader);
		request.addHeader(requireHeader);

		ClientTransaction clientTransaction = sipProvider.getNewClientTransaction(request);
		clientTransaction.sendRequest();
		printRequestSent(request);
	}

	/**
	 * The branch parameter value MUST be unique across space and time for
	 * all requests sent by the UA. The exceptions to this rule are CANCEL
	 * and ACK for non-2xx responses.
	 * @return
	 */
	private String generateBranchId() {
		//TODO  be unique across space and time
		int random = (int) (Math.random() * (Integer.MAX_VALUE - 0) + 0);
		return VIA_BRANCH_PARAM + random;
	}

	/**
	 * 
	 * @param to
	 * @param address
	 * @throws ParseException
	 *             if the URI string is malformed. signals that an error has
	 *             been reached unexpectedly while parsing the displayName
	 *             value.
	 * @throws InvalidArgumentException
	 *             if supplied sequence number is less than zero. if supplied
	 *             maxForwards is less than zero or greater than 255 if the
	 *             supplied port is invalid. if rport value is an illegal
	 *             integer ( <=0 ).
	 * @throws SipException
	 *             if the SipProvider cannot send the Request for any reason
	 * @throws SdpException
	 */
	public Call sendInvite(final SipUser from, final SipUser to, final String message)
			throws ParseException, InvalidArgumentException, SipException,
			SdpException {

		Call call = new Call(from, to);

		String fromName = from.getUsername();
		String toName = to.getUsername();

		// Headers
		FromHeader fromHeader = from.getFromHeader(addressFactory,
				headerFactory, null);
		ToHeader toHeader = to.getToHeader(addressFactory, headerFactory);

		SipURI requestURI = addressFactory.createSipURI(toName,
				to.getDomain());

		CallIdHeader callIdHeader = sipProvider.getNewCallId();

		CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(
				call.getCallSequence(),
				Request.INVITE);
		MaxForwardsHeader maxForwardsHeader = headerFactory
				.createMaxForwardsHeader(MAX_FORWARDS);

		ViaHeader viaHeader = headerFactory.createViaHeader(
				preferences.getPopulateAddress(), preferences.getPopulatePort(),
				sipTransportProtocol, generateBranchId());
		if (sipTransportProtocol.equalsIgnoreCase(ListeningPoint.UDP)) {
			// UDP Symmetric Response
			viaHeader.setRPort();
		}
		ArrayList<ViaHeader> viaHeaders = new ArrayList<>();
		viaHeaders.add(viaHeader);

		SipURI contactUri = addressFactory.createSipURI(fromName, preferences.getPopulateAddress() + ";ob");
		contactUri.setPort(preferences.getPopulatePort());

		Address contactAddress = addressFactory.createAddress(contactUri);
		contactAddress.setDisplayName(fromName);
		ContactHeader contactHeader = headerFactory
				.createContactHeader(contactAddress);

		// Allow header. Describes what methods we allow.
		String methods = Request.INVITE + ", " + Request.ACK + ", "
				+ Request.OPTIONS + ", " + Request.CANCEL + ", " + Request.BYE
				+ ", " + Request.INFO + ", " + Request.REFER + ", "
				+ Request.MESSAGE + ", " + Request.NOTIFY + ", "
				+ Request.SUBSCRIBE;
		AllowHeader allowHeader = headerFactory.createAllowHeader(methods);

		// For NAT Traversal
		SupportedHeader supportedHeader = headerFactory
				.createSupportedHeader("outbound");
		// TODO looks like hacking

		RouteHeader routeHeader = headerFactory
				.createRouteHeader(addressFactory.createAddress(
						"<sip:"+preferences.getProxyAddressString()+";lr>"));

		// Create the request
		Request request = messageFactory.createRequest(requestURI,
				Request.INVITE, callIdHeader, cSeqHeader, fromHeader, toHeader,
				viaHeaders, maxForwardsHeader);
		request.addHeader(allowHeader);

		// You can add extension headers of your own making
		// to the outgoing SIP request.
		// Add the extension header.
		request.addHeader(supportedHeader);
		request.addHeader(contactHeader);
		request.addHeader(routeHeader);

		MediaDescription sdp = new MediaDescriptionImpl();

		if (message != null) {
			Info info = new InformationField();
			info.setValue(message);

			sdp.setInfo(info);

			// Create ContentTypeHeader
			ContentTypeHeader contentTypeHeader = headerFactory
					.createContentTypeHeader("application", "sdp");
			request.setContent(sdp, contentTypeHeader);
		}

		ClientTransaction clientTransaction = sipProvider.getNewClientTransaction(request);

		clientTransaction.sendRequest();

		printRequestSent(request);

		call.setClientTransaction(clientTransaction);
		InviteMessage invmessage = new InviteMessage(request);
		call.setCurrentRequest(invmessage);
		return call;
	}


	/**
	 * Reset all current data.
	 */
	public void reset() {
	}

	/**
	 * Stops the sip stack.
	 * @throws ObjectInUseException if the stack is still in usage.
	 */
	public void shutdown() throws ObjectInUseException {
		shutdown = true;
		keepAliveThread.interrupt();
		sipStack.deleteSipProvider(sipProvider);
		sipStack.stop();
	}

	/*
	 * Setter and getter
	 */
	/**
	 * Creates a new SipMessageHandler object.
	 * @param preferences
	 * 		The preferences including the proxy address and transport protocol
	 * @param instance
	 * 		The persistent UserAgentInstance
	 * @return
	 *  	A new SipMessageHandler with it's own SipStack.
	 * @throws PeerUnavailableException
	 * @throws TransportNotSupportedException
	 * @throws ObjectInUseException
	 * @throws InvalidArgumentException
	 */
	public static SipMessageHandler createInstance(
			final SipPreferences preferences, final SipUAInstance instance)
					throws PeerUnavailableException, TransportNotSupportedException,
					ObjectInUseException, InvalidArgumentException {

		return new SipMessageHandler(preferences, instance);
	}


	/**
	 * Returns the SipProvider.
	 * @return the SipProvider.
	 */
	public SipProvider getSipProvider() {
		return sipProvider;
	}

	/**
	 * 
	 * @return
	 */
	public SipPreferences getPreferences() {
		return preferences;
	}

	public boolean isTerminated() {
		return shutdown;
	}



	/**
	 * Send a RINGING response to an specific INVITE.
	 * @param invite
	 * 				the related INVITE request
	 * @throws ParseException
	 * 				corrupted invite request
	 * @throws SipException
	 * 				ServerTransaction was <code>null</code> and can't create a
	 *              new one or<br />
	 * 				The SipProvider cannot send the response for any reason.
	 * @throws InvalidArgumentException
	 * 				Should never happen.
	 */
	public void sendRinging(final Call call)
			throws ParseException, SipException, InvalidArgumentException {

		Response response =
				messageFactory.createResponse(Response.RINGING, call.getRawRequest());

		sendResponse(call.getRawRequest(), response, call);

	}

	/**
	 * Send a BUSY_HERE response to an specific INVITE.
	 * @param invite
	 * 				the related INVITE request
	 * @throws ParseException
	 * 				corrupted invite request
	 * @throws SipException
	 * 				ServerTransaction was <code>null</code> and can't create a
	 *              new one or<br />
	 * 				The SipProvider cannot send the response for any reason.
	 * @throws InvalidArgumentException
	 * 				Should never happen.
	 */
	public void sendBusyHere(final Call call)
			throws ParseException, SipException, InvalidArgumentException {

		// Create a new response for the request
		Response response =
				messageFactory.createResponse(Response.BUSY_HERE, call.getRawRequest());

		sendResponse(call.getRawRequest(), response, call);
	}

	/**
	 * Send a sip cancel request.
	 * @param currentCall
	 * 
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 * @throws SipException
	 */
	public void sendCancel(final Call currentCall) {
		//TODO check this
	}

	/**
	 * Send an ACK using an established dialog.
	 * @throws InvalidArgumentException
	 * 				if there is a problem with the supplied cseq ( for example <= 0 ).
	 * @throws SipException
	 *              if the cseq does not relate to a previously sent INVITE or
	 *              if the Method that created the Dialog is not an INVITE ( for example SUBSCRIBE) or
	 *              if implementation cannot send the ACK Request for any reason
	 */
	public void sendAck(final Call call) throws InvalidArgumentException, SipException {
		Request ackRequest = call.getDialog().createAck(call.getCallSequence());

		LOGGER.debug("Sending ACK via established dialog...");
		call.getDialog().sendAck(ackRequest);
		printRequestSent(ackRequest);
	}

	/**
	 * Send bye.
	 * 
	 * @throws SipException
	 */
	public void sendBye(final Call call) throws SipException {
		Dialog dialog = call.getDialog();
		if (dialog == null) {
			LOGGER.error("No Dialog established... returning");
			return;
		}

		// create Request from dialog
		Request request = dialog.createRequest(Request.BYE);

		// Create the client transaction and send the request
		ClientTransaction clientTransaction = sipProvider
				.getNewClientTransaction(request);
		dialog.sendRequest(clientTransaction);
		printRequestSent(request);
	}

	public void sendQueued(final Call call) throws ParseException, SipException, InvalidArgumentException {
		Response response =
				messageFactory.createResponse(Response.QUEUED, call.getRawRequest());
		sendResponse(call.getRawRequest(), response, call);

	}


	public void sendDecline(final Call call) throws SipException, ParseException, InvalidArgumentException {
		Response response =
				messageFactory.createResponse(Response.DECLINE, call.getRawRequest());

		sendResponse(call.getRawRequest(), response, call);
	}

	public void sendNotFound(final Call call) throws ParseException, SipException, InvalidArgumentException {
		Response response =
				messageFactory.createResponse(Response.NOT_FOUND, call.getRawRequest());

		sendResponse(call.getRawRequest(), response, call);
	}


	public void sendNotAcceptable(final Call call) throws ParseException, SipException, InvalidArgumentException {
		Response response =
				messageFactory.createResponse(Response.NOT_ACCEPTABLE_HERE, call.getRawRequest());
		sendResponse(call.getRawRequest(), response, call);
	}

	private void sendResponse(final Request invite, final Response response, final Call call)
			throws SipException,
			InvalidArgumentException {

		ServerTransaction transaction = call.getServerTransaction();
		// Send the created response
		if (transaction == null) {
			LOGGER.warn("Transaction was null. Created a new Servertransaction");
			transaction = sipProvider.getNewServerTransaction(invite);
			call.setServerTransaction(transaction);
		}
		transaction.sendResponse(response);
		printResponseSent(response);
	}

	private final Thread keepAliveThread = new Thread(new Runnable() {
		@Override
		public void run() {
			String address = preferences.getSipProxyAddress().getAddress().getHostAddress();
			int port = preferences.getSipProxyAddress().getPort();
			try {
				while (!shutdown) {
					Thread.sleep(UDP_HEARTBEAT_TIME * SECONDS);
					((ListeningPointExt)listeningPoint).sendHeartbeat(address, port);
					LOGGER.trace("SIP - Keep alive sent");
				}
			} catch (IOException | InterruptedException e) {
				LOGGER.warn(e.toString());
				// TODO Handle exception properly
			}
			LOGGER.trace("KeepAliveThread terminates");
		}
	}, "KeepAliveThread");

	/*
	 * Printing methods
	 */

	private void printRequestSent(final Request request) {
		LOGGER.info("\n------\nREQUEST SENT\n" + "Method:\t"
				+ request.getMethod() + "\n------");
		LOGGER.trace("\n" + request + "\n");
	}

	private void printResponseSent(final Response response) {
		LOGGER.info("\n------\nRESPONSE SENT\n" + "Reason Phrase:\t"
				+ response.getReasonPhrase() + "\n------");
		LOGGER.trace("\n" + response + "\n");
	}












	public void sendOk(final Call call, final String message)
			throws ParseException, InvalidArgumentException, SipException, SdpException {

		Response okResponse = messageFactory.createResponse(Response.OK,
				call.getLastRequest().getRawRequest());

		FromHeader fromHeader = (FromHeader) okResponse.getHeader(FromHeader.NAME);
		String name = fromHeader.getAddress().getDisplayName();

		SipURI contactUri = addressFactory.createSipURI(
				name,
				preferences.getPopulateAddress() + ";ob");
		contactUri.setPort(preferences.getPopulatePort());

		Address contactAddress = addressFactory.createAddress(contactUri);
		contactAddress.setDisplayName(name);
		ContactHeader contactHeader = headerFactory
				.createContactHeader(contactAddress);
		contactHeader.setExpires(preferences.getExpireTime());
		okResponse.addHeader(contactHeader);

		ToHeader toHeader = (ToHeader) okResponse.getHeader(ToHeader.NAME);
		// Application is supposed to set.
		toHeader.setTag(getNewTag());

		MediaDescription sdp = new MediaDescriptionImpl();

		try {
			if (message != null) {
				Info info = new InformationField();
				info.setValue(message);
				sdp.setInfo(info);

				// Create ContentTypeHeader
				ContentTypeHeader contentTypeHeader = headerFactory
						.createContentTypeHeader("application", "sdp");
				okResponse.setContent(sdp, contentTypeHeader);
			}
		} catch (SdpException e) {
			LOGGER.error("Couldn't add SDP:" + e.getMessage());
			throw e;
		}

		ServerTransaction transaction = call.getServerTransaction();
		// Send the created response
		if (transaction == null) {
			LOGGER.warn("Transaction was null. Create a new Servertransaction");
			transaction = sipProvider.getNewServerTransaction(call.getRawRequest());
			call.setServerTransaction(transaction);
		}


		LOGGER.trace("Sending OK Response...");

		transaction.sendResponse(okResponse);

		printResponseSent(okResponse);

		LOGGER.trace("Dialog state after OK: "
				+ transaction.getDialog().getState());

		call.setDialog(transaction.getDialog());
	}
}