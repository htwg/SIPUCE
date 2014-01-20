package de.fhkn.in.uce.sip.ucesip.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.sip.core.ISipManager;
import de.fhkn.in.uce.sip.core.RegisterRefreshThread;
import de.fhkn.in.uce.sip.core.SipUser;
import de.fhkn.in.uce.sip.core.message.ErrorMessage;
import de.fhkn.in.uce.sip.core.message.IMessage;
import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;
import de.fhkn.in.uce.sip.ucesip.server.call.UCESipServerCall;
import de.fhkn.in.uce.sip.ucesip.settings.UCESipServerSettings;

/**
 * The handler for the server states.
 * 
 */
class ServerStateContext implements IServerStateContext {

	private static final Logger LOGGER = LoggerFactory.getLogger(UCESipUserAgentServer.class);

	private final ISipManager sipManager;
	private final SipUser serverUser;
	private final UCESipServerSettings sipSettings;
	private final Lock stateLock;
	private final Condition stateChangeCondition;
	private final Timer stateTimeoutTimer;
	private final Lock takeCallLock;
	private final Condition takeCallCondition;
	private final Map<String, UCESipServerCall> serverCallMap;
	private final RegisterRefreshThread registerRefreshThread;
	private final BlockingQueue<UCESipServerCall> callQueue;
	private final Timer callMapCleanupTimer;
	private final CallMapCleanupTask callMapCleanupTask;

	// The states can ask this variable if there was a shutdown request
	private volatile boolean isShutdown;
	private volatile StateTimeoutTask currentStateTimeoutTask;
	private volatile IServerState currentServerState;
	private volatile boolean isTaking;

	public ServerStateContext(final ISipManager sipManager, final SipUser serverUser, final UCESipServerSettings sipSettings) {
		this.sipManager = sipManager;
		this.serverUser = serverUser;
		this.sipSettings = sipSettings;

		this.stateLock = new ReentrantLock();
		this.stateChangeCondition = stateLock.newCondition();

		this.takeCallLock = new ReentrantLock();
		this.takeCallCondition = takeCallLock.newCondition();

		this.serverCallMap = new HashMap<String, UCESipServerCall>();
		this.registerRefreshThread = new RegisterRefreshThread(this.sipManager, this.serverUser);
		
		this.callMapCleanupTimer = new Timer("Server call map cleanup");
		this.callMapCleanupTask = new CallMapCleanupTask();

		this.callQueue = new ArrayBlockingQueue<>(sipSettings.getCallQueueCapacity());

		this.isTaking = false;
		this.isShutdown = false;
		this.stateTimeoutTimer = new Timer("Sip user agent server state timeout timer");
		this.currentServerState = new InitServerState(this);
	}

	public void register() throws UCESipException, InterruptedException {
		stateLock.lock();
		try {
			currentServerState.startRegister();
			while (true) {
				if (currentServerState.getCurrentState() == ServerStates.Registered) {
					break;
				}
				if (currentServerState.getCurrentState() == ServerStates.Error) {
					// check for asynchronous error
					throw ((ErrorServerState) currentServerState).getError();
				}
				stateChangeCondition.await();
			}
			long tp = sipSettings.getServerCallMapCleanupPeriodMillis();
			callMapCleanupTimer.scheduleAtFixedRate(callMapCleanupTask, tp, tp);
		} catch (InterruptedException e) {
			throw e;
		} finally {
			stateLock.unlock();
		}
	}

	/**
	 * Calls shutdown on current state and waits until a finished state is
	 * reached.
	 * 
	 * @throws InterruptedException
	 *             if the waiting for a finished state is interrupted.
	 */
	public void shutdown() throws InterruptedException {
		stateLock.lock();
		try {
			isShutdown = true;
			
			// interrupts any take operations
			takeCallLock.lock();
			try {
				takeCallCondition.signalAll();
			} finally {
				takeCallLock.unlock();
			}
			
			currentServerState.shutdown();
			while (true) {
				if (currentServerState.getCurrentState() == ServerStates.Finished) {
					break;
				}
				if (currentServerState.getCurrentState() == ServerStates.Error) {
					break;
				}
				stateChangeCondition.await();
			}
			callMapCleanupTimer.cancel();
			callMapCleanupTask.cancel();
			stateTimeoutTimer.cancel();
		} catch (InterruptedException e) {
			throw e;
		} finally {
			stateLock.unlock();
		}
	}

	private class CallMapCleanupTask extends TimerTask {

		private volatile boolean isCanceled;

		public CallMapCleanupTask() {
			this.isCanceled = false;
		}

		@Override
		public void run() {
			stateLock.lock();
			try {
				// check threadsafe if the state was canceled
				if (isCanceled) {
					return;
				}

				List<String> tRemoveKeyList = new ArrayList<>();
				for (Entry<String, UCESipServerCall> i : serverCallMap.entrySet()) {
					if (i.getValue().isFinished()) {
						tRemoveKeyList.add(i.getKey());
					}
				}
				// delete in map
				for (String string : tRemoveKeyList) {
					serverCallMap.remove(string);
				}

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

	@Override
	public void onNewCallReceived(final UCESipServerCall newCall, final int currentElementsInQueue) {
		takeCallLock.lock();
		try {
			if ((isTaking == false) || (currentElementsInQueue > 1)) {
				try {
					newCall.sendQueued();
					LOGGER.trace("sent queued to " + newCall.getClientUser().toString());
				} catch (UCESipException e) {
					LOGGER.info("Error sending queued on new call", e);
				}
			}
			takeCallCondition.signal();
		} finally {
			takeCallLock.unlock();
		}
	}

	public UCESipServerCall takeCall(final int timeoutMillis) throws InterruptedException, TimeoutException {
		while (true) {
			if (isShutdown) {
				throw new InterruptedException("Shutdown requested");
			}
			// Check unblocked if a new call is available
			UCESipServerCall tcall = pollCall();
			if (tcall != null) {
				return tcall;
			}
			waitOnTakeCallAvailableCond(timeoutMillis);
		}
	}

	private void waitOnTakeCallAvailableCond(final int timeoutMillis) throws TimeoutException, InterruptedException {
		takeCallLock.lock();
		try {
			try {
				isTaking = true;
				awaitTimeoutOnZeroInfinity(takeCallCondition, timeoutMillis);
			} finally {
				isTaking = false;
			}
		} finally {
			takeCallLock.unlock();
		}
	}

	/**
	 * Unfortunately the await function of condition variables does not wait if
	 * you pass zero as timeout. This function supports this.
	 * 
	 * @param cond
	 *            The condition to wait on.
	 * @param timeoutMillis
	 *            How long to wait for the condition before a TimeoutException
	 *            is thrown. If 0 the wait time is infinity.
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	private static void awaitTimeoutOnZeroInfinity(final Condition cond, final int timeoutMillis) throws TimeoutException, InterruptedException {
		if (timeoutMillis == 0) {
			// outer function ensures that there is a while around this
			// condition.
			cond.await();
		} else {
			boolean notTimedOut = cond.await(timeoutMillis, TimeUnit.MILLISECONDS);
			if (notTimedOut == false) {
				throw new TimeoutException("take call timed out");
			}
		}
	}

	private UCESipServerCall pollCall() {
		LOGGER.trace("takeCall stateLock.lock() before");
		stateLock.lock();
		try {
			LOGGER.trace("takeCall stateLock.lock() after");
			return currentServerState.pollCall();
		} finally {
			stateLock.unlock();
		}
	}

	@Override
	public ISipManager getSipManager() {
		return sipManager;
	}

	@Override
	public SipUser getServerUser() {
		return serverUser;
	}

	@Override
	public UCESipServerSettings getSipSettings() {
		return sipSettings;
	}

	@Override
	public Map<String, UCESipServerCall> getServerCallMap() {
		return serverCallMap;
	}

	/**
	 * Call only with state lock, from state itself it should have the lock
	 * already
	 */
	@Override
	public void changeState(final IServerState newState) {
		if (currentServerState.equals(newState) == false) {
			// reset state timeout timer.
			if (currentStateTimeoutTask != null) {
				currentStateTimeoutTask.cancel();
				currentStateTimeoutTask = null;
			}
			// check if the new state wants a timeout timer, and how long the
			// timeout shall be
			if (newState.getStateTimeoutMillis() != 0) {
				currentStateTimeoutTask = new StateTimeoutTask();
				stateTimeoutTimer.schedule(currentStateTimeoutTask, newState.getStateTimeoutMillis());
			}
			currentServerState = newState;
			LOGGER.trace("sip server changed state to " + newState.getCurrentState().toString());
			currentServerState.onInit();
			stateChangeCondition.signalAll();
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
				// check threadsafe if the state was canceled
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

	public void onInvite(final IMessage inviteMessage) {
		stateLock.lock();
		try {
			currentServerState.onInvite(inviteMessage);
		} finally {
			stateLock.unlock();
		}
	}

	public void onOk(final IMessage okmessage) {
		stateLock.lock();
		try {
			currentServerState.onOk(okmessage);
		} finally {
			stateLock.unlock();
		}
	}

	public void onBye(final IMessage message) {
		stateLock.lock();
		try {
			currentServerState.onBye(message);
		} finally {
			stateLock.unlock();
		}
	}

	public void onTimeOut() {
		stateLock.lock();
		try {
			currentServerState.onTimeOut();
		} finally {
			stateLock.unlock();
		}
	}

	public void onFailure(final ErrorMessage eMessage) {
		stateLock.lock();
		try {
			currentServerState.onFailure(eMessage);
		} finally {
			stateLock.unlock();
		}
	}

	@Override
	public RegisterRefreshThread getRegisterRefreshThread() {
		return registerRefreshThread;
	}

	@Override
	public BlockingQueue<UCESipServerCall> getCallQueue() {
		return callQueue;
	}

	@Override
	public boolean isShutdown() {
		return isShutdown;
	}

}