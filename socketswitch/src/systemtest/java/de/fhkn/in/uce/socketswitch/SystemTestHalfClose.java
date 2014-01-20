package de.fhkn.in.uce.socketswitch;

import java.io.IOException;
import java.net.Socket;

import de.fhkn.in.uce.socketswitch.HalfCloseSwitchableSocket;
import de.fhkn.in.uce.socketswitch.SwitchableSocket;

public class SystemTestHalfClose extends SystemTest{

	@Override
	SwitchableSocket createSwitchableSocket(Socket connection)
			throws IOException {
		return new HalfCloseSwitchableSocket(connection);
	}

	@Override
	int getStreamId() {
		return 1;
	}
	
}
