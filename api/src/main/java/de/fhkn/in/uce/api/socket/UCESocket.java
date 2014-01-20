package de.fhkn.in.uce.api.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.sip.ucesip.client.IUCESipUserAgentClient;
import de.fhkn.in.uce.sip.ucesip.client.UCESipUserAgentClient;
import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;

/**
 * UCE Client Socket. The socket establishes a new connection to a UCE Server
 * Socket via SIP. At first the data is relayed by an external relay server,
 * while a direct connection is tried to be established with a holepuncher. If
 * the second connection establishment is successful, the socket for the data
 * connection is switched.
 */
public final class UCESocket extends Socket {

	private static final Logger LOGGER = LoggerFactory.getLogger(UCESocket.class);

	private final Socket socket;
	private final IClientConnectionHandlerStrategy connStrategy;
	private final IUCESipUserAgentClient sipUAC;
	private final ConnectionUserData connectionUserData;

	/**
	 * Creates a new UCE Client Socket with a randomly generated SIP user and
	 * tries to connect to the specified UCE Server Socket using the toUser and
	 * toDomain parameters.
	 * 
	 * @param fromDomain
	 *            The SIP User Domain which is used for this UCE Client Socket.
	 * @param toUser
	 *            The SIP User Name of the UCE Server Socket to which the
	 *            connection shall connect.
	 * @param toDomain
	 *            The SIP User Domain of the UCE Server Socket to which the
	 *            connection shall connect.
	 * @param settings
	 *            The connection settings for the SIP and STUN Server.
	 * @throws UCEException
	 *             If cannot establish the connection.
	 */
	public UCESocket(final String fromDomain, final String toUser, final String toDomain, final UCEClientSettings settings) throws UCEException {

		this(new ConnectionUserData(UUID.randomUUID().toString(), fromDomain, toUser, toDomain), settings, new InstantConnectionStrategyFactory(),
				new TimeoutSocketSwitchFactory(), new StunEndpointFactory());
	}

	/**
	 * Creates a new UCE Client Socket with a specified SIP user and tries to
	 * connect to the specified UCE Server Socket using the toUser and toDomain
	 * parameters.
	 * 
	 * @param fromUser
	 *            The SIP User Name which is used for this UCE Client Socket.
	 * @param fromDomain
	 *            The SIP User Domain which is used for this UCE Client Socket.
	 * @param toUser
	 *            The SIP User Name of the UCE Server Socket to which the
	 *            connection shall connect.
	 * @param toDomain
	 *            The SIP User Domain of the UCE Server Socket to which the
	 *            connection shall connect.
	 * @param settings
	 *            The connection settings for the SIP and STUN Server.
	 * @throws UCEException
	 *             If cannot establish the connection.
	 */
	public UCESocket(final String fromUser, final String fromDomain, final String toUser, final String toDomain, final UCEClientSettings settings)
			throws UCEException {

		this(new ConnectionUserData(fromUser, fromDomain, toUser, toDomain), settings, new InstantConnectionStrategyFactory(),
				new HalfCloseSocketSwitchFactory(), new StunEndpointFactory());
	}

	/**
	 * Package private, to library users this should not be visible.
	 */
	UCESocket(final ConnectionUserData userData, final UCEClientSettings settings, IConnectionStrategyFactory strategyFactory,
			ISocketSwitchFactory socketSwitchFactory, IStunEndpointFactory stunEndpointFactory) throws UCEException {

		super();

		connectionUserData = userData;

		try {
			sipUAC = new UCESipUserAgentClient(connectionUserData.getFromUser(), connectionUserData.getFromUserDomain(),
					connectionUserData.getToUser(), connectionUserData.getToUserDomain(), settings.getSipSettings());
		} catch (UCESipException e) {
			throw new UCEException("Unable to create create the sip user agent. Perhaps the sip server is not reachable.", e);
		}

		connStrategy = strategyFactory.createClientStrategy(sipUAC, settings, socketSwitchFactory, stunEndpointFactory);

		try {
			socket = connStrategy.clientConnect();
		} catch (Exception e) {
			// shutdown sipuac on error
			shutdownSipUAC();
			throw new UCEException("Unable to connect to the UCE Server Socket", e);
		}
	}

	private void shutdownSipUAC() {
		try {
			sipUAC.shutdown();
		} catch (UCESipException e) {
			LOGGER.info("Unable to shutdown UAS", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#close()
	 */
	@Override
	public synchronized void close() throws IOException {
		connStrategy.clientClose();
		shutdownSipUAC();
		this.socket.close();
	}

	/**
	 * The connection is established in the constructor. So the method throws
	 * always a SocketException.
	 * 
	 * @throws SocketException
	 *             Already connected.
	 */
	@Override
	public void connect(final SocketAddress arg0, final int arg1) throws IOException {
		throw new SocketException("Already connected");
	}

	/**
	 * Local binding to a socket is not supported by UCE because UCE hides the
	 * underlying Socket connections.
	 * 
	 * @throws UnsupportedOperationException
	 *             Not supported in UCE context.
	 */
	@Override
	public void bind(final SocketAddress bindpoint) throws IOException {
		throw new UnsupportedOperationException("Not supported in UCE context");
	}

	/**
	 * Could mislead, because UCE Socket has many Sockets open (SIP, STUN, ...).
	 * To notify user returns 0.0.0.0.
	 * 
	 */
	@Override
	public InetAddress getLocalAddress() {
		try {
			return InetAddress.getByAddress(new byte[] { 0, 0, 0, 0 });
		} catch (UnknownHostException e) {
			assert (false);
			// we should never get here.
		}
		return null;
	}

	/**
	 * Could mislead, because UCE Socket has many sockets open (SIP, STUN, ...).
	 * To notify user return 0.0.0.0 and port 0.
	 * 
	 */
	@Override
	public SocketAddress getLocalSocketAddress() {
		return new InetSocketAddress(getLocalAddress(), getLocalPort());
	}

	/**
	 * setReuseAddress() is not supported in the uce socket, because uce uses
	 * several socket connections.
	 * 
	 */
	@Override
	public void setReuseAddress(final boolean on) throws SocketException {
		throw new UnsupportedOperationException("Not supported in UCE context");
	}

	/**
	 * setReuseAddress() is not supported. Return false and behave like a usual
	 * server socket.
	 * 
	 */
	@Override
	public boolean getReuseAddress() throws SocketException {
		if (this.socket.isClosed()) {
			throw new SocketException("Socket is closed");
		}
		return false;
	}

	/**
	 * Could mislead, because UCE Socket has many sockets open (SIP, STUN, ...).
	 * To notify user return 0.0.0.0.
	 * 
	 */
	@Override
	public InetAddress getInetAddress() {
		try {
			return InetAddress.getByAddress(new byte[] { 0, 0, 0, 0 });
		} catch (UnknownHostException e) {
			assert (false);
			// we should never get here.
		}
		return null;
	}

	/**
	 * Could mislead, because UCE Socket has many sockets open (SIP, STUN, ...).
	 * To notify user return 0.0.0.0:0.
	 * 
	 */
	@Override
	public SocketAddress getRemoteSocketAddress() {
		return new InetSocketAddress(getInetAddress(), getPort());
	}

	/**
	 * Could mislead, because UCE Socket has many ports open (SIP, STUN, ...).
	 * To notify user return 0.
	 * 
	 */
	@Override
	public int getLocalPort() {
		return 0;
	}

	/**
	 * Could mislead, because UCE Socket has many ports open (SIP, STUN, ...).
	 * To notify user return 0.
	 * 
	 */
	@Override
	public int getPort() {
		return 0;
	}

	/**
	 * Always returns true.
	 */
	@Override
	public boolean isBound() {
		return true;
	}

	/**
	 * Returns a string with the from user and to user SIP Adresses.
	 */
	@Override
	public String toString() {
		return "UCESocket [ " + connectionUserData.toString() + " ]";
	}

	/**
	 * Socket channels are not supported in the uce socket.
	 */
	@Override
	public SocketChannel getChannel() {
		throw new UnsupportedOperationException("Not supported in UCE context");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getInputStream()
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		return this.socket.getInputStream();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getKeepAlive()
	 */
	@Override
	public boolean getKeepAlive() throws SocketException {
		return this.socket.getKeepAlive();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getOOBInline()
	 */
	@Override
	public boolean getOOBInline() throws SocketException {
		return this.socket.getOOBInline();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getOutputStream()
	 */
	@Override
	public OutputStream getOutputStream() throws IOException {
		return this.socket.getOutputStream();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getReceiveBufferSize()
	 */
	@Override
	public synchronized int getReceiveBufferSize() throws SocketException {
		return this.socket.getReceiveBufferSize();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getSendBufferSize()
	 */
	@Override
	public synchronized int getSendBufferSize() throws SocketException {
		return this.socket.getSendBufferSize();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getSoLinger()
	 */
	@Override
	public int getSoLinger() throws SocketException {
		return this.socket.getSoLinger();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getSoTimeout()
	 */
	@Override
	public synchronized int getSoTimeout() throws SocketException {
		return this.socket.getSoTimeout();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getTcpNoDelay()
	 */
	@Override
	public boolean getTcpNoDelay() throws SocketException {
		return this.socket.getTcpNoDelay();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#getTrafficClass()
	 */
	@Override
	public int getTrafficClass() throws SocketException {
		return this.socket.getTrafficClass();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#isClosed()
	 */
	@Override
	public synchronized boolean isClosed() {
		return this.socket.isClosed();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#isConnected()
	 */
	@Override
	public boolean isConnected() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#isInputShutdown()
	 */
	@Override
	public boolean isInputShutdown() {
		return this.socket.isInputShutdown();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#isOutputShutdown()
	 */
	@Override
	public boolean isOutputShutdown() {
		return this.socket.isOutputShutdown();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#sendUrgentData(int)
	 */
	@Override
	public void sendUrgentData(final int data) throws IOException {
		this.socket.sendUrgentData(data);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#setKeepAlive(boolean)
	 */
	@Override
	public void setKeepAlive(final boolean on) throws SocketException {
		this.socket.setKeepAlive(on);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#setOOBInline(boolean)
	 */
	@Override
	public void setOOBInline(final boolean on) throws SocketException {
		this.socket.setOOBInline(on);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#setPerformancePreferences(int, int, int)
	 */
	@Override
	public void setPerformancePreferences(final int connectionTime, final int latency, final int bandwidth) {
		this.socket.setPerformancePreferences(connectionTime, latency, bandwidth);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#setReceiveBufferSize(int)
	 */
	@Override
	public synchronized void setReceiveBufferSize(final int size) throws SocketException {
		this.socket.setReceiveBufferSize(size);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#setSendBufferSize(int)
	 */
	@Override
	public synchronized void setSendBufferSize(final int size) throws SocketException {
		this.socket.setSendBufferSize(size);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#setSoLinger(boolean, int)
	 */
	@Override
	public void setSoLinger(final boolean on, final int linger) throws SocketException {
		this.socket.setSoLinger(on, linger);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#setSoTimeout(int)
	 */
	@Override
	public synchronized void setSoTimeout(final int timeout) throws SocketException {
		this.socket.setSoTimeout(timeout);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#setTcpNoDelay(boolean)
	 */
	@Override
	public void setTcpNoDelay(final boolean on) throws SocketException {
		this.socket.setTcpNoDelay(on);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#setTrafficClass(int)
	 */
	@Override
	public void setTrafficClass(final int tc) throws SocketException {
		this.socket.setTrafficClass(tc);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#shutdownInput()
	 */
	@Override
	public void shutdownInput() throws IOException {
		this.socket.shutdownInput();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.Socket#shutdownOutput()
	 */
	@Override
	public void shutdownOutput() throws IOException {
		this.socket.shutdownOutput();
	}
}