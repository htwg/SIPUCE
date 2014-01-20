package de.fhkn.in.uce.api.socket;

/**
 * Internal class to pool constructor informations.
 * 
 */
final class ConnectionUserData {

	private final String fromUser;
	private final String fromUserDomain;
	private final String toUser;
	private final String toUserDomain;

	public ConnectionUserData(String fromUser, String fromUserDomain, String toUser, String toUserDomain) {
		this.fromUser = fromUser;
		this.fromUserDomain = fromUserDomain;
		this.toUser = toUser;
		this.toUserDomain = toUserDomain;
	}

	public String getFromUser() {
		return fromUser;
	}

	public String getFromUserDomain() {
		return fromUserDomain;
	}

	public String getToUser() {
		return toUser;
	}

	public String getToUserDomain() {
		return toUserDomain;
	}

	@Override
	public String toString() {
		return "from=" + fromUser + "@" + fromUserDomain + ", to=" + toUser + "@" + toUserDomain;
	}

}
