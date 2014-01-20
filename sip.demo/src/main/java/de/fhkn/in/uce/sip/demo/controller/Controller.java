package de.fhkn.in.uce.sip.demo.controller;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.Observable;
import java.util.TooManyListenersException;

import javax.sdp.SdpException;
import javax.sip.InvalidArgumentException;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.SipException;
import javax.sip.TransportNotSupportedException;
import javax.sip.message.Request;

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
import de.fhkn.in.uce.sip.core.message.IResponseMessage;

public class Controller extends Observable implements IController, ISipManagerListener {

	private static final int INVITE_STATUS = 180;

	private static final int SIP_PROXY_PORT = 5060;
	private static final String SIP_PROXY_IP = "213.239.218.18";

	private SipUser user;
	private SipUAInstance instance;
	private SipPreferences preferences = null;

	private boolean registered;

	private ISipManager sipLayer;

	private SipUser toUSer;

	static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);
	private static final String DATABASE_NAME = "instances.data";

	public Controller() {
		preferences = new SipPreferences(new InetSocketAddress(SIP_PROXY_IP, SIP_PROXY_PORT));
		try {
			calculateIP();
		} catch (IOException e) {
			LOGGER.error("Can't estimate localhost address. Terminating");
		}
	}

	@Override
	public InetSocketAddress getPrivateAddress() {
		return preferences.getPrivateAddress();
	}

	@Override
	public void setPrivateAddress(final InetSocketAddress privateAddress) {
		preferences.setPrivateAddress(privateAddress);
	}

	@Override
	public InetSocketAddress getPublicAddress() {
		return preferences.getPublicAddress();
	}

	@Override
	public void setPublicAddress(final InetSocketAddress publicAddress) {
		preferences.setPublicAddress(publicAddress);
	}

	@Override
	public InetSocketAddress getSipProxyAddress() {
		return preferences.getSipProxyAddress();
	}

	@Override
	public void setSipProxyAddress(final InetSocketAddress sipProxyAddress) {
		preferences.setSipProxyAddress(sipProxyAddress);
	}

	@Override
	public void register() {
		if (registered) {
			LOGGER.debug("Already logged in");
			setChanged();
			notifyObservers();
			return;
		}
		try {
			sipLayer.register(user);
			registered = true;
		} catch (InvalidArgumentException | ParseException | SipException e) {
			LOGGER.error(e.getMessage(), e);
		}
		setChanged();
		notifyObservers();
	}

	@Override
	public void unregister() {
		registered = false;
		try {
			try {
				sipLayer.deregister(user);
			} catch (TooManyListenersException | InterruptedException e) {
				LOGGER.error(e.getMessage(), e);
			}
		} catch (ParseException e) {
			LOGGER.error(e.getMessage(), e);
		} catch (InvalidArgumentException e) {
			LOGGER.error(e.getMessage(), e);
		} catch (SipException e) {
			LOGGER.error(e.getMessage(), e);
		}
		setChanged();
		notifyObservers();
	}

	@Override
	public void connect(final String to) {
		SipUser toUser = new SipUser(to, user.getDomain());
		// Connect means:
		// invite and response with OK and ACK etc...
		try {
			// AllocRequestToTurn(L_PRIV_1)
			sipLayer.sendInvite(this.user, toUser);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	private void calculateIP() throws IOException {
		// //////////////// DEBUG
		// ///////////////////////////////////////////////////////
		LOGGER.debug("IPs of detected Network Interfaces");
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface
					.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface networkInterface = interfaces.nextElement();
				Enumeration<InetAddress> addresses = networkInterface
						.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress address = addresses.nextElement();
					if (address instanceof Inet4Address
							&& !address.isLoopbackAddress()
							&& !address.isLinkLocalAddress()
							&& !address.isAnyLocalAddress()) {
						LOGGER.debug(address.getHostAddress());
					}
				}
			}
		} catch (SocketException e1) {
			LOGGER.error("Can't get the network interfaces");
		}
		// ////////////////DEBUG ENDE
		// ///////////////////////////////////////////////////
	}

	@Override
	public void initSipStack() {
		LOGGER.debug("Initialize stack");
		LOGGER.debug(preferences.toString());
		try {
			sipLayer =  new SipManager(preferences, instance);
			sipLayer.addListener(user, this);
			LOGGER.debug("SipStack initialized");
		} catch (InvalidArgumentException ex) {
			LOGGER.error("IP, Port or Transaction are wrong : "
					+ ex.getMessage());
		} catch (PeerUnavailableException e) {
			LOGGER.error(String.format("Can't listen to %s.",
					preferences.getPrivateAddressString()));
		} catch (ObjectInUseException e) {
			LOGGER.error("SipStack is already in use.");
		} catch (TransportNotSupportedException e) {
			LOGGER.error("TCP/UDP not supported.");
		} catch (TooManyListenersException e) {
			LOGGER.error("SipStack has too many listener.");
		} catch (IOException e) {
			LOGGER.error(e.getMessage());
		}
		if (sipLayer == null) {
			LOGGER.error("Sip Layer is not instantiated.");
			return;
		}
	}

	@Override
	public void stop() {
		LOGGER.debug("Stopped Sip Layer.");
		try {
			if (registered) {
				unregister();
			}
			sipLayer.shutdown();
		} catch (ObjectInUseException | InterruptedException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	@Override
	public void printInfo() {
		System.out.println(preferences);

	}

	@Override
	public void disconnect() {
		try {
			sipLayer.sendBye(user, toUSer);
		} catch (Exception e) {
			e.getMessage();
		}
	}

	@Override
	public void setUser(final SipUser user) {
		this.user = user;
		setSipClientInstance(user.getUsername());
	}

	@Override
	public SipUser getUser() {
		return user;
	}

	private void setSipClientInstance(final String name) {
		PersistenceController persistenceController = new PersistenceController();
		persistenceController.open(DATABASE_NAME);
		instance = persistenceController.getInstanceByName(name);
		if (instance == null) {
			instance = new SipUAInstance(name);
			persistenceController.storeInstance(instance);
		}
		persistenceController.close();
	}

	@Override
	public void acceptCall() {
		try {
			sipLayer.sendOk(user, toUSer);
		} catch (SipException | InvalidArgumentException | ParseException
				| SdpException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	@Override
	public void declineCall() {
		try {
			sipLayer.sendDecline(user, toUSer);
		} catch (SipException | InvalidArgumentException | ParseException | SdpException e) {
			LOGGER.error("Call was declined", e);
		}
	}

	@Override
	public void onInvite(final IMessage inviteMessage) {
		try {
			toUSer = inviteMessage.getFromUser();
			sipLayer.sendRinging(user, toUSer);

			setChanged();
			notifyObservers(INVITE_STATUS);
		} catch (ParseException | SipException | InvalidArgumentException e) {
			LOGGER.error("Error on invite.", e);
		}
	}

	@Override
	public void onRinging(final IMessage ringingMessage) {
	}

	@Override
	public void onOk(final IMessage okmessage) {
		try {
			if (((IResponseMessage) okmessage).getRequestMethod() == Request.INVITE) {
				sipLayer.sendAck(user, okmessage.getToUser());
				setChanged();
				notifyObservers();
			}
		} catch (InvalidArgumentException | SipException e) {
			LOGGER.error("Error onOk.", e);
		}

	}

	@Override
	public void onAck(final IMessage message) {
		setChanged();
		notifyObservers();
	}

	@Override
	public void onBye(final IMessage message) {
		try {
			sipLayer.sendOk(user, message.getFromUser());
			setChanged();
			notifyObservers();
		} catch (SipException | InvalidArgumentException | ParseException
				| SdpException e) {
			LOGGER.error("Error onBye.", e);
		}

	}

	@Override
	public void onTimeOut() {
		setChanged();
		notifyObservers();

	}

	@Override
	public void onFailure(final ErrorMessage eMessage) {
		setChanged();
		notifyObservers();

	}

	@Override
	public void onDecline() {
		setChanged();
		notifyObservers();

	}
}
