package de.fhkn.in.uce.socketswitch;

import java.io.IOException;
import java.net.Socket;

public class TimeoutSwitchableInputStreamComplexTest extends
		ByteCountSwitchableInputStreamComplexTest {

	@Override
	SwitchableInputStream createSwitchableInputStream(Socket socket)
			throws IOException {
		return new TimeoutSwitchableInputStream(socket);
	}

	

}
