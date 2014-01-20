package de.fhkn.in.uce.socketswitch.demo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import de.fhkn.in.uce.socketswitch.SwitchableException;

public class SocketswitchAppTui implements Observer {

	private Controller controller;
	private Map<Character, TuiCommand> commands;

	public SocketswitchAppTui(Controller controller) {
		this.controller = controller;
		this.controller.addObserver(this);
		addImplementedCommands();
	}

	@Override
	public void update(Observable o, Object arg) {
		System.out.println("Open and switchable connections:");
		System.out.println(controller.getOpenedConnectionsList());
	}


	public boolean processInputLine(String line) {

		boolean continu = true;
		Scanner scanner = new Scanner(line);

		scanner.useDelimiter(" ");
		try {
			scanner.next();
			char command = line.charAt(0);
			TuiCommand tcmd = commands.get(command);
			if (tcmd == null) {
				System.out.println("command not found");
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
		commands = new HashMap<Character, TuiCommand>();
		commands.put('q', new QuitCommand());
		commands.put('c', new CreateSwitchableSocketCommand());
		commands.put('r', new ReadCommand());
		commands.put('w', new WriteCommand());
		commands.put('a', new ServerStartCommand());
		commands.put('s', new SwitchSocketCommand());
		commands.put('n', new NewConnectionCommand());
		commands.put('o', new PrintOpenConnectionsCommand());
		commands.put('h', new PrintCommandsCommand());
	}

	private interface TuiCommand {
		boolean execute(Scanner arguments);

		void printDescription();
	}
	
	private class PrintCommandsCommand implements TuiCommand {

		@Override
		public boolean execute(Scanner arguments) {
			for (Entry<Character, TuiCommand> command : commands.entrySet()) {
				System.out.println("- " + command.getKey());
				command.getValue().printDescription();
				System.out.println();
			}
			return true;
		}

		@Override
		public void printDescription() {
			System.out.println("print commands");
		}
	}

	private class QuitCommand implements TuiCommand {

		@Override
		public boolean execute(Scanner arguments) {
			System.out.println("Exiting");
			try {
				controller.shutdown();
			} catch (IOException e) {
				e.getMessage();
			}
			return false;
		}

		@Override
		public void printDescription() {
			System.out.println("Quit program");
		}
	}

	
	private class CreateSwitchableSocketCommand implements TuiCommand {

		@Override
		public boolean execute(Scanner arguments) {
			int tInt;
			try {
				if (arguments.hasNextInt()) {
					tInt = arguments.nextInt();
					System.out.printf("Create timeout switchable socket, socket index: %d%n", tInt);
					controller.createTimeoutSwitchableSocket(tInt);
				} else {
					String tStr = arguments.next();
					tInt = arguments.nextInt();
					System.out.printf("Create timeout switchable socket, socket host: %s, port: %d%n", tStr, tInt);
					controller.createTimeoutSwitchableSocket(tStr, tInt);
				}
			} catch (IOException ex) {
				ex.getMessage();
			}
			return true;
		}

		@Override
		public void printDescription() {
			System.out.println("Create timeout switchable socket");
		}
	}
	
	private class ReadCommand implements TuiCommand {

		@Override
		public boolean execute(Scanner arguments) {
			int tInt;
			try {
				tInt = arguments.nextInt();
				System.out.printf("read %d bytes...%n", tInt);
				String tStr = controller.readString(tInt);
				System.out.println(tStr);
			} catch (IOException ex) {
				ex.getMessage();
			}
			return true;
		}

		@Override
		public void printDescription() {
			System.out.println("read bytes");
		}
	}
	
	private class WriteCommand implements TuiCommand {

		@Override
		public boolean execute(Scanner arguments) {
			try {
				String tStr = arguments.next();
				System.out.printf("write string, %d bytes...%n", tStr.length());
				controller.writeString(tStr);
			} catch (IOException ex) {
				ex.getMessage();
			}
			return true;
		}

		@Override
		public void printDescription() {
			System.out.println("write bytes");
		}
	}
	
	private class ServerStartCommand implements TuiCommand {

		@Override
		public boolean execute(Scanner arguments) {
			try {
				System.out.printf("Starting server...%n");
				controller.startAcceptConnections();
				System.out.printf("Accepting new connections on port: %d%n", Controller.SERVERPORT);
			} catch (IOException ex) {
				ex.getMessage();
			}
			return true;
		}

		@Override
		public void printDescription() {
			System.out.println("start server");
		}
	}
	
	private class SwitchSocketCommand implements TuiCommand {

		@Override
		public boolean execute(Scanner arguments) {
			try {
				int tInt;
				if (arguments.hasNextInt()) {
					tInt = arguments.nextInt();
					System.out.printf("Switch to socket index: %d%n", tInt);
					controller.switchSocket(tInt);
				} else {
					String tStr = arguments.next();
					tInt = arguments.nextInt();
					System.out.printf("switch to socket host: %s, port: %d%n", tStr, tInt);
					controller.openAndSwitchSocket(tStr, tInt);
				}
			} catch (IOException | SwitchableException | TimeoutException | InterruptedException ex) {
				ex.getMessage();
			}
			return true;
		}

		@Override
		public void printDescription() {
			System.out.println("socket switch");
		}
	}
	
	private class NewConnectionCommand implements TuiCommand {

		@Override
		public boolean execute(Scanner arguments) {
			try {
				String tStr = arguments.next();
				int tInt = arguments.nextInt();
				System.out.printf("open new socket host: %s, port: %d%n", tStr, tInt);
				controller.openNewConnection(tStr, tInt);
			} catch (IOException ex) {
				ex.getMessage();
			}
			return true;
		}

		@Override
		public void printDescription() {
			System.out.println("open new connection");
		}
	}
	
	private class PrintOpenConnectionsCommand implements TuiCommand {

		@Override
		public boolean execute(Scanner arguments) {
			System.out.println("Open and switchable connections:");
			System.out.println(controller.getOpenedConnectionsList());
			return true;
		}

		@Override
		public void printDescription() {
			System.out.println("print connoctions can switch to");
		}
	}

}
