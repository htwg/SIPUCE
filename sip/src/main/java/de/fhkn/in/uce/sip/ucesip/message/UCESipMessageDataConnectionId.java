package de.fhkn.in.uce.sip.ucesip.message;

import java.util.UUID;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

public class UCESipMessageDataConnectionId implements IUCESipMessageData {

	private UUID connectionId;

	public UCESipMessageDataConnectionId() {
	}

	public UCESipMessageDataConnectionId(UUID connectionId) {
		this.connectionId = connectionId;
	}

	public UUID getConnectionId() {
		return connectionId;
	}

	@Override
	public void deserialize(String jsonString) {
		JSONObject jsonObjectParser = (JSONObject) JSONValue.parse(jsonString);
		connectionId = UUID.fromString((String) jsonObjectParser.get("connectionId"));
	}

	@Override
	public String serialize() {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("connectionId", connectionId.toString());

		return jsonObject.toJSONString();
	}
}
