package de.fhkn.in.uce.socketswitch;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import org.junit.Before;
import org.junit.Test;

public abstract class SwitchableOutputStreamTest {
	
	protected SwitchableOutputStream switchableOutputStream;

	protected Socket socketMock1;
	protected Socket socketMock2;
	protected Socket socketMock3;

	protected OutputStream outputStream1Mock;
	protected OutputStream outputStream2Mock;
	protected OutputStream outputStream3Mock;

	abstract SwitchableOutputStream createSwitchableOutputStream(Socket socket) throws IOException;

	@Before
	public void setUp() throws IOException {
		outputStream1Mock = mock(OutputStream.class);
		socketMock1 = mock(Socket.class);
		when(socketMock1.getOutputStream()).thenReturn(outputStream1Mock);

		outputStream2Mock = mock(OutputStream.class);
		socketMock2 = mock(Socket.class);
		when(socketMock2.getOutputStream()).thenReturn(outputStream2Mock);

		outputStream3Mock = mock(OutputStream.class);
		socketMock3 = mock(Socket.class);
		when(socketMock3.getOutputStream()).thenThrow(new IOException());
		
		switchableOutputStream = createSwitchableOutputStream(socketMock1);
	}

	@Test(expected = IOException.class)
	public void testWriteException() throws IOException, SwitchableException {
		byte[] b = new byte[] { 1, 2, 3, 4, 5 };

		doThrow(new IOException()).when(outputStream1Mock).write((byte[]) any(), anyInt(), anyInt());

		switchableOutputStream.write(b);
	}

	@Test
	public void testWrite() throws Exception {
		byte[] b = new byte[] { 1, 1, 1, 1, 1 };
		switchableOutputStream.write(b);
		
		verify(outputStream1Mock).write(b, 0, 5);
	}
	
	@Test
	public void testWriteByte() throws Exception {
		byte[] b = new byte[] { 1 };
		switchableOutputStream.write(1);
		
		verify(outputStream1Mock).write(b, 0, 1);
	}

}
