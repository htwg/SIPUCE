package de.fhkn.in.uce.socketswitch;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.SocketException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import de.fhkn.in.uce.socketswitch.simpleserver.ConnectionPurposeMessage;
import de.fhkn.in.uce.socketswitch.simpleserver.ServerFunction;

public abstract class ByteCountSwitchableInputStreamComplexTest extends
		SwitchableInputStreamComplexTest {

	
	ByteCountSwitchableInputStream getByteCountSwitchableInputStream() {
		return (ByteCountSwitchableInputStream)switchableInputStream;
	}

	@Test
	public final void testReadWithByteCount() throws IOException {
		byte[] b = new byte[5];
		byte[] a = new byte[] { 1, 2, 3, 4, 5 };

		selectServerFunction(ServerFunction.SendFiveBytesEverySecond, b.length);

		readFinish(b, getByteCountSwitchableInputStream());
		assertArrayEquals(a, b);
		assertEquals(5, getByteCountSwitchableInputStream().getReceivedBytesCount());
	}

	@Test
	public final void testReadToEOF2() throws Exception {
		byte[] b = new byte[15];
		byte[] a = new byte[] { 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 0, 0, 0, 0, 0 };

		ConnectionPurposeMessage tCp = new ConnectionPurposeMessage(
				ServerFunction.SendXBytes, UUID.randomUUID(), 10, 0);
		connection1ObjectOutputStream.writeObject(tCp);

		getByteCountSwitchableInputStream().allocateSwitchLock(3000);
		shutDownServerOutput(1);
		getByteCountSwitchableInputStream().switchStream(connection2, 3000, 10);
		getByteCountSwitchableInputStream().releaseSwitchLock();
		shutDownServerOutput(2);
		int test = getByteCountSwitchableInputStream().read(b, 0, 15);
		assertArrayEquals(a, b);
		assertEquals(10, test);
		test = getByteCountSwitchableInputStream().read(b, 0, 15);
		assertEquals(-1, test);
	}

	@Test
	public final void testSwitchStreamSwitchStreamRead() throws IOException,
			SwitchableException, IllegalAccessException, TimeoutException, InterruptedException  {
		byte[] b = new byte[15];
		byte[] a = new byte[] { 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5 };

		selectServerFunction(ServerFunction.SendFiveBytesEverySecond, b.length);

		getByteCountSwitchableInputStream().allocateSwitchLock(3000);
		shutDownServerOutput(1);
		getByteCountSwitchableInputStream().switchStream(connection2, 3000, 10);
		getByteCountSwitchableInputStream().releaseSwitchLock();

		getByteCountSwitchableInputStream().allocateSwitchLock(3000);
		shutDownServerOutput(2);
		getByteCountSwitchableInputStream().switchStream(connection2, 3000, 10);
		getByteCountSwitchableInputStream().releaseSwitchLock();

		readFinish(b, getByteCountSwitchableInputStream());
		assertArrayEquals(a, b);
	}

	@Test(expected = SocketException.class)
	public final void testDoubleSwitchWithException() throws IOException,
			SwitchableException, IllegalAccessException, TimeoutException, InterruptedException  {

		byte[] b = new byte[10];
		byte[] a = new byte[] { 1, 2, 3, 4, 5, 1, 2, 3, 4, 5 };

		selectServerFunction(ServerFunction.SendFiveBytesEverySecond, 5);

		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(100);
					getByteCountSwitchableInputStream().allocateSwitchLock(3000);
					shutDownServerOutput(1);
					selectServerFunction(
							ServerFunction.SendFiveBytesEverySecond, 5);
					getByteCountSwitchableInputStream().switchStream(connection2, 3000, 5);
					getByteCountSwitchableInputStream().releaseSwitchLock();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();

		readFinish(b, getByteCountSwitchableInputStream());
		assertArrayEquals(a, b);

		getByteCountSwitchableInputStream().allocateSwitchLock(3000);
		shutDownServerOutput(2);
		getByteCountSwitchableInputStream().switchStream(connection3, 3000, 10);
		getByteCountSwitchableInputStream().releaseSwitchLock();

		connection3.close();

		b = new byte[10];
		readFinish(b, getByteCountSwitchableInputStream());
	}

	@Test
	public final void testReadSwitchStreamRead() throws IOException,
			SwitchableException, IllegalAccessException, TimeoutException, InterruptedException  {
		byte[] b = new byte[5];
		byte[] a = new byte[] { 1, 2, 3, 4, 5 };

		selectServerFunction(ServerFunction.SendFiveBytesEverySecond, b.length);

		readFinish(b, getByteCountSwitchableInputStream());
		assertArrayEquals(a, b);

		getByteCountSwitchableInputStream().allocateSwitchLock(3000);
		shutDownServerOutput(1);
		getByteCountSwitchableInputStream().switchStream(connection2, 3000, 5);
		getByteCountSwitchableInputStream().releaseSwitchLock();

		readFinish(b, getByteCountSwitchableInputStream());
		assertArrayEquals(a, b);

	}

	@Test
	public final void testReadSwitchReceive10StreamRead() throws IOException,
			SwitchableException, IllegalAccessException, TimeoutException, InterruptedException  {
		byte[] b = new byte[5];
		byte[] a = new byte[] { 1, 2, 3, 4, 5 };

		selectServerFunction(ServerFunction.SendFiveBytesEverySecond,
				b.length * 3);

		readFinish(b, getByteCountSwitchableInputStream());
		assertArrayEquals(a, b);

		getByteCountSwitchableInputStream().allocateSwitchLock(3000);
		shutDownServerOutput(1);
		getByteCountSwitchableInputStream().switchStream(connection2, 3000, 5);
		getByteCountSwitchableInputStream().releaseSwitchLock();

		readFinish(b, getByteCountSwitchableInputStream());
		assertArrayEquals(a, b);

		readFinish(b, getByteCountSwitchableInputStream());
		assertArrayEquals(a, b);

	}

	@Test
	public final void testReadSwitchReceiveInBufferStreamRead()
			throws Exception {
		byte[] b = new byte[5];
		byte[] a = new byte[] { 1, 2, 3, 4, 5 };
		byte[] c = new byte[3];
		byte[] d = new byte[] { 1, 2, 3 };

		selectServerFunction(ServerFunction.SendFiveBytesEverySecond, b.length
				+ c.length);

		readFinish(b, getByteCountSwitchableInputStream());
		assertArrayEquals(a, b);

		getByteCountSwitchableInputStream().allocateSwitchLock(3000);
		Thread.sleep(1100);
		shutDownServerOutput(1);
		getByteCountSwitchableInputStream().switchStream(connection2, 3000, 10);
		getByteCountSwitchableInputStream().releaseSwitchLock();

		readFinish(c, getByteCountSwitchableInputStream());
		assertArrayEquals(d, c);

	}

	@Test
	public final void testReadSwitchCancelReadStreamRead() throws Exception {
		byte[] b = new byte[5];
		byte[] a = new byte[] { 1, 2, 3, 4, 5 };

		selectServerFunction(ServerFunction.SendFiveBytesEverySecond,
				b.length * 3);

		readFinish(b, getByteCountSwitchableInputStream());
		assertArrayEquals(a, b);

		getByteCountSwitchableInputStream().allocateSwitchLock(3000);
		getByteCountSwitchableInputStream().releaseSwitchLock();

		readFinish(b, getByteCountSwitchableInputStream());
		assertArrayEquals(a, b);

		readFinish(b, getByteCountSwitchableInputStream());
		assertArrayEquals(a, b);

	}

	@Test
	public final void testReadThreadSwitch() throws IOException {
		byte[] b = new byte[20];
		byte[] a = new byte[] { 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1,
				2, 3, 4, 5 };

		selectServerFunction(ServerFunction.SendFiveBytesEverySecond, 5);

		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(100);
					getByteCountSwitchableInputStream().allocateSwitchLock(3000);
					shutDownServerOutput(1);
					selectServerFunction(
							ServerFunction.SendFiveBytesEverySecond, 15);
					getByteCountSwitchableInputStream().switchStream(connection2, 3000, 5);
					getByteCountSwitchableInputStream().releaseSwitchLock();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();

		readFinish(b, getByteCountSwitchableInputStream());

		assertArrayEquals(a, b);

	}

	@Test
	public final void testReadThreadWaitSwitch() throws IOException,
			InterruptedException {
		byte[] b = new byte[10];
		byte[] a = new byte[] { 1, 2, 3, 4, 5, 1, 2, 3, 4, 5 };

		selectServerFunction(ServerFunction.SendFiveBytesEverySecond, 5);

		new Thread() {
			@Override
			public void run() {
				try {
					getByteCountSwitchableInputStream().allocateSwitchLock(3000);
					shutDownServerOutput(1);
					selectServerFunction(
							ServerFunction.SendFiveBytesEverySecond, 5);
					getByteCountSwitchableInputStream().switchStream(connection2, 3000, 5);
					getByteCountSwitchableInputStream().releaseSwitchLock();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();

		readFinish(b, getByteCountSwitchableInputStream());

		assertArrayEquals(a, b);
	}

	@Test
	public final void testReadAllBytesFromBuffer() throws IOException,
			SwitchableException, IllegalAccessException, TimeoutException, InterruptedException  {
		byte[] b = new byte[20];
		byte[] a = new byte[] { 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1,
				2, 3, 4, 5 };

		selectServerFunction(ServerFunction.SendXBytes, 20);

		getByteCountSwitchableInputStream().allocateSwitchLock(3000);
		shutDownServerOutput(1);
		getByteCountSwitchableInputStream().switchStream(connection2, 3000, 20);
		getByteCountSwitchableInputStream().releaseSwitchLock();

		readFinish(b, getByteCountSwitchableInputStream());

		assertArrayEquals(a, b);
	}

	@Test
	public final void testRead40Bytes() throws IOException,
			SwitchableException, IllegalAccessException, InterruptedException {
		byte[] b = new byte[40];
		byte[] a = new byte[] { 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1,
				2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2,
				3, 4, 5 };

		selectServerFunction(ServerFunction.SendXBytes, 20);

		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(250);
					getByteCountSwitchableInputStream().allocateSwitchLock(3000);
					shutDownServerOutput(1);
					selectServerFunction(ServerFunction.SendXBytes, 20);
					getByteCountSwitchableInputStream().switchStream(connection2, 3000, 20);
					getByteCountSwitchableInputStream().releaseSwitchLock();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();

		Thread.sleep(150);
		readFinish(b, getByteCountSwitchableInputStream());

		assertArrayEquals(a, b);
	}

	@Test(expected = IllegalStateException.class)
	public void testTwoSwitchsException() throws Exception {
		selectServerFunction(ServerFunction.SendFiveBytesEverySecond, 5);

		getByteCountSwitchableInputStream().allocateSwitchLock(3000);
		getByteCountSwitchableInputStream().allocateSwitchLock(3000);
	}

}
