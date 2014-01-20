package de.fhkn.in.uce.api.socket;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.stun.ucestun.exception.UCEStunException;

final class StunEndpointGeneratorThread extends Thread {
	
	private final int stunRefreshTimeMillis;
	private final Lock stunEndpointLock;
	private final Condition hasEndpointCond;
	private final UCEStunSettings stunSettings;
	private final IStunEndpointFactory stunEndpointFactory;
	
	private volatile IUCEStunEndpoint currentStunEndpoint;
	private volatile UCEStunException stunException;

	private static final Logger LOGGER = LoggerFactory.getLogger(StunEndpointGeneratorThread.class);

	public StunEndpointGeneratorThread(UCEStunSettings stunSettings, final int stunRefreshTimeMillis, IStunEndpointFactory stunEndpointFactory) {
		super("StunEndpointGeneratorThread");

		this.stunSettings = stunSettings;
		this.stunEndpointLock = new ReentrantLock(true);
		this.hasEndpointCond = this.stunEndpointLock.newCondition();
		this.currentStunEndpoint = null;
		this.stunException = null;
		this.stunRefreshTimeMillis = stunRefreshTimeMillis;
		this.stunEndpointFactory = stunEndpointFactory;
	}

	@Override
	public void run() {
		while (this.isInterrupted() == false) {
			IUCEStunEndpoint tStunEndpoint;

			// get a new endpoint and evaluate it
			try {
				tStunEndpoint = stunEndpointFactory.create(stunSettings);
				tStunEndpoint.evaluatePublicEndpoint();
			} catch (UCEStunException e) {
				// put null to waiting thread and
				stunException = e;
				tStunEndpoint = null;
			}

			IUCEStunEndpoint oldEndpoint;

			// exchange the new endpoint with the old one
			stunEndpointLock.lock();

			try {
				oldEndpoint = currentStunEndpoint;
				currentStunEndpoint = tStunEndpoint;
				hasEndpointCond.signal();
			} finally {
				stunEndpointLock.unlock();
			}

			try {
				if (oldEndpoint != null) {
					oldEndpoint.getStunEndpointSocket().close();
				}
			} catch (IOException e) {
				LOGGER.error("Unable to close socket to stun server", e);
			}

			if (stunException != null) {
				// exit on error
				return;
			}

			stunEndpointLock.lock();
			try {
				while (currentStunEndpoint != null) {
					boolean gotSignal = hasEndpointCond.await(stunRefreshTimeMillis, TimeUnit.MILLISECONDS);
					if (gotSignal == false) {
						// no one wanted the relay allocation, we have to
						// discard this relay allocation
						break;
					}
				}
			} catch (InterruptedException e) {
				// exit requested
				return;
			} finally {
				stunEndpointLock.unlock();
			}

		}
	}

	public synchronized void terminate() throws InterruptedException {
		LOGGER.info("Terminate StunEndpointGeneratorThread");

		if (this.isInterrupted() == true) {
			throw new IllegalStateException("StunEndpointGeneratorThread is terminated.");
		}
		this.interrupt();
		this.join();
	}

	public synchronized IUCEStunEndpoint takeStunEndpoint() throws UCEStunException, InterruptedException {
		if (stunException != null) {
			throw stunException;
		}

		if (this.isInterrupted() == true) {
			throw new IllegalStateException("StunEndpointGeneratorThread is terminated.");
		}

		IUCEStunEndpoint stunEndpoint;

		stunEndpointLock.lock();
		try {
			while (currentStunEndpoint == null) {
				hasEndpointCond.await();
			}
			stunEndpoint = currentStunEndpoint;
			currentStunEndpoint = null;
			hasEndpointCond.signal();
		} finally {
			stunEndpointLock.unlock();
		}

		if (stunException != null) {
			throw stunException;
		}
		return stunEndpoint;
	}
}