package de.fhkn.in.uce.sip;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.junit.Test;

public class SipDepTest {

	@Test
	public void testGetDefaultGatewayNetLocalAddress() throws UnknownHostException, IOException {
		Socket tSock = new Socket("web.de", 80);
		System.out.print(tSock.getLocalAddress());
		tSock.close();
	}

}
