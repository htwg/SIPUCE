package de.fhkn.in.uce.socketswitch;

import java.net.Socket;
import java.util.concurrent.TimeoutException;


abstract class ByteCountSwitchableInputStream extends
		SwitchableInputStream {

	public ByteCountSwitchableInputStream() {
		super();
	}

	public abstract long getReceivedBytesCount();
	
	public abstract void allocateSwitchLock(int timeoutMillis) throws SwitchableException, InterruptedException, TimeoutException;
	
	public abstract void releaseSwitchLock();
	
	public abstract void switchStream(Socket newSocket, int timeoutMillis, long mustReadCountIndex) throws SwitchableException, TimeoutException;

}