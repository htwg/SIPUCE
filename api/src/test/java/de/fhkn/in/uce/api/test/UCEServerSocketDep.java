package de.fhkn.in.uce.api.test;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;

public class UCEServerSocketDep {

	@Test
	public void test() {
		try {
			InetAddress.getByAddress(new byte[] {0, 0, 0, 0});
		} catch (UnknownHostException e) {
			fail("InetAddress.getByAddress(new byte[] {0, 0, 0, 0}); must not fail");
		}
	}

}
