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

import org.junit.Ignore;
import org.junit.Test;

import de.fhkn.in.uce.holepunching.core.ConnectionAuthenticator;
import de.fhkn.in.uce.holepunching.core.ConnectionListener;
import de.fhkn.in.uce.holepunching.core.HolePuncher;
import de.fhkn.in.uce.holepunching.core.authentication.SourceConnectionAuthenticator;
import de.fhkn.in.uce.holepunching.core.authentication.TargetConnectionAuthenticator;
import de.fhkn.in.uce.stun.ucestun.exception.UCEStunException;

public class HolepuncherTestParallel {

	private static final int STUN_PORT = 3478;
	private static final String STUN_IP = "213.239.218.18";

	@Test
	@Ignore
	public void testTowParallelHolepuncher() throws UnknownHostException,
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

		targetHolepuncher.start();
		Thread.sleep(100);
		sourceHolepuncher.start();
		
		System.out.println("THREADS JOINED");
		
		targetStunEndpoint.getStunEndpointSocket().close();
		
		
		IUCEStunEndpoint targetStunEndpoint2 = new UCEStunEndpoint(settings);
		targetStunEndpoint2.evaluatePublicEndpoint();
		Socket targetStunSocket2 = targetStunEndpoint2.getStunEndpointSocket();
		InetSocketAddress targetLocalAddress2 = (InetSocketAddress) targetStunSocket2
				.getLocalSocketAddress();
		SocketAddress targetLocalSocketAddress2 = new InetSocketAddress(0);
		
		IUCEStunEndpoint sourceStunEndpoint2 = new UCEStunEndpoint(settings);
		sourceStunEndpoint2.evaluatePublicEndpoint();
		Socket sourceStunSocket2 = sourceStunEndpoint2.getStunEndpointSocket();
		InetSocketAddress sourceLocalAddress2 = (InetSocketAddress) sourceStunSocket2
				.getLocalSocketAddress();
		SocketAddress sourceLocalSocketAddress2 = sourceStunSocket2
				.getLocalSocketAddress();

		UUID token2 = UUID.randomUUID();

		Thread targetHolepuncher2 = new Thread(new HolePuncherThread(
				sourceStunEndpoint2.getLocalEndpoint(),
				sourceStunEndpoint2.getPublicEnpoint(),
				targetLocalAddress2,
				targetLocalSocketAddress2,
				new TargetConnectionAuthenticator(token2)));
		
		Thread sourceHolepuncher2 = new Thread(new HolePuncherThread(
				targetStunEndpoint2.getLocalEndpoint(),
				targetStunEndpoint2.getPublicEnpoint(),
				sourceLocalAddress2,
				sourceLocalSocketAddress2,
				new SourceConnectionAuthenticator(token2)));
		
		targetHolepuncher2.start();
		Thread.sleep(100);
		sourceHolepuncher2.start();
		
		targetHolepuncher.join();
		sourceHolepuncher.join();
		targetHolepuncher2.join();
		sourceHolepuncher2.join();		
	}
	
	@Test
	public void testTowSequentHolepuncher() throws UnknownHostException,
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

		targetHolepuncher.start();
		Thread.sleep(100);
		sourceHolepuncher.start();
		
		targetHolepuncher.join();
		sourceHolepuncher.join();
		
		System.out.println("THREADS JOINED");		
		
		IUCEStunEndpoint targetStunEndpoint2 = new UCEStunEndpoint(settings);
		targetStunEndpoint2.evaluatePublicEndpoint();
		Socket targetStunSocket2 = targetStunEndpoint2.getStunEndpointSocket();
		InetSocketAddress targetLocalAddress2 = (InetSocketAddress) targetStunSocket2
				.getLocalSocketAddress();
		SocketAddress targetLocalSocketAddress2 = new InetSocketAddress(0);
		
		IUCEStunEndpoint sourceStunEndpoint2 = new UCEStunEndpoint(settings);
		sourceStunEndpoint2.evaluatePublicEndpoint();
		Socket sourceStunSocket2 = sourceStunEndpoint2.getStunEndpointSocket();
		InetSocketAddress sourceLocalAddress2 = (InetSocketAddress) sourceStunSocket2
				.getLocalSocketAddress();
		SocketAddress sourceLocalSocketAddress2 = sourceStunSocket2
				.getLocalSocketAddress();

		UUID token2 = UUID.randomUUID();

		Thread targetHolepuncher2 = new Thread(new HolePuncherThread(
				sourceStunEndpoint2.getLocalEndpoint(),
				sourceStunEndpoint2.getPublicEnpoint(),
				targetLocalAddress2,
				targetLocalSocketAddress2,
				new TargetConnectionAuthenticator(token2)));
		
		Thread sourceHolepuncher2 = new Thread(new HolePuncherThread(
				targetStunEndpoint2.getLocalEndpoint(),
				targetStunEndpoint2.getPublicEnpoint(),
				sourceLocalAddress2,
				sourceLocalSocketAddress2,
				new SourceConnectionAuthenticator(token2)));
		
		targetHolepuncher2.start();
		Thread.sleep(100);
		sourceHolepuncher2.start();
	
		targetHolepuncher2.join();
		sourceHolepuncher2.join();		
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
				System.out.println("TARGET THREAD started");
			} else {
				System.out.println("SOURCE THREAD started");
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
			

			try {
				Socket socket = socketQueue.take();
				if (auth instanceof SourceConnectionAuthenticator) {
					System.out.print("Source Thread reporting: ");
					if(socket.isConnected()) {
						System.out.println("Socket is connected !!!");
						System.out.println("SOURCE PORT: " + socket.getLocalPort());
					} else {
						System.out.println("Socket is NOT connected !!!");
						fail();
					}
					
				} else {
					System.out.print("Target Thread reporting: ");
					if(socket.isConnected()) {
						System.out.println("Socket is connected !!!");
						System.out.println("TARGET PORT: " + socket.getLocalPort());
					} else {
						System.out.println("Socket is NOT connected !!!");
						fail();
					}
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			holePuncher.shutdownNow();
		}
	}
}
