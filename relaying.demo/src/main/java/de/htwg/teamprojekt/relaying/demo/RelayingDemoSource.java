package de.htwg.teamprojekt.relaying.demo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RelayingDemoSource {
	
	private RelayingDemoSource() {}

	static final Logger LOGGER = LoggerFactory.getLogger(RelayingDemoSource.class);

	static final String RELAY_SERVER_IP = "213.239.218.18";
	static final int RELAY_SERVER_PORT = 10300;
	static final int RELAY_ALLOCATION_PORT = 10151;

	public static void main(String[] args) throws IOException {

		/*
		 * Hier den Port den der Relay Server allokiert manuell angeben (siehe
		 * logfile vom server) !!!!!!!!
		 */
		InetSocketAddress relayAddress = new InetSocketAddress(InetAddress.getByName(RELAY_SERVER_IP), RELAY_ALLOCATION_PORT);

		LOGGER.info("Starting Relaying Demo...");
		
		@SuppressWarnings("resource")
		final Socket socket = new Socket();
		socket.setReuseAddress(true);
		socket.connect(relayAddress);

		LOGGER.info("Connection established");
		LOGGER.info("Starting threads for processing ...");
		
		final Executor executor = Executors.newCachedThreadPool();
		executor.execute(new ReaderTask(socket.getOutputStream()));
		executor.execute(new PrinterTask(socket.getInputStream()));
		
		System.out.println("Ready to chat ...");
	}
}
