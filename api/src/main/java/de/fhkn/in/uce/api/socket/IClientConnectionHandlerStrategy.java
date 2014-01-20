package de.fhkn.in.uce.api.socket;

import java.net.Socket;

/**
 * Interface for the UCE Client Socket strategies how to connect and close a
 * connection to a UCE Server Socket.
 * 
 */
interface IClientConnectionHandlerStrategy {
	Socket clientConnect() throws UCEException;

	/**
	 * Close should not throw any Exceptions. Only IllegalStateException
	 * (Runtime Exception e.g.). Because we can only try to release the
	 * allocations we had made. Errors should be logged by the logger.
	 */
	void clientClose();

}
