package de.fhkn.in.uce.socketswitch.test.mocksocket;

import java.io.Serializable;

final class TestMessage implements Serializable {

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TestMessage)) {
			return false; // different class
		}
		TestMessage other = (TestMessage) obj;
		return (this.connectionId == other.connectionId) && (this.sentBytesCountIndex == other.sentBytesCountIndex);
	}

	private static final long serialVersionUID = -1718710637718946872L;

	private static final byte SOCKETSWITCH_MESSAGE_VERSION = 3;

	private long connectionId;
	private long sentBytesCountIndex;

	public TestMessage(long connectionId, long sentBytesCountIndex) {
		this.connectionId = connectionId;
		this.sentBytesCountIndex = sentBytesCountIndex;
	}

	public byte getSocketSwitchMessageVersion() {
		return SOCKETSWITCH_MESSAGE_VERSION;
	}

	public long getConnectionId() {
		return connectionId;
	}

	public void setConnectionId(long connectionId) {
		this.connectionId = connectionId;
	}

	public long getSentBytesCountIndex() {
		return sentBytesCountIndex;
	}

	public void setSentBytesCountIndex(long sentBytesCountIndex) {
		this.sentBytesCountIndex = sentBytesCountIndex;
	}

}
