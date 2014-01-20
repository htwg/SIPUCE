package de.fhkn.in.uce.socketswitch.demo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.TimeoutException;

import de.fhkn.in.uce.socketswitch.SwitchableException;
import de.fhkn.in.uce.socketswitch.SwitchableSocket;
import de.fhkn.in.uce.socketswitch.TimeoutSwitchableSocket;

public final class Controller extends Observable {

	private SwitchableSocket switchableSocket;
	private InputStream inputStream;
	private OutputStream outputStream;
	private ServerSocket serverSocket;
	private List<Socket> openedConnections;
	private volatile boolean stopAccepting;

	public static final int SERVERPORT = 3335;
	public static final long DEFAULTCONNECTIONID = 124234L;
	
	private long connectionId;

	public Controller() {
		connectionId = DEFAULTCONNECTIONID;
		openedConnections = new ArrayList<Socket>();

	}

	public void createTimeoutSwitchableSocket(String host, int port) throws IOException {
		synchronized (this) {
			if ((switchableSocket != null)) {
				throw new IllegalStateException("Switchable socket was created before.");
			}
			
			switchableSocket = new TimeoutSwitchableSocket(createSocket(host, port));
			inputStream = switchableSocket.getInputStream();
			outputStream = switchableSocket.getOutputStream();
		}
	}

	public void createTimeoutSwitchableSocket(int index) throws IOException {
		synchronized (this) {
			if ((switchableSocket != null)) {
				throw new IllegalStateException("Switchable socket was created before.");
			}
			
			switchableSocket = new TimeoutSwitchableSocket(openedConnections.get(index));
			openedConnections.remove(index);
			inputStream = switchableSocket.getInputStream();
			outputStream = switchableSocket.getOutputStream();
		}
	}

	public void setConnectionId(long connectionId) {
		synchronized (this) {
			this.connectionId = connectionId;
		}
	}

	public long getConnectionId() {
		return connectionId;
	}

	public String readString(int count) throws IOException {
		byte[] tBuf = new byte[count];
		
		if (inputStream.read(tBuf) >= 0) {
			return new String(tBuf, "UTF-8");
		} else {
			throw new IOException("Reached end of file while reading.");
		}
		
	}

	public void writeString(String str) throws IOException {
		outputStream.write(str.getBytes("UTF-8"));
	}

	public void startAcceptConnections() throws IOException{

		synchronized (this) {
			if (serverSocket != null) {
				throw new IllegalStateException("Server socket was created before.");
			}

			serverSocket = new ServerSocket(SERVERPORT);
		}

		stopAccepting = false;

		Thread acceptThread = new Thread() {
			@Override
			public void run() {
				try {
					while (stopAccepting == false) {
						Socket tSock = serverSocket.accept();
						synchronized (this) {
							openedConnections.add(tSock);
						}
						setChanged();
						notifyObservers();
					}
				} catch (IOException e) {
					e.getMessage();
				}
			}
		};
		
		acceptThread.start();
		
	}

	public String getOpenedConnectionsList() {
		synchronized (this) {
			StringBuilder tStrb = new StringBuilder();
			int index = 0;

			for (Socket i : openedConnections) {
				tStrb.append("index     to             port%n");
				tStrb.append(String.format("%-2d        %-14s %-10d%n", index, i.getInetAddress().getHostName(),
						i.getPort()));
			}
			return tStrb.toString();
		}
	}

	public void openAndSwitchSocket(String host, int port) throws SwitchableException, IOException, TimeoutException, InterruptedException  {
		synchronized (this) {
			if (switchableSocket == null) {
				throw new IllegalStateException("No switchable socket was created before.");
			}
			
			switchableSocket.switchSocket(createSocket(host, port), 0);
		}
	}
	
	public void switchSocket(int index) throws SwitchableException, TimeoutException, InterruptedException {
		synchronized (this) {
			if (switchableSocket == null) {
				throw new IllegalStateException("No switchable socket was created before.");
			}
			
			switchableSocket.switchSocket(openedConnections.get(index), 0);
			openedConnections.remove(index);
		}
	}
	
	public void openNewConnection(String host, int port) throws IOException {
		synchronized (this) {
			openedConnections.add(createSocket(host, port));
		}
		setChanged();
		notifyObservers();
	}
	
	public void shutdown() throws IOException {
		stopAccepting = true;
		for (Socket i : openedConnections) {
			i.close();
		}
		if (switchableSocket != null) {
			switchableSocket.close();
		}
		if (serverSocket != null) {
			serverSocket.close();
			serverSocket = null;
		}
	}
	
	private Socket createSocket(String host, int port) throws IOException {
		if (host.equals(".")) {
			return new Socket("localhost", port);
		} else {
			return new Socket(host, port);
		}
	}

}
