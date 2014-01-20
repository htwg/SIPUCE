package de.fhkn.in.uce.socketswitch.simpleserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Handles the client connection
class SocketConnectionHandler implements Runnable {
	private final Socket client;
	private static Logger LOGGER = LoggerFactory
			.getLogger("de.htwg.teamprojekt.socketswitch.simpleserver");

	SocketConnectionHandler(Socket client) {
		this.client = client;
	}

	public void run() {
		try {
			// read and service request on client
			LOGGER.info("running service, " + Thread.currentThread());
			OutputStream out = client.getOutputStream();
			InputStream in = client.getInputStream();

			ObjectInputStream objIn = new ObjectInputStream(in);

			while (true) {
				ConnectionPurposeMessage conPurpose = (ConnectionPurposeMessage) objIn
						.readObject();

				// read the type of the expected function of the client
				switch (conPurpose.getExecuteServerFunction()) {
				case SendFiveBytesEverySecond:
					ConnectionTests.sendFiveBytesEverySecond(out, conPurpose.getParameter());
					break;
				case SendFiveBytesEveryTenSeconds:
					ConnectionTests.sendFiveBytesEveryTenSeconds(out, conPurpose.getParameter());
					break;
				case SendXBytes:
					ConnectionTests.SendXBytes(out, conPurpose.getParameter());
					break;
				case ShutdownOutput:
					client.shutdownOutput();
					break;
				default:
					LOGGER.info("Function not supported");
					break;
				}
			}

		} catch (Exception e) {

		} finally {
			if (!client.isClosed()) {
				try {
					client.close();
				} catch (IOException e) {
				}
			}
		}
	} // Ende run
}