package de.fhkn.in.uce.api.demo.chat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.fhkn.in.uce.api.socket.UCERelaySettings;
import de.fhkn.in.uce.api.socket.UCEServerSettings;
import de.fhkn.in.uce.api.socket.UCEServerSocket;
import de.fhkn.in.uce.api.socket.UCEStunSettings;
import de.fhkn.in.uce.sip.ucesip.settings.UCESipServerSettings;

public final class ChatTarget {
	
	private final String targetId;
	
	private static final int STUN_PORT = 3478;
	private static final int PROXY_PORT = 5060;
	private static final int RELAY_PORT = 10300;

	/**
	 * Creates a chat target with the given id.
	 * 
	 * @param targetId
	 *            the id of the target
	 */
	public ChatTarget(final String targetId) {
		this.targetId = targetId;
	}

	/**
	 * Creates a UCE target socket and waits for a source.
	 * @throws IOException
	 * 
	 * @throws Exception
	 */
	public void startChatTarget() throws IOException {

		System.out.println("Starting target " + this.targetId + " and waiting for source ...");
		
		UCEServerSettings serverSettings = new UCEServerSettings(
		new UCEStunSettings(new InetSocketAddress("213.239.218.18", STUN_PORT)),
		new UCESipServerSettings(new InetSocketAddress("213.239.218.18", PROXY_PORT)),
		new UCERelaySettings(new InetSocketAddress("213.239.218.18", RELAY_PORT)));
		
		@SuppressWarnings("resource")
		final UCEServerSocket uceServerSocket = new UCEServerSocket(this.targetId,"213.239.218.18", serverSettings);
		
		Socket socketTpPartner = uceServerSocket.accept();
		System.out.println("Connection established ...");
		System.out.println("Starting threads for processing ...");
		final Executor executor = Executors.newCachedThreadPool();
		executor.execute(new ReaderTask(socketTpPartner.getOutputStream()));
		executor.execute(new PrinterTask(socketTpPartner.getInputStream()));
		System.out.println("Ready to chat ...");
	}

	/**
	 * Starts the chat target. Args: targetId
	 * 
	 * @param args
	 * @throws IOException
	 * @throws Exception
	 */
	public static void main(final String[] args) throws IOException {
		final ChatTarget target = new ChatTarget("test");
		target.startChatTarget();
	}
}
