package de.fhkn.in.uce.api.socket.socketswitch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.fhkn.in.uce.api.socket.RelayingClientMock;
import de.fhkn.in.uce.relaying.core.IRelayingClient;
import de.fhkn.in.uce.relaying.core.RelayingClient;

public class RelayServerSocketswitchDepTest {
	
	Socket serverRelaySocket;
	Socket clientRelaySocket;
	IRelayingClient tRelCl;
	
	private static final boolean USE_RELAY_MOCK = false;
		
	@Before
	public void setup() throws IOException, InterruptedException {
		String relayserver = "";
		
		if (USE_RELAY_MOCK) {
			relayserver = "localhost";
			tRelCl = new RelayingClientMock();
		} else {
			relayserver = "213.239.218.18";
			tRelCl = new RelayingClient(new InetSocketAddress(relayserver, 10300));
		}
		
		InetSocketAddress tEndpointAddr = tRelCl.createAllocation();
		
		clientRelaySocket = new Socket(relayserver, tEndpointAddr.getPort());
		
		serverRelaySocket = tRelCl.accept();
	}
	
	
	@After
	public void tearDown() throws IOException {
		tRelCl.discardAllocation();
		serverRelaySocket.close();
		clientRelaySocket.close();
	}

	
	@Test
	public void testSimpleTransfer() throws IOException, InterruptedException {
		
		clientRelaySocket.shutdownOutput();
		
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					serverRelaySocket.getOutputStream().write(createBytes(100000));
					serverRelaySocket.shutdownOutput();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		});
		
		t.start();
		
		
		readFinishAndCompare(clientRelaySocket.getInputStream(), createBytes(100000));
		
		t.join();
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
	
	
	/**
	 * Reads the specified number of bytes and returns them as an array.
	 * 
	 */
	private static byte[] readFinishAndCompare(InputStream in, byte[] bytesToCompare) throws IOException {
		int bytesRead = 0;
		byte[] buffer = new byte[bytesToCompare.length];

		while (bytesRead < buffer.length) {
			int tLen = buffer.length - bytesRead;
			int tRead = in.read(buffer, bytesRead, tLen);
			System.out.println("bytes read: " + bytesRead + " currentRead: " + tRead);
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

		return buffer;
	}

}
