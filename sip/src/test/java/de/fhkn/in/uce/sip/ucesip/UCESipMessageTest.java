package de.fhkn.in.uce.sip.ucesip;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.junit.Assert;
import org.junit.Test;

import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;
import de.fhkn.in.uce.sip.ucesip.message.IUCESipMessage;
import de.fhkn.in.uce.sip.ucesip.message.UCESipMessage;
import de.fhkn.in.uce.sip.ucesip.message.UCESipMessageDataConnectionId;
import de.fhkn.in.uce.sip.ucesip.message.UCESipMessageDataRelay;
import de.fhkn.in.uce.sip.ucesip.message.UCESipMessageDataStun;

public class UCESipMessageTest {

	@Test
	public void jsonParserTest() throws UnknownHostException {
		InetSocketAddress expectedPrivateEndpoint = new InetSocketAddress(InetAddress.getByName("192.168.20.18"), 2020);
		InetSocketAddress exptectedPublicEndpoint = new InetSocketAddress(InetAddress.getByName("15.132.158.10"), 5060);

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("privateEndpointIp", expectedPrivateEndpoint.getAddress().getHostAddress());
		jsonObject.put("privateEndpointPort", expectedPrivateEndpoint.getPort());
		jsonObject.put("publicEndpointIp", exptectedPublicEndpoint.getAddress().getHostAddress());
		jsonObject.put("publicEndpointPort", exptectedPublicEndpoint.getPort());

		String jsonString = jsonObject.toJSONString();

		JSONObject jsonObjectParser = (JSONObject) JSONValue.parse(jsonString);
		InetSocketAddress privateEndpoint = new InetSocketAddress((String) jsonObjectParser.get("privateEndpointIp"), (Integer) jsonObjectParser.get("privateEndpointPort"));
		InetSocketAddress publicEndpoint = new InetSocketAddress((String) jsonObjectParser.get("publicEndpointIp"), (Integer) jsonObjectParser.get("publicEndpointPort"));

		Assert.assertEquals(expectedPrivateEndpoint, privateEndpoint);
		Assert.assertEquals(exptectedPublicEndpoint, publicEndpoint);
	}

	@Test
	public void messageDataConnectionIdTest() throws UnknownHostException {
		UUID connectionId = UUID.randomUUID();
		String expectedJSONString = "{\"connectionId\":" + "\"" + connectionId.toString() + "\"}";

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("connectionId", connectionId.toString());

		UCESipMessageDataConnectionId connectionIdData = new UCESipMessageDataConnectionId(connectionId);
		Assert.assertEquals(expectedJSONString, connectionIdData.serialize());

		connectionIdData.deserialize(jsonObject.toJSONString());
		Assert.assertEquals(connectionId, connectionIdData.getConnectionId());
	}

	@Test
	public void messageDataRelayTest() throws UnknownHostException {
		InetSocketAddress expectedRelayEndpoint = new InetSocketAddress(InetAddress.getByName("192.168.20.18"), 2020);
		String expectedJSONString = "{\"relayEndpointPort\":" + expectedRelayEndpoint.getPort() + "," + "\"relayEndpointIp\":\"" + expectedRelayEndpoint.getAddress().getHostAddress() + "\"}";

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("relayEndpointPort", expectedRelayEndpoint.getPort());
		jsonObject.put("relayEndpointIp", expectedRelayEndpoint.getAddress().getHostAddress());

		UCESipMessageDataRelay relayData = new UCESipMessageDataRelay(expectedRelayEndpoint);
		Assert.assertEquals(expectedJSONString, relayData.serialize());

		relayData.deserialize(jsonObject.toJSONString());
		Assert.assertEquals(expectedRelayEndpoint, relayData.getRelayEndpoint());
	}

	@Test
	public void messageDataStunTest() throws UnknownHostException {
		InetSocketAddress expectedPrivateEndpoint = new InetSocketAddress(InetAddress.getByName("192.168.20.18"), 2020);
		InetSocketAddress expectedPublicEndpoint = new InetSocketAddress(InetAddress.getByName("5.132.158.10"), 5060);

		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("\"publicEndpointIp\":\"");
		sb.append(expectedPublicEndpoint.getAddress().getHostAddress());
		sb.append("\",");
		sb.append("\"localEndpointIp\":\"");
		sb.append(expectedPrivateEndpoint.getAddress().getHostAddress());
		sb.append("\",");
		sb.append("\"publicEndpointPort\":");
		sb.append(expectedPublicEndpoint.getPort());
		sb.append(",");
		sb.append("\"localEndpointPort\":");
		sb.append(expectedPrivateEndpoint.getPort());
		sb.append("}");

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("publicEndpointPort", expectedPublicEndpoint.getPort());
		jsonObject.put("publicEndpointIp", expectedPublicEndpoint.getAddress().getHostAddress());
		jsonObject.put("localEndpointPort", expectedPrivateEndpoint.getPort());
		jsonObject.put("localEndpointIp", expectedPrivateEndpoint.getAddress().getHostAddress());

		UCESipMessageDataStun stunData = new UCESipMessageDataStun(expectedPrivateEndpoint, expectedPublicEndpoint);
		Assert.assertEquals(sb.toString(), stunData.serialize());

		UCESipMessageDataStun stunData2 = new UCESipMessageDataStun();
		stunData2.deserialize(jsonObject.toJSONString());
		Assert.assertEquals(expectedPrivateEndpoint, stunData2.getLocalEndpoint());
		Assert.assertEquals(expectedPublicEndpoint, stunData2.getPublicEndpoint());
	}

	@Test
	public void messageTest() throws UnknownHostException, UCESipException {
		IUCESipMessage msg = new UCESipMessage();

		UUID expectedConnectionId = UUID.randomUUID();
		UCESipMessageDataConnectionId connectionIdData = new UCESipMessageDataConnectionId(expectedConnectionId);

		InetSocketAddress expectedRelayEndpoint = new InetSocketAddress(InetAddress.getByName("158.245.18.0"), 2025);
		UCESipMessageDataRelay relayData = new UCESipMessageDataRelay(expectedRelayEndpoint);

		InetSocketAddress expectedPrivateEndpoint = new InetSocketAddress(InetAddress.getByName("192.168.20.18"), 2020);
		InetSocketAddress expectedPublicEndpoint = new InetSocketAddress(InetAddress.getByName("5.132.158.10"), 5060);
		UCESipMessageDataStun stunData = new UCESipMessageDataStun(expectedPrivateEndpoint, expectedPublicEndpoint);

		msg.addMessageData("ConnectionIdMessage", connectionIdData);
		msg.addMessageData("StunMessage", stunData);
		msg.addMessageData("RelayMessage", relayData);

		String json = msg.serialize();

		IUCESipMessage msg2 = new UCESipMessage(json);
		Assert.assertEquals(msg2.getMessageData("ConnectionIdMessage", UCESipMessageDataConnectionId.class).getConnectionId(), expectedConnectionId);
		Assert.assertEquals(msg2.getMessageData("StunMessage", UCESipMessageDataStun.class).getPublicEndpoint(), expectedPublicEndpoint);
		Assert.assertEquals(msg2.getMessageData("StunMessage", UCESipMessageDataStun.class).getLocalEndpoint(), expectedPrivateEndpoint);
		Assert.assertEquals(msg2.getMessageData("RelayMessage", UCESipMessageDataRelay.class).getRelayEndpoint(), expectedRelayEndpoint);
	}
}
