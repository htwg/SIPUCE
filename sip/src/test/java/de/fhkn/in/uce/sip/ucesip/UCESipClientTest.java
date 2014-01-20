package de.fhkn.in.uce.sip.ucesip;

import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import de.fhkn.in.uce.sip.ucesip.client.UCESipUserAgentClient;
import de.fhkn.in.uce.sip.ucesip.server.UCESipUserAgentServer;
import de.fhkn.in.uce.sip.ucesip.settings.UCESipClientSettings;
import de.fhkn.in.uce.sip.ucesip.settings.UCESipServerSettings;

public class UCESipClientTest {

	@SuppressWarnings("unused")
	private UCESipUserAgentClient uceSipClient;
	private static UCESipUserAgentServer uceSipServer;
	private static UCESipClientSettings sipClientSettings;
	private static UCESipServerSettings sipServerSettings;
	private static String testUser = "testUser";
	private static String serverSip = "213.239.218.18";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		sipClientSettings = new UCESipClientSettings(new InetSocketAddress(serverSip, 5060));
		sipServerSettings = new UCESipServerSettings(new InetSocketAddress(serverSip, 5060));
		uceSipServer = new UCESipUserAgentServer(testUser, serverSip, sipServerSettings);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		uceSipServer.shutdown();
	}

	@Before
	public void setUp() throws Exception {
		String user = UUID.randomUUID().toString();
		uceSipClient = new UCESipUserAgentClient(user, serverSip, testUser, serverSip, sipClientSettings);
	}

	@After
	public void tearDown() throws Exception {

	}

	@Test
	@Ignore
	public void test() {
		fail("Not yet implemented");
	}

}
