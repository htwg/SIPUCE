package de.fhkn.in.uce.socketswitch.simpleserver;

public enum ServerFunction {
	SwitchToThisConnection,
	SendFiveBytesEverySecond,
	SendFiveBytesEveryTenSeconds,
	ReplyBytesXBytesStep,
	SendXBytes,
	ShutdownOutput
}
