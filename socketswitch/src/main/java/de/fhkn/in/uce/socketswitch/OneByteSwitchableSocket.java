package de.fhkn.in.uce.socketswitch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.TimeoutException;

public final class OneByteSwitchableSocket extends SwitchableSocket {

	private final OneByteSocketSwitcher switcher;
	private final OneByteSwitchableInputStream switchableInputStream;
	private final ByteCountSwitchableOutputStream switchableOutputStream;
	
	public OneByteSwitchableSocket(Socket socket) throws IOException {
		super(socket);
		
		switchableInputStream = new OneByteSwitchableInputStream(socket);
		switchableOutputStream = new ByteCountSwitchableOutputStream(socket);
		switcher = new OneByteSocketSwitcher(switchableInputStream, switchableOutputStream);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return switchableInputStream;
	}


	@Override
	public OutputStream getOutputStream() throws IOException {
		return switchableOutputStream;
	}
	
	@Override
	protected void implSwitchSocket(Socket newSocket, int timeoutMillis)
			throws SwitchableException, TimeoutException, InterruptedException {
		switcher.switchSocket(newSocket, timeoutMillis);		
	}
}
