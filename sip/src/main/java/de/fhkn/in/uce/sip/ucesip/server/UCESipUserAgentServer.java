package de.fhkn.in.uce.sip.ucesip.server;

import java.io.IOException;
import java.util.TooManyListenersException;
import java.util.concurrent.TimeoutException;

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
import de.fhkn.in.uce.sip.ucesip.server.call.UCESipServerCall;
import de.fhkn.in.uce.sip.ucesip.settings.UCESipServerSettings;

/**
 * @startuml
 * 
 * [*] 	-d-> Init
 * 
 * Init -d-> WaitForRegisterResponse : register / sendRegister
 * Init -> Finished : shutdown
 * 
 * WaitForRegisterResponse -d-> Registered : onOk / startRegisterRefreshThread
 * WaitForRegisterResponse -d-> Error : onFailure, onTimeOut, onStateTimeout / sendDerigister, declineCurrentCalls
 * 
 * 
 * state Registered {
 * 
 * 	[*] -> Listening
 * 
 * 	Listening -> Listening : onInvite / sendQueued
 * 	Listening -d-> Accepting : takeCall / blocking take()
 * 
 * 	Accepting -u-> Listening : take returned
 * 	Accepting -> Accepting : onInvite / call queue not empty ? -> sendQueued
 * 
 * }
 * 
 * Registered -d-> WaitForDerigisterResponse : shutdown / decline calls in call queue, sendDerigister
 * 
 * WaitForDerigisterResponse -d-> WaitForCallsFinished : onOk, onStateTimeout
 * 
 * WaitForCallsFinished -d-> Finished : calls finished
 * WaitForCallsFinished -d-> Error : onStateTimeout
 * 
 * Finished -d-> [*]
 * Error -d-> [*]
 * 
 * @enduml
 * @author Johannes Haeussler, Felix Herz, Janosch
 * 
 */
public class UCESipUserAgentServer implements IUCESipUserAgentServer, ISipManagerListener {

	static final Logger LOGGER = LoggerFactory.getLogger(UCESipUserAgentServer.class);

	private final ServerStateContext serverStateContext;

	/**
	 * Creates a new UserAgent Server instance in UCE context.
	 * 
	 * @param serverUser
	 *            The name of the user
	 * @param serverDomain
	 *            The domain the user is located
	 * @param settings
	 *            Settings about proxy, protocol, {@link UceSipSettings}
	 * @throws UCESipException
	 */
	public UCESipUserAgentServer(final String serverUser, final String serverDomain, final UCESipServerSettings sipSettings) throws UCESipException {
		this(new SipUser(serverUser, serverDomain), sipSettings);
	}

	private UCESipUserAgentServer(final SipUser serverUser, final UCESipServerSettings sipSettings) throws UCESipException {
		this(serverUser, sipSettings, createSipManager(serverUser, sipSettings));
	}

	private UCESipUserAgentServer(final SipUser serverUser, final UCESipServerSettings sipSettings, final ISipManager sipManager) {
		serverStateContext = new ServerStateContext(sipManager, serverUser, sipSettings);

		sipManager.addListener(serverUser, this);
	}

	private static ISipManager createSipManager(final SipUser serverUser, final UCESipServerSettings sipSettings) throws UCESipException {
		try {
			SipPreferences sipPreferences = new SipPreferences(sipSettings.getProxyAddress());
			sipPreferences.setSipTransportProtocol(sipSettings.getTransportProtocolString());
			sipPreferences.setExpireTime(sipSettings.getExpireTime());

			return new SipManager(sipPreferences, new SipUAInstance(serverUser.getUsername()));
		} catch (PeerUnavailableException | TransportNotSupportedException | ObjectInUseException | InvalidArgumentException
				| TooManyListenersException | IOException e) {
			throw new UCESipException("Could not initialize UCESipServer", e);
		}
	}

	@Override
	public void register() throws UCESipException, InterruptedException {
		LOGGER.trace("sip server register");
		serverStateContext.register();
	}

	@Override
	public void shutdown() throws UCESipException, InterruptedException {
		LOGGER.trace("sip server shutdown");
		serverStateContext.shutdown();
	}

	@Override
	public UCESipServerCall takeCall(final int timeoutMillis) throws InterruptedException, TimeoutException {
		return serverStateContext.takeCall(timeoutMillis);
	}

	@Override
	public void onInvite(final IMessage inviteMessage) {
		serverStateContext.onInvite(inviteMessage);
	}

	@Override
	public void onOk(final IMessage okmessage) {
		serverStateContext.onOk(okmessage);
	}

	@Override
	public void onBye(final IMessage message) {
		serverStateContext.onBye(message);
	}

	@Override
	public void onTimeOut() {
		serverStateContext.onTimeOut();
	}

	@Override
	public void onFailure(final ErrorMessage eMessage) {
		serverStateContext.onFailure(eMessage);
	}

	@Override
	public void onRinging(final IMessage ringingMessage) {
		// Should never get a ringing on the server
		LOGGER.info("Received not expected ringing: " + ringingMessage.getMessage());
	}

	@Override
	public void onAck(final IMessage message) {
		// An Ack shouldn't arrive yet
		LOGGER.info("Received not expected ack: " + message.getMessage());
	}

	@Override
	public void onDecline() {
		// Does not happen on server side
		LOGGER.info("Received not expected decline");
	}

}
