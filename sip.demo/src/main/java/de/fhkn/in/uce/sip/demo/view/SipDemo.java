package de.fhkn.in.uce.sip.demo.view;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import de.fhkn.in.uce.sip.demo.controller.Controller;
import de.fhkn.in.uce.sip.demo.controller.IController;

public final class SipDemo {

	private SipDemo() {}

	public static void main(String[] args) throws IOException {

		System.out.println("------ TestClient 1.0 ------");

		IController controller = new Controller();
		TUI tui = new TUI(controller);

		tui.initializeMock();
		// continue to read user input on the tui until the user decides to quit
		boolean continu = true;

		InputStreamReader isr = new InputStreamReader(System.in, "UTF-8");
		BufferedReader br = new BufferedReader(isr);

		while (continu) {
			continu = tui.processInputLine(br.readLine());
		}
	}
}