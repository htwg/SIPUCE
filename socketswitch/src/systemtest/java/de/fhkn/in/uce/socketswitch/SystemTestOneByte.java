package de.fhkn.in.uce.socketswitch;

import java.io.IOException;
import java.net.Socket;

import de.fhkn.in.uce.socketswitch.SwitchableSocket;
import de.fhkn.in.uce.socketswitch.OneByteSwitchableSocket;

public class SystemTestOneByte extends SystemTest {

	@Override
	SwitchableSocket createSwitchableSocket(Socket connection)
			throws IOException {
		return new OneByteSwitchableSocket(connection);
	}

	@Override
	int getStreamId() {
		return 2;
	}

}
