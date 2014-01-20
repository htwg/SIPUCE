package de.fhkn.in.uce.sip.ucesip.message;

import java.net.InetSocketAddress;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

public class UCESipMessageDataRelay implements IUCESipMessageData {

	private InetSocketAddress relayEndpoint;
	
	public UCESipMessageDataRelay() {
	}

	public UCESipMessageDataRelay(InetSocketAddress relayEndpoint) {
		this.relayEndpoint = relayEndpoint;
	}

	public InetSocketAddress getRelayEndpoint() {
		return relayEndpoint;
	}

	@Override
	public void deserialize(String jsonString) {
		JSONObject jsonObject = (JSONObject) JSONValue.parse(jsonString);
		relayEndpoint = new InetSocketAddress(
				(String) jsonObject.get("relayEndpointIp"),
				(Integer) jsonObject.get("relayEndpointPort"));
	}

	@Override
    public String serialize() {
    	JSONObject jsonObject = new JSONObject();
		jsonObject.put("relayEndpointIp", relayEndpoint.getAddress().getHostAddress());
		jsonObject.put("relayEndpointPort", relayEndpoint.getPort());

        return jsonObject.toJSONString();
    }
}
