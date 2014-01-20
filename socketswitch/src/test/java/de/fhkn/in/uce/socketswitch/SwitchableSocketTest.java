package de.fhkn.in.uce.socketswitch;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.fhkn.in.uce.socketswitch.test.mocksocket.MockSocket;

public class SwitchableSocketTest {
	
	private MockSocket mockSocket1;
	private MockSocket mockSocket2;
	
	private Socket mockSocket1o;
	private Socket mockSocket2o;

	

	@Before
	public void setUp() throws Exception {
		mockSocket1 = new MockSocket(10);
		mockSocket2 = new MockSocket(10);
		
		mockSocket1o = mockSocket1.getOppsositeSocket();
		mockSocket2o = mockSocket2.getOppsositeSocket();
		
	}

	@After
	public void tearDown() throws Exception {
		mockSocket1.close();
		mockSocket2.close();
		
		mockSocket1o.close();
		mockSocket2o.close();
	}
	
	private SwitchableSocket getSwitchableSocket(Socket socket) throws IOException {
		return new TimeoutSwitchableSocket(socket);
	}

	
	@Test
	public void testMockSocket() throws IOException {
		assertDuplexConnection(100, mockSocket1, mockSocket1o);
	}
	
	
	@Test
	public void testSwitch() throws IOException, SwitchableException, InterruptedException, TimeoutException {
		assertDuplexConnection(100, mockSocket1, mockSocket1o);
		
		System.out.println("TEST Step 1 fin");
		
		final SwitchableSocket sws = getSwitchableSocket(mockSocket1);
		final SwitchableSocket swso = getSwitchableSocket(mockSocket1o);
		
		assertDuplexConnection(100, sws, swso);
		
		System.out.println("TEST Step 2 fin");
		
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					swso.switchSocket(mockSocket2, 1000);
				} catch (SwitchableException | TimeoutException | InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		t.start();
		sws.switchSocket(mockSocket2o, 1000);
		t.join();
		
		assertDuplexConnection(100, sws, swso);
	}
	
	
	private void assertDuplexConnection(int bytesToSendCount, Socket socket, Socket oppositeSocket) throws IOException {
		byte[] bSend = createDetBytes(bytesToSendCount);
		byte[] b = new byte[bytesToSendCount];
		
		socket.getOutputStream().write(bSend);
		assertEquals(bytesToSendCount, oppositeSocket.getInputStream().read(b));
		assertArrayEquals(bSend, b);
		
		oppositeSocket.getOutputStream().write(bSend);
		assertEquals(bytesToSendCount, socket.getInputStream().read(b));
		assertArrayEquals(bSend, b);
	}
	
	private byte[] createDetBytes(int byteCount) {
		byte[] t = new byte[byteCount];
		byte b = 0;
		for(int i = 0; i < byteCount; i++) {
			t[i] = b;
			b++;
		}
		return t;
	}
	
	

}
