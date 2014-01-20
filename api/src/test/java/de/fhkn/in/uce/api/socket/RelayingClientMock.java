package de.fhkn.in.uce.api.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.relaying.core.IRelayingClient;

public class RelayingClientMock implements IRelayingClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(RelayingClientMock.class);

	private final Object lock;

	private volatile AllocationServerThread allocThread;
	private volatile Socket serverRelSock;
	private volatile boolean isShut;

	public RelayingClientMock() {
		lock = new Object();
		isShut = false;
	}

	@Override
	public InetSocketAddress createAllocation() throws IOException {
		synchronized (lock) {
			if (isShut) {
				throw new IllegalStateException("Was shutdown");
			}
			ServerSocket tServSock = new ServerSocket(0);
			allocThread = new AllocationServerThread(tServSock);
			allocThread.start();
			serverRelSock = new Socket("localhost", tServSock.getLocalPort());
			InetSocketAddress tret = new InetSocketAddress(tServSock.getLocalPort());
			LOGGER.debug("created allocation server:" + tret);
			return tret;
		}
	}

	@Override
	public void discardAllocation() throws IOException {
		synchronized (lock) {
			if (isShut) {
				throw new IllegalStateException("Was shutdown");
			}
			LOGGER.debug("discard relay server socket port: " + serverRelSock.getRemoteSocketAddress());

			// we have to wait for incoming allocations
			(new Timer()).schedule(new TerminateTask(), 30000);

			isShut = true;
		}
	}
	
	private class TerminateTask extends TimerTask {
		
		@Override
		public void run() {
			allocThread.terminate();
		}
	}
	
	
	
	

	@Override
	public Socket accept() throws IOException, InterruptedException {
		synchronized (lock) {
			if (isShut) {
				throw new IllegalStateException("Was shutdown");
			}
			return serverRelSock;
		}
	}

	private static class AllocationServerThread extends Thread {

		private final ServerSocket serverSocket;
		private final Object lock;

		private volatile RelayingThread toRelClient;
		private volatile RelayingThread toRelServer;
		private volatile Socket fromServerSock;
		private volatile Socket fromClientSock;

		private volatile boolean wasInit;

		public AllocationServerThread(ServerSocket serverSocket) {
			this.serverSocket = serverSocket;
			this.lock = new Object();
			this.wasInit = false;
		}

		@Override
		public void run() {
			try {
				try {
					fromServerSock = serverSocket.accept();
				} catch (IOException e) {
					return;
				}
				try {
					fromClientSock = serverSocket.accept();
				} catch (IOException e) {
					try {
						fromServerSock.close();
					} catch (IOException e1) {
					}
					return;
				}
			} finally {
				try {
					serverSocket.close();
				} catch (IOException e) {
				}
			}
			synchronized (lock) {
				toRelClient = new RelayingThread(fromServerSock, fromClientSock, "from server");
				toRelServer = new RelayingThread(fromClientSock, fromServerSock, "from client");
				toRelClient.start();
				toRelServer.start();

				wasInit = true;
			}
		}
		
		

		public void terminate() {
		
			// close server socket to wakeup or do nothing
			LOGGER.debug("terminate server from timer");
			try {
				serverSocket.close();
			} catch (IOException e) {
				LOGGER.info("Relaying Mock error closing server socket", e);
			}
			// wait until this thread is finished
			try {
				this.join();
			} catch (InterruptedException e1) {
			}

			// check if something was init
			synchronized (lock) {
				if (wasInit) {
					toRelClient.terminate();
					toRelServer.terminate();
					try {
						fromServerSock.close();
					} catch (IOException e) {
						LOGGER.info("Relaying Mock fromServerSock close error", e);
					}
					try {
						fromClientSock.close();
					} catch (IOException e) {
						LOGGER.info("Relaying Mock fromClientSock close error", e);
					}
				}
			}

		}
	}

	private static class RelayingThread extends Thread {

		private final Socket inSock;
		private final Socket outSock;
		private final String name;

		public RelayingThread(Socket in, Socket out, String name) {
			this.inSock = in;
			this.outSock = out;
			this.name = name;
		}

		@Override
		public void run() {
			OutputStream out;
			InputStream in;

			try {
				out = outSock.getOutputStream();
				in = inSock.getInputStream();
			} catch (IOException e) {
				LOGGER.info("Relaying Mock error getting input or output stream", e);
				return;
			}

			byte[] buffer = new byte[10000];

			try {
				while (this.isInterrupted() == false) {
					int tRead = in.read(buffer);
					LOGGER.info("Relaying Mock server relaying thread read " + tRead + " " + name);
					if (tRead == -1) {
						break;
					}
					out.write(buffer, 0, tRead);
				}
				inSock.shutdownInput();
				outSock.shutdownOutput();
				LOGGER.info("Relaying Mock server relaying thread end " + name);
			} catch (IOException e) {
				LOGGER.info("Relaying Mock relaying error", e);
			}
		}

		public void terminate() {
			// this.interrupt();
			/*
			 * try { inSock.close(); } catch (IOException e) {
			 * LOGGER.info("Relaying Mock close error", e); } try {
			 * outSock.close(); } catch (IOException e) {
			 * LOGGER.info("Relaying Mock close error", e); }
			 */
			try {
				this.join();
			} catch (InterruptedException e) {
			}

		}

	}

}
