package de.fhkn.in.uce.socketswitch.simpleserver;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Thread for accepting client connections
class NetworkService implements Runnable {
	private final ServerSocket serverSocket;
	private final ExecutorService pool;
	private static Logger LOGGER = LoggerFactory.getLogger("de.htwg.teamprojekt.socketswitch.simpleserver");

	public NetworkService(ExecutorService pool, ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
		this.pool = pool;
	}

	public void run() {
		try {
			while (true) {
				Socket cs = serverSocket.accept();
				// starts the client handler.
				pool.execute(ConnectionHandlerFactory.Instance.createConnectionHandler(cs));
			}
		} catch (Exception e) {
			LOGGER.error("{}", e.getStackTrace());
		}
	}
}