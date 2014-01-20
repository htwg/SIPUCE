package de.fhkn.in.uce.socketswitch.simpleserver;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.socketswitch.HalfCloseSwitchableSocket;

import de.fhkn.in.uce.socketswitch.SwitchableSocket;
import de.fhkn.in.uce.socketswitch.TimeoutSwitchableSocket;
import de.fhkn.in.uce.socketswitch.OneByteSwitchableSocket;

// Handles the client connection
public class SwitchableSocketConnectionHandler implements Runnable {

	private static Logger LOGGER = LoggerFactory
			.getLogger("de.htwg.teamprojekt.socketswitch.simpleserver");

	private static Map<UUID, SwitchableSocket> connections;

	static {
		connections = Collections
				.synchronizedMap(new HashMap<UUID, SwitchableSocket>());
	}

	private final Socket socket;
	private SwitchableSocket switchableSocket;

	public SwitchableSocketConnectionHandler(Socket client) {
		this.socket = client;
	}

	public void run() {
		try {
			// read and service request on client
			LOGGER.info("running service, " + Thread.currentThread());
			OutputStream out;
			InputStream in;
			InputStream inSocket = socket.getInputStream();

			ObjectInputStream objIn = new ObjectInputStream(inSocket);

			ConnectionPurposeMessage conPurpose = (ConnectionPurposeMessage) objIn
					.readObject();

			if (conPurpose.getExecuteServerFunction() == ServerFunction.SwitchToThisConnection) {
				LOGGER.info("SwitchToThisConnection: "
						+ conPurpose.getConnectionId());
				connections.get(conPurpose.getConnectionId()).switchSocket(
						this.socket, 3000);
				return;
			}

			LOGGER.info("New switchableSocket: " + conPurpose.getConnectionId());
			switch (conPurpose.getParameter()) {
			case 0:
				TimeoutSwitchableSocket tSock = new TimeoutSwitchableSocket(
						this.socket);
				tSock.setPeriodicReadTimeout(100);
				switchableSocket = tSock;
				break;
			case 1:
				switchableSocket = new HalfCloseSwitchableSocket(this.socket);
				break;
			case 2:
				switchableSocket = new OneByteSwitchableSocket(this.socket);
				break;
			default:
				LOGGER.info("Stream ID not supported");
				return;
			}
			connections.put(conPurpose.getConnectionId(), switchableSocket);
			out = switchableSocket.getOutputStream();
			in = switchableSocket.getInputStream();

			// read the type of the expected function of the client
			switch (conPurpose.getExecuteServerFunction()) {
			case ReplyBytesXBytesStep:

				int arraySize = conPurpose.getParameter2();

				while (true) {
					byte[] byteBuffer = new byte[arraySize];
					int tReadBytes = in.read(byteBuffer);

					if (tReadBytes == -1) {
						LOGGER.info("Read -1");
						break;
					}

					try {
						out.write(byteBuffer, 0, tReadBytes);
					} catch (SocketException e) {
						System.out.println("switchableSocket.isClosed(): "
								+ switchableSocket.isClosed());
						System.out.println("socket.isClosed(): "
								+ socket.isClosed());
						throw e;
					}
				}
				System.out
						.println("shutting down output because server is done now");
				switchableSocket.shutdownOutput();
				break;
			default:
				LOGGER.info("Function {} not supported",
						conPurpose.getExecuteServerFunction());
				break;
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			LOGGER.info("thread end");
		}
	}

}
