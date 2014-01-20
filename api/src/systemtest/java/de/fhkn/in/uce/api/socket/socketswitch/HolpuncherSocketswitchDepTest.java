package de.fhkn.in.uce.api.socket.socketswitch;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import de.fhkn.in.uce.api.socket.IUCEStunEndpoint;
import de.fhkn.in.uce.api.socket.UCEStunEndpoint;
import de.fhkn.in.uce.api.socket.UCEStunSettings;
import de.fhkn.in.uce.holepunching.core.ConnectionAuthenticator;
import de.fhkn.in.uce.holepunching.core.ConnectionListener;
import de.fhkn.in.uce.holepunching.core.HolePuncher;
import de.fhkn.in.uce.holepunching.core.authentication.SourceConnectionAuthenticator;
import de.fhkn.in.uce.holepunching.core.authentication.TargetConnectionAuthenticator;
import de.fhkn.in.uce.socketswitch.HalfCloseSwitchableSocket;
import de.fhkn.in.uce.socketswitch.SwitchableException;
import de.fhkn.in.uce.socketswitch.SwitchableSocket;
import de.fhkn.in.uce.stun.ucestun.exception.UCEStunException;

public class HolpuncherSocketswitchDepTest {

	private final static int SERVERPORT = 6324;
	private final static String SERVER = "localhost";
	
	private static final int STUN_PORT = 3478;
	private static final String STUN_IP = "213.239.218.18";
	
	private SwitchableSocket serverSwSocket;
	private SwitchableSocket clientSwSocket;


	protected SwitchableSocket createSwitchableSocket(Socket socket) throws IOException {
		return new HalfCloseSwitchableSocket(socket);
	}

	@Test
	public void test1() throws UnknownHostException, IOException, InterruptedException, UCEStunException {
		int bytesToSend = 10000000;

		ServerSocket serverSocket = new ServerSocket(SERVERPORT);
		Socket tClientSock = new Socket(SERVER, SERVERPORT);

		clientSwSocket = createSwitchableSocket(tClientSock);
		serverSwSocket = createSwitchableSocket(serverSocket.accept());
		
		serverSocket.close();

	
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
		
		
		WriteThread tw = new WriteThread(serverSwSocket, createBytes(bytesToSend));

tw.start();
		
		Thread.sleep(800);
		
		targetHolepuncher.start();
		Thread.sleep(100);
		sourceHolepuncher.start();
		
		
		
		//Thread.sleep(100);
		
		
		
		
		handleConnectionClient(clientSwSocket.getInputStream(), bytesToSend);

		targetHolepuncher.join();
		sourceHolepuncher.join();
		tw.join();
		
		serverSwSocket.close();
		clientSwSocket.close();

		
	}

	private byte[] readFinish(InputStream in, int bytesMustRead) throws IOException {
		int bytesRead = 0;
		byte[] buffer = new byte[bytesMustRead];

		while (bytesRead < bytesMustRead) {
			int tLen = bytesMustRead - bytesRead;
			int tRead = in.read(buffer, bytesRead, tLen);
			// System.out.println("bytes read: " + bytesRead + " currentRead: "
			// + tRead);
			// No EOF expected
			assertNotEquals(-1, tRead);
			// can read 0 bytes???, no we should not, so assert. (ObjectInput
			// stream throws error on read if 0 were received)
			assertNotEquals(0, tRead);
			bytesRead += tRead;
		}
		// EOF is expected
		int tbyte = in.read();
		assertEquals(-1, tbyte);

		return buffer;
	}

	private void handleConnectionClient(InputStream in, int byteCount) throws IOException {

		byte[] b = readFinish(in, byteCount);
		assertArrayEquals(createBytes(byteCount), b);
	}

	private static byte[] createBytes(final int count) {
		byte[] b = new byte[count];
		byte val = 0;
		for (int i = 0; i < b.length; i++) {
			b[i] = val;
			val++;
		}
		return b;
	}

	private class WriteThread extends Thread {

		private final byte[] bToSend;
		private final Socket sock;

		public WriteThread(Socket sock, byte[] bytesToSend) {
			super("ServerThread");
			this.bToSend = bytesToSend;
			this.sock = sock;
		}

		@Override
		public void run() {
			try {
				Thread.sleep(100);
				handleConnectionServer(sock.getOutputStream(), bToSend);

				// sock.shutdownOutput();
				sock.close();

			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}

		private void handleConnectionServer(OutputStream out, byte[] bytesToSend) throws IOException, InterruptedException {

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
				// Thread.sleep(1);
			}
			System.out.println("bytes sent: " + sentByteCount);
		}

	}
	
	
	private class HolePuncherThread implements Runnable {

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
						
						clientSwSocket.switchSocket(socket, 1000);

						System.out.println("client switched");
					} else {
						System.out.println("Socket is NOT connected !!!");
						fail();
					}
					
				} else {
					System.out.print("Target Thread reporting: ");
					if(socket.isConnected()) {
						System.out.println("Socket is connected !!!");
						System.out.println("TARGET PORT: " + socket.getLocalPort());
						
						serverSwSocket.switchSocket(socket, 1000);

						System.out.println("server switched");
					} else {
						System.out.println("Socket is NOT connected !!!");
						fail();
					}
				}
			} catch (InterruptedException | SwitchableException | TimeoutException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			holePuncher.shutdownNow();
		}
	}

}
