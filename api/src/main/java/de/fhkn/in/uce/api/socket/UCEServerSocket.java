package de.fhkn.in.uce.api.socket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.sip.ucesip.server.IUCESipUserAgentServer;
import de.fhkn.in.uce.sip.ucesip.server.UCESipUserAgentServer;

public class UCEServerSocket extends ServerSocket {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClientConnectionHandlerStrategy.class);

	private final IServerConnectionHandlerStrategy connStrategy;
	private final IUCESipUserAgentServer sipUAS;

	private volatile boolean closed = false;
	private volatile int acceptTimeoutMillis = 0;

	/**
	 * Creates a new UCE Server Socket to which UCE Client Sockets can connect
	 * to. After creating the UCE Server Socket is in listening mode and UCE
	 * Client Sockets may connect.
	 * 
	 * @param serverUser
	 *            The SIP User of the UCE Server Socket.
	 * @param serverDomain
	 *            The SIP User domain of the UCE Server Socket.
	 * @param settings
	 *            The connection settings for the SIP, STUN and Relay Server.
	 * @throws IOException
	 *             If the UCE Server Socket could not be created.
	 */
	public UCEServerSocket(final String serverUser, final String serverDomain, final UCEServerSettings settings) throws IOException {
		this(serverUser, serverDomain, settings, new InstantConnectionStrategyFactory(), new TimeoutSocketSwitchFactory(),
				new RelayingClientFactory(), new StunEndpointFactory());
	}

	/*
	 * Package private, to library users this should not be visible.
	 */
	UCEServerSocket(final String serverUser, final String serverDomain, final UCEServerSettings settings, IConnectionStrategyFactory strategyFactory,
			ISocketSwitchFactory socketSwitchFactory, IRelayingClientFactory relayingClientFactory, IStunEndpointFactory stunEndpointFactory)
			throws IOException {

		super();

		try {
			sipUAS = new UCESipUserAgentServer(serverUser, serverDomain, settings.getSipSettings());
			sipUAS.register();
		} catch (Exception e) {
			// Must throw IOException, because the super class constructor
			// throws one.
			throw new IOException("Unable to create create the sip user agent. Perhaps the sip server is not reachable.", e);
		}

		connStrategy = strategyFactory.createServerStrategy(sipUAS, settings, socketSwitchFactory, relayingClientFactory, stunEndpointFactory);

		try {
			connStrategy.serverListen();
		} catch (UCEException e) {
			connStrategy.serverClose();
			closeSipUAS();
			// Must throw IOException, because the super class constructor
			// throws one.
			throw new IOException("Error on listening for new UCE Client Socket connections.", e);
		}
	}

	/**
	 * Returns a new connection which was requested to this server socket. The
	 * relay connection to the client is offered and the holepuncher starts
	 * trying. On success of the holepuncher the returned socket switches to the
	 * holepuncher connection.
	 * 
	 * @exception IOException
	 *                if an I/O error occurs when waiting for a connection.
	 * @exception SocketTimeoutException
	 *                if a timeout was previously set with setSoTimeout and the
	 *                timeout has been reached.
	 * 
	 * @return the new Socket
	 */
	@Override
	public Socket accept() throws IOException {
		// not synchronized because accept and close must be parallely callable.
		if (isClosed()) {
			throw new SocketException("Socket is closed");
		}
		try {
			return connStrategy.serverAccept(acceptTimeoutMillis);
		} catch (UCEException e) {
			throw new IOException("Error on accepting UCE Socket", e);
		} catch (TimeoutException e) {
			throw new SocketTimeoutException("Accept timed out");
		}
	}

	/**
	 * Closes this socket. Tries to release all allocated resources.
	 * 
	 * Any thread currently blocked in {@link #accept()} will throw a
	 * {@link IOException}.
	 * 
	 * @exception IOException
	 *                is not thrown.
	 */
	@Override
	public synchronized void close() throws IOException {
		if (isClosed()) {
			return;
		}
		connStrategy.serverClose();
		closeSipUAS();
		closed = true;
	}
	
	private void closeSipUAS() {
		try {
			sipUAS.shutdown();
		} catch (Exception e) {
			LOGGER.info("Error closing SipUAS", e);
		}
	}

	/**
	 * The bind function is not supported, because UCE uses many underlying
	 * connections and sockets.
	 * 
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void bind(final SocketAddress endpoint, final int backlog) throws IOException {
		throw new UnsupportedOperationException("Not supported in UCE context");
	}

	/**
	 * 
	 * Could mislead, because UCE Socket has many sockets open (SIP, STUN, ...).
	 * To notify user return 0.0.0.0.
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
	 * Could mislead, because UCE Socket has many ports open (SIP, STUN, ...).
	 * To notify user return 0.
	 */
	@Override
	public int getLocalPort() {
		return 0;
	}

	/**
	 * Could mislead, because UCE Socket has many sockets open (SIP, STUN, ...).
	 * To notify user return 0.0.0.0 and port 0.
	 */
	@Override
	public SocketAddress getLocalSocketAddress() {
		return new InetSocketAddress(getInetAddress(), getLocalPort());
	}

	/**
	 * Here bound means, that the socket is in listening mode. If the socket is
	 * not closed true is returned, because after instantiation the UCE Server
	 * Socket is listening.
	 */
	@Override
	public boolean isBound() {
		if (isClosed()) {
			return false;
		}
		return true;
	}

	/**
	 * Returns the closed state of the ServerSocket.
	 * 
	 * @return true if the socket has been closed
	 */
	@Override
	public synchronized boolean isClosed() {
		return closed;
	}

	/**
	 * Sets the timeout for the accept() call. With this option set to a
	 * non-zero timeout, a call to accept() for this ServerSocket will block for
	 * only this amount of time. If the timeout expires, a
	 * <B>java.net.SocketTimeoutException</B> is raised, though the UCE Server
	 * Socket is still valid. The option <B>must</B> be enabled prior to
	 * entering the blocking operation to have effect. The timeout must be > 0.
	 * A timeout of zero is interpreted as an infinite timeout.
	 * 
	 * @param timeout
	 *            the specified timeout, in milliseconds
	 * @exception SocketException
	 *                if the socket is closed
	 * @see #getSoTimeout()
	 */
	@Override
	public void setSoTimeout(final int timeout) throws SocketException {
		if (isClosed()) {
			throw new SocketException("Socket is closed");
		}
		acceptTimeoutMillis = timeout;
	}

	/**
	 * Retrieve setting for the accept() timeout. 0 returns implies that the
	 * option is disabled (i.e., timeout of infinity).
	 * 
	 * @return the accept() timeout value
	 * @exception SocketException
	 *                if the socket is closed
	 * @see #setSoTimeout(int)
	 */
	@Override
	public int getSoTimeout() throws IOException {
		if (isClosed()) {
			throw new SocketException("Socket is closed");
		}
		return acceptTimeoutMillis;
	}

	/**
	 * setReuseAddress() is not supported in the uce socket, because uce uses
	 * several socket connections.
	 */
	@Override
	public void setReuseAddress(final boolean on) throws SocketException {
		throw new UnsupportedOperationException("Not supported in UCE context");
	}

	/**
	 * setReuseAddress() is not supported. Return false and behave like a usual
	 * server socket.
	 */
	@Override
	public boolean getReuseAddress() throws SocketException {
		throw new UnsupportedOperationException("Not supported in UCE context");
	}

	/**
	 * This option is currently not supported.
	 */
	@Override
	public void setReceiveBufferSize(final int size) throws SocketException {
		throw new UnsupportedOperationException("Not supported in UCE context");
	}

	/**
	 * This option is currently not supported.
	 */
	@Override
	public int getReceiveBufferSize() throws SocketException {
		throw new UnsupportedOperationException("Not supported in UCE context");
	}

	/**
	 * This option is currently not supported.
	 */
	@Override
	public ServerSocketChannel getChannel() {
		throw new UnsupportedOperationException("Not supported in UCE context");
	}

	/**
	 * This option is currently not supported.
	 */
	@Override
	public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
		throw new UnsupportedOperationException("Not supported in UCE context");
	}
}
