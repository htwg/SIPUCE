package de.fhkn.in.uce.holepunching.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import de.fhkn.in.uce.api.socket.IUCEStunEndpoint;
import de.fhkn.in.uce.api.socket.UCEStunEndpoint;
import de.fhkn.in.uce.api.socket.UCEStunSettings;
import de.fhkn.in.uce.holepunching.core.ConnectionAuthenticator;
import de.fhkn.in.uce.holepunching.core.ConnectionListener;
import de.fhkn.in.uce.holepunching.core.HolePuncher;
import de.fhkn.in.uce.holepunching.core.authentication.TargetConnectionAuthenticator;
import de.fhkn.in.uce.stun.ucestun.exception.UCEStunException;

public final class HolePunchingDemoTarget 
{
	private HolePunchingDemoTarget() {}
	
	private static final int STUN_PORT = 3478;
	private static final String STUN_IP = "213.239.218.18";
	
	public static void main(String[] args) throws UCEStunException, InterruptedException, IOException
	{		
    	UCEStunSettings settings = new UCEStunSettings(new InetSocketAddress(STUN_IP, STUN_PORT));
    	IUCEStunEndpoint stunEndpoint = new UCEStunEndpoint(settings);
    	stunEndpoint.evaluatePublicEndpoint();
		System.out.printf("Local Endpoint to Stun-Server: %s%n", stunEndpoint.getLocalEndpoint());
		System.out.printf("Public Endpoint to Stun-Server: %s%n", stunEndpoint.getPublicEnpoint());
		
		Socket ss = stunEndpoint.getStunEndpointSocket();
		InetSocketAddress localAddress = (InetSocketAddress) ss.getLocalSocketAddress();
		
		String privateIP;
		int privatePort;
		
		String publicIP;
		int publicPort;
		
		String input = null;
		
		Scanner scanner = new Scanner(System.in, "UTF-8");
		
		System.out.println("Starting HolePunchingDemoTarget...");
		
		System.out.print("Private Endpoint: ");
		input = scanner.nextLine();
		privateIP = input.substring(0, input.indexOf(':'));
		privatePort = Integer.parseInt(input.substring(input.indexOf(':') + 1));
		
		System.out.print("Public Endpoint: ");
		input = scanner.nextLine();
		publicIP = input.substring(0, input.indexOf(':'));
		publicPort = Integer.parseInt(input.substring(input.indexOf(':') + 1));
		
		BlockingQueue<Socket> socketQueue = new LinkedBlockingQueue<Socket>();
		ConnectionListener connectionListener = new ConnectionListener(localAddress.getAddress(), localAddress.getPort());
		SocketAddress localSocketAddress = new InetSocketAddress(0);
		
		HolePuncher holePuncher = new HolePuncher(connectionListener, localSocketAddress, socketQueue);
        
		InetSocketAddress privateEndpoint = new InetSocketAddress(privateIP, privatePort);
		InetSocketAddress publicEndpoint = new InetSocketAddress(publicIP, publicPort);
		
		ConnectionAuthenticator targetAuth = new TargetConnectionAuthenticator(UUID.fromString("5445cec0-afa2-11e2-9e96-0800200c9a66"));
		
		holePuncher.establishHolePunchingConnection(privateEndpoint.getAddress(), privateEndpoint.getPort(), publicEndpoint.getAddress(), publicEndpoint.getPort(), targetAuth);
		
        Socket socketToSource = socketQueue.take();
        
		System.out.println("Connection established");
		System.out.println("Starting threads for processing ...");
		
		final Executor chatExecutor = Executors.newCachedThreadPool();
		chatExecutor.execute(new ReaderTask(socketToSource.getOutputStream(), scanner));
		chatExecutor.execute(new PrinterTask(socketToSource.getInputStream()));
		System.out.println("Ready to chat ...");
	}
}
