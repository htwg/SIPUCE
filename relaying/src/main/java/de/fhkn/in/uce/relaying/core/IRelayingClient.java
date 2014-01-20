package de.fhkn.in.uce.relaying.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;


public interface IRelayingClient {

	/**
	 * Creates a new allocation on the relay server for this relay client. Also
	 * initiates the periodic refresh requests for the allocation. You can only
	 * create one allocation with a single {@link RelayingClient} instance.
	 * 
	 * @return the public endpoint of the allocation on the relay server
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws IllegalStateException
	 *             if you try to create an allocation after you already created
	 *             an allocation with this {@link RelayingClient} instance
	 *             successfully
	 */
	InetSocketAddress createAllocation() throws IOException;

	/**
	 * Discards the allocation by this client on the relay server. Also
	 * terminates the periodic refresh requests for the allocation.
	 * 
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws IllegalStateException
	 *             if the allocation of this client is already discarded, or if
	 *             no allocation is created before
	 */
	void discardAllocation() throws IOException;

	/**
	 * Returns a socket to the relay server to relay data between this client
	 * and a peer. This method blocks until a new socket is available or the
	 * thread gets interrupted while waiting.
	 * 
	 * @return a socket to the relay server
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws InterruptedException
	 *             if interrupted while waiting
	 * @throws IllegalStateException
	 *             if no allocation is created before or the allocation is
	 *             discarded
	 */
	Socket accept() throws IOException, InterruptedException;

}