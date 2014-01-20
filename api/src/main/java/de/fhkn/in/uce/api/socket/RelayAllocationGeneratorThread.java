package de.fhkn.in.uce.api.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.relaying.core.IRelayingClient;

final class RelayAllocationGeneratorThread extends Thread {

	private static final Logger LOGGER = LoggerFactory.getLogger(RelayAllocationGeneratorThread.class);
	private static final RelayAllocation ERROR_ALLOCATION = new RelayAllocation(null, null);

	private final int relayHoldingTimeMillis;
	private final Lock allocationLock;
	private final Condition hasAllocationCond;
	private final InetSocketAddress relayServerAddress;
	private final IRelayingClientFactory relayingClientFactory;
	
	private volatile RelayAllocation currentAllocation;
	private volatile IOException allocationException;

	public RelayAllocationGeneratorThread(final InetSocketAddress relayServerAddress, final int relayHoldingTimeMillis, IRelayingClientFactory relayingClientFactory) {
		super("RelayAllocationGeneratorThread");

		this.relayServerAddress = relayServerAddress;
		this.allocationLock = new ReentrantLock(true);
		this.hasAllocationCond = allocationLock.newCondition();
		this.relayHoldingTimeMillis = relayHoldingTimeMillis;
		this.relayingClientFactory = relayingClientFactory;
		
		this.currentAllocation = null;
		this.allocationException = null;
	}

	@Override
	public void run() {
		while (this.isInterrupted() == false) {
			RelayAllocation tRelAlloc;

			// get a new allocation
			try {
				IRelayingClient tRelCl = relayingClientFactory.create(relayServerAddress);
				InetSocketAddress tEndpointAddr = tRelCl.createAllocation();
				tRelAlloc = new RelayAllocation(tRelCl, tEndpointAddr);
			} catch (IOException e1) {
				// put null to waiting thread
				allocationException = e1;
				tRelAlloc = ERROR_ALLOCATION;
			}

			RelayAllocation oldAlloc;

			// exchange the new allocation with the old one or set the new
			// one
			allocationLock.lock();

			try {
				oldAlloc = currentAllocation;
				currentAllocation = tRelAlloc;
				hasAllocationCond.signal();
			} finally {
				allocationLock.unlock();
			}

			// if no one has the old allocation discard it
			discardOldAlloc(oldAlloc);

			if (tRelAlloc.equals(ERROR_ALLOCATION)) {
				// exit on error
				return;
			}

			allocationLock.lock();
			try {
				while (currentAllocation != null) {
					boolean gotSignal = hasAllocationCond.await(relayHoldingTimeMillis, TimeUnit.MILLISECONDS);
					if (gotSignal == false) {
						// no one wanted the relay allocation, we have to
						// discard this relay allocation
						break;
					}
				}
			} catch (InterruptedException e) {
				// exit requested
				discardOldAlloc(currentAllocation);
				return;
			} finally {
				allocationLock.unlock();
			}

		}
	}

	private void discardOldAlloc(RelayAllocation oldAlloc) {
		if (oldAlloc == null) {
			return;
		}
		try {
			LOGGER.info("discarding relay allocation: " + oldAlloc.getEndpointAddr());
			oldAlloc.getRelayingClient().discardAllocation();
		} catch (IOException e) {
			LOGGER.error("Unable to discard relay allocation", e);
		}
	}

	public synchronized void terminate() throws InterruptedException {
		LOGGER.info("Terminate RelayAllocationGeneratorThread");

		if (this.isInterrupted() == true) {
			throw new IllegalStateException("RelayAllocationGenerator is terminated.");
		}
		this.interrupt();
		this.join();
	}

	public synchronized RelayAllocation takeRelayAllocation() throws InterruptedException, IOException {
		if (allocationException != null) {
			throw allocationException;
		}

		if (this.isInterrupted() == true) {
			throw new IllegalStateException("RelayAllocationGenerator is terminated.");
		}

		RelayAllocation relayAlloc;

		allocationLock.lock();
		try {
			while (currentAllocation == null) {
				hasAllocationCond.await();
			}
			relayAlloc = currentAllocation;
			currentAllocation = null;
			hasAllocationCond.signal();
		} finally {
			allocationLock.unlock();
		}

		if (relayAlloc.equals(ERROR_ALLOCATION)) {
			throw allocationException;
		}
		LOGGER.info("returning relay allocation: " + relayAlloc.getEndpointAddr());
		return relayAlloc;
	}
}