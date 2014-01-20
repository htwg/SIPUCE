package de.fhkn.in.uce.sip;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TooManyListenersException;
import java.util.UUID;

import javax.sip.InvalidArgumentException;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.SipException;
import javax.sip.TransportNotSupportedException;
import javax.sip.message.Request;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fhkn.in.uce.sip.core.ISipManager;
import de.fhkn.in.uce.sip.core.ISipManagerListener;
import de.fhkn.in.uce.sip.core.SipManager;
import de.fhkn.in.uce.sip.core.SipPreferences;
import de.fhkn.in.uce.sip.core.SipUAInstance;
import de.fhkn.in.uce.sip.core.SipUser;
import de.fhkn.in.uce.sip.core.message.ErrorMessage;
import de.fhkn.in.uce.sip.core.message.IMessage;
import de.fhkn.in.uce.sip.core.message.IResponseMessage;

public class UseOfWrongProxyAddress {

	private static SipManager sipManager1;
	private static SipPreferences preferences1;
	private static SipUAInstance instance1;
	private static SipUser sipUser1;


	private final Queue<IMessage> queue = new LinkedList<>();
	private final Object lock = new Object();
	private boolean error;
	private boolean user1Registered;

	@BeforeClass
	public static void setUp() throws Exception {
		instance1 = new SipUAInstance("TestUserAgent1", UUID.randomUUID());

		preferences1 = new SipPreferences(new InetSocketAddress(
				"213.239.218.18", 5060));

		sipManager1 = new SipManager(preferences1, instance1);

		sipUser1 = new SipUser("SipManagerTestUser1", "213.239.218.19");
	}

	@AfterClass
	public static void tearDown() throws Exception {
		sipManager1.shutdown();
	}

	@Test
	public void registerWithWrongDomain() {
		try {
			synchronized (lock) {
				sipManager1.addListener(sipUser1, listener1);
				sipManager1.register(sipUser1);
				while(!error && !(user1Registered)) {
					lock.wait();
				}
			}

			if (!error) {
				sipManager1.deregister(sipUser1);
				fail();
			}

		} catch (InvalidArgumentException | TooManyListenersException
				| ParseException | SipException | InterruptedException e) {
			e.printStackTrace();
			fail();
		}
	}


	@Test(expected=ConnectException.class)
	public void initStackToWrongIP() throws PeerUnavailableException, TransportNotSupportedException, ObjectInUseException, InvalidArgumentException, TooManyListenersException, IOException, InterruptedException {
		SipUAInstance instance = new SipUAInstance("TestUserAgent1", UUID.randomUUID());

		SipPreferences preferences = new SipPreferences(new InetSocketAddress(
				"213.239.218.19", 5060));

		ISipManager sipManager = new SipManager(preferences, instance);
		sipManager.shutdown();
	}

	@Test(expected=ConnectException.class)
	public void initStackToWrongPort() throws PeerUnavailableException, TransportNotSupportedException, ObjectInUseException, InvalidArgumentException, TooManyListenersException, IOException, InterruptedException {
		SipUAInstance instance = new SipUAInstance("TestUserAgent1", UUID.randomUUID());

		SipPreferences preferences = new SipPreferences(new InetSocketAddress(
				"213.239.218.18", 5065));

		ISipManager sipManager = new SipManager(preferences, instance);
		sipManager.shutdown();
	}

	@Test
	public void initStackToWrongPort80() throws PeerUnavailableException, TransportNotSupportedException, ObjectInUseException, InvalidArgumentException, TooManyListenersException, IOException, InterruptedException {
		SipPreferences preferences = new SipPreferences(new InetSocketAddress(
				"213.239.218.18", 80));

		ISipManager sipManager = new SipManager(preferences, instance1);
		try {
			synchronized (lock) {
				sipManager.addListener(sipUser1, listener1);
				sipManager.register(sipUser1);
				while(!error && !(user1Registered)) {
					lock.wait();
				}
			}

			if (!error) {
				sipManager.deregister(sipUser1);
				fail();
			}

		} catch (InvalidArgumentException | TooManyListenersException
				| ParseException | SipException | InterruptedException e) {
			e.printStackTrace();
			fail();
		} finally {
			sipManager.shutdown();
		}
	}

	@Test(expected=ConnectException.class)
	public void initStackToGoogle() throws PeerUnavailableException, TransportNotSupportedException, ObjectInUseException, InvalidArgumentException, TooManyListenersException, IOException, InterruptedException {
		SipUAInstance instance = new SipUAInstance("TestUserAgent1", UUID.randomUUID());

		SipPreferences preferences = new SipPreferences(new InetSocketAddress(
				"109.193.193.30", 5060));

		ISipManager sipManager = new SipManager(preferences, instance);
		try {
			synchronized (lock) {
				sipManager1.addListener(sipUser1, listener1);
				sipManager1.register(sipUser1);
				while(!error && !(user1Registered)) {
					lock.wait();
				}
			}

			if (!error) {
				sipManager1.deregister(sipUser1);
				fail();
			}

		} catch (InvalidArgumentException | TooManyListenersException
				| ParseException | SipException | InterruptedException e) {
			e.printStackTrace();
			fail();
		}
		sipManager.shutdown();
	}


	ISipManagerListener listener1 = new ISipManagerListener() {

		@Override
		public void onInvite(final IMessage inviteMessage) {}

		@Override
		public void onRinging(final IMessage ringingMessage) { }

		@Override
		public void onOk(final IMessage okmessage) {
			if (((IResponseMessage) okmessage).getRequestMethod() == Request.REGISTER) {
				user1Registered = true;
				synchronized (lock) {
					queue.add(okmessage);
					lock.notify();
				}
				return;
			}
		}

		@Override
		public void onAck(final IMessage message) { }

		@Override
		public void onBye(final IMessage message) { }

		@Override
		public void onTimeOut() {
			System.err.println("TIMED_OUT");
			error = true;
			synchronized (lock) {
				lock.notifyAll();
			}
		}

		@Override
		public void onFailure(final ErrorMessage eMessage) {
			System.err.println("FAILED");
			error = true;
			queue.add(eMessage);
			synchronized (lock) {
				lock.notifyAll();
			}
		}

		@Override
		public void onDecline() { }
	};

}
