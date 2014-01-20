package de.fhkn.in.uce.socketswitch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fhkn.in.uce.socketswitch.SwitchableSocket;
import de.fhkn.in.uce.socketswitch.simpleserver.ConnectionPurposeMessage;
import de.fhkn.in.uce.socketswitch.simpleserver.ServerFunction;
import etm.contrib.renderer.SimpleHtmlRenderer;
import etm.core.configuration.BasicEtmConfigurator;
import etm.core.configuration.EtmManager;
import etm.core.monitor.EtmMonitor;
import etm.core.monitor.EtmPoint;

public abstract class SystemTest {

	private volatile boolean stopSwitching = false;

	private static final String SERVER = "localhost";
	// private static final String SERVER = "192.168.220.100";
	// private static final String SERVER = "141.37.84.190";
	// private static final String SERVER = "192.168.28.163";
	// private static final String SERVER = "213.239.218.18";

	private static final int PORT = 3145;
	private static final int SSLPORT = 3146;
	
	private static final EtmMonitor ETMMONITOR = EtmManager.getEtmMonitor();
	private static volatile String TESTNAME;
	private static enum SocketType {
		Mock, SSL, Real
	}
	private static SocketType testSocketType = SocketType.Real;
	
	private static final Map<String, ArrayList<Long>> ALLLISTS = new HashMap<String, ArrayList<Long>>();

	private ISocketFactory socketFactory;

	public SystemTest() {
		switch(testSocketType) {
		case Mock:
			socketFactory = new MockSocketFactory(2);
			break;
		case Real:
			socketFactory = new RealSocketFactory(SERVER, PORT);
			break;
		case SSL:
			// Keystore muss zuvor erzeugt werden! http://www.tutorials.de/java/267445-ssl-socketverbindung-mit-java.html
			System.setProperty("javax.net.ssl.keyStore", "C:\\Users\\tipartl\\Documents\\SSLKeyStore");
			System.setProperty("javax.net.ssl.keyStorePassword", "asdasd");
			System.setProperty("javax.net.ssl.trustStore", "C:\\Users\\tipartl\\Documents\\SSLKeyStore");
			System.setProperty("javax.net.ssl.trustStorePassword", "asdasd");
			socketFactory = new SysTestSSLSocketFactory(SERVER, SSLPORT);
			break;
		}
	}

	abstract SwitchableSocket createSwitchableSocket(Socket connection)
			throws IOException;

	abstract int getStreamId();

	@BeforeClass
	public static void setup() {
		BasicEtmConfigurator.configure();
		ETMMONITOR.start();
	}

	@AfterClass
	public static void shutdown() throws FileNotFoundException {
		// visualize results
		ETMMONITOR.render(new SimpleHtmlRenderer(new PrintWriter(new File(
				"output.html"))));
		ETMMONITOR.stop();

		for (String list : ALLLISTS.keySet()) {
			long sum = 0;
			long max = 0;
			long min = Long.MAX_VALUE;
			for (Long time : ALLLISTS.get(list)) {
				if (time > max) {
					max = time;
				}
				if (time < min) {
					min = time;
				}
				sum += time;
			}
			long avg = sum / ALLLISTS.get(list).size();
			NumberFormat nb = NumberFormat.getNumberInstance(Locale.GERMAN);
			System.out.printf(
					"%s - switches: %s avg: %s, min: %s, max: %s, total: %s%n",
					list,
					nb.format(ALLLISTS.get(list).size()),
					nb.format((double) avg / 1000000.0),
					nb.format((double) min / 1000000.0),
					nb.format((double) max / 1000000.0),
					nb.format((double) sum / 1000000.0));
		}
	}

	@Test
	public void testDefaultSwitchUsage() throws IOException,
			InterruptedException {
		TESTNAME = "DefaultSwitchUsage";
		final UUID connId = UUID.randomUUID();
		final int sendBytes = 50;

		Socket tSock = socketFactory.createConnection();
		ObjectOutputStream objectOut = new ObjectOutputStream(
				tSock.getOutputStream());
		ConnectionPurposeMessage tCp = new ConnectionPurposeMessage(
				ServerFunction.ReplyBytesXBytesStep, connId, getStreamId(),
				5000);
		objectOut.writeObject(tCp);

		final SwitchableSocket swSock = createSwitchableSocket(tSock);
		InputStream in = swSock.getInputStream();

		Thread tw = startWriteThread(swSock, sendBytes, 1000);

		// sleep to prevent switching before creating socket
		Thread.sleep(500);

		Thread ts = startSwitchThread(swSock, connId, 1000, 0);

		int tMustRead = sendBytes;
		byte tReadValue = 0;
		while (tMustRead > 0) {
			byte[] b = new byte[1000];
			int currReadBytes = in.read(b);
			assertTrue(currReadBytes != -1);
			for (int i = 0; i < currReadBytes; i++) {
				assertEquals(tReadValue, b[i]);
				tReadValue++;
			}
			tMustRead -= currReadBytes;
		}

		stopSwitching = true;
		ts.join();
		tw.join();
		System.out.println("Closing Client-Socket");
		swSock.close();
	}

	@Test
	public void testFastWriteSwitchesAfter() throws IOException,
			InterruptedException {
		TESTNAME = "FastWriteSwitchesAfter";
		final UUID connId = UUID.randomUUID();
		final int sendBytes = 500000;

		Socket tSock = socketFactory.createConnection();
		ObjectOutputStream objectOut = new ObjectOutputStream(
				tSock.getOutputStream());
		ConnectionPurposeMessage tCp = new ConnectionPurposeMessage(
				ServerFunction.ReplyBytesXBytesStep, connId, getStreamId(),
				5000);
		objectOut.writeObject(tCp);

		final SwitchableSocket swSock = createSwitchableSocket(tSock);
		InputStream in = swSock.getInputStream();

		Thread tw = startFastWriteThread(swSock, sendBytes);

		// sleep to prevent switching before creating socket
		Thread.sleep(500);

		Thread ts = startSwitchThread(swSock, connId, 1000, 10);

		Thread.sleep(10000);

		int tMustRead = sendBytes;
		byte tReadValue = 0;
		while (tMustRead > 0) {
			byte[] b = new byte[1000];
			int currReadBytes = in.read(b);
			assertTrue(currReadBytes != -1);
			for (int i = 0; i < currReadBytes; i++) {
				assertEquals(tReadValue, b[i]);
				tReadValue++;
			}
			tMustRead -= currReadBytes;
		}

		stopSwitching = true;
		ts.join();
		tw.join();
		System.out.println("Closing Client-Socket");
		swSock.close();
	}

	@Test
	public void testManySwitches10MB() throws IOException, InterruptedException {
		TESTNAME = "ManySwitches10MB";
		final UUID connId = UUID.randomUUID();
		final int sendBytes = 10000000;

		Socket tSock = socketFactory.createConnection();
		ObjectOutputStream objectOut = new ObjectOutputStream(
				tSock.getOutputStream());
		ConnectionPurposeMessage tCp = new ConnectionPurposeMessage(
				ServerFunction.ReplyBytesXBytesStep, connId, getStreamId(),
				1000);
		objectOut.writeObject(tCp);

		final SwitchableSocket swSock = createSwitchableSocket(tSock);
		InputStream in = swSock.getInputStream();

		Thread tw = startWriteThread(swSock, sendBytes, 10);

		// sleep to prevent switching before creating socket
		Thread.sleep(500);

		Thread ts = startSwitchThread(swSock, connId, 0, 0);

		Thread.sleep(1000);

		int tMustRead = sendBytes;
		byte tReadValue = 0;
		while (tMustRead > 0) {
			byte[] b = new byte[2000];
			int currReadBytes = in.read(b);
			assertTrue(currReadBytes != -1);
			for (int i = 0; i < currReadBytes; i++) {
				assertEquals(tReadValue, b[i]);
				tReadValue++;
			}
			tMustRead -= currReadBytes;
			System.out.println("mustRead: " + tMustRead);
		}

		stopSwitching = true;
		ts.join();
		tw.join();
		System.out.println("Closing Client-Socket");
		swSock.close();
	}

	@Test
	public void test() throws IOException, InterruptedException {
		TESTNAME = "1";
		final UUID connId = UUID.randomUUID();
		final int sendBytes = 100000;

		Socket tSock = socketFactory.createConnection();
		ObjectOutputStream objectOut = new ObjectOutputStream(
				tSock.getOutputStream());
		ConnectionPurposeMessage tCp = new ConnectionPurposeMessage(
				ServerFunction.ReplyBytesXBytesStep, connId, getStreamId(), 100);
		objectOut.writeObject(tCp);

		final SwitchableSocket swSock = createSwitchableSocket(tSock);
		InputStream in = swSock.getInputStream();

		Thread tw = startWriteThread(swSock, sendBytes, 0);

		// sleep to prevent switching before creating socket
		Thread.sleep(500);

		Thread ts = startSwitchThread(swSock, connId, 0, 10);

		Thread.sleep(1000);

		int tMustRead = sendBytes;
		byte tReadValue = 0;
		while (tMustRead > 0) {
			byte[] b = new byte[2000];
			int currReadBytes = in.read(b);
			assertTrue(currReadBytes != -1);
			for (int i = 0; i < currReadBytes; i++) {
				assertEquals(tReadValue, b[i]);
				tReadValue++;
			}
			tMustRead -= currReadBytes;
		}

		stopSwitching = true;
		ts.join();
		tw.join();

		System.out.println("Closing Client-Socket");
		swSock.close();
	}

	private Thread startWriteThread(final SwitchableSocket swSock,
			final int bytesToSend, final int sleep) {
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					OutputStream out = swSock.getOutputStream();
					int remainingBytes = bytesToSend;
					int bytesBlockCount = 0;
					byte tValue = 0;

					while (remainingBytes > 0) {
						if (++bytesBlockCount > 5000) {
							bytesBlockCount = 1;
						}
						if (remainingBytes - bytesBlockCount < 0) {
							bytesBlockCount = remainingBytes;
						}
						byte[] b = new byte[bytesBlockCount];
						for (int i = 0; i < bytesBlockCount; i++) {
							b[i] = tValue;
							tValue++;
						}
						out.write(b);
						remainingBytes -= bytesBlockCount;
						if (sleep != 0) {
							Thread.sleep(sleep);
						}
					}
					swSock.shutdownOutput();
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.printf("write thread finished\n");
			}
		};
		t.start();
		return t;
	}

	private Thread startFastWriteThread(final SwitchableSocket swSock,
			final int bytesToSend) {
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					OutputStream out = swSock.getOutputStream();
					byte tValue = 0;
					byte[] b = new byte[bytesToSend];
					for (int i = 0; i < bytesToSend; i++) {
						b[i] = tValue;
						tValue++;
					}

					out.write(b);

					swSock.shutdownOutput();
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.printf("write thread finished\n");
			}
		};
		t.start();
		return t;
	}

	private Thread startSwitchThread(final SwitchableSocket swSock,
			final UUID connId, final int startWait, final int waitIncrement) {
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					int waitTime = startWait;
					ArrayList<Long> durationList = new ArrayList<>();
					while (true) {
						if (stopSwitching) {
							break;
						}
						Socket tSock2 = socketFactory.createConnection();
						ObjectOutputStream objectOut2 = new ObjectOutputStream(
								tSock2.getOutputStream());
						ConnectionPurposeMessage tCp2 = new ConnectionPurposeMessage(
								ServerFunction.SwitchToThisConnection, connId,
								0, 0);
						objectOut2.writeObject(tCp2);

						EtmPoint point = ETMMONITOR.createPoint("test "
								+ TESTNAME);

						swSock.switchSocket(tSock2, 30000);

						point.collect();
						durationList.add(((SwitchableOutputStream) swSock
								.getOutputStream()).getSwitchDuration());
						System.out.println("Switched");

						Thread.sleep(waitTime);
						if (waitTime < 1000) {
							waitTime += waitIncrement;
						}
					}
					ALLLISTS.put("test " + TESTNAME, durationList);
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.printf("switch thread finished%n");
			}
		};
		t.start();
		return t;
	}
}
