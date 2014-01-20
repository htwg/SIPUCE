package de.fhkn.in.uce.socketswitch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.TimeoutException;

import de.fhkn.in.uce.socketswitch.SwitchableSocket;

public final class HalfCloseSwitchableSocket extends SwitchableSocket {

	private final HalfCloseSocketSwitcher switcher;
	private final HalfCloseSwitchableInputStream inputStream;
	private final HalfCloseSwitchableOutputStream outputStream;
	
	public HalfCloseSwitchableSocket(Socket socket) throws IOException {
		super(socket);
		
		inputStream = new HalfCloseSwitchableInputStream(socket);
		outputStream = new HalfCloseSwitchableOutputStream(socket);
		switcher = new HalfCloseSocketSwitcher(inputStream, outputStream);
	}

	@Override
	protected void implSwitchSocket(Socket newSocket, int timeoutMillis)
			throws SwitchableException, TimeoutException, InterruptedException {
		switcher.switchSocket(newSocket, timeoutMillis);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return inputStream;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return outputStream;
	}

}
