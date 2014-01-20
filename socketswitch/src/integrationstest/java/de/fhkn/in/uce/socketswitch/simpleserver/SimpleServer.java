package de.fhkn.in.uce.socketswitch.simpleserver;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.io.*;
import java.net.*;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleServer {
	private ExecutorService pool;
	private ServerSocket serverSocket;
	private ServerSocket sslServerSocket;
	private static final int SERVERPORT = 3145;
	private static final int SSLSERVERPORT = 3146;
	private static final int THREADPOOLSIZE = 50;
	private static Logger LOGGER = LoggerFactory.getLogger("SimpleServer");
	private static boolean USESSL = false;
	
	public void startServer(int threadPoolsize, int serverPort,
			int sslServerPort) throws IOException {
		
		if (USESSL) {
			// Keystore muss zuvor erzeugt werden! http://www.tutorials.de/java/267445-ssl-socketverbindung-mit-java.html
			System.setProperty("javax.net.ssl.keyStore", "C:\\Users\\tipartl\\Documents\\SSLKeyStore");
			System.setProperty("javax.net.ssl.keyStorePassword", "asdasd");
			System.setProperty("javax.net.ssl.trustStore", "C:\\Users\\tipartl\\Documents\\SSLKeyStore");
			System.setProperty("javax.net.ssl.trustStorePassword", "asdasd");
		}

		pool = Executors.newFixedThreadPool(threadPoolsize);

		serverSocket = new ServerSocket(serverPort);

		sslServerSocket = (SSLServerSocket) SSLServerSocketFactory.getDefault()
				.createServerSocket(SSLSERVERPORT);

		// Starting thread for client communication handling.
		Thread t1 = new Thread(new NetworkService(pool, serverSocket));
		Thread t2 = new Thread(new NetworkService(pool, sslServerSocket));

		LOGGER.info(String.format("Starting switchable socket test server, poolsize=%d, port=%d, ssl-port=%d",
				threadPoolsize, serverPort, sslServerPort));
	
		t1.start();
		t2.start();
	}

	public void shutdownServer() {
		LOGGER.info("Shutdown server");
		pool.shutdown();
		try {
			LOGGER.info("ServerSocket close");
			serverSocket.close();
			sslServerSocket.close();
			// wait 4 seconds before closing the server
			LOGGER.info("Wait maximum 4 seconds until all threads are closed.");
			pool.awaitTermination(4L, TimeUnit.SECONDS);
			LOGGER.info("Server ended.");
		} catch (Exception e) {
			LOGGER.error("{}", e.getStackTrace());
		}
	}

	public static void main(String[] args) throws IOException {
		ConnectionHandlerFactory.Instance.setMakeSwitchable(true);

		final SimpleServer tServ = new SimpleServer();
		tServ.startServer(THREADPOOLSIZE, SERVERPORT, SSLSERVERPORT);

		// hook for control + c
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				tServ.shutdownServer();
			}
		});
	}

}
