package de.fhkn.in.uce.sip.core;

import java.util.Random;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.ServerTransaction;
import javax.sip.message.Request;

import de.fhkn.in.uce.sip.core.message.IRequestMessage;
import de.fhkn.in.uce.sip.core.message.IResponseMessage;

public class Call {

	private static final double FACTOR = 0.99;

	private final SipUser clientUser;
	private final SipUser serverUser;

	private Dialog dialog;

	private ServerTransaction serverTransaction;
	private ClientTransaction clientTransaction;

	//Call specific Attributes
	private long callSequence = -1L;

	private IRequestMessage lastRequest = null;
	private IResponseMessage lastResponse = null;

	public void setLastResponse(final IResponseMessage lastResponse) {
		this.lastResponse = lastResponse;
	}

	public Call(final SipUser client, final SipUser server) {
		clientUser = client;
		serverUser = server;
	}

	public Dialog getDialog() {
		return dialog;
	}

	public SipUser getClientUser() {
		return clientUser;
	}

	public SipUser getServerUser() {
		return serverUser;
	}

	public ServerTransaction getServerTransaction() {
		return serverTransaction;
	}

	public ClientTransaction getClientTransaction() {
		return clientTransaction;
	}

	public IRequestMessage getLastRequest() {
		return lastRequest;
	}

	public Request getRawRequest() {
		return lastRequest.getRawRequest();
	}

	public void setDialog(final Dialog dialog) {
		this.dialog = dialog;
	}

	public void setServerTransaction(final ServerTransaction serverTransaction) {
		this.serverTransaction = serverTransaction;
	}

	public void setClientTransaction(final ClientTransaction clientTransaction) {
		this.clientTransaction = clientTransaction;
	}

	public void setCurrentRequest(final IRequestMessage inviteRequest) {
		lastRequest = inviteRequest;
	}

	public IResponseMessage getLastResponse() {
		return lastResponse;
	}

	public long getCallSequence() {
		if (callSequence == -1L) {
			Random rd = new Random();
			Number factoredInt = rd.nextInt() * FACTOR;
			callSequence = Math.abs(factoredInt.intValue());
		}
		return callSequence;
	}

	public void setCallSequence(final long callSequence) {
		this.callSequence = callSequence;
	}


}
