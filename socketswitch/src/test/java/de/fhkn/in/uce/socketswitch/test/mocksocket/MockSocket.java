package de.fhkn.in.uce.socketswitch.test.mocksocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class MockSocket extends Socket {

	private boolean closed = false;
	private Object closeLock = new Object();
	private boolean shutOut = false;
	private boolean connected = false;

	private final MockInputStream mockInputStream;
	private final MockOutputStream mockOutputStream;

	private final ArrayBlockingQueue<Object> outputQueue;
	private final ArrayBlockingQueue<Object> inputQueue;
	
	private final MockSocket oppositeSocket;
	
	private static final AtomicLong objectIdGenerator = new AtomicLong();
	
	private final long objectId;

	public MockSocket(int queueSize) {
		objectId = objectIdGenerator.incrementAndGet();
		
		outputQueue = new ArrayBlockingQueue<>(queueSize);
		inputQueue = new ArrayBlockingQueue<>(queueSize);
		mockInputStream = new MockInputStream(inputQueue);
		mockOutputStream = new MockOutputStream(outputQueue);
		
		oppositeSocket = new MockSocket(this, inputQueue, outputQueue);
	} 

	private MockSocket(MockSocket oppositeSocket, ArrayBlockingQueue<Object> outputQueue,
			ArrayBlockingQueue<Object> inputQueue) {
		objectId = objectIdGenerator.incrementAndGet();
		
		this.outputQueue = outputQueue;
		this.inputQueue = inputQueue;
		mockInputStream = new MockInputStream(inputQueue);
		mockOutputStream = new MockOutputStream(outputQueue);
		// with the queues we are connected
		connected = true;
		
		this.oppositeSocket = oppositeSocket;
	}

	public Socket getOppsositeSocket() {
		// now the opposite has his socket so we are connected...?
		connected = true;
		return oppositeSocket;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		if (isClosed())
			throw new SocketException("Socket is closed");
		if (!isConnected())
			throw new SocketException("Socket is not connected");
		if (isInputShutdown())
			throw new SocketException("Socket input is shutdown");
		return mockInputStream;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		if (isClosed())
			throw new SocketException("Socket is closed");
		if (!isConnected())
			throw new SocketException("Socket is not connected");
		if (isOutputShutdown())
			throw new SocketException("Socket output is shutdown");
		return mockOutputStream;
	}

	@Override
	public synchronized int getSoTimeout() throws SocketException {
		if (isClosed())
			throw new SocketException("Socket is closed");
		return mockInputStream.getReadTimeout();
	}

	@Override
	public synchronized void setSoTimeout(int timeout) throws SocketException {
		if (isClosed())
			throw new SocketException("Socket is closed");
		if (timeout < 0)
			throw new IllegalArgumentException("timeout can't be negative");
		mockInputStream.setReadTimeout(timeout);
	}

	

	@Override
	public void shutdownOutput() throws IOException {
		if (isClosed())
			throw new SocketException("Socket is closed");
		if (!isConnected())
			throw new SocketException("Socket is not connected");
		if (isOutputShutdown())
			throw new SocketException("Socket output is already shutdown");
		shutOut = true;
		mockOutputStream.shutdownOutput();
	}

	@Override
	public String toString() {
		return "Mock Socket id: " + objectId;
	}

	@Override
	public boolean isOutputShutdown() {
		return shutOut;
	}

	@Override
	public boolean isClosed() {
		synchronized (closeLock) {
			return closed;
		}
	}

	@Override
	public boolean isConnected() {
		return connected;
	}

	@Override
	public synchronized void close() throws IOException {
		synchronized (closeLock) {
			if (isClosed())
				return;
			closed = true;
			mockOutputStream.shutdownOutput();
			//oppositeSocket.closeRequest();
		}
	}
	
	public void closeRequest() {
		closed = true;
	}
	
	@Override
	public boolean isInputShutdown() {
		return mockInputStream.isInputShutdown();
	}
	
	
	
	@Override
	public void shutdownInput() throws IOException {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public void connect(SocketAddress endpoint) throws IOException {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public void connect(SocketAddress endpoint, int timeout) throws IOException {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public void bind(SocketAddress bindpoint) throws IOException {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public InetAddress getInetAddress() {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public InetAddress getLocalAddress() {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public int getPort() {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public int getLocalPort() {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public SocketAddress getRemoteSocketAddress() {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public SocketAddress getLocalSocketAddress() {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public SocketChannel getChannel() {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public void setTcpNoDelay(boolean on) throws SocketException {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public boolean getTcpNoDelay() throws SocketException {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public void setSoLinger(boolean on, int linger) throws SocketException {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public int getSoLinger() throws SocketException {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public void sendUrgentData(int data) throws IOException {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public void setOOBInline(boolean on) throws SocketException {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public boolean getOOBInline() throws SocketException {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public synchronized void setSendBufferSize(int size) throws SocketException {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public synchronized int getSendBufferSize() throws SocketException {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public synchronized void setReceiveBufferSize(int size)
			throws SocketException {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public synchronized int getReceiveBufferSize() throws SocketException {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public void setKeepAlive(boolean on) throws SocketException {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public boolean getKeepAlive() throws SocketException {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public void setTrafficClass(int tc) throws SocketException {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public int getTrafficClass() throws SocketException {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public void setReuseAddress(boolean on) throws SocketException {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public boolean getReuseAddress() throws SocketException {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public boolean isBound() {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

	@Override
	public void setPerformancePreferences(int connectionTime, int latency,
			int bandwidth) {
		throw new RuntimeException(
				"Function in mock implementation not supported");
	}

}
