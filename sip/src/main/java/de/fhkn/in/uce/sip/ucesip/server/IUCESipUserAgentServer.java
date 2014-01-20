package de.fhkn.in.uce.sip.ucesip.server;

import java.util.concurrent.TimeoutException;

import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;
import de.fhkn.in.uce.sip.ucesip.server.call.UCESipServerCall;

public interface IUCESipUserAgentServer {

	void register() throws UCESipException, InterruptedException;

	void shutdown() throws UCESipException, InterruptedException;

	UCESipServerCall takeCall(int timeoutMillis) throws InterruptedException, TimeoutException;
}
