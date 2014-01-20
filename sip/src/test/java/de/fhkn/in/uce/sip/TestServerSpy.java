package de.fhkn.in.uce.sip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TooManyListenersException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import javax.sdp.SdpException;
import javax.sip.InvalidArgumentException;
import javax.sip.ObjectInUseException;
import javax.sip.SipException;
import javax.sip.message.Request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.sip.core.ISipManagerListener;
import de.fhkn.in.uce.sip.core.SipManager;
import de.fhkn.in.uce.sip.core.SipUser;
import de.fhkn.in.uce.sip.core.message.ByeMessage;
import de.fhkn.in.uce.sip.core.message.ErrorMessage;
import de.fhkn.in.uce.sip.core.message.IMessage;
import de.fhkn.in.uce.sip.core.message.IResponseMessage;
import de.fhkn.in.uce.sip.core.message.InviteMessage;
import de.fhkn.in.uce.sip.core.message.OkMessage;

public class TestServerSpy extends Thread implements ISipManagerListener{


	private static final Logger LOGGER = LoggerFactory.getLogger(TestServerSpy.class);

	private final Queue<IMessage> queue = new LinkedList<>();
	private final BlockingQueue<IMessage> inviteQueue = new LinkedBlockingDeque<>();
	private final Object lock = new Object();

	private static SipUser sipUserServer;
	private static SipManager sipManagerServer;
	private State state;

	private SipUser currentClient;
	private int inviteCount;

	private boolean shutdown;

	enum State {REGISTRATING, CALLING, HANGING_UP, UNREGISTERING}

	public TestServerSpy(final SipUser serverUser, final SipManager manager) {
		sipUserServer = serverUser;
		sipManagerServer = manager;
		sipManagerServer.addListener(sipUserServer, this);
	}

	public void blockingRegister() throws InterruptedException, IllegalStateException, ParseException, InvalidArgumentException, SipException {
		synchronized (lock) {
			state = State.REGISTRATING;
			sipManagerServer.register(sipUserServer);
			lock.wait();
		}
	}
	public void blockingDeregister() throws InterruptedException, IllegalStateException, ParseException, InvalidArgumentException, SipException, TooManyListenersException {
		synchronized (lock) {
			state = State.UNREGISTERING;
			sipManagerServer.deregister(sipUserServer);
			lock.wait();
		}
	}


	public void accept() throws InterruptedException, SipException, InvalidArgumentException, ParseException, SdpException {
		IMessage invite = inviteQueue.take();
		synchronized (lock) {
			currentClient = invite.getFromUser();
			sipManagerServer.sendOk(sipUserServer, currentClient);
			lock.wait();
		}
	}

	@Override
	public void onInvite(final IMessage inviteMessage) {
		LOGGER.info(sipUserServer.getUsername() + ": Invite from " + inviteMessage.getFromUser().getUsername());
		queue.add(inviteMessage);
		inviteCount++;
		try {
			sipManagerServer.sendQueued(sipUserServer, inviteMessage.getFromUser());
			inviteQueue.add(inviteMessage);
		} catch (ParseException | SipException | InvalidArgumentException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onRinging(final IMessage ringingMessage) {
		queue.add(ringingMessage);
		LOGGER.error(sipUserServer.getUsername() + ": RINGING on Server?");
	}

	@Override
	public void onOk(final IMessage okmessage) {
		String method = ((IResponseMessage) okmessage).getRequestMethod();
		LOGGER.info(sipUserServer.getUsername() + ": "+ method + " OK");

		if ( method.equals(Request.REGISTER)
				&& (state == State.REGISTRATING
				|| state == State.UNREGISTERING)) {
			synchronized (lock) {
				queue.add(okmessage);
				lock.notify();
			}
			return;
		} else {
			LOGGER.warn(sipUserServer.getUsername() + ": Unexpexted OK message");
		}
		queue.add(okmessage);

	}

	@Override
	public void onAck(final IMessage message) {
		queue.add(message);
		LOGGER.info(sipUserServer.getUsername() + ": ACK");
	}

	@Override
	public void onBye(final IMessage message) {
		LOGGER.info(sipUserServer.getUsername() + ": BYE from " + message.getFromUser().getUsername());
		queue.add(message);
		assertEquals(currentClient, message.getFromUser());
		try {
			sipManagerServer.sendOk(sipUserServer, currentClient);
			synchronized (lock) {
				lock.notify();
			}
		} catch (InvalidArgumentException | SipException | ParseException | SdpException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onTimeOut() {
		LOGGER.error(sipUserServer.getUsername() + ": Last request timed out");
		synchronized (lock) {
			lock.notifyAll();
		}
	}

	@Override
	public void onFailure(final ErrorMessage eMessage) {
		queue.add(eMessage);
		LOGGER.error(sipUserServer.getUsername() + ": " + eMessage.getRequestMethod() +" Failure: " + eMessage.getMessage());
		synchronized (lock) {
			lock.notifyAll();
		}
	}

	@Override
	public void onDecline() {
		LOGGER.info(sipUserServer.getUsername() + ": DECLINE");
	}

	public void verify() {

		IMessage message = queue.poll();
		assertEquals(OkMessage.class, message.getClass());
		assertEquals(Request.REGISTER, ((OkMessage) message).getRequestMethod());

		for (int i = 0; i <inviteCount*2; i++) {
			//Invite
			message = queue.poll();
			assertEquals(sipUserServer, message.getToUser());
			assertTrue(message.getClass() == InviteMessage.class
					|| message.getClass() == ByeMessage.class);
		}

		//Deregister
		message = queue.poll();
		assertEquals(OkMessage.class, message.getClass());
		assertEquals(Request.REGISTER, ((OkMessage) message).getRequestMethod());

	}

	public void shutdown() {
		shutdown = true;

	}

	@Override
	public void run() {
		try {
			blockingRegister();
			try {
				while (!shutdown) {
					accept();
				}
			} catch (InterruptedException e) {
			} catch (Exception e) {
				LOGGER.error(e.toString());
				try {
					blockingDeregister();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				org.junit.Assert.fail();
				return;
			}
			blockingDeregister();

		} catch (Exception e2) {
			e2.printStackTrace();
		}
		try {
			sipManagerServer.shutdown();
		} catch (ObjectInUseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
