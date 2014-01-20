package de.fhkn.in.uce.sip.ucesip.client;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.sip.core.ISipManager;
import de.fhkn.in.uce.sip.core.SipUser;
import de.fhkn.in.uce.sip.core.message.ErrorMessage;
import de.fhkn.in.uce.sip.core.message.IMessage;
import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;
import de.fhkn.in.uce.sip.ucesip.message.IUCESipMessage;
import de.fhkn.in.uce.sip.ucesip.message.UCESipMessage;
import de.fhkn.in.uce.sip.ucesip.settings.UCESipClientSettings;

class InviteStateContext implements IInviteStateObserver {

	static final Logger LOGGER = LoggerFactory.getLogger(InviteStateContext.class);

	private final ISipManager sipManager;
	private final SipUser toUser;
	private final SipUser fromUser;
	private final UCESipClientSettings sipSettings;

	private final Lock inviteResponseLock;
	private final Condition inviteResponseCondition;

	private final Lock stateLock;
	private final Condition stateChangeCondition;

	private final Timer stateTimeoutTimer;

	private volatile StateTimeoutTask currentStateTimeoutTask;

	private volatile IInviteState inviteState;

	private volatile UCESipMessage inviteResponseMessage;
	private volatile Exception inviteResponseError;

	InviteStateContext(final SipUser sipUser, final SipUser toSipUser, final UCESipClientSettings settings, ISipManager sipManager)
			throws UCESipException {

		this.toUser = toSipUser;
		this.fromUser = sipUser;
		this.sipManager = sipManager;
		this.sipSettings = settings;

		this.inviteResponseLock = new ReentrantLock();
		this.inviteResponseCondition = inviteResponseLock.newCondition();

		this.stateLock = new ReentrantLock();
		this.stateChangeCondition = stateLock.newCondition();

		this.stateTimeoutTimer = new Timer("Sip user agent client state timeout timer");

		this.inviteResponseError = null;
		this.inviteResponseMessage = null;
		this.inviteState = new InitInviteState(this);
	}

	public void shutdown() {
		callShutdownOnState();

		try {
			waitForAFinishedState();
		} catch (Exception e) {
			LOGGER.info("Shutdown failed", e);
		}
	}

	public IUCESipMessage inviteUAS(IUCESipMessage message) throws UCESipException, InterruptedException {

		stateLock.lock();
		try {
			inviteState.inviteUAS(message);
		} finally {
			stateLock.unlock();
		}

		Exception tRespError;
		UCESipMessage tRespMsg;

		inviteResponseLock.lock();
		try {
			while (true) {
				if (inviteResponseMessage != null) {
					break;
				}
				if (inviteResponseError != null) {
					break;
				}
				inviteResponseCondition.await();
			}
			tRespError = inviteResponseError;
			tRespMsg = inviteResponseMessage;
		} finally {
			inviteResponseLock.unlock();
		}

		if (inviteResponseError != null) {
			throw new UCESipException("Error on handling connection to server", tRespError);
		}

		return tRespMsg;
	}

	@Override
	public UCESipClientSettings getSipSettings() {
		return sipSettings;
	}

	@Override
	public SipUser getFromUser() {
		return fromUser;
	}

	@Override
	public SipUser getToUser() {
		return toUser;
	}

	@Override
	public ISipManager getSipManager() {
		return sipManager;
	}

	public void onOk(IMessage okmessage) {
		stateLock.lock();
		try {
			inviteState.onOk(okmessage);
		} finally {
			stateLock.unlock();
		}
	}

	public void onTimeOut() {
		stateLock.lock();
		try {
			inviteState.onTimeOut();
		} finally {
			stateLock.unlock();
		}
	}

	public void onDecline() {
		stateLock.lock();
		try {
			inviteState.onDecline();
		} finally {
			stateLock.unlock();
		}
	}

	public void onFailure(ErrorMessage eMessage) {
		stateLock.lock();
		try {
			inviteState.onFailure(eMessage);
		} finally {
			stateLock.unlock();
		}
	}

	/**
	 * Is called within a state.
	 */
	@Override
	public void setInviteResponseMessage(UCESipMessage msg) {
		inviteResponseLock.lock();
		try {
			inviteResponseMessage = msg;
			inviteResponseCondition.signal();
		} finally {
			inviteResponseLock.unlock();
		}
	}

	/**
	 * Is called within a state.
	 */
	@Override
	public void setInviteResponseError(Exception error) {
		inviteResponseLock.lock();
		try {
			inviteResponseMessage = null;
			inviteResponseError = error;
			inviteResponseCondition.signal();
		} finally {
			inviteResponseLock.unlock();
		}
	}

	private void waitForAFinishedState() throws InterruptedException {
		stateLock.lock();
		try {
			while (true) {
				if (inviteState.getState() == InviteStates.Finished) {
					break;
				}
				if (inviteState.getState() == InviteStates.Error) {
					break;
				}
				stateChangeCondition.await();
			}
		} catch (InterruptedException e) {
			throw e;
		} finally {
			stateLock.unlock();
		}
	}

	private void callShutdownOnState() {
		stateLock.lock();
		try {
			inviteState.shutdown();
		} finally {
			stateLock.unlock();
		}
	}

	/**
	 * Call only with having the stateLock lock! Only change state with this
	 * method!
	 * 
	 * @param state
	 *            Old or new state.
	 */
	@Override
	public void changeState(IInviteState state) {
		if (inviteState.equals(state) == false) {
			// reset state timeout timer.
			if (currentStateTimeoutTask != null) {
				currentStateTimeoutTask.cancel();
				currentStateTimeoutTask = null;
			}
			if (state.getStateTimeoutMillis() != 0) {
				currentStateTimeoutTask = new StateTimeoutTask(state);
				stateTimeoutTimer.schedule(currentStateTimeoutTask, state.getStateTimeoutMillis());
			}
			inviteState = state;
			stateChangeCondition.signal();
		}
	}

	private class StateTimeoutTask extends TimerTask {

		private final IInviteState state;

		private volatile boolean isCanceled;

		public StateTimeoutTask(IInviteState state) {
			this.isCanceled = false;
			this.state = state;
		}

		@Override
		public void run() {
			stateLock.lock();
			try {
				// check threadsafe if the state is canceled
				if (isCanceled) {
					return;
				}
				state.onStateTimeout();
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