package de.fhkn.in.uce.api.socket;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.UUID;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.fhkn.in.uce.api.socket.UCEClientSettings;
import de.fhkn.in.uce.api.socket.UCEException;
import de.fhkn.in.uce.api.socket.UCEServerSettings;
import de.fhkn.in.uce.api.socket.UCEServerSocket;
import de.fhkn.in.uce.api.socket.UCESocket;
import de.fhkn.in.uce.api.socket.UCETestSettings;


public abstract class UCESystemTest {

	public abstract UCEServerSocket createServerSocket(String sipUser, String sipServer, UCEServerSettings settings) throws IOException;

	public abstract UCESocket createClientSocket(String fromDomain, String sipUser, String sipServer, UCEClientSettings settings) throws UCEException;

	private String sipServerUser;
	private final String sipServerDomain = "213.239.218.18";

	protected static UCEClientSettings currentClientSettings = UCETestSettings.CLIENT_SETTINGS;
	protected static UCEServerSettings currentServerSettings = UCETestSettings.SERVER_SETTINGS;

	@Before
	public void initialize() throws IOException, UCEException {
		sipServerUser = UUID.randomUUID().toString();
	}

	@Ignore
	@Test(expected = IOException.class)
	public void testAcceptAbbort() throws Exception {
		final UCEServerSocket serverSocket = createServerSocket(sipServerUser, sipServerDomain, currentServerSettings);


		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(100);
					serverSocket.close();
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}
			}
		});

		t.start();

		serverSocket.accept();
	}

	@Test
	public void testServerQueuedAccept() throws InterruptedException, UCEException, IOException {
		int bytecount = 1000;
		currentClientSettings = UCETestSettings.CLIENT_SETTINGS_WITHOUT_HOLEPUNCHER;
		currentServerSettings = UCETestSettings.SERVER_SETTINGS_WITHOUT_HOLEPUNCHER;

		UCEServerSocket serverSocket = createServerSocket(sipServerUser, sipServerDomain, currentServerSettings);

		Thread tServer = new ServerThread(serverSocket, bytecount, 1, false, 1);

		tServer.start();

		Thread.sleep(200);

		UCESocket uceClientSocket = createClientSocket(sipServerDomain, sipServerUser, sipServerDomain, currentClientSettings);

		System.out.println("start handle Connection Client");
		handleConnectionClient(uceClientSocket.getOutputStream(), uceClientSocket.getInputStream(), bytecount);
		System.out.println("handle Connection Client finished");

		uceClientSocket.close();

		tServer.join();

		serverSocket.close();
	}

	@Test(expected = UCEException.class)
	public void testUceServerDeclined() throws InterruptedException, UCEException, IOException {

		currentClientSettings = UCETestSettings.CLIENT_SETTINGS_WITHOUT_HOLEPUNCHER;
		currentServerSettings = UCETestSettings.SERVER_SETTINGS_WITHOUT_HOLEPUNCHER;

		UCEServerSocket serverSocket = createServerSocket(sipServerUser, sipServerDomain, currentServerSettings);

		Thread tServer = new ServerThread(serverSocket, 1000, 1, true, 1);

		tServer.start();

		Thread.sleep(200);

		UCESocket tSock = createClientSocket(sipServerDomain, sipServerUser, sipServerDomain, currentClientSettings);
		tSock.close();

		tServer.join();

		// is closed in thread
		//serverSocket.close();
	}

	@Test
	public void testUceServerClient1kB() throws InterruptedException, UCEException, IOException {
		testUceMultipleInvitesAccept(1, 1000);
	}

	@Test
	public void testUceServerClient10MB() throws InterruptedException, UCEException, IOException {
		testUceMultipleInvitesAccept(1, 10000000);
	}

	@Test
	public void testUceServerClient0Bytes() throws InterruptedException, UCEException, IOException {
		testUceMultipleInvitesAccept(1, 0);
	}

	@Test
	public void testUce2InvitesAccept() throws UCEException, IOException, InterruptedException {
		testUceMultipleInvitesAccept(2, 1000);
	}

	public void testUceMultipleInvitesAccept(final int clientsCount, final int bytecount) throws UCEException, IOException, InterruptedException {

		UCEServerSocket serverSocket = createServerSocket(sipServerUser, sipServerDomain, currentServerSettings);

		int clientThreadsCount = clientsCount - 1;
		Thread tServer = new ServerThread(serverSocket, bytecount, 0, false, clientsCount);

		Thread[] tClients = new Thread[clientThreadsCount];

		tServer.start();

		for (int i = 0; i < tClients.length; i++) {
			tClients[i] = new ClientThread(bytecount);
			tClients[i].start();
			Thread.sleep(100);
			System.out.print('|');
		}

		System.out.println();

		UCESocket uceClientSocket = createClientSocket(sipServerDomain, sipServerUser, sipServerDomain, currentClientSettings);

		//System.out.println("start handle Connection Client");
		handleConnectionClient(uceClientSocket.getOutputStream(), uceClientSocket.getInputStream(), bytecount);
		//System.out.println("handle Connection Client finished");

		uceClientSocket.close();

		for (int i = 0; i < tClients.length; i++) {
			tClients[i].join();
			System.out.print('.');
		}
		tServer.interrupt();
		tServer.join();

		serverSocket.close();

		System.out.println();
	}

	@Test
	public void testUceTwoInvitesAcceptSequently() throws UCEException, IOException, InterruptedException {

		currentClientSettings = UCETestSettings.CLIENT_SETTINGS_WITHOUT_HOLEPUNCHER;
		currentServerSettings = UCETestSettings.SERVER_SETTINGS_WITHOUT_HOLEPUNCHER;

		int bytecount = 100000;

		UCEServerSocket serverSocket = createServerSocket(sipServerUser, sipServerDomain, currentServerSettings);

		Thread tServer = new ServerThread(serverSocket, bytecount, 0, false, 2);

		Thread tClient1 = new ClientThread(bytecount);

		tServer.start();

		tClient1.start();
		tClient1.join();

		UCESocket uceClientSocket = createClientSocket(sipServerDomain, sipServerUser, sipServerDomain, currentClientSettings);

		System.out.println("start handle Connection Client");
		handleConnectionClient(uceClientSocket.getOutputStream(), uceClientSocket.getInputStream(), bytecount);
		System.out.println("handle Connection Client finished");

		uceClientSocket.close();

		tServer.join();

		serverSocket.close();
	}

	/**
	 * Creates an deterministic byte array with bytes from 0 to 255.
	 * 
	 * @param count
	 * @return
	 */
	private static byte[] createBytes(final int count) {
		byte[] b = new byte[count];
		byte val = 0;
		for (int i = 0; i < b.length; i++) {
			b[i] = val;
			val++;
		}
		return b;
	}

	private static void handleConnectionClient(final OutputStream out, final InputStream in, final int byteCount) throws IOException {


		byte[] b = readFinishAndCompare(in, createBytes(byteCount));
		assertArrayEquals(createBytes(byteCount), b);
	}

	private static void handleConnectionServer(final OutputStream out, final InputStream in, final int byteCount) throws IOException {

		byte[] bytesToSend = createBytes(byteCount);

		int tcpPackageByteCount = 1;
		int sentByteCount = 0;

		while (sentByteCount < bytesToSend.length) {
			// if the package size of the last bytes is larger than the
			// remaining bytes
			int bytesCanSend = Math.min(tcpPackageByteCount, bytesToSend.length - sentByteCount);
			out.write(bytesToSend, sentByteCount, bytesCanSend);

			sentByteCount += bytesCanSend;

			if (tcpPackageByteCount < 5000) {
				tcpPackageByteCount++;
			}
		}
		//System.out.println("bytes sent: " + sentByteCount);
	}

	/**
	 * Reads the specified number of bytes and returns them as an array.
	 * 
	 */
	private static byte[] readFinishAndCompare(final InputStream in, final byte[] bytesToCompare) throws IOException {
		int bytesRead = 0;
		byte[] buffer = new byte[bytesToCompare.length];

		while (bytesRead < buffer.length) {
			int tLen = buffer.length - bytesRead;
			int tRead = in.read(buffer, bytesRead, tLen);
			// System.out.println("bytes read: " + bytesRead + " currentRead: "
			// + tRead);
			// No EOF expected
			assertNotEquals(-1, tRead);
			// can read 0 bytes???, no we should not, so assert. (ObjectInput
			// stream throws error on read if 0 were received)
			assertNotEquals(0, tRead);
			// assert that the read bytes are the right one
			for (int i = bytesRead; i < (bytesRead + tRead); i++) {
				assertEquals(bytesToCompare[i], buffer[i]);
			}
			bytesRead += tRead;
		}
		// EOF is expected
		int tbyte = in.read();
		assertEquals(-1, tbyte);

		//System.out.println("bytes read: " + bytesRead);

		return buffer;
	}

	private class ServerThread extends Thread {
		private final int bytecount;
		private final int secondsToWaitAction;
		private final boolean instantClose;
		private final int numberOfAcceptedClient;
		private final UCEServerSocket serverSock;


		public ServerThread(final UCEServerSocket serverSock, final int bytecount, final int secondsToWaitAction, final boolean instantClose, final int numberOfAcceptedClient) {
			super("Test Server Thread");

			this.bytecount = bytecount;
			this.secondsToWaitAction = secondsToWaitAction;
			this.instantClose = instantClose;
			this.numberOfAcceptedClient = numberOfAcceptedClient;
			this.serverSock = serverSock;
		}

		@Override
		public void run() {
			try {


				for (int i = 0; i < numberOfAcceptedClient; i++) {
					if (secondsToWaitAction > 0) {
						Thread.sleep(secondsToWaitAction * 1000);
					}

					if (this.instantClose) {
						serverSock.close();
						return;
					}

					Socket socketTpPartner = serverSock.accept();

					//System.out.println("SERVER - start handle Connection Server");
					handleConnectionServer(socketTpPartner.getOutputStream(), socketTpPartner.getInputStream(), bytecount);
					//System.out.println("SERVER - handle Connection Server finished");
					socketTpPartner.close();
				}
				//System.out.println("SERVER - ServerSocket closed");
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.printf("Server thread finished%n");
		}
	}

	// own thread because of multiple invites to one serversocket
	private class ClientThread extends Thread {
		private final int bytecount;

		public ClientThread(final int bytecount) {
			super("Test Client Thread");
			this.bytecount = bytecount;
		}

		@Override
		public void run() {
			try {
				UCESocket uceClientSocket = createClientSocket(sipServerDomain, sipServerUser, sipServerDomain, currentClientSettings);

				//System.out.println("start handle Connection Client");
				handleConnectionClient(uceClientSocket.getOutputStream(), uceClientSocket.getInputStream(), bytecount);
				//System.out.println("handle Connection Client finished");

				//System.out.println("uceClientSocket.close();");

				uceClientSocket.close();
			} catch (IOException | UCEException e) {
				e.printStackTrace();
			}
		}
	}

}
