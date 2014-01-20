package de.fhkn.in.uce.socketswitch;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

public final class TimeoutSwitchableInputStreamTest extends ByteCountSwitchableInputStreamTest {

	@Override
	SwitchableInputStream createSwitchableInputStream(Socket socket) throws IOException {
		return new TimeoutSwitchableInputStream(socket);
	}
	
	@Test
	public void testSimpleRead() throws IOException {
		byte[] b = new byte[10];

		setReadReturnRightSize();

		getByteCountSwitchableInputStream().read(b);
		verify(inputStream1Mock).read(b, 0, 10);
	}

	@Test
	public void testReadBytesCount() throws IOException {
		byte[] b = new byte[10];

		setReadReturnRightSize();

		getByteCountSwitchableInputStream().read(b);
		assertEquals(10L, getByteCountSwitchableInputStream().getReceivedBytesCount());
	}

	@Test
	public void testSimpleSwitch() throws IOException, SwitchableException, TimeoutException, InterruptedException {
		byte[] b = new byte[10];

		setReadReturnRightSize();

		getByteCountSwitchableInputStream().allocateSwitchLock(3000);
		getByteCountSwitchableInputStream().switchStream(socketMock2, 100, 0);
		getByteCountSwitchableInputStream().releaseSwitchLock();
		getByteCountSwitchableInputStream().read(b);

		verify(inputStream2Mock).read(b, 0, 10);
	}

	@Test
	public void testBufferBytesSwitch() throws IOException, SwitchableException, TimeoutException, InterruptedException {
		byte[] b = new byte[10];

		setReadReturnRightSize();

		getByteCountSwitchableInputStream().allocateSwitchLock(3000);
		getByteCountSwitchableInputStream().switchStream(socketMock2, 100, 10);
		getByteCountSwitchableInputStream().releaseSwitchLock();
		// read buffered
		getByteCountSwitchableInputStream().read(b);
		// read from new stream
		getByteCountSwitchableInputStream().read(b);

		verify(inputStream1Mock).read(b, 0, 10);
		verify(inputStream2Mock).read(b, 0, 10);
	}

	@Test
	public void testMultibleBufferBytesReadSwitch() throws IOException,
			SwitchableException, TimeoutException, InterruptedException  {
		byte[] b = new byte[10];
		byte[] a = new byte[7];

		setReadReturnRightSize();

		getByteCountSwitchableInputStream().allocateSwitchLock(3000);
		getByteCountSwitchableInputStream().switchStream(socketMock2, 100, 10);
		getByteCountSwitchableInputStream().releaseSwitchLock();
		// read buffered
		getByteCountSwitchableInputStream().read(a);
		// read last bytes buffered
		getByteCountSwitchableInputStream().read(b);
		// read new stream
		getByteCountSwitchableInputStream().read(a);

		verify(inputStream1Mock).read((byte[]) any(), eq(0), eq(10));
		verify(inputStream2Mock).read((byte[]) any(), eq(0), eq(7));
	}

	@Test
	public void testCancelSwitch() throws IOException, SwitchableException, TimeoutException, InterruptedException  {
		byte[] b = new byte[10];

		setReadReturnRightSize();

		getByteCountSwitchableInputStream().allocateSwitchLock(3000);
		getByteCountSwitchableInputStream().releaseSwitchLock();
		// read buffered bytes
		getByteCountSwitchableInputStream().read(b);
		// read from old stream
		getByteCountSwitchableInputStream().read(b);

		verify(inputStream1Mock, times(2)).read((byte[]) any(), eq(0), eq(10));
	}

	@Test
	public void testTwoSwitches() throws IOException, SwitchableException, TimeoutException, InterruptedException  {
		byte[] b = new byte[20];

		setReadReturnRightSize();

		getByteCountSwitchableInputStream().allocateSwitchLock(3000);
		getByteCountSwitchableInputStream().switchStream(socketMock2, 100, 10);
		getByteCountSwitchableInputStream().releaseSwitchLock();

		getByteCountSwitchableInputStream().allocateSwitchLock(3000);
		getByteCountSwitchableInputStream().switchStream(socketMock3, 100, 20);
		getByteCountSwitchableInputStream().releaseSwitchLock();
		// read buffered bytes
		getByteCountSwitchableInputStream().read(b);

		verify(inputStream1Mock).read((byte[]) any(), eq(0), eq(10));
		verify(inputStream2Mock).read((byte[]) any(), eq(0), eq(10));
	}

	@Test
	public void testTwoSwitchesWithoutSocketReading() throws IOException,
			SwitchableException, TimeoutException, InterruptedException  {
		byte[] b = new byte[20];

		setReadReturnRightSize();

		getByteCountSwitchableInputStream().allocateSwitchLock(3000);
		getByteCountSwitchableInputStream().switchStream(socketMock2, 100, 10);
		getByteCountSwitchableInputStream().releaseSwitchLock();

		getByteCountSwitchableInputStream().allocateSwitchLock(3000);
		getByteCountSwitchableInputStream().switchStream(socketMock3, 100, 10);
		getByteCountSwitchableInputStream().releaseSwitchLock();
		// read buffered bytes
		getByteCountSwitchableInputStream().read(b);

		verify(inputStream1Mock).read((byte[]) any(), eq(0), eq(10));
		verifyZeroInteractions(inputStream2Mock);
	}

	@Test
	public void testReadByte() throws IOException, SwitchableException {

		setReadReturnRightSize();

		getByteCountSwitchableInputStream().read();

		verify(inputStream1Mock).read((byte[]) any(), eq(0), eq(1));
	}

	@Test
	public void testReadByteEOF() throws IOException, SwitchableException {
		int b;

		when(inputStream1Mock.read((byte[]) any(), eq(0), eq(1))).thenReturn(0);

		b = getByteCountSwitchableInputStream().read();

		verify(inputStream1Mock).read((byte[]) any(), eq(0), eq(1));
		assertEquals(-1, b);
	}

	@Test
	public void testReadWithPeriodicTimeout() throws IOException,
			SwitchableException {
		byte[] b = new byte[10];

		setTimeoutExceptionOnFirstCallStreamMock1();

		getByteCountSwitchableInputStream().read(b);

		verify(inputStream1Mock, times(2)).read((byte[]) any(), anyInt(),
				anyInt());
	}

	@Test
	public void testReadWithNotReachedTimeout() throws IOException,
			SwitchableException {
		byte[] b = new byte[10];

		setTimeoutExceptionOnFirstCallStreamMock1();

		((TimeoutSwitchableInputStream) switchableInputStream)
				.setReadTimeout(100);
		((TimeoutSwitchableInputStream) switchableInputStream)
				.setPeriodicReadTimeout(50);

		getByteCountSwitchableInputStream().read(b);

		verify(inputStream1Mock, times(2)).read((byte[]) any(), anyInt(),
				anyInt());
	}

	@Test(expected = SocketTimeoutException.class)
	public void testReadWithTimeout() throws IOException {
		byte[] b = new byte[10];

		setTimeoutExceptionOnFirstCallStreamMock1();

		((TimeoutSwitchableInputStream) switchableInputStream)
				.setReadTimeout(1);

		getByteCountSwitchableInputStream().read(b);
	}


	@Test(expected = TimeoutException.class)
	public void testSwitchWithTimeout() throws IOException, SwitchableException, TimeoutException, InterruptedException  {

		setTimeoutExceptionOnFirstCallStreamMock1();

		getByteCountSwitchableInputStream().allocateSwitchLock(3000);
		getByteCountSwitchableInputStream().switchStream(socketMock2, 50, 10);
		getByteCountSwitchableInputStream().releaseSwitchLock();
	}

	@Test(expected = SwitchableException.class)
	public void testSwitchWithSocketException() throws IOException,
			SwitchableException, TimeoutException, InterruptedException  {

		doThrow(new SocketException()).when(socketMock2).setSoTimeout(anyInt());

		getByteCountSwitchableInputStream().allocateSwitchLock(3000);
		getByteCountSwitchableInputStream().switchStream(socketMock2, 50, 0);
		getByteCountSwitchableInputStream().releaseSwitchLock();
	}


	@Test(expected = SocketException.class)
	public void testSetPeriodicReadTimeoutWithSocketError()
			throws SocketException {

		doThrow(new SocketException()).when(socketMock1).setSoTimeout(anyInt());

		((TimeoutSwitchableInputStream) switchableInputStream)
				.setPeriodicReadTimeout(100);
	}

	@Test
	public void testSetReadTimeout() throws IOException, SwitchableException {

		((TimeoutSwitchableInputStream) switchableInputStream)
				.setReadTimeout(2000);

	}

}
