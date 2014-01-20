package de.fhkn.in.uce.api.socket.remotetest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ConnectionHandler extends Thread {

	private final Socket sock;

	public ConnectionHandler(Socket sock) {
		this.sock = sock;
	}

	@Override
	public void run() {
		try {
			handleConnection();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleConnection() throws IOException {
		byte[] tTmpBytes = new byte[500];
		InputStream in = sock.getInputStream();
		OutputStream out = sock.getOutputStream();

		while (true) {
			int tReadBytes = in.read(tTmpBytes);

			if (tReadBytes == -1) {
				System.out.println("Read -1");
				break;
			}

			out.write(tTmpBytes, 0, tReadBytes);
		}

		System.out.println("shutting down output because server is done now");
		sock.close();
	}

}
