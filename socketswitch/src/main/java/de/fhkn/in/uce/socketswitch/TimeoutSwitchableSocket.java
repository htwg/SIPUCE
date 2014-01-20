package de.fhkn.in.uce.socketswitch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.TimeoutException;

/**
 * Can be used as an SwitchableSocket instance. Defines the concrete internal functionality. As SwitchableInputStream the TimeoutSwitchableInputStream, 
 * as SwitchableOutputStream
 *
 */
public final class TimeoutSwitchableSocket extends SwitchableSocket {
	
	private final ByteCountSocketSwitcher switcher;
	private final TimeoutSwitchableInputStream switchableInputStream;
	private final ByteCountSwitchableOutputStream switchableOutputStream;
	
	public TimeoutSwitchableSocket(Socket socket) throws IOException {
		super(socket);
		
		switchableInputStream = new TimeoutSwitchableInputStream(socket);
		switchableOutputStream = new ByteCountSwitchableOutputStream(socket);
		switcher = new ByteCountSocketSwitcher(switchableInputStream, switchableOutputStream);
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

	@Override
	public synchronized void setSoTimeout(int timeout) throws SocketException {
		switchableInputStream.setReadTimeout(timeout);
	}
	
	public void setPeriodicReadTimeout(int timeoutMillis) throws SocketException {
		switchableInputStream.setPeriodicReadTimeout(timeoutMillis);
	}
}
