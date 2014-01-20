package de.fhkn.in.uce.stun.demo;

import java.net.InetSocketAddress;

import de.fhkn.in.uce.api.socket.IUCEStunEndpoint;
import de.fhkn.in.uce.api.socket.UCEStunEndpoint;
import de.fhkn.in.uce.api.socket.UCEStunSettings;
import de.fhkn.in.uce.stun.ucestun.exception.UCEStunException;

/**
 * Hello world!
 *
 */
public final class UCEStunDemo 
{
	private UCEStunDemo() {}
	
	private static final int STUN_PORT = 3478;
	private static final String STUN_IP = "213.239.218.18";
	
    public static void main( String[] args ) throws UCEStunException
    {
        System.out.println("Starting UCEStunDemo...");

    	UCEStunSettings settings = new UCEStunSettings(new InetSocketAddress(STUN_IP, STUN_PORT));
    	IUCEStunEndpoint stunEndpoint = new UCEStunEndpoint(settings);
        
        System.out.printf("My Local Endpoint is: %s%n", stunEndpoint.getLocalEndpoint());
        System.out.printf("My Public Endpoint is: %s%n", stunEndpoint.getPublicEnpoint());
        
        System.out.println("Finishing UCEStunDemo...");
    }
}
