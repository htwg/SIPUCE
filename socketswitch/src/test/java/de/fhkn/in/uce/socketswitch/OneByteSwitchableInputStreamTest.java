package de.fhkn.in.uce.socketswitch;

import java.io.IOException;
import java.net.Socket;

public final class OneByteSwitchableInputStreamTest extends
		ByteCountSwitchableInputStreamTest {

	@Override
	SwitchableInputStream createSwitchableInputStream(Socket socket) throws IOException {
		return new OneByteSwitchableInputStream(socket);
	}
	
	OneByteSwitchableInputStream getOneByteSwitchableInputStream() {
		return (OneByteSwitchableInputStream)switchableInputStream;
	}

//	@Test
//	public void testSimpleRead() throws IOException {
//		byte[] b = new byte[10];
//
//		setReadReturnRightSize();
//
//		getOneByteSwitchableInputStream().read(b);
//	}
//
//	@Test
//	public void testSimpleReadWithoutData() throws IOException {
//		byte[] b = new byte[10];
//
//		setReadReturnRightSize();
//		setAvailableReturn(100);
//
//		getOneByteSwitchableInputStream().read(b);
//		verify(inputStream1Mock).read(b, 0, 1);
//		verify(inputStream1Mock).read(b, 1, 9);
//	}
//
//	@Test
//	public void testReadBytesCount() throws IOException {
//		byte[] b = new byte[10];
//
//		setReadReturnRightSize();
//		setAvailableReturn(100);
//
//		getOneByteSwitchableInputStream().read(b);
//		assertEquals(10L, getOneByteSwitchableInputStream().getReceivedBytesCount());
//	}
//
//	@Test(expected = IOException.class)
//	public void testSocketExceptionAllowed() throws IOException,
//			SwitchableException {
//		byte[] b = new byte[20];
//
//		doThrow(new IOException()).when(inputStream1Mock).read((byte[]) any(),
//				anyInt(), anyInt());
//
//		getOneByteSwitchableInputStream().read(b);
//	}
//
//	@Test
//	public void testCloseStreamWhileSwitch() throws IOException {
//
//		byte[] b = new byte[20];
//
//		setReadReturnRightSize();
//
//		when(inputStream1Mock.read((byte[]) any(), anyInt(), anyInt()))
//				.thenAnswer(new Answer<Integer>() {
//					int count = 0;
//
//					@Override
//					public Integer answer(InvocationOnMock invocation)
//							throws Throwable {
//						if (++count == 3) {
//							Thread.sleep(200);
//							throw new SocketException();
//						}
//
//						return (Integer) invocation.getArguments()[2];
//					}
//				});
//
//		setAvailableReturn(9);
//
//		new Thread() {
//			@Override
//			public void run() {
//				try {
//					Thread.sleep(100);
//					getOneByteSwitchableInputStream().prepareToSwitchStream(10, 3000);
//					getOneByteSwitchableInputStream().switchStream(socketMock2);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//
//			}
//		}.start();
//
//		getOneByteSwitchableInputStream().read(b);
//
//		verify(inputStream1Mock).read(b, 0, 1);
//		verify(inputStream1Mock).read(b, 10, 1);
//		verify(inputStream1Mock).read(b, 1, 9);
//		verify(inputStream2Mock).read(b, 10, 1);
//		verify(inputStream2Mock).read(b, 11, 9);
//	}
//
//	@Test
//	public void testSimpleSwitch() throws IOException, SwitchableException, TimeoutException {
//		byte[] b = new byte[10];
//
//		setReadReturnRightSize();
//		setAvailableReturn(100);
//
//		getOneByteSwitchableInputStream().prepareToSwitchStream(0, 100);
//		getOneByteSwitchableInputStream().switchStream(socketMock2);
//		getOneByteSwitchableInputStream().read(b);
//
//		verify(inputStream2Mock).read(b, 0, 1);
//		verify(inputStream2Mock).read(b, 1, 9);
//	}
//
//	@Test
//	public final void readAllBytesFromBuffer() throws IOException,
//			SwitchableException, IllegalAccessException, TimeoutException {
//		byte[] b = new byte[20];
//		setReadReturnRightSize();
//
//		getOneByteSwitchableInputStream().prepareToSwitchStream(20, 3000);
//		getOneByteSwitchableInputStream().switchStream(socketMock2);
//		getOneByteSwitchableInputStream().read(b);
//		verify(inputStream1Mock).read(b, 0, 20);
//	}
//
//	@Test(expected = SwitchableException.class)
//	public final void testSoTimeout() throws IOException, SwitchableException,
//			IllegalAccessException, TimeoutException {
//
//		doThrow(new SocketTimeoutException()).when(inputStream1Mock).read(
//				(byte[]) any(), anyInt(), anyInt());
//
//		getOneByteSwitchableInputStream().prepareToSwitchStream(10, 1);
//	}
//
//	@Test(expected = SwitchableException.class)
//	public final void testSoTimeoutResetTimeoutError() throws IOException,
//			SwitchableException, IllegalAccessException, TimeoutException {
//
//		setReadReturnRightSize();
//		doThrow(new SocketException()).when(socketMock1).setSoTimeout(anyInt());
//
//		getOneByteSwitchableInputStream().prepareToSwitchStream(10, 1);
//		getOneByteSwitchableInputStream().switchStream(socketMock2);
//	}
//
//	@Test
//	public final void testSwitchWhileSecondInternalRead() throws IOException,
//			SwitchableException, IllegalAccessException {
//		byte[] b = new byte[20];
//
//		setReadReturnRightSize();
//		setAvailableReturn(0);
//
//		when(inputStream1Mock.read((byte[]) any(), anyInt(), anyInt()))
//				.thenAnswer(new Answer<Integer>() {
//					int count = 0;
//
//					@Override
//					public Integer answer(InvocationOnMock invocation)
//							throws Throwable {
//						if (++count == 2) {
//							Thread.sleep(500);
//							setAvailableReturn(100);
//							throw new SocketException();
//						}
//						return (Integer) invocation.getArguments()[2];
//					}
//				});
//
//		new Thread() {
//			@Override
//			public void run() {
//				try {
//					Thread.sleep(100);
//					getOneByteSwitchableInputStream().prepareToSwitchStream(1, 3000);
//					getOneByteSwitchableInputStream().switchStream(socketMock2);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//
//			}
//		}.start();
//
//		getOneByteSwitchableInputStream().read(b);
//
//		verify(inputStream1Mock).read(b, 0, 1);
//		verify(inputStream1Mock).read(b, 1, 0);
//		verify(inputStream1Mock).close();
//		verify(inputStream2Mock).read(b, 1, 1);
//		verify(inputStream2Mock).read(b, 2, 18);
//
//	}
//
//	@Test
//	public final void testReadOneByte() throws IOException,
//			SwitchableException, IllegalAccessException {
//		byte[] b = new byte[1];
//		setReadReturnRightSize();
//
//		getOneByteSwitchableInputStream().read(b);
//		verify(inputStream1Mock).read(b, 0, 1);
//		verify(inputStream1Mock).read(b, 1, 0);
//	}
//
//	@Test
//	public final void testReadAfterClose() throws IOException,
//			SwitchableException, IllegalAccessException {
//		byte[] b = new byte[20];
//		byte[] c = new byte[14];
//
//		setReadReturnRightSize();
//		setAvailableReturn(5);
//
//		when(inputStream1Mock.read((byte[]) any(), anyInt(), anyInt()))
//				.thenAnswer(new Answer<Integer>() {
//					@Override
//					public Integer answer(InvocationOnMock invocation)
//							throws Throwable {
//						Thread.sleep(500);
//						return (Integer) invocation.getArguments()[2];
//					}
//				});
//
//		new Thread() {
//			@Override
//			public void run() {
//				try {
//					Thread.sleep(100);
//					getOneByteSwitchableInputStream().prepareToSwitchStream(20, 3000);
//					getOneByteSwitchableInputStream().switchStream(socketMock2);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//
//			}
//		}.start();
//
//		getOneByteSwitchableInputStream().read(b);
//
//		verify(inputStream1Mock).read(b, 0, 1);
//		verify(inputStream1Mock).read(b, 1, 5);
//		verify(inputStream1Mock).read(c, 0, 14);
//
//	}
//
//	@Test
//	public void testReadBufferPartial() throws IOException, SwitchableException, TimeoutException {
//		byte[] b = new byte[7];
//		byte[] c = new byte[33];
//		byte[] d = new byte[20];
//
//		setReadReturnRightSize();
//
//		getOneByteSwitchableInputStream().prepareToSwitchStream(20, 100);
//		getOneByteSwitchableInputStream().switchStream(socketMock2);
//
//		setAvailableReturn(6);
//		getOneByteSwitchableInputStream().read(b);
//
//		getOneByteSwitchableInputStream().prepareToSwitchStream(40, 100);
//		getOneByteSwitchableInputStream().switchStream(socketMock3);
//
//		setAvailableReturn(100);
//		getOneByteSwitchableInputStream().read(c);
//		getOneByteSwitchableInputStream().read(d);
//
//		verify(inputStream1Mock).read(d, 0, 20);
//		verify(inputStream2Mock).read(c, 13, 20);
//		verify(inputStream3Mock).read(d, 0, 1);
//		verify(inputStream3Mock).read(d, 1, 19);
//
//	}
//
//	@Test
//	public void testSimpleSwitchWithIOException() throws IOException,
//			SwitchableException, TimeoutException {
//		byte[] b = new byte[10];
//
//		setReadReturnRightSize();
//		setAvailableReturn(100);
//
//		doThrow(new IOException()).when(socketMock2).getInputStream();
//
//		getOneByteSwitchableInputStream().prepareToSwitchStream(0, 100);
//		try {
//			getOneByteSwitchableInputStream().switchStream(socketMock2);
//		} catch (SwitchableException e) {
//			doReturn(inputStream2Mock).when(socketMock2).getInputStream();
//			getOneByteSwitchableInputStream().prepareToSwitchStream(0, 100);
//			getOneByteSwitchableInputStream().switchStream(socketMock2);
//			getOneByteSwitchableInputStream().read(b);
//		}
//		verify(inputStream2Mock).read(b, 0, 1);
//		verify(inputStream2Mock).read(b, 1, 9);
//
//	}
//
//	@Test
//	public void testReadWithIOException() throws IOException,
//			SwitchableException {
//		byte[] b = new byte[10];
//
//		setReadReturnRightSize();
//		setAvailableReturn(100);
//
//		doThrow(new IOException()).when(inputStream1Mock).read((byte[]) any(),
//				eq(0), eq(1));
//		doThrow(new IOException()).when(inputStream1Mock).read((byte[]) any(),
//				eq(1), eq(9));
//
//		try {
//			getOneByteSwitchableInputStream().read(b);
//		} catch (IOException e) {
//
//			doAnswer(new Answer<Integer>() {
//				@Override
//				public Integer answer(InvocationOnMock invocation)
//						throws Throwable {
//					return (Integer) invocation.getArguments()[2];
//				}
//			}).when(inputStream1Mock).read((byte[]) any(), eq(0), eq(1));
//
//			try {
//				getOneByteSwitchableInputStream().read(b);
//			} catch (IOException f) {
//				doAnswer(new Answer<Integer>() {
//					@Override
//					public Integer answer(InvocationOnMock invocation)
//							throws Throwable {
//						return (Integer) invocation.getArguments()[2];
//					}
//				}).when(inputStream1Mock).read((byte[]) any(), eq(1), eq(9));
//
//				getOneByteSwitchableInputStream().read(b);
//			}
//		}
//
//		verify(inputStream1Mock, times(3)).read(b, 0, 1);
//		verify(inputStream1Mock, times(2)).read(b, 1, 9);
//
//	}
//
//	@Test
//	public void testReadAfterCancel() throws IOException, SwitchableException {
//		byte[] b = new byte[20];
//
//		setReadReturnRightSize();
//		setAvailableReturn(9);
//
//		when(inputStream1Mock.read((byte[]) any(), anyInt(), anyInt()))
//				.thenAnswer(new Answer<Integer>() {
//					@Override
//					public Integer answer(InvocationOnMock invocation)
//							throws Throwable {
//						Thread.sleep(200);
//						return (Integer) invocation.getArguments()[2];
//					}
//				});
//
//		new Thread() {
//			@Override
//			public void run() {
//				try {
//					Thread.sleep(100);
//					getOneByteSwitchableInputStream().prepareToSwitchStream(10, 3000);
//					getOneByteSwitchableInputStream().cancelStreamSwitch();
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		}.start();
//
//		getOneByteSwitchableInputStream().read(b);
//
//		verify(inputStream1Mock).read(b, 0, 1);
//		verify(inputStream1Mock).read(b, 1, 9);
//		verify(inputStream1Mock).read(b, 10, 1);
//		verify(inputStream1Mock).read(b, 11, 9);
//	}
//
//	@Test
//	public void testErrorWhileClosing() throws IOException, SwitchableException {
//		byte[] b = new byte[20];
//
//		doThrow(new SocketException()).when(inputStream1Mock).close();
//		setReadReturnRightSize();
//		setAvailableReturn(9);
//
//		when(inputStream1Mock.read((byte[]) any(), anyInt(), anyInt()))
//				.thenAnswer(new Answer<Integer>() {
//					int count = 0;
//
//					@Override
//					public Integer answer(InvocationOnMock invocation)
//							throws Throwable {
//						if (++count == 3) {
//							Thread.sleep(200);
//							throw new SocketException();
//						}
//						return (Integer) invocation.getArguments()[2];
//					}
//				});
//
//		new Thread() {
//			@Override
//			public void run() {
//				try {
//					Thread.sleep(100);
//					getOneByteSwitchableInputStream().prepareToSwitchStream(10, 3000);
//					getOneByteSwitchableInputStream().switchStream(socketMock2);
//				} catch (Exception e) {
//					try {
//						doNothing().when(inputStream1Mock).close();
//						getOneByteSwitchableInputStream().switchStream(socketMock2);
//					} catch (SwitchableException | IOException e1) {
//						e1.printStackTrace();
//					}
//				}
//			}
//		}.start();
//
//		getOneByteSwitchableInputStream().read(b);
//
//		verify(inputStream1Mock).read(b, 0, 1);
//		verify(inputStream1Mock).read(b, 1, 9);
//		verify(inputStream2Mock).read(b, 10, 1);
//		verify(inputStream2Mock).read(b, 11, 9);
//	}
//
//	@Test
//	public void testBufferBytesSwitch() throws IOException, SwitchableException, TimeoutException {
//		byte[] b = new byte[10];
//
//		setReadReturnRightSize();
//		setAvailableReturn(100);
//
//		getOneByteSwitchableInputStream().prepareToSwitchStream(10, 100);
//		getOneByteSwitchableInputStream().switchStream(socketMock2);
//		// read buffered
//		getOneByteSwitchableInputStream().read(b);
//		// read from new stream
//		getOneByteSwitchableInputStream().read(b);
//
//		verify(inputStream1Mock).read(b, 0, 10);
//		verify(inputStream2Mock).read(b, 0, 1);
//		verify(inputStream2Mock).read(b, 1, 9);
//	}
//
//	@Test
//	public void testMultibleBufferBytesReadSwitch() throws IOException,
//			SwitchableException, TimeoutException {
//		byte[] b = new byte[10];
//		byte[] a = new byte[7];
//
//		setReadReturnRightSize();
//		setAvailableReturn(100);
//
//		getOneByteSwitchableInputStream().prepareToSwitchStream(10, 100);
//		getOneByteSwitchableInputStream().switchStream(socketMock2);
//		// read buffered
//		getOneByteSwitchableInputStream().read(a);
//		// read from new stream
//		getOneByteSwitchableInputStream().read(b);
//
//		verify(inputStream1Mock).read((byte[]) any(), eq(0), eq(10));
//		verify(inputStream2Mock).read((byte[]) any(), eq(3), eq(7));
//	}
//
//	@Test
//	public void testCancelSwitch() throws IOException, SwitchableException, TimeoutException {
//		byte[] b = new byte[10];
//
//		setReadReturnRightSize();
//		setAvailableReturn(100);
//
//		getOneByteSwitchableInputStream().prepareToSwitchStream(10, 100);
//		getOneByteSwitchableInputStream().cancelStreamSwitch();
//		// read buffered bytes
//		getOneByteSwitchableInputStream().read(b);
//		// read from old stream
//		getOneByteSwitchableInputStream().read(b);
//
//		verify(inputStream1Mock).read((byte[]) any(), eq(0), eq(10));
//		verify(inputStream1Mock).read((byte[]) any(), eq(0), eq(1));
//		verify(inputStream1Mock).read((byte[]) any(), eq(1), eq(9));
//	}
//
//	@Test
//	public void testTwoSwitches() throws IOException, SwitchableException, TimeoutException {
//		byte[] b = new byte[20];
//
//		setReadReturnRightSize();
//		setAvailableReturn(100);
//
//		getOneByteSwitchableInputStream().prepareToSwitchStream(10, 100);
//		getOneByteSwitchableInputStream().switchStream(socketMock2);
//
//		getOneByteSwitchableInputStream().prepareToSwitchStream(20, 100);
//		getOneByteSwitchableInputStream().switchStream(socketMock3);
//		// read buffered bytes
//		getOneByteSwitchableInputStream().read(b);
//
//		verify(inputStream1Mock).read((byte[]) any(), eq(0), eq(10));
//		verify(inputStream2Mock).read((byte[]) any(), eq(10), eq(10));
//	}
//
//	@Test
//	public void testReadByte() throws IOException, SwitchableException {
//
//		setReadReturnRightSize();
//		setAvailableReturn(100);
//
//		getOneByteSwitchableInputStream().read();
//
//		verify(inputStream1Mock).read((byte[]) any(), eq(0), eq(1));
//	}
//
//	

}
