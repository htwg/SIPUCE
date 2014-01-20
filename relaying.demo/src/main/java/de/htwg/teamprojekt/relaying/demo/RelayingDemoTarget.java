package de.htwg.teamprojekt.relaying.demo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.relaying.core.RelayingClient;

public final class RelayingDemoTarget 
{
	private RelayingDemoTarget() {}
	
	static final Logger LOGGER = LoggerFactory.getLogger(RelayingDemoTarget.class);
	
	static final String RELAY_SERVER_IP = "213.239.218.18";
	static final int RELAY_SERVER_PORT = 10300;
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		InetSocketAddress relayAddress = new InetSocketAddress(InetAddress.getByName(RELAY_SERVER_IP), RELAY_SERVER_PORT);
		
		LOGGER.info("Starting Relaying Demo...");

		// Connection to Relay Client
		RelayingClient relayingClient = new RelayingClient(relayAddress);
		
		LOGGER.debug("Creating allocation at relay server");
		
        InetSocketAddress endpointAtRelay = relayingClient.createAllocation();
        if (endpointAtRelay.getAddress().isAnyLocalAddress()) {
        	endpointAtRelay = new InetSocketAddress(relayAddress.getAddress(), endpointAtRelay.getPort());
        }
        LOGGER.debug("Allocation at relay server created: {}", endpointAtRelay.toString());
		
		Socket relaySocket = relayingClient.accept();
		
		LOGGER.info("Connection established");
		LOGGER.info("Starting threads for processing ...");
		
		final Executor executor = Executors.newCachedThreadPool();
		executor.execute(new ReaderTask(relaySocket.getOutputStream()));
		executor.execute(new PrinterTask(relaySocket.getInputStream()));
		
		System.out.println("Ready to chat ...");
	}
	
	
}
