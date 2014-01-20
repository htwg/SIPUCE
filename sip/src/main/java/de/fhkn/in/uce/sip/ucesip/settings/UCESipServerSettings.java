package de.fhkn.in.uce.sip.ucesip.settings;

import java.net.InetSocketAddress;

public class UCESipServerSettings extends UCESipSettings {
	
	private static final int DEFAULT_CALL_QUEUE_CAPACITY = 1000;
	private static final int DEFAULT_WAIT_FOR_CALLS_FINISHED_TIMEOUT = 5000;
	private static final int DEFAULT_WAIT_FOR_DEREGISTER_RESPONSE_TIMEOUT = 5000;
	private static final int DEFAULT_WAIT_FOR_REGISTER_RESPONSE_TIMEOUT = 5000;
	private static final int DEFAULT_SERVER_CALL_WAIT_FOR_BYE_TIMEOUT = 5000;
	private static final int DEFAULT_SERVER_CALLMAP_CLEANUP_PERIOD = 1000;

	private final int callQueueCapacity;
	private final int waitForCallsFinishedTimeout;
	private final int waitForDeregisterResponseTimeout;
	private final int waitForRegisterResponseTimeout;
	private final int serverCallWaitForByeTimeout;
	private final int serverCallMapCleanupPeriodMillis;

	public UCESipServerSettings(int callQueueCapacity, int waitForCallsFinishedTimeout, int waitForDeregisterResponseTimeout,
			int waitForRegisterResponseTimeout, int serverCallWaitForByeTimeout, int serverCallMapCleanupPeriodMillis, int expireTime,
			TransportProtocol transportProtocol, InetSocketAddress proxyAddress) {
		super(expireTime, transportProtocol, proxyAddress);
		this.callQueueCapacity = callQueueCapacity;
		this.waitForCallsFinishedTimeout = waitForCallsFinishedTimeout;
		this.waitForDeregisterResponseTimeout = waitForDeregisterResponseTimeout;
		this.waitForRegisterResponseTimeout = waitForRegisterResponseTimeout;
		this.serverCallWaitForByeTimeout = serverCallWaitForByeTimeout;
		this.serverCallMapCleanupPeriodMillis = serverCallMapCleanupPeriodMillis;

	}

	public UCESipServerSettings(final InetSocketAddress proxyAddress) {
		super(proxyAddress);
		this.callQueueCapacity = DEFAULT_CALL_QUEUE_CAPACITY;
		this.waitForCallsFinishedTimeout = DEFAULT_WAIT_FOR_CALLS_FINISHED_TIMEOUT;
		this.waitForDeregisterResponseTimeout = DEFAULT_WAIT_FOR_DEREGISTER_RESPONSE_TIMEOUT;
		this.waitForRegisterResponseTimeout = DEFAULT_WAIT_FOR_REGISTER_RESPONSE_TIMEOUT;
		this.serverCallWaitForByeTimeout = DEFAULT_SERVER_CALL_WAIT_FOR_BYE_TIMEOUT;
		this.serverCallMapCleanupPeriodMillis = DEFAULT_SERVER_CALLMAP_CLEANUP_PERIOD;
	}

	public int getCallQueueCapacity() {
		return callQueueCapacity;
	}

	public int getWaitForCallsFinishedTimeout() {
		return waitForCallsFinishedTimeout;
	}

	public int getWaitForDeregisterResponseTimeout() {
		return waitForDeregisterResponseTimeout;
	}

	public int getWaitForRegisterResponseTimeout() {
		return waitForRegisterResponseTimeout;
	}

	public int getServerCallWaitForByeTimeout() {
		return serverCallWaitForByeTimeout;
	}

	public long getServerCallMapCleanupPeriodMillis() {
		return serverCallMapCleanupPeriodMillis;
	}

}
