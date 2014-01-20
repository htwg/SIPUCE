package de.fhkn.in.uce.sip;

import static org.junit.Assert.assertEquals;
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
import de.fhkn.in.uce.sip.core.message.InviteMessage;
import de.fhkn.in.uce.sip.core.message.OkMessage;

public class ServerRecievesInviteAnswersRINGINGButNotOk {

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
		instance1 = new SipUAInstance("User1", UUID.randomUUID());
		instance2 = new SipUAInstance("User2", UUID.randomUUID());

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
	public void inviteNotAnsweredByUAS() {
		try {
			synchronized (lock) {
				sipManagerClient.addListener(sipUserClient, listenerClient);
				sipManagerClient.register(sipUserClient);


				sipManagerServer.addListener(sipUserServer, listenerServer);
				sipManagerServer.register(sipUserServer);
				while(!error && !(user1Registered && user2Registered)) {
					lock.wait();
				}
			}

			sipManagerClient.sendInvite(sipUserClient, sipUserServer);
			synchronized (lock) {
				lock.wait();
			}

			if (!error) {
				System.out.println("-------\n\n Invite is through \n\n ---------");

				sipManagerServer.sendBye(sipUserClient, sipUserServer);
				synchronized (lock) {
					lock.wait();
				}
			}

			sipManagerClient.deregister(sipUserClient);
			sipManagerServer.deregister(sipUserServer);

			Thread.sleep(100);

			//Register Responses
			assertEquals(OkMessage.class, queue.poll().getClass());
			assertEquals(OkMessage.class, queue.poll().getClass());

			//Invite
			assertEquals(InviteMessage.class, queue.poll().getClass());

			//Deregister
			assertEquals(OkMessage.class, queue.poll().getClass());
			assertEquals(OkMessage.class, queue.poll().getClass());

			if (!error) {
				fail();
			}

		} catch (InvalidArgumentException | TooManyListenersException
				| ParseException | SipException | InterruptedException | SdpException e) {
			e.printStackTrace();
			fail();
		}
	}


	ISipManagerListener listenerClient = new ISipManagerListener() {

		@Override
		public void onInvite(final IMessage inviteMessage) {
			System.err.println("Implementation is wrong");
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
			System.err.println("TIME OUT");
			error = true;
			synchronized (lock) {
				lock.notifyAll();
			}
		}

		@Override
		public void onFailure(final ErrorMessage eMessage) {
			System.err.println("FAIL");
			error = true;
			queue.add(eMessage);
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
				sipManagerServer.sendRinging(sipUserServer, sipUserClient);
			} catch (ParseException | SipException | InvalidArgumentException e) {
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
				user2Registered = true;
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
