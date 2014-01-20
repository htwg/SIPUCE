package de.fhkn.in.uce.api.test;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;

public class SipMessageTest {

	@Test
	public void testSimpleSerialize() throws UnknownHostException {
		ISipMessage msg = new SipMessage();
		msg.addMessageData(
				"Stun",
				new StunSipMessageData(InetAddress.getByName("192.168.101.123")));

		String strSer = msg.serialize();

		ISipMessage msgDes = new SipMessage(strSer);
		InetAddress addr = msgDes.getMessageData("Stun",
				StunSipMessageData.class).getPublicEndpoint();

		assertEquals(InetAddress.getByName("192.168.101.123"), addr);
	}

}

interface ISipMessage {
	void addMessageData(String messageKey, ISipMessageData data);

	Set<String> getMessageKeys();

	<T extends ISipMessageData> T getMessageData(String messageKey,
			Class<T> classOfT);

	String serialize();
}

interface ISipMessageData {
	String serialize();

	void deserialize(String data);
}

class StunSipMessageData implements ISipMessageData {

	private InetAddress publicEndpoint;

	public StunSipMessageData() {

	}

	public StunSipMessageData(InetAddress publicEndpoint) {
		this.publicEndpoint = publicEndpoint;
	}

	public InetAddress getPublicEndpoint() {
		return publicEndpoint;
	}

	@Override
	public void deserialize(String data) {
		try {
			publicEndpoint = InetAddress.getByName(data);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String serialize() {
		return publicEndpoint.getHostAddress();
	}

}

class SipMessage implements ISipMessage {

	private final Map<String, String> messageData;

	public SipMessage() {
		messageData = new HashMap<String, String>();
	}

	public SipMessage(String data) {
		messageData = new HashMap<String, String>();
		String[] objects = data.split("#");
		for (String obj : objects) {
			String[] objKeyVal = obj.split("~");
			messageData.put(objKeyVal[0], objKeyVal[1]);
		}
	}

	@Override
	public void addMessageData(String messageKey, ISipMessageData data) {
		messageData.put(messageKey, data.serialize());
	}

	@Override
	public Set<String> getMessageKeys() {
		return messageData.keySet();
	}

	@Override
	public <T extends ISipMessageData> T getMessageData(String messageKey,
			Class<T> classOfT) {
		T msgData = null;
		try {
			msgData = classOfT.newInstance();
			msgData.deserialize(messageData.get(messageKey));
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return msgData;
	}

	@Override
	public String serialize() {
		StringBuilder tStrb = new StringBuilder();
		for (Entry<String, String> msgObj : messageData.entrySet()) {
			tStrb.append('#');
			tStrb.append(msgObj.getKey());
			tStrb.append('~');
			tStrb.append(msgObj.getValue());
		}
		if (tStrb.length() > 0) {
			tStrb.deleteCharAt(0);
		}
		return tStrb.toString();
	}

}
