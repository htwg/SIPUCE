package de.fhkn.in.uce.socketswitch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeoutException;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ByteCountSwitchableOutputStreamTest extends
		SwitchableOutputStreamTest {

	@Override
	SwitchableOutputStream createSwitchableOutputStream(Socket socket) throws IOException {
		return new ByteCountSwitchableOutputStream(
				socketMock1);
	}
	
	private ByteCountSwitchableOutputStream getSwitchableOutputStream() {
		return (ByteCountSwitchableOutputStream) switchableOutputStream;
	}

	@Test
	public void testWrittenBytesCount() throws Exception {
		byte[] b = new byte[] { 1, 1, 1, 1, 1 };
		switchableOutputStream.write(b);

		verify(outputStream1Mock).write(b, 0, 5);
		assertEquals(5, getSwitchableOutputStream().getWrittenBytesCountIndex());
	}

	@Test
	public void testWrittenBytesCountFromSwitch() throws Exception {
		byte[] b = new byte[] { 1, 1, 1, 1, 1 };
		switchableOutputStream.write(b);

		getSwitchableOutputStream().allocateSwitchLock();
		assertEquals(5L, getSwitchableOutputStream().getWrittenBytesCountIndex());

		verify(outputStream1Mock).write(b, 0, 5);

	}

	@Test(expected = IllegalStateException.class)
	public void testWrongPrepareSwitchUsage() throws TimeoutException {

		getSwitchableOutputStream().allocateSwitchLock();
		getSwitchableOutputStream().allocateSwitchLock();

	}

	@Test(expected = IllegalStateException.class)
	public void testWrongSwitchUsage() throws IOException, SwitchableException,
			TimeoutException, InterruptedException {

		getSwitchableOutputStream().switchStream(socketMock2, 100);

	}

	@Test(expected = IllegalStateException.class)
	public void testWrongCancelSwitchUsage() throws IOException,
			SwitchableException {

		getSwitchableOutputStream().releaseSwitchLock();

	}

	@Test(expected = TimeoutException.class)
	@SuppressWarnings("rawtypes")
	public void testSwitchTimeoutException() throws IOException,
			InterruptedException, TimeoutException, SwitchableException {

		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Thread.sleep(100);
				return null;
			}
		}).when(outputStream1Mock).write((byte[]) any(), anyInt(), anyInt());

		new Thread() {
			@Override
			public void run() {
				byte[] b = new byte[] { 1, 2, 3, 4, 5 };
				try {
					switchableOutputStream.write(b);
				} catch (IOException e) {
					fail();
				}
			};
		}.start();

		Thread.sleep(10);
		getSwitchableOutputStream().allocateSwitchLock();
		getSwitchableOutputStream().switchStream(socketMock2, 10);
		getSwitchableOutputStream().releaseSwitchLock();

	}

	@Test
	public void testWriteSwitchWrite() throws Exception {
		byte[] b = new byte[] { 1, 1, 1, 1, 1 };
		switchableOutputStream.write(b);

		getSwitchableOutputStream().allocateSwitchLock();
		getSwitchableOutputStream().switchStream(socketMock2, 100);
		getSwitchableOutputStream().releaseSwitchLock();

		switchableOutputStream.write(b);

		verify(outputStream1Mock).write(b, 0, 5);
		verify(outputStream2Mock).write(b, 0, 5);
	}

	@Test
	public void testWriteSwitchCancelWrite() throws Exception {
		byte[] b = new byte[] { 1, 1, 1, 1, 1 };
		switchableOutputStream.write(b);

		getSwitchableOutputStream().allocateSwitchLock();
		getSwitchableOutputStream().releaseSwitchLock();

		switchableOutputStream.write(b);

		verify(outputStream1Mock, times(2)).write(b, 0, 5);
	}

	@Test(expected = SwitchableException.class)
	public void testSwitchGetStreamException() throws Exception {

		getSwitchableOutputStream().allocateSwitchLock();
		getSwitchableOutputStream().switchStream(socketMock3, 100);
		getSwitchableOutputStream().releaseSwitchLock();

	}



	
}
