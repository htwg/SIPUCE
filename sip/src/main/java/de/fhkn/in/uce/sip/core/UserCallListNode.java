package de.fhkn.in.uce.sip.core;

import java.util.Map;
import java.util.TreeMap;

public class UserCallListNode {
	private final ISipManagerListener listener;
	private final Map<SipUser, Call> userCallsList;

	public UserCallListNode(ISipManagerListener listener) {
		this.listener = listener;
		userCallsList = new TreeMap<>();
	}

	public void addCall(SipUser user, Call call) {
		userCallsList.put(user, call);
	}

	public ISipManagerListener getListener() {
		return listener;
	}

	public Call getCall(SipUser user) {
		return userCallsList.get(user);
	}

	public void removeCall(SipUser user) {
		userCallsList.remove(user);
	}
}
