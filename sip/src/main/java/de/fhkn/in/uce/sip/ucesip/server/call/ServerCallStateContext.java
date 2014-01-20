package de.fhkn.in.uce.sip.ucesip.server.call;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.sip.core.ISipManager;
import de.fhkn.in.uce.sip.core.SipUser;
import de.fhkn.in.uce.sip.core.message.IMessage;
import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;
import de.fhkn.in.uce.sip.ucesip.message.IUCESipMessage;
import de.fhkn.in.uce.sip.ucesip.settings.UCESipServerSettings;

class ServerCallStateContext {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(UCESipServerCall.class);

	private final ISipManager sipManager;
	private final SipUser serverUser;
	private final SipUser clientUser;
	private final UCESipServerSettings sipSettings;
	private final Lock stateLock;
	private final Condition stateChangeCondition;
	private final Timer stateTimeoutTimer;

	private volatile StateTimeoutTask currentStateTimeoutTask;
	private volatile IServerCallState currentServerState;

	public ServerCallStateContext(ISipManager sipManager, SipUser serverUser, SipUser clientUser, UCESipServerSettings sipSettings) {
		this.sipManager = sipManager;
		this.serverUser = serverUser;
		this.clientUser = clientUser;
		this.sipSettings = sipSettings;
		this.stateLock = new ReentrantLock();
		this.stateChangeCondition = stateLock.newCondition();

		this.stateTimeoutTimer = new Timer("Sip user agent server call client: " + clientUser.toString() + " state timeout timer");
		this.currentServerState = new InitServerCallState(this);
	}

	public SipUser getServerUser() {
		return serverUser;
	}

	public ISipManager getSipManager() {
		return sipManager;
	}

	public SipUser getClientUser() {
		return clientUser;
	}

	public UCESipServerSettings getSipSettings() {
		return sipSettings;
	}

	public void onBye(IMessage message) {
		stateLock.lock();
		try {
			currentServerState.onBye(message);
		} finally {
			stateLock.unlock();
		}
	}

	public void sendDecline() throws UCESipException {
		stateLock.lock();
		try {
			currentServerState.sendDecline();
		} finally {
			stateLock.unlock();
		}
	}

	public void sendOK(final IUCESipMessage okMessage) throws UCESipException {
		stateLock.lock();
		try {
			LOGGER.trace("sip server call sendOK");
			currentServerState.sendOK(okMessage);
		} finally {
			stateLock.unlock();
		}
	}

	public void sendQueued() throws UCESipException {
		stateLock.lock();
		try {
			currentServerState.sendQueued();
		} finally {
			stateLock.unlock();
		}
	}

	public void sendRinging() throws UCESipException {
		stateLock.lock();
		try {
			LOGGER.trace("sip server call sendRinging");
			currentServerState.sendRinging();
		} finally {
			stateLock.unlock();
		}
	}
	
	public boolean isFinished() {
		stateLock.lock();
		try {
			return currentServerState.isFinished();
		} finally {
			stateLock.unlock();
		}
	}

	/**
	 * Call only with state lock, from state itself it should have the lock
	 * already
	 */
	public void changeState(IServerCallState newState) {
		if (currentServerState.equals(newState) == false) {
			// reset state timeout timer.
			if (currentStateTimeoutTask != null) {
				currentStateTimeoutTask.cancel();
				currentStateTimeoutTask = null;
			}
			if (newState.getStateTimeoutMillis() != 0) {
				currentStateTimeoutTask = new StateTimeoutTask();
				stateTimeoutTimer.schedule(currentStateTimeoutTask, newState.getStateTimeoutMillis());
			}
			currentServerState = newState;
			stateChangeCondition.signal();
			LOGGER.trace("sip server call client: " + clientUser.toString() + " changed state to " + newState.getCurrentState().toString());
		}
	}

	private class StateTimeoutTask extends TimerTask {

		private volatile boolean isCanceled;

		public StateTimeoutTask() {
			this.isCanceled = false;
		}

		@Override
		public void run() {
			stateLock.lock();
			try {
				// check threadsafe if the state is canceled
				if (isCanceled) {
					return;
				}
				currentServerState.onStateTimeout();
			} finally {
				stateLock.unlock();
			}
		}

		/**
		 * Must be called within the state lock.
		 */
		@Override
		public boolean cancel() {
			isCanceled = true;
			return super.cancel();
		}
	}

}