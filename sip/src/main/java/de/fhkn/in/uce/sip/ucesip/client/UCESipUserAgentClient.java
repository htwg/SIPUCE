package de.fhkn.in.uce.sip.ucesip.client;

import java.io.IOException;
import java.util.TooManyListenersException;

import javax.sip.InvalidArgumentException;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.TransportNotSupportedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.sip.core.ISipManager;
import de.fhkn.in.uce.sip.core.ISipManagerListener;
import de.fhkn.in.uce.sip.core.SipManager;
import de.fhkn.in.uce.sip.core.SipPreferences;
import de.fhkn.in.uce.sip.core.SipUAInstance;
import de.fhkn.in.uce.sip.core.SipUser;
import de.fhkn.in.uce.sip.core.message.ErrorMessage;
import de.fhkn.in.uce.sip.core.message.IMessage;
import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;
import de.fhkn.in.uce.sip.ucesip.message.IUCESipMessage;
import de.fhkn.in.uce.sip.ucesip.settings.UCESipClientSettings;
import de.fhkn.in.uce.sip.ucesip.settings.UCESipSettings;

/**
 * @startuml
 * [*] -d-> Init
 * 
 * Init -r-> WaitForInviteResponse : inviteUCESipUAS / sendInvite
 * Init -d-> Finished : shutdown / #
 * 
 * WaitForInviteResponse -d-> WaitForDeregisterResponse : onFailure, onDecline, onTimeOut, onStateTimeout / derigister
 * WaitForInviteResponse -d-> WaitForByeResponse : onOk / sendBye
 * WaitForInviteResponse -> WaitForInviteResponse : onRinging
 * 
 * WaitForByeResponse -d-> WaitForDeregisterResponse : onOk, onFailure, onDecline, onTimeOut, onStateTimeout / derigister
 * 
 * WaitForDeregisterResponse -d-> Finished : onOk / shutdownManager
 * WaitForDeregisterResponse -d-> Error : onFailure, onDecline, onTimeOut, onStateTimeout / shutdownManager
 * 
 * Finished -d-> [*]
 * 
 * Error -d-> [*]
 * 
 * @enduml
 * @author Johannes Haeussler, Felix Herz, Janosch
 * 
 */
public class UCESipUserAgentClient implements IUCESipUserAgentClient, ISipManagerListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(UCESipUserAgentClient.class);

	private final InviteStateContext inviteStateContext;

	private volatile boolean inviteWasCalled;
	private volatile boolean isShutdown;

	public UCESipUserAgentClient(final String fromUser, final String fromDomain, final String toUser, final String toDomain,
			final UCESipClientSettings settings) throws UCESipException {
		this(new SipUser(fromUser, fromDomain), new SipUser(toUser, toDomain), settings);
	}

	UCESipUserAgentClient(final String fromUser, final String fromDomain, final String toUser, final String toDomain,
			final UCESipClientSettings settings, final ISipManager sipManager) throws UCESipException {
		this(new SipUser(fromUser, fromDomain), new SipUser(toUser, toDomain), settings, sipManager);
	}

	private UCESipUserAgentClient(final SipUser sipUser, final SipUser toSipUser, final UCESipClientSettings settings) throws UCESipException {
		this(sipUser, toSipUser, settings, createSipManager(sipUser, settings));
	}

	private UCESipUserAgentClient(final SipUser sipUser, final SipUser toSipUser, final UCESipClientSettings settings, final ISipManager sipManager)
			throws UCESipException {
		this.inviteWasCalled = false;
		this.isShutdown = false;

		// init before add listener, after add listener methods could be invoked.
		this.inviteStateContext = new InviteStateContext(sipUser, toSipUser, settings, sipManager);

		sipManager.addListener(sipUser, this);
	}

	private static ISipManager createSipManager(final SipUser sipUser, final UCESipSettings settings) throws UCESipException {
		try {
			SipPreferences sipPreferences = new SipPreferences(settings.getProxyAddress());
			sipPreferences.setSipTransportProtocol(settings.getTransportProtocolString());
			sipPreferences.setExpireTime(settings.getExpireTime());
			return new SipManager(sipPreferences, new SipUAInstance(sipUser.getUsername()));
		} catch (PeerUnavailableException | TransportNotSupportedException | ObjectInUseException | InvalidArgumentException
				| TooManyListenersException | IOException e) {
			throw new UCESipException("Unable to initiate User Agent Client", e);
		}
	}

	@Override
	public synchronized IUCESipMessage inviteUCESipUAS(final IUCESipMessage message) throws UCESipException, InterruptedException {
		if (isShutdown == true) {
			throw new IllegalStateException("SipUserAgentClient is shutted down.");
		}
		if (inviteWasCalled == true) {
			throw new IllegalStateException("function can only be called once.");
		}
		inviteWasCalled = true;

		return inviteStateContext.inviteUAS(message);
	}

	@Override
	public synchronized void shutdown() throws UCESipException {
		if (isShutdown == true) {
			return;
		}

		inviteStateContext.shutdown();

		isShutdown = true;
	}

	@Override
	public void onTimeOut() {
		inviteStateContext.onTimeOut();
	}

	@Override
	public void onDecline() {
		inviteStateContext.onDecline();
	}

	@Override
	public void onFailure(final ErrorMessage eMessage) {
		inviteStateContext.onFailure(eMessage);
	}

	@Override
	public void onOk(final IMessage okmessage) {
		inviteStateContext.onOk(okmessage);
	}


	@Override
	public void onInvite(final IMessage inviteMessage) {
		LOGGER.info("Received not expected invite: " + inviteMessage.getMessage());
	}

	@Override
	public void onRinging(final IMessage ringingMessage) {
		LOGGER.info("Received not expected ringing: " + ringingMessage.getMessage());
	}

	@Override
	public void onAck(final IMessage message) {
		LOGGER.info("Received not expected ack: " + message.getMessage());
	}

	@Override
	public void onBye(final IMessage message) {
		LOGGER.info("Received not expected bye: " + message.getMessage());
	}



}