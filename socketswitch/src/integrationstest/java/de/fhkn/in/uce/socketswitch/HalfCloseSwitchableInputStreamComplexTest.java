package de.fhkn.in.uce.socketswitch;

import java.io.IOException;
import java.net.Socket;


public class HalfCloseSwitchableInputStreamComplexTest extends
		SwitchableInputStreamComplexTest {

	@Override
	SwitchableInputStream createSwitchableInputStream(Socket socket) throws IOException {
		return new HalfCloseSwitchableInputStream(socket);
	}

}
