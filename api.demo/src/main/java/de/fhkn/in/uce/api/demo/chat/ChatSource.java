package de.fhkn.in.uce.api.demo.chat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.fhkn.in.uce.api.socket.UCEClientSettings;
import de.fhkn.in.uce.api.socket.UCEException;
import de.fhkn.in.uce.api.socket.UCESocket;
import de.fhkn.in.uce.api.socket.UCEStunSettings;
import de.fhkn.in.uce.sip.ucesip.settings.UCESipClientSettings;

public final class ChatSource {
	
	private final String targetId;
	
	private static final int STUN_PORT = 3478;
	private static final int PROXY_PORT = 5060;

	/**
	 * Creates a chat source and connects to the given target id.
	 * 
	 * @param targetId
	 */
	public ChatSource(final String targetId) {
		this.targetId = targetId;
	}

	/**
	 * Establishes a connection by using a UCE socket and starts threads to
	 * process incoming messages and command line input.
	 * @throws UCESipException
	 * @throws UCEStunException
	 * @throws UCEException
	 * @throws IOException
	 * 
	 * @throws Exception
	 */
	public void startChatSource() throws UCEException, IOException {

		System.out.println("Connecting to target " + this.targetId + " ...");
		
		UCEClientSettings clientSettings = new UCEClientSettings(
		new UCEStunSettings(new InetSocketAddress("213.239.218.18", STUN_PORT)),
		new UCESipClientSettings(new InetSocketAddress("213.239.218.18", PROXY_PORT)));
		
		@SuppressWarnings("resource")
		final UCESocket socketTpPartner = new UCESocket("213.239.218.18", this.targetId, "213.239.218.18", clientSettings);

		System.out.println("Connection to " + this.targetId + " established ...");
		System.out.println("Starting threads for processing ...");
		final Executor executor = Executors.newCachedThreadPool();
		executor.execute(new ReaderTask(socketTpPartner.getOutputStream()));
		executor.execute(new PrinterTask(socketTpPartner.getInputStream()));
		System.out.println("Ready to chat ...");
	}

	/**
	 * Starts the chat source. args: targetId
	 * 
	 * @param args
	 *            the id of the target
	 * @throws UCEException
	 * @throws UCESipException
	 * @throws UCEStunException
	 * @throws IOException
	 * @throws Exception
	 */
	public static void main(final String[] args) throws UCEException, IOException {
		final ChatSource source = new ChatSource("test");
		source.startChatSource();
	}
}
