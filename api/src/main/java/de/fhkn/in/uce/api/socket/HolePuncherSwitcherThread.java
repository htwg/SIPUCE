package de.fhkn.in.uce.api.socket;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.holepunching.core.ConnectionAuthenticator;
import de.fhkn.in.uce.holepunching.core.ConnectionListener;
import de.fhkn.in.uce.holepunching.core.HolePuncher;
import de.fhkn.in.uce.socketswitch.SwitchableException;
import de.fhkn.in.uce.socketswitch.SwitchableSocket;

class HolePuncherSwitcherThread extends Thread {

	private static final Logger LOGGER = LoggerFactory.getLogger(HolePuncherSwitcherThread.class);

	private final int switchTimeoutMillis;
	private final InetSocketAddress privateEndpoint;
	private final InetSocketAddress publicEndpoint;
	private final ConnectionAuthenticator authenticator;
	private final SwitchableSocket relaySwitchableSocket;
	private final BlockingQueue<Socket> socketQueue;
	private final HolePuncher holePuncher;

	public HolePuncherSwitcherThread(InetSocketAddress privateEndpoint, InetSocketAddress publicEndpoint, InetSocketAddress localAddress,
			SocketAddress localSocketAddress, ConnectionAuthenticator auth, SwitchableSocket relaySwitchableSocket, final int switchTimeoutMillis) {

		super("HolePuncherSwitcherThread");

		this.privateEndpoint = privateEndpoint;
		this.publicEndpoint = publicEndpoint;
		this.authenticator = auth;
		this.relaySwitchableSocket = relaySwitchableSocket;

		// holepuncher must be initialized for possible termination
		// these constructors have only a short initialization
		this.socketQueue = new LinkedBlockingQueue<Socket>();
		ConnectionListener connectionListener = new ConnectionListener(localAddress.getAddress(), localAddress.getPort());
		this.holePuncher = new HolePuncher(connectionListener, localSocketAddress, socketQueue);
		this.switchTimeoutMillis = switchTimeoutMillis;
	}

	protected void socketSwitched() {
		// only a method to overwrite in the sub class
	}

	@Override
	public void run() {
		try {
			LOGGER.debug("Start holepunching");

			holePuncher.establishHolePunchingConnection(privateEndpoint.getAddress(), privateEndpoint.getPort(), publicEndpoint.getAddress(),
					publicEndpoint.getPort(), authenticator);

			// Don't have to check the terminate flag, socketQueue.take() throws
			// InterruptedException if interrupt() is called in the terminate
			// function.

			Socket socketToTarget = socketQueue.take();

			if (socketToTarget.isConnected()) {
				LOGGER.debug("Socketswitch " + this.getClass().toString() + " start: " + relaySwitchableSocket.getPort());
				relaySwitchableSocket.switchSocket(socketToTarget, switchTimeoutMillis);
				LOGGER.debug("Socketswitch " + this.getClass().toString() + " end: " + relaySwitchableSocket.getPort());
				socketSwitched();
			}
		} catch (SwitchableException | TimeoutException e) {
			LOGGER.info("should not happen: ", e);
		} catch (InterruptedException e) {
			// This exception occurs if the thread is interrupted while waiting
			// (wait or sleep) - It is OK
			// do not need to interrupt current thread, the current thread was
			// interrupted.
		} finally {
			// terminates the holepuncher threads on error and success
			holePuncher.shutdownNow();
		}
	}

	public void terminate() throws InterruptedException {
		LOGGER.info("Terminate HolePuncherSwitcherThread");

		this.interrupt();
		this.join();
	}
}
