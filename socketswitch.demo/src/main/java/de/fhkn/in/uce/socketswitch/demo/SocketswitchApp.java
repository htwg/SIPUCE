package de.fhkn.in.uce.socketswitch.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class SocketswitchApp {

	private SocketswitchApp() {}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		boolean continu = true;
		
		SocketswitchAppTui tui = new SocketswitchAppTui(new Controller());
		
		InputStreamReader isr = new InputStreamReader(System.in, "UTF-8");
		BufferedReader br = new BufferedReader(isr);

		while (continu) {
			continu = tui.processInputLine(br.readLine());
		}

		br = null;
		isr = null;
	}

}
