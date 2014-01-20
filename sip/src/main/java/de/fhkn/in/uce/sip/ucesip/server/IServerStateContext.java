package de.fhkn.in.uce.sip.ucesip.server;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import de.fhkn.in.uce.sip.core.ISipManager;
import de.fhkn.in.uce.sip.core.RegisterRefreshThread;
import de.fhkn.in.uce.sip.core.SipUser;
import de.fhkn.in.uce.sip.ucesip.server.call.UCESipServerCall;
import de.fhkn.in.uce.sip.ucesip.settings.UCESipServerSettings;

/**
 * Function which the states need from the State Context.
 *
 */
interface IServerStateContext {
	void changeState(IServerState newState);

	UCESipServerSettings getSipSettings();

	SipUser getServerUser();

	ISipManager getSipManager();

	void onNewCallReceived(UCESipServerCall newCall, int currentElementsInQueue);

	Map<String, UCESipServerCall> getServerCallMap();

	RegisterRefreshThread getRegisterRefreshThread();

	BlockingQueue<UCESipServerCall> getCallQueue();

	boolean isShutdown();

}