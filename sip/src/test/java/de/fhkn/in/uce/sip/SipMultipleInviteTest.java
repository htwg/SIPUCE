package de.fhkn.in.uce.sip;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.TooManyListenersException;
import java.util.UUID;

import javax.sdp.SdpException;
import javax.sip.InvalidArgumentException;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.SipException;
import javax.sip.TransportNotSupportedException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.fhkn.in.uce.sip.core.SipManager;
import de.fhkn.in.uce.sip.core.SipPreferences;
import de.fhkn.in.uce.sip.core.SipUAInstance;
import de.fhkn.in.uce.sip.core.SipUser;

public class SipMultipleInviteTest {

	private static SipManager sipManagerClient1, sipManagerClient2,sipManagerClient3, sipManagerServer;
	private static SipPreferences preferencesClient1, preferencesClient2, preferencesClient3, preferencesServer;
	private static SipUAInstance instanceClient1,instanceClient2,instanceClient3, instanceServer;
	private static SipUser sipUserClient1,sipUserClient2,sipUserClient3, sipUserServer;

	private static TestClientSpy client1, client2, client3;
	private static TestServerSpy server;

	@Before
	public void setUp() throws Exception {
		instanceServer = new SipUAInstance("SipManagerTestUserServer", UUID.randomUUID());

		preferencesServer = new SipPreferences(new InetSocketAddress(
				"213.239.218.18", 5060));

		instanceClient1 = new SipUAInstance("SipManagerTestUserClient", UUID.randomUUID());
		preferencesClient1 = new SipPreferences(new InetSocketAddress("213.239.218.18", 5060));
		sipManagerClient1 = new SipManager(preferencesClient1, instanceClient1);

		instanceClient2 = new SipUAInstance("SipManagerTestUserClient", UUID.randomUUID());
		preferencesClient2 = new SipPreferences(new InetSocketAddress("213.239.218.18", 5060));
		sipManagerClient2 = new SipManager(preferencesClient2, instanceClient2);

		instanceClient3 = new SipUAInstance("SipManagerTestUserClient", UUID.randomUUID());
		preferencesClient3 = new SipPreferences(new InetSocketAddress(
				"213.239.218.18", 5060));
		sipManagerClient3 = new SipManager(preferencesClient3, instanceClient3);
		sipManagerServer = new SipManager(preferencesServer, instanceServer);

		sipUserClient1 = new SipUser("SipManagerTestUserClient1", "213.239.218.18");
		sipUserClient2 = new SipUser("SipManagerTestUserClient2", "213.239.218.18");
		sipUserClient3 = new SipUser("SipManagerTestUserClient3", "213.239.218.18");

		sipUserServer = new SipUser("SipManagerTestUserServer", "213.239.218.18");

		client1 = new TestClientSpy(sipUserClient1, sipUserServer, sipManagerClient1);
		client2 = new TestClientSpy(sipUserClient2, sipUserServer, sipManagerClient2);
		client3 = new TestClientSpy(sipUserClient3, sipUserServer, sipManagerClient3);

		server = new TestServerSpy(sipUserServer, sipManagerServer);
	}

	@After
	public void tearDown() throws Exception {
		sipManagerClient1.shutdown();
		sipManagerServer.shutdown();
		server.shutdown();
	}

	@Test
	public void threeCensequtiveInvites() {
		try {
			server.start();

			client1.blockingRegister();
			client2.blockingRegister();
			client3.blockingRegister();

			client1.connectTo(sipUserServer);
			client2.connectTo(sipUserServer);
			client3.connectTo(sipUserServer);
			server.interrupt();

			client1.blockingDeregister();
			client2.blockingDeregister();
			client3.blockingDeregister();

			client1.verify();
			client2.verify();
			client3.verify();
			server.verify();
		} catch (InvalidArgumentException | TooManyListenersException
				| ParseException | SipException | InterruptedException | SdpException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void threeParallelInvites() {
		try {
			server.start();
			client1.start();
			client2.start();
			client3.start();
			client1.join();
			client2.join();
			client3.join();
			server.interrupt();
			server.join();

			client1.verify();
			client2.verify();
			client3.verify();
			server.verify();

		} catch (InterruptedException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void nInvitesEachOnOwnManager() throws PeerUnavailableException, TransportNotSupportedException, ObjectInUseException, InvalidArgumentException, TooManyListenersException, IOException {


		LinkedList<TestClientSpy> list = new LinkedList<>();
		for (int i = 0; i < 100; i++) {
			SipUAInstance instance = new SipUAInstance("SipManagerTestUserClient" + i, UUID.randomUUID());
			SipPreferences preferences = new SipPreferences(new InetSocketAddress("213.239.218.18", 5060));
			SipManager sipManagerClient = new SipManager(preferences, instance);
			SipUser sipUser = new SipUser("SipManagerTestUserClient" + i, "213.239.218.18");
			TestClientSpy client = new TestClientSpy(sipUser, sipUserServer, sipManagerClient);
			list.add(client);
		}

		try {
			server.start();
			Thread.sleep(100);
			for (TestClientSpy testClientSpy : list) {
				testClientSpy.start();
			}
			for (TestClientSpy testClientSpy : list) {
				testClientSpy.join();
			}
			server.interrupt();
			server.join();
			for (TestClientSpy testClientSpy : list) {
				testClientSpy.verify();
			}
			server.verify();
		} catch (InterruptedException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void nClientsOnSameManager() throws PeerUnavailableException, TransportNotSupportedException, ObjectInUseException, InvalidArgumentException, TooManyListenersException, IOException {

		SipUAInstance instance = new SipUAInstance("SipManagerTestUserClient", UUID.randomUUID());
		SipPreferences preferences = new SipPreferences(new InetSocketAddress("213.239.218.18", 5060));
		SipManager sipManagerClient = new SipManager(preferences, instance);

		LinkedList<TestClientSpy> list = new LinkedList<>();
		for (int i = 0; i < 100; i++) {
			SipUser sipUser = new SipUser("SipManagerTestUserClient" + i, "213.239.218.18");
			TestClientSpy client = new TestClientSpy(sipUser, sipUserServer, sipManagerClient);
			list.add(client);
		}

		try {
			server.start();

			for (TestClientSpy testClientSpy : list) {
				testClientSpy.start();
			}
			for (TestClientSpy testClientSpy : list) {
				testClientSpy.join();
			}
			server.interrupt();
			server.join();
			for (TestClientSpy testClientSpy : list) {
				testClientSpy.verify();
			}
			server.verify();

		} catch (InterruptedException e) {
			e.printStackTrace();
			fail();
		}
	}

}
