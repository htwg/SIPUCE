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
import de.fhkn.in.uce.sip.core.message.ByeMessage;
import de.fhkn.in.uce.sip.core.message.ErrorMessage;
import de.fhkn.in.uce.sip.core.message.IMessage;
import de.fhkn.in.uce.sip.core.message.IResponseMessage;
import de.fhkn.in.uce.sip.core.message.InviteMessage;
import de.fhkn.in.uce.sip.core.message.OkMessage;

public class SipWorkFlowTest {

	private static SipManager sipManagerClient, sipManagerServer;
	private static SipPreferences preferencesClient, preferencesServer;
	private static SipUAInstance instanceClient, instanceServer;
	private static SipUser sipUserClient, sipUserServer;


	private final Queue<IMessage> queue = new LinkedList<>();
	private final Object lock = new Object();
	private boolean error;
	private boolean user1Registered;
	private boolean user2Registered;

	@BeforeClass
	public static void setUp() throws Exception {
		instanceClient = new SipUAInstance("User1", UUID.randomUUID());
		instanceServer = new SipUAInstance("User2", UUID.randomUUID());

		preferencesClient = new SipPreferences(new InetSocketAddress(
				"213.239.218.18", 5060));
		//		preferencesClient.setSipTransportProtocol("tcp");
		preferencesServer = new SipPreferences(new InetSocketAddress(
				"213.239.218.18", 5060));
		//		preferencesServer.setSipTransportProtocol("tcp");

		sipManagerClient = new SipManager(preferencesClient, instanceClient);
		sipManagerServer = new SipManager(preferencesServer, instanceServer);

		sipUserClient = new SipUser("SipManagerTestUserClient1", "213.239.218.18");
		sipUserServer = new SipUser("SipManagerTestUserServer1", "213.239.218.18");
	}

	@AfterClass
	public static void tearDown() throws Exception {
		sipManagerClient.shutdown();
		sipManagerServer.shutdown();
	}

	@Test
	public void sipWorkflowtest() {
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
			System.out.println("-------\n\n Invite is through \n\n ---------");

			sipManagerClient.sendBye(sipUserClient, sipUserServer);
			synchronized (lock) {
				lock.wait();
			}
			synchronized (lock) {
				sipManagerClient.deregister(sipUserClient);
				lock.wait();
			}
			synchronized (lock) {
				sipManagerServer.deregister(sipUserServer);
				lock.wait();
			}

			//Register Responses
			assertEquals(OkMessage.class, queue.poll().getClass());
			assertEquals(OkMessage.class, queue.poll().getClass());

			//Invite
			assertTrue(queue.poll() instanceof InviteMessage);
			assertEquals(OkMessage.class, queue.poll().getClass());
			//assertTrue(queue.poll() instanceof AckMessage);

			//Bye
			assertEquals(ByeMessage.class, queue.poll().getClass());
			assertEquals(OkMessage.class, queue.poll().getClass());

			//Deregister
			assertEquals(OkMessage.class, queue.poll().getClass());
			assertEquals(OkMessage.class, queue.poll().getClass());

		} catch (InvalidArgumentException | TooManyListenersException
				| ParseException | SipException | InterruptedException | SdpException e) {
			e.printStackTrace();
			fail();
		}
	}


	ISipManagerListener listenerClient = new ISipManagerListener() {

		@Override
		public void onInvite(final IMessage inviteMessage) {
			queue.add(inviteMessage);
			System.err.println("Invite on Client");
			fail();
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
			if (((IResponseMessage) okmessage).getRequestMethod() == Request.BYE) {
				synchronized (lock) {
					queue.add(okmessage);
					lock.notify();
				}
				return;
			}
			if (((IResponseMessage) okmessage).getRequestMethod() == Request.INVITE) {
				synchronized (lock) {
					//					try {
					//						sipManagerClient.sendAck(sipUserClient, sipUserServer);
					//					} catch (InvalidArgumentException | SipException e) {
					//						// TODO Auto-generated catch block
					//						e.printStackTrace();
					//					}
					lock.notifyAll();
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
