package de.fhkn.in.uce.sip.ucesip.message;

import java.net.InetSocketAddress;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

public class UCESipMessageDataStun implements IUCESipMessageData {

	private InetSocketAddress publicEndpoint;
	private InetSocketAddress localEndpoint;

	public UCESipMessageDataStun() {
	}

	public UCESipMessageDataStun(InetSocketAddress loaclEndpoint,
			InetSocketAddress publicEndpoint) {
		this.publicEndpoint = publicEndpoint;
		this.localEndpoint = loaclEndpoint;
	}

	public InetSocketAddress getPublicEndpoint() {
		return publicEndpoint;
	}

	public InetSocketAddress getLocalEndpoint() {
		return localEndpoint;
	}

	@Override
	public void deserialize(String jsonString) {
		JSONObject jsonObject = (JSONObject) JSONValue.parse(jsonString);
		localEndpoint = new InetSocketAddress(
				(String) jsonObject.get("localEndpointIp"),
				(Integer) jsonObject.get("localEndpointPort"));
		publicEndpoint = new InetSocketAddress(
				(String) jsonObject.get("publicEndpointIp"),
				(Integer) jsonObject.get("publicEndpointPort"));
	}

	@Override
    public String serialize() {
    	JSONObject jsonObject = new JSONObject();
    	jsonObject.put("localEndpointIp", localEndpoint.getAddress().getHostAddress());
		jsonObject.put("localEndpointPort", localEndpoint.getPort());
		jsonObject.put("publicEndpointIp", publicEndpoint.getAddress().getHostAddress());
		jsonObject.put("publicEndpointPort", publicEndpoint.getPort());

        return jsonObject.toJSONString();
    }
}
