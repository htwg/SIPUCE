package de.fhkn.in.uce.api.socket;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Test;

import de.fhkn.in.uce.holepunching.core.ConnectionAuthenticator;
import de.fhkn.in.uce.holepunching.core.ConnectionListener;
import de.fhkn.in.uce.holepunching.core.HolePuncher;
import de.fhkn.in.uce.holepunching.core.authentication.SourceConnectionAuthenticator;
import de.fhkn.in.uce.holepunching.core.authentication.TargetConnectionAuthenticator;
import de.fhkn.in.uce.stun.ucestun.exception.UCEStunException;

public class HolepuncherTest {

	private static final int STUN_PORT = 3478;
	private static final String STUN_IP = "213.239.218.18";

	@Test
	public void testHolepuncherCorrectClose() throws UnknownHostException,
			IOException, InterruptedException, UCEStunException {

		UCEStunSettings settings = new UCEStunSettings(new InetSocketAddress(
				STUN_IP, STUN_PORT));

		IUCEStunEndpoint targetStunEndpoint = new UCEStunEndpoint(settings);
		targetStunEndpoint.evaluatePublicEndpoint();
		Socket targetStunSocket = targetStunEndpoint.getStunEndpointSocket();
		InetSocketAddress targetLocalAddress = (InetSocketAddress) targetStunSocket
				.getLocalSocketAddress();
		SocketAddress targetLocalSocketAddress = new InetSocketAddress(0);

		IUCEStunEndpoint sourceStunEndpoint = new UCEStunEndpoint(settings);
		sourceStunEndpoint.evaluatePublicEndpoint();
		Socket sourceStunSocket = sourceStunEndpoint.getStunEndpointSocket();
		InetSocketAddress sourceLocalAddress = (InetSocketAddress) sourceStunSocket
				.getLocalSocketAddress();
		SocketAddress sourceLocalSocketAddress = sourceStunSocket
				.getLocalSocketAddress();

		UUID token = UUID.fromString("5445cec0-afa2-11e2-9e96-0800200c9a66");

		Thread targetHolepuncher = new Thread(new HolePuncherThread(
				sourceStunEndpoint.getLocalEndpoint(),
				sourceStunEndpoint.getPublicEnpoint(),
				targetLocalAddress,
				targetLocalSocketAddress,
				new TargetConnectionAuthenticator(token)));
		Thread sourceHolepuncher = new Thread(new HolePuncherThread(
				targetStunEndpoint.getLocalEndpoint(),
				targetStunEndpoint.getPublicEnpoint(),
				sourceLocalAddress,
				sourceLocalSocketAddress,
				new SourceConnectionAuthenticator(token)));

		sourceHolepuncher.start();
		Thread.sleep(100);
		targetHolepuncher.start();

		targetHolepuncher.join();
		sourceHolepuncher.join();
	}

	private static class HolePuncherThread implements Runnable {

		private InetSocketAddress privateEndpoint;
		private InetSocketAddress publicEndpoint;
		private InetSocketAddress localAdress;
		private SocketAddress localSocketAddress;
		private ConnectionAuthenticator auth;

		public HolePuncherThread(InetSocketAddress privateEndpoint,
				InetSocketAddress publicEndpoint,
				InetSocketAddress localAdress,
				SocketAddress localSocketAddress, ConnectionAuthenticator auth) {
			this.privateEndpoint = privateEndpoint;
			this.publicEndpoint = publicEndpoint;
			this.localAdress = localAdress;
			this.localSocketAddress = localSocketAddress;
			this.auth = auth;
		}

		@Override
		public void run() {
			if(auth instanceof TargetConnectionAuthenticator) {
				System.out.println("I AM THE TARGET THREAD");
			} else {
				System.out.println("I AM THE SOURCE THREAD");
			}
			
			BlockingQueue<Socket> socketQueue = new LinkedBlockingQueue<Socket>();
			ConnectionListener connectionListener = new ConnectionListener(
					localAdress.getAddress(), localAdress.getPort());

			HolePuncher holePuncher = new HolePuncher(connectionListener,
					localSocketAddress, socketQueue);

			holePuncher
					.establishHolePunchingConnection(
							privateEndpoint.getAddress(),
							privateEndpoint.getPort(),
							publicEndpoint.getAddress(),
							publicEndpoint.getPort(), auth);
			
			if(auth instanceof TargetConnectionAuthenticator) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				holePuncher.shutdownNow();
			}
			

			try {
				Socket socketToSource = socketQueue.take();
				if (auth instanceof SourceConnectionAuthenticator) {
					System.out.println("Source Thread reporting: ");
					if(socketToSource.isConnected()) {
						System.out.println("Socket is connected !!!");
						fail();
					} else {
						System.out.println("Socket is NOT connected !!!");
					}
					
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
}
