package de.fhkn.in.uce.api.socket;

import java.net.InetSocketAddress;

import de.fhkn.in.uce.api.socket.UCEClientSettings;
import de.fhkn.in.uce.api.socket.UCEServerSettings;
import de.fhkn.in.uce.sip.ucesip.settings.UCESipClientSettings;
import de.fhkn.in.uce.sip.ucesip.settings.UCESipServerSettings;
import de.fhkn.in.uce.sip.ucesip.settings.UCESipSettings.TransportProtocol;

public class UCETestSettings {

	public static final UCEClientSettings CLIENT_SETTINGS;
	public static final UCEServerSettings SERVER_SETTINGS;

	public static final UCEClientSettings CLIENT_SETTINGS_WITHOUT_HOLEPUNCHER;
	public static final UCEServerSettings SERVER_SETTINGS_WITHOUT_HOLEPUNCHER;

	public static final UCEClientSettings LOCAL_CLIENT_SETTINGS_WITHOUT_HOLEPUNCHER;
	public static final UCEServerSettings LOCAL_SERVER_SETTINGS_WITHOUT_HOLEPUNCHER;

	public static final UCEClientSettings LOCAL_CLIENT_SETTINGS;
	public static final UCEServerSettings LOCAL_SERVER_SETTINGS;

	public static final UCEClientSettings CLIENT_SETTINGS_OPENSIPS;
	public static final UCEServerSettings SERVER_SETTINGS_OPENSIPS;

	public static final UCEClientSettings CLIENT_SETTINGS_OPENJSIP;
	public static final UCEServerSettings SERVER_SETTINGS_OPENJSIP;

	static {

		CLIENT_SETTINGS = new UCEClientSettings(new UCEStunSettings(new InetSocketAddress("213.239.218.18", 3478)), new UCESipClientSettings(120,
				TransportProtocol.Udp, new InetSocketAddress("213.239.218.18", 5060), 5000, 1000, 1000));

		SERVER_SETTINGS = new UCEServerSettings(new UCEStunSettings(new InetSocketAddress("213.239.218.18", 3478)), new UCESipServerSettings(1000,
				5000, 5000, 5000, 5000, 1000, 120, TransportProtocol.Udp, new InetSocketAddress("213.239.218.18", 5060)), new UCERelaySettings(
				new InetSocketAddress("213.239.218.18", 10300)));

		CLIENT_SETTINGS_WITHOUT_HOLEPUNCHER = new UCEClientSettings(new UCEStunSettings(new InetSocketAddress("213.239.218.18", 3478)),
				new UCESipClientSettings(120, TransportProtocol.Udp, new InetSocketAddress("213.239.218.18", 5060), 60000, 1000, 1000), 10000, false);

		SERVER_SETTINGS_WITHOUT_HOLEPUNCHER = new UCEServerSettings(new UCEStunSettings(new InetSocketAddress("213.239.218.18", 3478)),
				new UCESipServerSettings(1000, 5000, 5000, 5000, 5000, 1000, 120, TransportProtocol.Udp,
						new InetSocketAddress("213.239.218.18", 5060)), new UCERelaySettings(new InetSocketAddress("213.239.218.18", 10300)), 5000,
				5000, 10000, false);

		LOCAL_CLIENT_SETTINGS_WITHOUT_HOLEPUNCHER = new UCEClientSettings(new UCEStunSettings(new InetSocketAddress("10.211.55.17", 3478)),
				new UCESipClientSettings(120, TransportProtocol.Udp, new InetSocketAddress("10.211.55.17", 5060), 5000, 1000, 1000), 10000, false);

		LOCAL_SERVER_SETTINGS_WITHOUT_HOLEPUNCHER = new UCEServerSettings(
				new UCEStunSettings(new InetSocketAddress("10.211.55.17", 3478)),
				new UCESipServerSettings(1000, 5000, 5000, 5000, 5000, 1000, 120, TransportProtocol.Udp, new InetSocketAddress("10.211.55.17", 5060)),
				new UCERelaySettings(new InetSocketAddress("10.211.55.17", 10300)), 5000, 5000, 10000, false);

		LOCAL_CLIENT_SETTINGS = new UCEClientSettings(new UCEStunSettings(new InetSocketAddress("10.211.55.17", 3478)), new UCESipClientSettings(120,
				TransportProtocol.Udp, new InetSocketAddress("10.211.55.17", 5060), 5000, 1000, 1000));

		LOCAL_SERVER_SETTINGS = new UCEServerSettings(new UCEStunSettings(new InetSocketAddress("10.211.55.17", 3478)), new UCESipServerSettings(
				1000, 5000, 5000, 5000, 5000, 1000, 120, TransportProtocol.Udp, new InetSocketAddress("10.211.55.17", 5060)), new UCERelaySettings(
				new InetSocketAddress("10.211.55.17", 10300)));

		CLIENT_SETTINGS_OPENSIPS = new UCEClientSettings(new UCEStunSettings(new InetSocketAddress("213.239.218.18", 3478)),
				new UCESipClientSettings(120, TransportProtocol.Udp, new InetSocketAddress("213.239.218.18", 5060), 5000, 1000, 1000));

		SERVER_SETTINGS_OPENSIPS = new UCEServerSettings(new UCEStunSettings(new InetSocketAddress("213.239.218.18", 3478)),
				new UCESipServerSettings(1000, 5000, 5000, 5000, 5000, 1000, 120, TransportProtocol.Udp,
						new InetSocketAddress("213.239.218.18", 5060)), new UCERelaySettings(new InetSocketAddress("213.239.218.18", 10300)));

		CLIENT_SETTINGS_OPENJSIP = new UCEClientSettings(new UCEStunSettings(new InetSocketAddress("213.239.218.18", 3478)),
				new UCESipClientSettings(120, TransportProtocol.Udp, new InetSocketAddress("213.239.218.18", 5061), 5000, 1000, 1000));

		SERVER_SETTINGS_OPENJSIP = new UCEServerSettings(new UCEStunSettings(new InetSocketAddress("213.239.218.18", 3478)),
				new UCESipServerSettings(1000, 5000, 5000, 5000, 5000, 1000, 120, TransportProtocol.Udp,
						new InetSocketAddress("213.239.218.18", 5061)), new UCERelaySettings(new InetSocketAddress("213.239.218.18", 10300)));
	}
}
