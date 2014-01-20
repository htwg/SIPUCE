package de.fhkn.in.uce.socketswitch;

import java.io.IOException;
import java.net.Socket;

import de.fhkn.in.uce.socketswitch.OneByteSwitchableInputStream;


public class OneByteSwitchableInputStreamComplexTest extends ByteCountSwitchableInputStreamComplexTest {

	@Override
	SwitchableInputStream createSwitchableInputStream(Socket socket)
			throws IOException {
		return new OneByteSwitchableInputStream(socket);
	}


}
