package de.fhkn.in.uce.api.socket.remotetest;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Test;

import de.fhkn.in.uce.api.socket.UCEException;
import de.fhkn.in.uce.api.socket.UCESocket;
import de.fhkn.in.uce.api.socket.UCETestSettings;

public class UCERemoteSystemTest {
	
	private final String toUser = "janosch1";
	private final String sipServer = "213.239.218.18";
	
	
	
	@Test
	public void testOneClient() throws UCEException, IOException, InterruptedException {
		byte[] oneMega = createBytes(1000000);
		
		UCESocket uceClientSocket = new UCESocket(sipServer, toUser, sipServer,
				UCETestSettings.CLIENT_SETTINGS);
		
		Thread writer = new Thread(new ClientWriteThread(uceClientSocket, oneMega));
		writer.start();
		
		byte[] arrived = readFinishAndCompare(uceClientSocket.getInputStream(), oneMega);
		
		assertArrayEquals(oneMega, arrived);
		
		uceClientSocket.close();
	}
	
	@Test
	public void testTwoClientsSequently() throws UCEException, IOException, InterruptedException {
		byte[] oneMega = createBytes(1000000);
		byte[] hundretKilo = createBytes(100000);
		
		UCESocket uceClientSocket = new UCESocket(sipServer, toUser, sipServer,
				UCETestSettings.CLIENT_SETTINGS);
		
		UCESocket uceClientSocket2 = new UCESocket(sipServer, toUser, sipServer,
				UCETestSettings.CLIENT_SETTINGS);
		
		Thread writer = new Thread(new ClientWriteThread(uceClientSocket, oneMega));
		writer.start();
		
		Thread writer2 = new Thread(new ClientWriteThread(uceClientSocket2, hundretKilo));
		writer2.start();
		
		byte[] arrived = readFinishAndCompare(uceClientSocket.getInputStream(), oneMega);
		assertArrayEquals(oneMega, arrived);
		
		byte[] arrived2 = readFinishAndCompare(uceClientSocket2.getInputStream(), hundretKilo);
		assertArrayEquals(hundretKilo, arrived2);
		
		writer.join();
		writer2.join();
		
		uceClientSocket.close();
		uceClientSocket2.close();
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
	 * @param in
	 * @param bytesMustRead
	 * @return
	 * @throws IOException
	 */
	private byte[] readFinishAndCompare(InputStream in, byte[] bytesToCompare)
			throws IOException {
		int bytesRead = 0;
		byte[] buffer = new byte[bytesToCompare.length];

		while (bytesRead < buffer.length) {
			int tLen = buffer.length - bytesRead;
			int tRead = in.read(buffer, bytesRead, tLen);
//			System.out.println("bytes read: " + bytesRead + " currentRead: " + tRead);
			// No EOF expected
			assertNotEquals(-1, tRead);
			// can read 0 bytes???, no we should not, so assert. (ObjectInput stream throws error on read if 0 were received)
			assertNotEquals(0, tRead);
			// assert that the read bytes are the right one
			for (int i = bytesRead; i < tRead; i++) {
				assertEquals(bytesToCompare[i], buffer[i]);
			}
			bytesRead += tRead;
		}
		// EOF is expected
		int tbyte = in.read();
		assertEquals(-1, tbyte);
		
		return buffer;
	}
	
	// own thread to write bytes to an outputstream
	private static class ClientWriteThread implements Runnable {
		private final OutputStream out;
		private final byte[] toWrite;
		private final UCESocket socket;
		
		public ClientWriteThread(final UCESocket socket, final byte[] toWrite) throws IOException {
			this.out = socket.getOutputStream();
			this.toWrite = toWrite;
			this.socket = socket;
		}
		
		@Override
		public void run() {
			try {
				out.write(toWrite);
				socket.shutdownOutput();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
