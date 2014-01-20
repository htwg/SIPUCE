package de.fhkn.in.uce.socketswitch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeoutException;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Encapsulates the Socket functionality and allows additionally the switching
 * of the underlying socket connection. This abstract class gives only the
 * infrastructure. The concrete implementations must be chosen by the extending
 * classes (factory pattern). All socket operations are thread save, also the
 * switching to a new socket connection. Writing and reading on the streams must
 * be thread safe by implemented by the underlying switchable streams.
 * 
 */
public abstract class SwitchableSocket extends Socket {

	@NonNull
	private volatile Socket currentSocket;
	private final Map<SocketSetOptions, ISetSocketOption> executedSocketOptions;

	SwitchableSocket(Socket socket) throws IOException {
		this.currentSocket = socket;
		this.executedSocketOptions = new HashMap<SocketSetOptions, ISetSocketOption>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getInputStream()
	 */
	@Override
	public abstract InputStream getInputStream() throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getOutputStream()
	 */
	@Override
	public abstract OutputStream getOutputStream() throws IOException;

	protected abstract void implSwitchSocket(Socket newSocket, int timeoutMillis)
			throws SwitchableException, TimeoutException, InterruptedException;

	/**
	 * Switches the underlying socket to the new socket connection. For success
	 * the new connection should be established.
	 * 
	 * @param newSocket
	 * @param newSocket
	 * @throws SwitchableException
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	public synchronized void switchSocket(Socket newSocket, int timeoutMillis)
			throws SwitchableException, TimeoutException, InterruptedException {
		if (currentSocket.isClosed()) {
			throw new SwitchableException("Socket is closed");
		}

		implSwitchSocket(newSocket, timeoutMillis);

		try {
			// make sure the old socket is closed.
			currentSocket.close();
		} catch (IOException e) {
		}
		currentSocket = newSocket;

		try {
			// set socket options from old socket to new
			for (Entry<SocketSetOptions, ISetSocketOption> option : executedSocketOptions
					.entrySet()) {
				option.getValue().set(currentSocket);
			}
		} catch (IOException e) {
			throw new SwitchableException("Error setting old socket options!",
					e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#setSoTimeout(int)
	 */
	@Override
	public synchronized void setSoTimeout(int timeout) throws SocketException {
		ISetSocketOption tSetOpt = new SoTimeoutSetSocketOption(timeout);
		try {
			tSetOpt.set(currentSocket);
			executedSocketOptions.put(SocketSetOptions.SoTimeout, tSetOpt);
		} catch (SocketException ex) {
			throw ex;
		} catch (IOException e) {
			// cannot throw io exception
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#connect(java.net.SocketAddress)
	 */
	@Override
	public synchronized void connect(SocketAddress endpoint) throws IOException {
		currentSocket.connect(endpoint);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#connect(java.net.SocketAddress, int)
	 */
	@Override
	public synchronized void connect(SocketAddress endpoint, int timeout)
			throws IOException {
		currentSocket.connect(endpoint, timeout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#bind(java.net.SocketAddress)
	 */
	@Override
	public synchronized void bind(SocketAddress bindpoint) throws IOException {
		currentSocket.bind(bindpoint);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getInetAddress()
	 */
	@Override
	public synchronized InetAddress getInetAddress() {
		return currentSocket.getInetAddress();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getLocalAddress()
	 */
	@Override
	public synchronized InetAddress getLocalAddress() {
		return currentSocket.getLocalAddress();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getPort()
	 */
	@Override
	public synchronized int getPort() {
		return currentSocket.getPort();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getLocalPort()
	 */
	@Override
	public synchronized int getLocalPort() {
		return currentSocket.getLocalPort();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getRemoteSocketAddress()
	 */
	@Override
	public synchronized SocketAddress getRemoteSocketAddress() {
		return currentSocket.getRemoteSocketAddress();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getLocalSocketAddress()
	 */
	@Override
	public synchronized SocketAddress getLocalSocketAddress() {
		return currentSocket.getLocalSocketAddress();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getChannel()
	 */
	@Override
	public synchronized SocketChannel getChannel() {
		return currentSocket.getChannel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#setTcpNoDelay(boolean)
	 */
	@Override
	public synchronized void setTcpNoDelay(boolean on) throws SocketException {
		ISetSocketOption tSetOpt = new TcpNoDelaySetSocketOption(on);
		try {
			tSetOpt.set(currentSocket);
			executedSocketOptions.put(SocketSetOptions.TcpNoDelay, tSetOpt);
		} catch (SocketException ex) {
			throw ex;
		} catch (IOException e) {
			// cannot throw io exception
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getTcpNoDelay()
	 */
	@Override
	public synchronized boolean getTcpNoDelay() throws SocketException {
		return currentSocket.getTcpNoDelay();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#setSoLinger(boolean, int)
	 */
	@Override
	public synchronized void setSoLinger(boolean on, int linger)
			throws SocketException {
		ISetSocketOption tSetOpt = new SoLingerSetSocketOption(on, linger);
		try {
			tSetOpt.set(currentSocket);
			executedSocketOptions.put(SocketSetOptions.SoLinger, tSetOpt);
		} catch (SocketException ex) {
			throw ex;
		} catch (IOException e) {
			// cannot throw io exception
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getSoLinger()
	 */
	@Override
	public synchronized int getSoLinger() throws SocketException {
		return currentSocket.getSoLinger();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#sendUrgentData(int)
	 */
	@Override
	public synchronized void sendUrgentData(int data) throws IOException {
		currentSocket.sendUrgentData(data);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#setOOBInline(boolean)
	 */
	@Override
	public synchronized void setOOBInline(boolean on) throws SocketException {
		ISetSocketOption tSetOpt = new OOBInlineSetSocketOption(on);
		try {
			tSetOpt.set(currentSocket);
			executedSocketOptions.put(SocketSetOptions.OOBInline, tSetOpt);
		} catch (SocketException ex) {
			throw ex;
		} catch (IOException e) {
			// cannot throw io exception
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getOOBInline()
	 */
	@Override
	public synchronized boolean getOOBInline() throws SocketException {
		return currentSocket.getOOBInline();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getSoTimeout()
	 */
	@Override
	public synchronized int getSoTimeout() throws SocketException {
		return currentSocket.getSoTimeout();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#setSendBufferSize(int)
	 */
	@Override
	public synchronized void setSendBufferSize(int size) throws SocketException {
		ISetSocketOption tSetOpt = new SendBufferSizeSetSocketOption(size);
		try {
			tSetOpt.set(currentSocket);
			executedSocketOptions.put(SocketSetOptions.SendBufferSize, tSetOpt);
		} catch (SocketException ex) {
			throw ex;
		} catch (IOException e) {
			// cannot throw io exception
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getSendBufferSize()
	 */
	@Override
	public synchronized int getSendBufferSize() throws SocketException {
		return currentSocket.getSendBufferSize();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#setReceiveBufferSize(int)
	 */
	@Override
	public synchronized void setReceiveBufferSize(int size)
			throws SocketException {
		ISetSocketOption tSetOpt = new ReceiveBufferSizeSetSocketOption(size);
		try {
			tSetOpt.set(currentSocket);
			executedSocketOptions.put(SocketSetOptions.ReceiveBufferSize,
					tSetOpt);
		} catch (SocketException ex) {
			throw ex;
		} catch (IOException e) {
			// cannot throw io exception
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getReceiveBufferSize()
	 */
	@Override
	public synchronized int getReceiveBufferSize() throws SocketException {
		return currentSocket.getReceiveBufferSize();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#setKeepAlive(boolean)
	 */
	@Override
	public synchronized void setKeepAlive(boolean on) throws SocketException {
		ISetSocketOption tSetOpt = new KeepAliveSetSocketOption(on);
		try {
			tSetOpt.set(currentSocket);
			executedSocketOptions.put(SocketSetOptions.KeepAlive, tSetOpt);
		} catch (SocketException ex) {
			throw ex;
		} catch (IOException e) {
			// cannot throw io exception
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getKeepAlive()
	 */
	@Override
	public synchronized boolean getKeepAlive() throws SocketException {
		return currentSocket.getKeepAlive();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#setTrafficClass(int)
	 */
	@Override
	public synchronized void setTrafficClass(int tc) throws SocketException {
		ISetSocketOption tSetOpt = new TrafficClassSetSocketOption(tc);
		try {
			tSetOpt.set(currentSocket);
			executedSocketOptions.put(SocketSetOptions.TrafficClass, tSetOpt);
		} catch (SocketException ex) {
			throw ex;
		} catch (IOException e) {
			// cannot throw io exception
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getTrafficClass()
	 */
	@Override
	public synchronized int getTrafficClass() throws SocketException {
		return currentSocket.getTrafficClass();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#setReuseAddress(boolean)
	 */
	@Override
	public synchronized void setReuseAddress(boolean on) throws SocketException {
		ISetSocketOption tSetOpt = new ReuseAddressSetSocketOption(on);
		try {
			tSetOpt.set(currentSocket);
			executedSocketOptions.put(SocketSetOptions.ReuseAddress, tSetOpt);
		} catch (SocketException ex) {
			throw ex;
		} catch (IOException e) {
			// cannot throw io exception
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getReuseAddress()
	 */
	@Override
	public synchronized boolean getReuseAddress() throws SocketException {
		return currentSocket.getReuseAddress();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#close()
	 */
	@Override
	public synchronized void close() throws IOException {
		currentSocket.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#shutdownInput()
	 */
	@Override
	public synchronized void shutdownInput() throws IOException {
		ISetSocketOption tSetOpt = new ShutdownInputSetSocketOption();
		tSetOpt.set(currentSocket);
		executedSocketOptions.put(SocketSetOptions.ShutdownInput, tSetOpt);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#shutdownOutput()
	 */
	@Override
	public synchronized void shutdownOutput() throws IOException {
		ISetSocketOption tSetOpt = new ShutdownOutputSetSocketOption();
		tSetOpt.set(currentSocket);
		executedSocketOptions.put(SocketSetOptions.ShutdownOutput, tSetOpt);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#toString()
	 */
	@Override
	public synchronized String toString() {
		return currentSocket.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#isConnected()
	 */
	@Override
	public synchronized boolean isConnected() {
		return currentSocket.isConnected();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#isBound()
	 */
	@Override
	public synchronized boolean isBound() {
		return currentSocket.isBound();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#isClosed()
	 */
	@Override
	public synchronized boolean isClosed() {
		return currentSocket.isClosed();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#isInputShutdown()
	 */
	@Override
	public synchronized boolean isInputShutdown() {
		return currentSocket.isInputShutdown();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#isOutputShutdown()
	 */
	@Override
	public synchronized boolean isOutputShutdown() {
		return currentSocket.isOutputShutdown();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#setPerformancePreferences(int, int, int)
	 */
	@Override
	public synchronized void setPerformancePreferences(int connectionTime,
			int latency, int bandwidth) {
		ISetSocketOption tSetOpt = new PerformancePreferencesSetSocketOption(
				connectionTime, latency, bandwidth);

		try {
			tSetOpt.set(currentSocket);
			executedSocketOptions.put(SocketSetOptions.PerformancePreferences,
					tSetOpt);
		} catch (IOException e) {
			// this function cannot throw exception
		}
	}

	private enum SocketSetOptions {
		PerformancePreferences, ShutdownOutput, ShutdownInput, ReuseAddress, TrafficClass, KeepAlive, ReceiveBufferSize, SendBufferSize, OOBInline, SoLinger, TcpNoDelay, SoTimeout
	}

	private interface ISetSocketOption {
		void set(Socket socket) throws IOException;
	}

	private static class PerformancePreferencesSetSocketOption implements
			ISetSocketOption {

		private int connectionTime;
		private int latency;
		private int bandwidth;

		PerformancePreferencesSetSocketOption(int connectionTime, int latency,
				int bandwidth) {
			this.connectionTime = connectionTime;
			this.latency = latency;
			this.bandwidth = bandwidth;
		}

		@Override
		public void set(Socket socket) throws IOException {
			socket.setPerformancePreferences(connectionTime, latency, bandwidth);
		}
	}

	private static class ShutdownOutputSetSocketOption implements
			ISetSocketOption {

		@Override
		public void set(Socket socket) throws IOException {
			socket.shutdownOutput();
		}
	}

	private static class ShutdownInputSetSocketOption implements
			ISetSocketOption {

		@Override
		public void set(Socket socket) throws IOException {
			socket.shutdownInput();
		}
	}

	private static class ReuseAddressSetSocketOption implements
			ISetSocketOption {

		private boolean on;

		public ReuseAddressSetSocketOption(boolean on) {
			this.on = on;
		}

		@Override
		public void set(Socket socket) throws IOException {
			socket.setReuseAddress(on);
		}
	}

	private static class TrafficClassSetSocketOption implements
			ISetSocketOption {

		private int tc;

		public TrafficClassSetSocketOption(int tc) {
			this.tc = tc;
		}

		@Override
		public void set(Socket socket) throws IOException {
			socket.setTrafficClass(tc);
		}
	}

	private static class KeepAliveSetSocketOption implements ISetSocketOption {

		private boolean on;

		public KeepAliveSetSocketOption(boolean on) {
			this.on = on;
		}

		@Override
		public void set(Socket socket) throws IOException {
			socket.setKeepAlive(on);
		}
	}

	private static class ReceiveBufferSizeSetSocketOption implements
			ISetSocketOption {

		private int size;

		public ReceiveBufferSizeSetSocketOption(int size) {
			this.size = size;
		}

		@Override
		public void set(Socket socket) throws IOException {
			socket.setReceiveBufferSize(size);
		}
	}

	private static class SendBufferSizeSetSocketOption implements
			ISetSocketOption {

		private int size;

		public SendBufferSizeSetSocketOption(int size) {
			this.size = size;
		}

		@Override
		public void set(Socket socket) throws IOException {
			socket.setSendBufferSize(size);
		}
	}

	private static class OOBInlineSetSocketOption implements ISetSocketOption {

		private boolean on;

		public OOBInlineSetSocketOption(boolean on) {
			this.on = on;
		}

		@Override
		public void set(Socket socket) throws IOException {
			socket.setOOBInline(on);
		}
	}

	private static class SoLingerSetSocketOption implements ISetSocketOption {

		private int linger;
		private boolean on;

		public SoLingerSetSocketOption(boolean on, int linger) {
			this.on = on;
			this.linger = linger;
		}

		@Override
		public void set(Socket socket) throws IOException {
			socket.setSoLinger(on, linger);
		}
	}

	private static class TcpNoDelaySetSocketOption implements ISetSocketOption {

		private boolean on;

		public TcpNoDelaySetSocketOption(boolean on) {
			this.on = on;
		}

		@Override
		public void set(Socket socket) throws IOException {
			socket.setTcpNoDelay(on);
		}
	}

	private static class SoTimeoutSetSocketOption implements ISetSocketOption {

		private int timeout;

		public SoTimeoutSetSocketOption(int on) {
			this.timeout = on;
		}

		@Override
		public void set(Socket socket) throws IOException {
			socket.setSoTimeout(timeout);
		}
	}

}
