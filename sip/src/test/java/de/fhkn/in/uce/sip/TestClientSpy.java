package de.fhkn.in.uce.sip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TooManyListenersException;

import javax.sdp.SdpException;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.message.Request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.sip.core.ISipManagerListener;
import de.fhkn.in.uce.sip.core.SipManager;
import de.fhkn.in.uce.sip.core.SipUser;
import de.fhkn.in.uce.sip.core.message.ErrorMessage;
import de.fhkn.in.uce.sip.core.message.IMessage;
import de.fhkn.in.uce.sip.core.message.IResponseMessage;
import de.fhkn.in.uce.sip.core.message.OkMessage;

public class TestClientSpy extends Thread implements ISipManagerListener {


	private static final Logger LOGGER = LoggerFactory.getLogger(TestClientSpy.class);

	private final Queue<IMessage> queue = new LinkedList<>();
	private final Object lock = new Object();

	private final SipUser sipUserClient;

	private SipUser sipUserServer;
	private final SipManager sipManagerClient;
	private State state;

	enum State {REGISTRATING, CALLING, HANGING_UP, UNREGISTERING}

	public TestClientSpy(final SipUser clientUser, final SipManager manager) {
		sipUserClient = clientUser;
		sipManagerClient = manager;
		sipManagerClient.addListener(sipUserClient, this);
	}
	public TestClientSpy(final SipUser clientUser, final SipUser serverUser, final SipManager manager) {
		sipUserClient = clientUser;
		sipUserServer = serverUser;
		sipManagerClient = manager;
		sipManagerClient.addListener(sipUserClient, this);
	}

	public void blockingRegister() throws InterruptedException, IllegalStateException, ParseException, InvalidArgumentException, SipException {
		synchronized (lock) {
			state = State.REGISTRATING;
			sipManagerClient.register(sipUserClient);
			lock.wait();
		}
	}
	public void blockingDeregister() throws InterruptedException, IllegalStateException, ParseException, InvalidArgumentException, SipException, TooManyListenersException {
		synchronized (lock) {
			state = State.UNREGISTERING;
			sipManagerClient.deregister(sipUserClient);
			lock.wait();
		}
	}

	public void connectTo(final SipUser server) throws InterruptedException, ParseException, InvalidArgumentException, SipException, SdpException {
		sipUserServer = server;
		state = State.CALLING;
		synchronized (lock) {
			sipManagerClient.sendInvite(sipUserClient, server);
			lock.wait();
		}
		state = State.HANGING_UP;
		synchronized (lock) {
			sipManagerClient.sendBye(sipUserClient, server);
			lock.wait();
		}
	}

	@Override
	public void onInvite(final IMessage inviteMessage) {
		queue.add(inviteMessage);
		LOGGER.error(sipUserClient.getUsername() + ": Invite on Client");
		fail();
	}

	@Override
	public void onRinging(final IMessage ringingMessage) {
		queue.add(ringingMessage);
		LOGGER.info(sipUserClient.getUsername() + ": RINGING");
	}

	@Override
	public void onOk(final IMessage okmessage) {
		String method = ((IResponseMessage) okmessage).getRequestMethod();
		LOGGER.info(sipUserClient.getUsername() + ": "+ method + " OK");

		if ( method.equals(Request.REGISTER)
				&& (state == State.REGISTRATING
				|| state == State.UNREGISTERING)) {
			synchronized (lock) {
				queue.add(okmessage);
				lock.notify();
			}
			return;
		} else if (method.equals(Request.BYE)
				&& state == State.HANGING_UP) {
			synchronized (lock) {
				queue.add(okmessage);
				lock.notify();
			}
			return;
		} else if (method.equals(Request.INVITE)
				&& state == State.CALLING) {
			synchronized (lock) {
				lock.notifyAll();
			}
		} else {
			LOGGER.warn(sipUserClient.getUsername() + ": Unexpexted OK message");
		}
		queue.add(okmessage);

	}

	@Override
	public void onAck(final IMessage message) {
		queue.add(message);
		LOGGER.info(sipUserClient.getUsername() + ": ACK");
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
		LOGGER.error(sipUserClient.getUsername() + ": Last request timed out");
		synchronized (lock) {
			lock.notifyAll();
		}
		fail();
	}

	@Override
	public void onFailure(final ErrorMessage eMessage) {
		queue.add(eMessage);
		LOGGER.error(sipUserClient.getUsername() + ": " + eMessage.getRequestMethod() +" Failure: " + eMessage.getMessage());
		synchronized (lock) {
			lock.notifyAll();
		}
		fail();
	}

	@Override
	public void onDecline() {
		LOGGER.info(sipUserClient.getUsername() + ": DECLINE");
	}

	public void verify() {

		IMessage message = queue.poll();
		assertEquals(OkMessage.class, message.getClass());
		assertEquals(Request.REGISTER, ((OkMessage) message).getRequestMethod());

		//Invite
		message = queue.poll();
		assertEquals(OkMessage.class, message.getClass());
		assertEquals(Request.INVITE, ((OkMessage) message).getRequestMethod());
		assertEquals(sipUserClient, ((OkMessage) message).getFromUser());
		assertEquals(sipUserServer, ((OkMessage) message).getToUser());

		//Bye
		message = queue.poll();
		assertEquals(OkMessage.class, message.getClass());
		//		if (!Request.BYE.equals(((OkMessage) message).getRequestMethod())) {
		//			System.err.println(((OkMessage) message).getRawResponse());
		//		}
		assertEquals(Request.BYE, ((OkMessage) message).getRequestMethod());

		//Deregister
		message = queue.poll();
		assertEquals(OkMessage.class, message.getClass());
		assertEquals(Request.REGISTER, ((OkMessage) message).getRequestMethod());

	}

	@Override
	public void run() {
		try {
			blockingRegister();
			try {
				connectTo(sipUserServer);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
				try {
					blockingDeregister();
				} catch (IllegalStateException | InterruptedException
						| ParseException | InvalidArgumentException | SipException
						| TooManyListenersException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				org.junit.Assert.fail();
			}
			blockingDeregister();
		} catch (IllegalStateException | InterruptedException | ParseException
				| InvalidArgumentException | SipException | TooManyListenersException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
