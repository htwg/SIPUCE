/*
 * Copyright (c) 2012 Alexander Diener,
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.fhkn.in.uce.turn.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ServerSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.core.socketlistener.SocketListener;
import de.fhkn.in.uce.core.socketlistener.SocketTaskFactory;
import de.fhkn.in.uce.turn.server.connectionhandling.HandleMessageTaskFactory;
import de.fhkn.in.uce.turn.server.connectionhandling.MessageDispatcherTaskFactory;

/**
 * A Server that implements TURN-like behavior, to relay TCP data. But it is NOT
 * conform to the TURN standard (RFC 5766).
 * 
 * Hosts that want to allocate relay mappings on the server are called clients
 * and hosts that want to communicate with the clients through the relay server
 * are called peers.
 * 
 * A {@link RelayServer} maintains both control and data connections with its
 * clients over TCP. Control connections are responsible for allocation of relay
 * endpoints and for sending refresh messages. Data Connections are responsible
 * for sending connection bind requests and for the real relaying stuff.
 * 
 * @author Daniel Maier
 * 
 */
public final class RelayServer {
	
	private static final int MAX_ARGS = 4;
	
    private static final Logger LOGGER = LoggerFactory.getLogger(RelayServer.class);
    
    private final Thread relaySocketListener;
    
    private final InetSocketAddress primaryAddress;
    private final InetSocketAddress secondaryAddress;
    private final SocketTaskFactory handleMessageTaskFactory;
    private final ExecutorService handleExecutor;
    private final ExecutorService socketListenerExecutor;

    /**
     * Creates a {@link RelayServer}. Has to be started via
     * {@link RelayServer#start() start()} in order that it is able to handle
     * incoming connections.
     * 
     * @param port
     *            the port on which the {@link RelayServer} listens for incoming
     *            control connections from clients
     * @throws IOException
     *             if an I/O error occurs
     */
    public RelayServer(int relayPort, final InetSocketAddress primaryAddress, final InetSocketAddress secondaryAddress) throws IOException {
    	
    	this.primaryAddress = primaryAddress;
        this.secondaryAddress = secondaryAddress;
        this.handleMessageTaskFactory = new HandleMessageTaskFactory(this.primaryAddress, this.secondaryAddress);
        this.handleExecutor = Executors.newCachedThreadPool();
        this.socketListenerExecutor = Executors.newCachedThreadPool();
    	
        Map<UUID, BlockingQueue<Socket>> connIDToQueue = new ConcurrentHashMap<UUID, BlockingQueue<Socket>>();
        Executor controlConnectionHandlerExecutor = Executors.newCachedThreadPool();
        Executor relayExecutor = Executors.newCachedThreadPool();
        this.relaySocketListener = new SocketListener(relayPort, ServerSocketFactory.getDefault(), Executors.newCachedThreadPool(),
                new MessageDispatcherTaskFactory(connIDToQueue, controlConnectionHandlerExecutor, relayExecutor));
    }

    /**
     * Starts this {@link RelayServer}. Can be started only once.
     * @throws IOException 
     */
    public void start() throws IOException {
    	
        this.relaySocketListener.start();
        
        for (SocketListener socketListener : this.getListWithSocketListeners()) {
            this.socketListenerExecutor.execute(socketListener);
        }
    }
    
    private List<SocketListener> getListWithSocketListeners() throws IOException {
        final List<SocketListener> result = new ArrayList<SocketListener>();
        result.add(this.createStunSocketListener(this.primaryAddress));
        result.add(this.createStunSocketListener(new InetSocketAddress(this.primaryAddress.getAddress(),this.secondaryAddress.getPort())));
        result.add(this.createStunSocketListener(new InetSocketAddress(this.secondaryAddress.getAddress(),this.primaryAddress.getPort())));
        result.add(this.createStunSocketListener(this.secondaryAddress));
        return result;
    }
    
    private SocketListener createStunSocketListener(final InetSocketAddress listenerAddress) throws IOException {
        final ServerSocket serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(listenerAddress);
        return new SocketListener(serverSocket, this.handleExecutor, this.handleMessageTaskFactory);
    }

    /**
     * Stops this {@link RelayServer}.
     */
    public void stop() {
        this.relaySocketListener.interrupt();
    }

    /**
     * Creates and starts a new {@link RelayServer} instance.
     * 
     * @param args
     *            arguments for the {@link RelayServer}. An array with length of
     *            one is expected. It should contain the following value:
     *            args[0] the port on which the {@link RelayServer} listens for
     *            incoming control connections from clients via TCP. If nothing
     *            is defined, port 10300 is chosen as default.
     * @throws IOException
     *             if an I/O error occurs
     * @throws IllegalArgumentException
     *             if args[0] is set and it is not an integer value
     */
    public static void main(String[] args) throws IOException {
       
    	if (args.length != MAX_ARGS) {
            throw new IllegalArgumentException("Arguments: relayPort stunPort primaryStunIP secondaryStunIP");
        }
        
    	int i = 0;
    	
        int relayPort = Integer.parseInt(args[i++]);
        int stunPort = Integer.parseInt(args[i++]);
        
        final String primaryIp = args[i++];
        final String secondaryIp = args[i++
                                        ];
        final int primaryPort = stunPort;
        final int secondaryPort = stunPort + 1;
        
        final InetSocketAddress primaryAddress = new InetSocketAddress(primaryIp, primaryPort);
        final InetSocketAddress secondaryAddress = new InetSocketAddress(secondaryIp, secondaryPort);

        RelayServer relayServer = new RelayServer(relayPort, primaryAddress, secondaryAddress);
        LOGGER.info("Relay-Server is running on ports " + relayPort + "/" + stunPort + " with primaryIP " + primaryIp + " and secondaryIP " + secondaryIp);
        relayServer.start();
    }
}
