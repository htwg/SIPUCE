package de.fhkn.in.uce.api.socket.remotetest;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.fhkn.in.uce.api.socket.UCEServerSocket;
import de.fhkn.in.uce.api.socket.UCETestSettings;

public class UCERemoteSystemTestServer {

	private static String SERVER_USER = "janosch1";
	private static String SERVER_DOMAIN = "213.239.218.18";

	private volatile boolean shutdown;
	private final Thread t;

	public UCERemoteSystemTestServer(int threadPoolsize) throws IOException {
		shutdown = false;
		final ExecutorService pool = Executors.newFixedThreadPool(threadPoolsize);

		t = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					handleServer(pool);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		t.start();
	}

	private void handleServer(ExecutorService pool) throws IOException {
		UCEServerSocket serverSocket = new UCEServerSocket(SERVER_USER, SERVER_DOMAIN, UCETestSettings.SERVER_SETTINGS_OPENSIPS);

		while (shutdown == false) {
			Socket cs = serverSocket.accept();
			// starts the client handler.
			pool.execute(new ConnectionHandler(cs));
		}

		serverSocket.close();
	}

	public void shutdown() throws InterruptedException {
		shutdown = true;
		t.interrupt();
		t.join();
	}

	public static void main(String[] args) throws IOException {
		new UCERemoteSystemTestServer(10);
	}

}
