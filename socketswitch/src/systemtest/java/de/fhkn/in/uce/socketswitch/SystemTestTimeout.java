package de.fhkn.in.uce.socketswitch;

import java.io.IOException;
import java.net.Socket;

import de.fhkn.in.uce.socketswitch.SwitchableSocket;
import de.fhkn.in.uce.socketswitch.TimeoutSwitchableSocket;

public class SystemTestTimeout extends SystemTest {

	@Override
	SwitchableSocket createSwitchableSocket(Socket connection)
			throws IOException {
		TimeoutSwitchableSocket tsock = new TimeoutSwitchableSocket(connection);
		tsock.setPeriodicReadTimeout(100);
		return tsock;
	}

	@Override
	int getStreamId() {
		return 0;
	}

}
