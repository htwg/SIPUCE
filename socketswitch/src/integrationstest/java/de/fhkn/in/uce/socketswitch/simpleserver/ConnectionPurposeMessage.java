package de.fhkn.in.uce.socketswitch.simpleserver;

import java.io.Serializable;
import java.util.UUID;

public final class ConnectionPurposeMessage implements Serializable {
	private static final long serialVersionUID = 183183775191098007L;
	
	private ServerFunction executeServerFunction;
	private UUID connectionId;
	private int parameter;
	private int parameter2;
	
	public ConnectionPurposeMessage(ServerFunction executeServerFunction, UUID connectionId, int parameter, int parameter2) {
		this.connectionId = connectionId;
		this.executeServerFunction = executeServerFunction;
		this.parameter = parameter;
		this.parameter2 = parameter2;
	}
	
	public ServerFunction getExecuteServerFunction() {
		return executeServerFunction;
	}
	
	public UUID getConnectionId() {
		return connectionId;
	}
	
	public int getParameter() {
		return parameter;
	}
	
	public int getParameter2() {
		return parameter2;
	}

}
