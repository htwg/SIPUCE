package de.fhkn.in.uce.sip.core;

import java.text.ParseException;
import java.util.TooManyListenersException;

import javax.sdp.SdpException;
import javax.sip.InvalidArgumentException;
import javax.sip.ObjectInUseException;
import javax.sip.SipException;

/**
 * Defines the interface to use the SIP stack propperly.
 * @author feherz
 *
 */
public interface ISipManager {

	void addListener(SipUser user, ISipManagerListener listener);

	void removeListener(SipUser user);

	/**
	 * Sends a REGISTER message to the registrar using the information
	 * from the sip user. An update thread is start for every registered
	 * user.
	 * @param user The sip user to register
	 * @throws IllegalStateException
	 * @throws InterruptedException
	 * @throws SipException
	 * @throws ParseException
	 * @throws TooManyListenersException
	 * @throws InvalidArgumentException
	 */
	void register(SipUser user) throws ParseException, InvalidArgumentException, SipException;

	/**
	 * Sends a REGISTER message with an expire time of zero to the registrar
	 * using the information from the sip user.
	 * @param user The sip user to deregister
	 * @throws InterruptedException
	 * @throws SipException
	 * @throws ParseException
	 * @throws TooManyListenersException
	 * @throws InvalidArgumentException
	 */
	void deregister(SipUser user) throws InvalidArgumentException, TooManyListenersException, ParseException, SipException, InterruptedException;

	/**
	 * Sends out a basic INVITE request with no SDP information to
	 * the specified sip user.
	 * @param to The sip user which is the destination.
	 * @return TODO
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 * @throws SipException
	 * @throws SdpException
	 */
	void sendInvite(final SipUser from, final SipUser to)
			throws ParseException, InvalidArgumentException, SipException, SdpException;

	/**
	 * Sends out an INVITE request with the message as SDP message to
	 * the specified sip user
	 * @param to The sip user which is the destination.
	 * @param message The SDP Message
	 * @return TODO
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 * @throws SipException
	 * @throws SdpException
	 */
	void sendInvite(final SipUser from, final SipUser to, final String message)
			throws ParseException, InvalidArgumentException, SipException, SdpException;

	/**
	 * Sends out a RINGING response to the spicified sip user.
	 * @param call The sip user which is the destination.
	 * @throws InvalidArgumentException
	 * @throws SipException
	 * @throws ParseException
	 */
	void sendRinging(final SipUser from, final SipUser to) throws ParseException, SipException, InvalidArgumentException;

	/**
	 * Sends out a basic OK response with no SDP message to the specified sip user.
	 * @param call
	 */
	void sendOk(final SipUser from, final SipUser to)
			throws SipException, InvalidArgumentException, ParseException, SdpException ;

	/**
	 * Sends out a OK response with a SDP message to the specified sip user.
	 * @param call
	 * @param message The SDP Message
	 */
	void sendOk(final SipUser from, final SipUser to, final String message)
			throws SipException, InvalidArgumentException, ParseException, SdpException ;

	/**
	 * Sends out a BYE request to the specified sip user.
	 * @param call
	 */
	void sendBye(final SipUser from, final SipUser to)
			throws SipException, InvalidArgumentException, ParseException, SdpException;

	/**
	 * Stops the sip stack if possible.
	 * @throws ObjectInUseException
	 * @throws InterruptedException
	 */
	void shutdown()
			throws ObjectInUseException, InterruptedException;


	/**
	 * Sends out a Queued response to the specified sip user.
	 * @param call
	 */
	void sendQueued(final SipUser from, final SipUser to)
			throws SipException, InvalidArgumentException, ParseException, SdpException;

	void sendDecline(final SipUser from, final SipUser to)
			throws SipException, InvalidArgumentException, ParseException, SdpException;

	void sendAck(final SipUser from, final SipUser to) throws InvalidArgumentException, SipException;

	SipPreferences getPreferences();
}
