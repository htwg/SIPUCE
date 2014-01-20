package de.fhkn.in.uce.socketswitch;

final class OneByteSocketSwitcher extends ByteCountSocketSwitcher {

	private final OneByteSwitchableInputStream hcSwInputStream;
	
	public OneByteSocketSwitcher(
			OneByteSwitchableInputStream switchableInputStream,
			ByteCountSwitchableOutputStream switchableOutputStream) {
		super(switchableInputStream, switchableOutputStream);
		hcSwInputStream = switchableInputStream;
	}
	
	@Override
	protected void afterSwitchStream() {
		hcSwInputStream.acceptClosing();
	}
	
}
