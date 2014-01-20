package de.fhkn.in.uce.sip.ucesip.message;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

public class UCESipMessage implements IUCESipMessage {

	private final Map<String, String> messageData;

	public UCESipMessage() {
		messageData = new HashMap<String, String>();
	}
	
	public UCESipMessage(String jsonString) {
		messageData = new HashMap<String, String>();
		
		JSONObject jsonObject = (JSONObject) JSONValue.parse(jsonString);
		
		for (Entry<String, Object> e : jsonObject.entrySet()) {
			messageData.put(e.getKey(), (String) e.getValue());
		}
	}

	@Override
	public void addMessageData(String messageKey, IUCESipMessageData data) {
		messageData.put(messageKey, data.serialize());
	}

	@Override
	public Set<String> getMessageKeys() {
		return messageData.keySet();
	}

	@Override
	public <T extends IUCESipMessageData> T getMessageData(String messageKey,
			Class<T> classOfT) throws UCESipException {
		T msgData = null;
        try {
            msgData = classOfT.newInstance();
            msgData.deserialize(messageData.get(messageKey));
        } catch (InstantiationException | IllegalAccessException e) {
            throw new UCESipException("Unable to deserialize parameters of Ok Message", e);
        }
        return msgData;
	}

	@Override
	public String serialize() {
		JSONObject jsonObject = new JSONObject();
		
		for (Entry<String, String> msgObj : messageData.entrySet()) {
            jsonObject.put(msgObj.getKey(), msgObj.getValue());
        }

        return jsonObject.toJSONString();
	}
}
