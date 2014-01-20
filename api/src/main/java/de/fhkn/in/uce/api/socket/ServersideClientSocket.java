package de.fhkn.in.uce.api.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


final class ServersideClientSocket extends Socket {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServersideClientSocket.class);

	private final HolePuncherSwitcherThread holePuncherSwitcherThread;
	private final Socket socket;
	private final IUCEStunEndpoint stunEndpoint;

	public ServersideClientSocket(ServerHolePuncherSwitcherThread holePuncherSwitcherThread, Socket socket, IUCEStunEndpoint stunEndpoint) {
		this.holePuncherSwitcherThread = holePuncherSwitcherThread;
		this.socket = socket;
		this.stunEndpoint = stunEndpoint;
	}

	@Override
	public synchronized void close() throws IOException {
		
		LOGGER.info("closing");
		// we have to close the socket first! Because the relay server notices
		// only if we close the connection (most relevant on mock relaying
		// client)
		IOException ex = null;
		try {
			this.socket.close();
		} catch (IOException e) {
			ex = e;
		}

		try {
			// stop holepuncher and discard relay allocation.
			holePuncherSwitcherThread.terminate();
		} catch (InterruptedException e) {
			LOGGER.info("Unable to terminate relay allocation generator", e);
		}

		// release stun endpoint
		try {
			stunEndpoint.getStunEndpointSocket().close();
		} catch (IOException e) {
			LOGGER.info("Unable to release stun endpoint", e);
		}

		if (ex != null) {
			throw ex;
		}
	}

	@Override
	public boolean equals(Object obj) {
		return this.socket.equals(obj);
	}

	@Override
	public int hashCode() {
		return this.socket.hashCode();
	}

	@Override
	public void bind(final SocketAddress bindpoint) throws IOException {
		this.socket.bind(bindpoint);
	}

	@Override
	public void connect(final SocketAddress arg0, final int arg1) throws IOException {
		this.socket.connect(arg0, arg1);
	}

	@Override
	public SocketChannel getChannel() {
		return this.socket.getChannel();
	}

	@Override
	public InetAddress getInetAddress() {
		return this.socket.getInetAddress();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return this.socket.getInputStream();
	}

	@Override
	public boolean getKeepAlive() throws SocketException {
		return this.socket.getKeepAlive();
	}

	@Override
	public InetAddress getLocalAddress() {
		return this.socket.getLocalAddress();
	}

	@Override
	public int getLocalPort() {
		return this.socket.getLocalPort();
	}

	@Override
	public SocketAddress getLocalSocketAddress() {
		return this.socket.getLocalSocketAddress();
	}

	@Override
	public boolean getOOBInline() throws SocketException {
		return this.socket.getOOBInline();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return this.socket.getOutputStream();
	}

	@Override
	public int getPort() {
		return this.socket.getPort();
	}

	@Override
	public synchronized int getReceiveBufferSize() throws SocketException {
		return this.socket.getReceiveBufferSize();
	}

	@Override
	public SocketAddress getRemoteSocketAddress() {
		return this.socket.getRemoteSocketAddress();
	}

	@Override
	public boolean getReuseAddress() throws SocketException {
		return this.socket.getReuseAddress();
	}

	@Override
	public synchronized int getSendBufferSize() throws SocketException {
		return this.socket.getSendBufferSize();
	}

	@Override
	public int getSoLinger() throws SocketException {
		return this.socket.getSoLinger();
	}

	@Override
	public synchronized int getSoTimeout() throws SocketException {
		return this.socket.getSoTimeout();
	}

	@Override
	public boolean getTcpNoDelay() throws SocketException {
		return this.socket.getTcpNoDelay();
	}

	@Override
	public int getTrafficClass() throws SocketException {
		return this.socket.getTrafficClass();
	}

	@Override
	public boolean isBound() {
		return this.socket.isBound();
	}

	@Override
	public boolean isClosed() {
		return this.socket.isClosed();
	}

	@Override
	public boolean isConnected() {
		return this.socket.isConnected();
	}

	@Override
	public boolean isInputShutdown() {
		return this.socket.isInputShutdown();
	}

	@Override
	public boolean isOutputShutdown() {
		return this.socket.isOutputShutdown();
	}

	@Override
	public void sendUrgentData(final int data) throws IOException {
		this.socket.sendUrgentData(data);
	}

	@Override
	public void setKeepAlive(final boolean on) throws SocketException {
		this.socket.setKeepAlive(on);
	}

	@Override
	public void setOOBInline(final boolean on) throws SocketException {
		this.socket.setOOBInline(on);
	}

	@Override
	public void setPerformancePreferences(final int connectionTime, final int latency, final int bandwidth) {
		this.socket.setPerformancePreferences(connectionTime, latency, bandwidth);
	}

	@Override
	public synchronized void setReceiveBufferSize(final int size) throws SocketException {
		this.socket.setReceiveBufferSize(size);
	}

	@Override
	public void setReuseAddress(final boolean on) throws SocketException {
		this.socket.setReuseAddress(on);
	}

	@Override
	public synchronized void setSendBufferSize(final int size) throws SocketException {
		this.socket.setSendBufferSize(size);
	}

	@Override
	public void setSoLinger(final boolean on, final int linger) throws SocketException {
		this.socket.setSoLinger(on, linger);
	}

	@Override
	public synchronized void setSoTimeout(final int timeout) throws SocketException {
		this.socket.setSoTimeout(timeout);
	}

	@Override
	public void setTcpNoDelay(final boolean on) throws SocketException {
		this.socket.setTcpNoDelay(on);
	}

	@Override
	public void setTrafficClass(final int tc) throws SocketException {
		this.socket.setTrafficClass(tc);
	}

	@Override
	public void shutdownInput() throws IOException {
		this.socket.shutdownInput();
	}

	@Override
	public void shutdownOutput() throws IOException {
		this.socket.shutdownOutput();
	}

	@Override
	public String toString() {
		return this.socket.toString();
	}

}
