package de.fhkn.in.uce.socketswitch;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.junit.Before;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public abstract class SwitchableInputStreamTest {
	
	protected SwitchableInputStream switchableInputStream;

	protected Socket socketMock1;
	protected Socket socketMock2;
	protected Socket socketMock3;

	protected InputStream inputStream1Mock;
	protected InputStream inputStream2Mock;
	protected InputStream inputStream3Mock;

	abstract SwitchableInputStream createSwitchableInputStream(Socket socket) throws IOException;

	@Before
	public void setUp() throws IOException {
		inputStream1Mock = mock(InputStream.class);
		socketMock1 = mock(Socket.class);
		when(socketMock1.getInputStream()).thenReturn(inputStream1Mock);

		inputStream2Mock = mock(InputStream.class);
		socketMock2 = mock(Socket.class);
		when(socketMock2.getInputStream()).thenReturn(inputStream2Mock);

		inputStream3Mock = mock(InputStream.class);
		socketMock3 = mock(Socket.class);
		when(socketMock3.getInputStream()).thenReturn(inputStream3Mock);
		
		switchableInputStream = createSwitchableInputStream(socketMock1);
	}

	protected void setReadReturnRightSize() throws IOException {
		when(inputStream1Mock.read((byte[]) any(), anyInt(), anyInt()))
				.thenAnswer(new Answer<Integer>() {
					@Override
					public Integer answer(InvocationOnMock invocation)
							throws Throwable {
						return (Integer) invocation.getArguments()[2];
					}
				});

		when(inputStream2Mock.read((byte[]) any(), anyInt(), anyInt()))
				.thenAnswer(new Answer<Integer>() {
					@Override
					public Integer answer(InvocationOnMock invocation)
							throws Throwable {
						return (Integer) invocation.getArguments()[2];
					}
				});

		when(inputStream3Mock.read((byte[]) any(), anyInt(), anyInt()))
				.thenAnswer(new Answer<Integer>() {
					@Override
					public Integer answer(InvocationOnMock invocation)
							throws Throwable {
						return (Integer) invocation.getArguments()[2];
					}
				});
	}

	protected void setAvailableReturn(final int value) throws IOException {
		when(inputStream1Mock.available()).thenAnswer(new Answer<Integer>() {
			@Override
			public Integer answer(InvocationOnMock invocation) {
				return value;
			}
		});

		when(inputStream2Mock.available()).thenAnswer(new Answer<Integer>() {
			@Override
			public Integer answer(InvocationOnMock invocation) {
				return value;
			}
		});
		when(inputStream3Mock.available()).thenAnswer(new Answer<Integer>() {
			@Override
			public Integer answer(InvocationOnMock invocation) {
				return value;
			}
		});
	}

	protected void setTimeoutExceptionOnFirstCallStreamMock1()
			throws IOException {
		Answer<Integer> tAnsw = new Answer<Integer>() {
			int calls = 0;

			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				Integer lengthArg = (Integer) invocation.getArguments()[2];

				calls++;
				if (calls == 1) {
					throw new SocketTimeoutException();
				}
				return lengthArg;
			}
		};
		when(inputStream1Mock.read((byte[]) any(), anyInt(), anyInt()))
				.thenAnswer(tAnsw);
	}

}
