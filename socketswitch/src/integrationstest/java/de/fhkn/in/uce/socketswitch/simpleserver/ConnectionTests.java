package de.fhkn.in.uce.socketswitch.simpleserver;

import java.io.OutputStream;

public final class ConnectionTests {

	public static void sendFiveBytesEverySecond(OutputStream out, int max)
			throws Exception {
		int bytesWritten = 0;
		while (max > bytesWritten) {
			byte[] b = new byte[] { 1, 2, 3, 4, 5 };
			out.write(b);
			bytesWritten += b.length;
			Thread.sleep(1000);
		}
	}

	public static void sendFiveBytesEveryTenSeconds(OutputStream out, int max)
			throws Exception {
		int bytesWritten = 0;
		while (max > bytesWritten) {
			byte[] b = new byte[] { 1, 2, 3, 4, 5 };
			out.write(b);
			bytesWritten += b.length;
			Thread.sleep(10000);
		}

	}

	public static void SendXBytes(OutputStream out, int count) throws Exception {
		byte[] b =  new byte[count];

		for (int i = 1; i <= count; i++) {
			b[i-1] = 6;
			switch (i % 5) {
			case 1:
				b[i-1] = 1;
				break;
			case 2:
				b[i-1] = 2;
				break;
			case 3:
				b[i-1] = 3;
				break;
			case 4:
				b[i-1] = 4;
				break;
			case 0:
				b[i-1] = 5;
				break;
			}
		}
		out.write(b);
	}

}
