package de.fhkn.in.uce.sip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TooManyListenersException;
import java.util.UUID;

import javax.sdp.SdpException;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.message.Request;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fhkn.in.uce.sip.core.ISipManagerListener;
import de.fhkn.in.uce.sip.core.SipManager;
import de.fhkn.in.uce.sip.core.SipPreferences;
import de.fhkn.in.uce.sip.core.SipUAInstance;
import de.fhkn.in.uce.sip.core.SipUser;
import de.fhkn.in.uce.sip.core.message.ErrorMessage;
import de.fhkn.in.uce.sip.core.message.IMessage;
import de.fhkn.in.uce.sip.core.message.IResponseMessage;
import de.fhkn.in.uce.sip.core.message.OkMessage;

public class CallNotRegisteredServer {

	private static SipManager sipManagerClient, sipManagerServer;
	private static SipPreferences preferences1, preferences2;
	private static SipUAInstance instance1, instance2;
	private static SipUser sipUserClient, sipUserServer;


	private final Queue<IMessage> queue = new LinkedList<>();
	private final Object lock = new Object();
	private boolean error;
	private boolean user1Registered;
	private boolean user2Registered;

	@BeforeClass
	public static void setUp() throws Exception {
		instance1 = new SipUAInstance("TestUserAgent1", UUID.randomUUID());
		instance2 = new SipUAInstance("TestUserAgent2", UUID.randomUUID());

		preferences1 = new SipPreferences(new InetSocketAddress(
				"213.239.218.18", 5060));
		preferences2 = new SipPreferences(new InetSocketAddress(
				"213.239.218.18", 5060));

		sipManagerClient = new SipManager(preferences1, instance1);
		sipManagerServer = new SipManager(preferences2, instance2);

		sipUserClient = new SipUser("SipManagerTestUser1", "213.239.218.18");
		sipUserServer = new SipUser("SipManagerTestUser2", "213.239.218.18");
	}

	@AfterClass
	public static void tearDown() throws Exception {
		sipManagerClient.shutdown();
		sipManagerServer.shutdown();
	}

	@Test
	public void registerTest() {
		try {
			synchronized (lock) {
				sipManagerClient.addListener(sipUserClient, listenerClient);
				sipManagerClient.register(sipUserClient);

				while(!error && !(user1Registered)) {
					lock.wait();
				}
			}

			sipManagerClient.sendInvite(sipUserClient, sipUserServer);
			synchronized (lock) {
				lock.wait();
			}
			if (!error) {
				System.out.println("-------\n\n Invite is through \n\n ---------");

				sipManagerClient.sendBye(sipUserClient, sipUserServer);
				synchronized (lock) {
					lock.wait(1000);
				}
			}

			sipManagerClient.deregister(sipUserClient);

			Thread.sleep(100);

			//Register Responses
			assertTrue(queue.poll() instanceof OkMessage);

			//Error
			assertEquals(queue.poll().getClass() , ErrorMessage.class);

			//Deregister
			assertEquals(queue.poll().getClass() , OkMessage.class);

		} catch (InvalidArgumentException | TooManyListenersException
				| ParseException | SipException | InterruptedException | SdpException e) {
			e.printStackTrace();
			fail();
		}
	}


	public boolean isUser2Registered() {
		return user2Registered;
	}

	public void setUser2Registered(final boolean user2Registered) {
		this.user2Registered = user2Registered;
	}


	ISipManagerListener listenerClient = new ISipManagerListener() {

		@Override
		public void onInvite(final IMessage inviteMessage) {
			queue.add(inviteMessage);
			try {
				sipManagerClient.sendOk(sipUserClient, sipUserServer);
			} catch (SipException | InvalidArgumentException | ParseException
					| SdpException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onRinging(final IMessage ringingMessage) {
			queue.add(ringingMessage);
		}

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
			if (((IResponseMessage) okmessage).getRequestMethod() == Request.INVITE) {
				try {
					sipManagerClient.sendAck(sipUserClient, sipUserServer);
					synchronized (lock) {
						lock.notifyAll();
					}
				} catch (InvalidArgumentException | SipException e) {
					e.printStackTrace();
				}
			}
			queue.add(okmessage);

		}

		@Override
		public void onAck(final IMessage message) {
			queue.add(message);
		}

		@Override
		public void onBye(final IMessage message) {
			queue.add(message);
			try {
				sipManagerClient.sendOk(sipUserClient, sipUserServer);
			} catch (InvalidArgumentException | SipException | ParseException | SdpException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onTimeOut() {
			System.err.println("TIME_OUT");
			error = true;
			synchronized (lock) {
				lock.notifyAll();
			}
		}

		@Override
		public void onFailure(final ErrorMessage eMessage) {
			System.err.println("FAIL");
			queue.add(eMessage);
			error = true;
			synchronized (lock) {
				lock.notifyAll();
			}
		}

		@Override
		public void onDecline() {

		}
	};

	ISipManagerListener listenerServer = new ISipManagerListener() {

		@Override
		public void onInvite(final IMessage inviteMessage) {
			queue.add(inviteMessage);
			try {
				sipManagerServer.sendOk(sipUserServer, sipUserClient);
			} catch (SipException | InvalidArgumentException | ParseException
					| SdpException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onRinging(final IMessage ringingMessage) {
			queue.add(ringingMessage);
		}

		@Override
		public void onOk(final IMessage okmessage) {
			queue.add(okmessage);
			if (((IResponseMessage) okmessage).getRequestMethod() == Request.REGISTER) {
				setUser2Registered(true);
				synchronized (lock) {
					lock.notify();
				}
			}
			if (((IResponseMessage) okmessage).getRequestMethod() == Request.INVITE) {
				try {
					sipManagerServer.sendAck(sipUserServer, sipUserClient);
					synchronized (lock) {
						lock.notifyAll();
					}
				} catch (InvalidArgumentException | SipException e) {
					e.printStackTrace();
				}
			}
			if (((IResponseMessage) okmessage).getRequestMethod() == Request.BYE) {
				synchronized (lock) {
					lock.notifyAll();
				}
			}

		}

		@Override
		public void onAck(final IMessage message) {
			queue.add(message);
		}

		@Override
		public void onBye(final IMessage message) {
			queue.add(message);
			try {
				sipManagerServer.sendOk(sipUserServer, sipUserClient);
			} catch (InvalidArgumentException | SipException | ParseException | SdpException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onTimeOut() {
			synchronized (lock) {
				lock.notifyAll();
			}
		}

		@Override
		public void onFailure(final ErrorMessage eMessage) {
			queue.add(eMessage);
			synchronized (lock) {
				lock.notifyAll();
			}
		}

		@Override
		public void onDecline() {

		}
	};
}
