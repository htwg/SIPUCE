package de.fhkn.in.uce.sip;

import static org.junit.Assert.fail;

import java.net.InetSocketAddress;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;
import de.fhkn.in.uce.sip.ucesip.server.UCESipUserAgentServer;
import de.fhkn.in.uce.sip.ucesip.settings.UCESipServerSettings;
import de.fhkn.in.uce.sip.ucesip.settings.UCESipSettings.TransportProtocol;

public class RegisterUpdateTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		UCESipServerSettings seeting = new UCESipServerSettings(1000, 5000, 5000, 5000, 5000, 1000, 20, TransportProtocol.Udp, new InetSocketAddress(
				"213.239.218.18", 5060));
		try {
			UCESipUserAgentServer server = new UCESipUserAgentServer("registerUpdateTestUser", "213.239.218.18", seeting);
			server.register();
			for (int i = 40; i > 0; i--) {
				System.out.println(i + "s");
				Thread.sleep(1000);
			}

			server.shutdown();
		} catch (UCESipException | InterruptedException e) {
			e.printStackTrace();
			fail();
		}
	}

}
