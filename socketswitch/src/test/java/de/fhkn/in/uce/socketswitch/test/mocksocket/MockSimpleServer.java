package de.fhkn.in.uce.socketswitch.test.mocksocket;

import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.fhkn.in.uce.socketswitch.simpleserver.SwitchableSocketConnectionHandler;

public class MockSimpleServer {
	private ExecutorService pool;
	private static final int THREADPOOLSIZE = 50;
	
	public MockSimpleServer() {
		pool = Executors.newFixedThreadPool(THREADPOOLSIZE);
	}
	
	public void startConnectionHandler(Socket cs) {
		pool.execute(new SwitchableSocketConnectionHandler(cs));
	}
}
