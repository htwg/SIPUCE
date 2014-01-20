package de.fhkn.in.uce.socketswitch;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

public class LocalSystemTest {

	private final static int SERVERPORT = 6324;
	private final ISocketFactory socketFactory;

	public LocalSystemTest() {
		socketFactory = new RealSocketFactory("localhost", SERVERPORT);
	}

	protected SwitchableSocket createSwitchableSocket(Socket socket) throws IOException {
		return new HalfCloseSwitchableSocket(socket);
	}

	@Test
	public void test1() throws UnknownHostException, IOException, InterruptedException {
		int bytesToSend = 10000000;
		
		ConnectionsThread tc = new ConnectionsThread();
		
		SwitchableSocket tswServerSock = tc.getServerSwSocket();
		
		WriteThread tw = new WriteThread(tswServerSock, createBytes(bytesToSend));
		
		tc.start();
		tw.start();
		
		SwitchableSocket tswSock = tc.getClientSwSocket();
		handleConnectionClient(tswSock.getInputStream(), bytesToSend);
		
		tc.interrupt();
		tc.join();
		
		tswSock.close();
		
		tw.join();
	}
	
	
	
	private byte[] readFinish(InputStream in, int bytesMustRead)
			throws IOException {
		int bytesRead = 0;
		byte[] buffer = new byte[bytesMustRead];

		while (bytesRead < bytesMustRead) {
			int tLen = bytesMustRead - bytesRead;
			int tRead = in.read(buffer, bytesRead, tLen);
			//System.out.println("bytes read: " + bytesRead + " currentRead: " + tRead);
			// No EOF expected
			assertFalse(-1 == tRead);
			// can read 0 bytes???, no we should not, so assert. (ObjectInput stream throws error on read if 0 were received)
			assertFalse(0 == tRead);
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

	
	
	
	private class ConnectionsThread extends Thread {

		private final SwitchableSocket serverSwSocket;
		private final SwitchableSocket clientSwSocket;
		
		private final ServerSocket serverSocket;

		public ConnectionsThread() throws IOException {
			super("SwitchThread");
			
			serverSocket = new ServerSocket(SERVERPORT);
			
			clientSwSocket = createSwitchableSocket(socketFactory.createConnection());
			serverSwSocket = createSwitchableSocket(serverSocket.accept());
		}

		@Override
		public void run() {
			try {
				//while (this.isInterrupted() == false) {
					Thread.sleep(200);
					// create new connection
					final Socket tnewClientSock = socketFactory.createConnection();
					final Socket tnewServerSock = serverSocket.accept();
					
					Thread ts = new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								clientSwSocket.switchSocket(tnewClientSock, 1000);
							} catch (SwitchableException | TimeoutException | InterruptedException e) {
								e.printStackTrace();
							}
							
						}
					});
					
					ts.start();
					
					serverSwSocket.switchSocket(tnewServerSock, 1000);
					
					System.out.println("switched");
				//}
				
				serverSocket.close();
			} catch (InterruptedException | IOException | SwitchableException | TimeoutException e) {
				e.printStackTrace();
			}
		}

		public SwitchableSocket getServerSwSocket() {
			return serverSwSocket;
		}

		public SwitchableSocket getClientSwSocket() {
			return clientSwSocket;
		}
		
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
				
				//sock.shutdownOutput();
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
				//Thread.sleep(1);
			}
			System.out.println("bytes sent: " + sentByteCount);
		}

	}

}
