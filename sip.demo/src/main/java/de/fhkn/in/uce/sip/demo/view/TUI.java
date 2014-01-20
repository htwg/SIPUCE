package de.fhkn.in.uce.sip.demo.view;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.Scanner;

import de.fhkn.in.uce.sip.core.SipUser;
import de.fhkn.in.uce.sip.demo.controller.IController;

public class TUI implements Observer {


	//Only used in initializeMock()
	private static final int DEFAULT_PROXY_PORT = 5060;
	private static final String DEFAULT_PROXY_IP = "213.239.218.18";

	private final IController controller;
	private Map<String, TuiCommand> commands;

	/**
	 * Creates a new Instance of the textual user interface.
	 * The controller is required and MUST NOT be null.
	 * @param controller
	 * @throws IllegalArgumentException if controller is null.
	 */
	public TUI(final IController controller) {
		if(controller == null) {
			throw new IllegalArgumentException("Server must not be null.");
		}
		this.controller = controller;
		this.controller.addObserver(this);
		addImplementedCommands();
	}

	/**
	 * Automatically initialize the SipPreferences before creating the SipStack
	 * using default values..
	 * <ul>
	 *   <li>Username = set by user input</li>
	 *   <li>Domain = 213.239.218.18</li>
	 *   <li>Proxy IP = 213.239.218.18</li>
	 *   <li>Proxy port = 5065</li>
	 * </ul>
	 */
	@SuppressWarnings("resource")
	public void initializeMock() {
		Scanner scanner = new Scanner(System.in, "UTF-8");
		System.out.print("Your SIP Username:");
		String username = scanner.next();
		controller.setUser(new SipUser(username, DEFAULT_PROXY_IP));
		controller.setSipProxyAddress(new InetSocketAddress(DEFAULT_PROXY_IP, DEFAULT_PROXY_PORT));
		controller.initSipStack();
		printTUI();
	}

	/**
	 * Initialize the SipPreferences before creating the SipStack using user
	 * inputs.
	 * <ul>
	 *   <li>Username</li>
	 *   <li>Domain</li>
	 *   <li>Proxy IP</li>
	 *   <li>Proxy port</li>
	 * </ul>
	 */
	@SuppressWarnings({ "unused", "resource" })
	private void initialize() {

		Scanner scanner = new Scanner(System.in, "UTF-8");
		System.out.print("Your SIP Username:");
		String username = scanner.next();

		System.out.print("Your SIP Domain:");
		String domain = scanner.next();

		SipUser user = new SipUser(username, domain);
		controller.setUser(user);

		System.out.print("Your SIP Proxy IP:");
		String proxy = scanner.next();

		System.out.print("Your SIP Proxy port:");
		int proxyPort = scanner.nextInt();
		controller.setSipProxyAddress(new InetSocketAddress(proxy, proxyPort));

		controller.initSipStack();
		printTUI();
	}

	/**
	 * Prints the menu.
	 */
	public void printTUI() {
		System.out.println("%n------%n Menu%n------");
		System.out.printf("Following commands are allowed:%n" +
				"\t r            - register %s%n" +
				"\t u            - unregister %s%n" +
				"\t i <USERNAME> - invite <username>@%s%n" +
				"\t b            - Send bye to current session %n" +
				"\t info         - print extra infos%n" +
				"\t h            - print command descriptions%n" +
				"\t q -> stop and quit%n",
				controller.getUser(),
				controller.getUser(),
				controller.getUser().getDomain());
		System.out.print(">>");
	}

	/**
	 * On update from the controller, print the menu again.
	 */
	@Override
	public void update(final Observable o, final Object arg) {
		if (arg instanceof Integer) {
			printAcceptCallQuestion();
		} else {
			printTUI();
		}

	}

	private void printAcceptCallQuestion() {
		System.out.println("Do you want to accept this call (y/n): ");
		System.out.print(">>");
	}

	/**
	 * Interpret the user input and execute the commands.
	 * @param line
	 * @return <code>false</code> if the system should quit.
	 */
	public boolean processInputLine(final String line) {
		boolean continu = true;
		Scanner scanner = new Scanner(line);
		scanner.useDelimiter(" ");
		scanner.next();
		try {
			String[] commandLine = line.split(" ");
			TuiCommand tcmd = commands.get(commandLine[0]);
			if (tcmd == null) {
				System.out.println("command not found: " + commandLine[0]);
			} else {
				continu = tcmd.execute(scanner);
			}
		} catch (Exception ex) {
			ex.getMessage();
		} finally {
			scanner.close();
			scanner = null;
		}
		return continu;
	}

	private void addImplementedCommands() {
		commands = new HashMap<String, TuiCommand>();
		commands.put("q", new QuitCommand());
		commands.put("r", new RegisterCommand());
		commands.put("u", new UnregisterCommand());
		commands.put("i", new InviteCommand());
		commands.put("b", new ByeCommand());
		commands.put("info", new PrintInfoCommand());
		commands.put("h", new PrintCommandsCommand());
		commands.put("y", new AcceptCallCommand());
		commands.put("n", new DeclineCallCommand());
	}



	/*
	 * 
	 *  Command Classes
	 * 
	 * 
	 */

	private interface TuiCommand {
		boolean execute(Scanner arguments);

		void printDescription();
	}

	private class PrintCommandsCommand implements TuiCommand {

		@Override
		public boolean execute(final Scanner arguments) {
			for (Entry<String, TuiCommand> command : commands.entrySet()) {
				System.out.println("- " + command.getKey());
				command.getValue().printDescription();
				System.out.println();
			}
			printTUI();
			return true;
		}

		@Override
		public void printDescription() {
			System.out.println("print commands");
		}
	}

	private class AcceptCallCommand implements TuiCommand {

		@Override
		public boolean execute(final Scanner arguments) {
			controller.acceptCall();
			printTUI();
			return true;
		}

		@Override
		public void printDescription() {
			System.out.println("Accept this call");
		}
	}

	private class DeclineCallCommand implements TuiCommand {

		@Override
		public boolean execute(final Scanner arguments) {
			controller.declineCall();
			printTUI();
			return true;
		}

		@Override
		public void printDescription() {
			System.out.println("Decline this call");
		}
	}

	private class QuitCommand implements TuiCommand {

		@Override
		public boolean execute(final Scanner arguments) {
			System.out.println("Exiting");
			controller.stop();
			return false;
		}

		@Override
		public void printDescription() {
			System.out.println("Quit program");
		}
	}

	private class RegisterCommand implements TuiCommand {

		@Override
		public boolean execute(final Scanner arguments) {
			controller.register();
			return true;
		}

		@Override
		public void printDescription() {
			System.out.println(
					"Register SIP User Agent at specified Proxy Server.");
		}
	}

	private class UnregisterCommand implements TuiCommand {

		@Override
		public boolean execute(final Scanner arguments) {
			controller.unregister();
			return true;
		}

		@Override
		public void printDescription() {
			System.out.println(
					"Unregister SIP User Agent at specified Proxy Server.");
		}
	}

	private class InviteCommand implements TuiCommand {

		@Override
		public boolean execute(final Scanner arguments) {
			controller.connect(arguments.next());
			return true;
		}

		@Override
		public void printDescription() {
			System.out.println(
					"Invite another SIP User Agent by giving his name as a " +
					"parameter.");
		}
	}

	private class ByeCommand implements TuiCommand {

		@Override
		public boolean execute(final Scanner arguments) {
			controller.disconnect();
			return true;
		}

		@Override
		public void printDescription() {
			System.out.println(
					"End the current session by sending a Bye Request.");
		}
	}

	private class PrintInfoCommand implements TuiCommand {

		@Override
		public boolean execute(final Scanner arguments) {
			controller.printInfo();
			printTUI();
			return true;
		}

		@Override
		public void printDescription() {
			System.out.println("Print some information about your address.");
		}
	}
}
