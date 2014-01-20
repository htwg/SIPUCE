package de.fhkn.in.uce.api.socket;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.csvreader.CsvWriter;

import de.fhkn.in.uce.api.socket.ConnectionUserData;
import de.fhkn.in.uce.api.socket.HalfCloseSocketSwitchFactory;
import de.fhkn.in.uce.api.socket.InstantConnectionStrategyFactory;
import de.fhkn.in.uce.api.socket.OnDemandConnectionStrategyFactory;
import de.fhkn.in.uce.api.socket.RelayingClientFactory;
import de.fhkn.in.uce.api.socket.StunEndpointFactory;
import de.fhkn.in.uce.api.socket.UCEClientSettings;
import de.fhkn.in.uce.api.socket.UCEException;
import de.fhkn.in.uce.api.socket.UCEServerSettings;
import de.fhkn.in.uce.api.socket.UCEServerSocket;
import de.fhkn.in.uce.api.socket.UCESocket;
import de.fhkn.in.uce.api.socket.UCETestSettings;
import etm.contrib.renderer.SimpleHtmlRenderer;
import etm.core.configuration.BasicEtmConfigurator;
import etm.core.configuration.EtmManager;
import etm.core.monitor.EtmMonitor;
import etm.core.monitor.EtmPoint;

public class UCESystemPerformanceTest {

	private static final EtmMonitor ETMMONITOR = EtmManager.getEtmMonitor();

	private static String TESTNAME;

	private static final int CONNECTION_MAX = 10;

	// use random user, so multible tests may run to the same time
	private final String toUser = UUID.randomUUID().toString();

	private static String DOMAIN;

	private static UCEClientSettings CLIENT_SETTINGS;
	private static UCEServerSettings SERVER_SETTINGS;

	private LinkedList<Double> instantValues = new LinkedList<Double>();
	private LinkedList<Double> onDemandValues = new LinkedList<Double>();

	private static enum ProxyImpl {
		opensips, local
	};

	private static ProxyImpl testProxyImpl = ProxyImpl.opensips;

	@Before
	public void setUp() throws IOException, UCEException {

		BasicEtmConfigurator.configure();
		ETMMONITOR.start();

		switch (testProxyImpl) {
		case opensips:
			DOMAIN = "213.239.218.18";
			CLIENT_SETTINGS = UCETestSettings.CLIENT_SETTINGS_WITHOUT_HOLEPUNCHER;
			SERVER_SETTINGS = UCETestSettings.SERVER_SETTINGS_WITHOUT_HOLEPUNCHER;
			break;
		case local:
			DOMAIN = "10.211.55.17";
			CLIENT_SETTINGS = UCETestSettings.LOCAL_CLIENT_SETTINGS_WITHOUT_HOLEPUNCHER;
			SERVER_SETTINGS = UCETestSettings.LOCAL_SERVER_SETTINGS_WITHOUT_HOLEPUNCHER;
			break;
		}
	}

	@Test
	public void testInstantStrategyConnectionTime() throws UCEException, IOException, InterruptedException {

		TESTNAME = "InstantStrategy";

		InstantServerSocketThread serverSocketThread = new InstantServerSocketThread();
		serverSocketThread.start();

		System.out.println("Connecting to target " + this.toUser + " ...");

		for (int i = 0; i < CONNECTION_MAX; i++) {
			System.out.println("Socket number: " + i);
			this.measureInstantConnection(DOMAIN, this.toUser, DOMAIN, CLIENT_SETTINGS);
		}

		serverSocketThread.join();

		(new File("results")).mkdirs();
		this.exportValuesToCSV("results/" + TESTNAME + ".csv", instantValues);
	}

	@Test
	public void testOnDemandStrategyConnectionTime() throws UCEException, IOException, InterruptedException {

		TESTNAME = "OnDemandStrategy";

		OnDemandServerSocketThread serverSocketThread = new OnDemandServerSocketThread();
		serverSocketThread.start();

		System.out.println("Connecting to target " + this.toUser + " ...");

		for (int i = 0; i < CONNECTION_MAX; i++) {
			System.out.println("Socket number: " + i);
			this.measureOnDemandConnection(DOMAIN, this.toUser, DOMAIN, CLIENT_SETTINGS);
		}

		serverSocketThread.join();

		this.exportValuesToCSV("results/" + TESTNAME + ".csv", onDemandValues);
	}

	@After
	public void teardown() throws IOException {
		ETMMONITOR.render(new SimpleHtmlRenderer(new PrintWriter(new File("results/PerformanceTest.html"))));
		ETMMONITOR.stop();
	}

	private void measureInstantConnection(String fromUserDomain, String toUser, String toUserDomain, UCEClientSettings clientSettings)
			throws UCEException, IOException {

		ConnectionUserData tUsD = new ConnectionUserData(UUID.randomUUID().toString(), fromUserDomain, toUser, toUserDomain);

		System.out.println("Start connect");

		EtmPoint point = ETMMONITOR.createPoint(TESTNAME);

		final UCESocket socketToPartner = new UCESocket(tUsD, clientSettings, new InstantConnectionStrategyFactory(),
				new HalfCloseSocketSwitchFactory(), new StunEndpointFactory());
		point.collect();

		this.instantValues.add(point.getTransactionTime());

		System.out.println("End connect");

		socketToPartner.close();
	}

	private void measureOnDemandConnection(String fromUserDomain, String toUser, String toUserDomain, UCEClientSettings clientSettings)
			throws UCEException, IOException {

		ConnectionUserData tUsD = new ConnectionUserData(UUID.randomUUID().toString(), fromUserDomain, toUser, toUserDomain);

		System.out.println("Start connect");

		EtmPoint point = ETMMONITOR.createPoint(TESTNAME);

		final UCESocket socketToPartner = new UCESocket(tUsD, clientSettings, new OnDemandConnectionStrategyFactory(),
				new HalfCloseSocketSwitchFactory(), new StunEndpointFactory());
		point.collect();

		this.onDemandValues.add(point.getTransactionTime());

		System.out.println("End connect");

		socketToPartner.close();
	}

	private void exportValuesToCSV(String filename, LinkedList<Double> values) throws IOException {

		CsvWriter csvOutput = new CsvWriter(filename);

		for (int i = 0; i < values.size(); i++) {
			csvOutput.write(String.valueOf(values.get(i)));
			csvOutput.endRecord();
		}

		csvOutput.close();
	}

	private class InstantServerSocketThread extends Thread {

		UCEServerSocket uceServerSocket;

		public InstantServerSocketThread() throws IOException {
			uceServerSocket = new UCEServerSocket(toUser, DOMAIN, SERVER_SETTINGS, new InstantConnectionStrategyFactory(),
					new HalfCloseSocketSwitchFactory(), new RelayingClientFactory(), new StunEndpointFactory());
		}

		@Override
		public void run() {

			System.out.println("UCEServerSocket is listening... ");

			try {

				for (int i = 0; i < CONNECTION_MAX; i++) {
					uceServerSocket.accept().close();
				}

				uceServerSocket.close();

				System.out.println("UCEServerSocket is closed... ");

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private class OnDemandServerSocketThread extends Thread {

		UCEServerSocket uceServerSocket;

		public OnDemandServerSocketThread() throws IOException {
			uceServerSocket = new UCEServerSocket(toUser, DOMAIN, SERVER_SETTINGS, new OnDemandConnectionStrategyFactory(),
					new HalfCloseSocketSwitchFactory(), new RelayingClientFactory(), new StunEndpointFactory());
		}

		@Override
		public void run() {

			System.out.println("UCEServerSocket is listening... ");

			try {

				for (int i = 0; i < CONNECTION_MAX; i++) {
					uceServerSocket.accept().close();
				}

				uceServerSocket.close();

				System.out.println("UCEServerSocket is closed... ");

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
