package es.upm.dit.cnvr_fcon.bank_2025.bank;

import es.upm.dit.cnvr_fcon.bank_2025.common.*;
import es.upm.dit.cnvr_fcon.bank_2025.interfaces.*;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the main in the application. The code that is executed
 * for providing the application.
 * 
 * Its main duties is to create a ManagerBank. It also provides the API
 * for clients
 *
 * @author aalonso
 * @since 20251017
 */
public class MainBank {

	private Random random = new Random();
	private String cluster;
	private NodeJG nodeJG;
	private ServiceInterface services;

	static {
		System.setProperty("java.util.logging.SimpleFormatter.format",
				"[%1$tF %1$tT][%4$-7s] [%5$s] [%2$-7s] %n");

		// "[%1$tF %1$tT] [%2$-7s] %3$s %n");
		// "[%1$tF %1$tT] [%4$-7s] %5$s %n");
		// "%4$s: %5$s [%1$tc]%n");
		// "%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS %1$Tp %2$s%n%4$s: %5$s%n");
	}

	public static Logger LOGGER = Logger.getLogger(MainBank.class.getName());

	public MainBank(String cluster) {
		configureLogger1();
		this.cluster = cluster;
		this.nodeJG = new NodeJG(this.cluster);
		this.services = nodeJG.getServices();
	}

	//////////////////////////////////////////////////////////////////////////

	// Three options are provided for configuring the logs.
	// Select the best option. Ensure that the paths are valid

	private void configureLogger() {
		System.setProperty("java.util.logging.config.file", "/Users/aalonso/Desktop/logging.properties");
		LOGGER = Logger.getLogger(MainBank.class.getName());
		LOGGER.setLevel(Level.FINEST); // FINEST
	}

	private void configureLogger1() {
		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.FINEST); // FINEST
		LOGGER.addHandler(handler);
		LOGGER.setLevel(Level.FINEST); // FINEST
	}

	private void configureFile() {
		try {
			ConsoleHandler handler = new ConsoleHandler();
			FileHandler fileHandler = new FileHandler("/Users/aalonso/log/zk.log");
			handler.setLevel(Level.INFO);
			LOGGER.addHandler(handler);
			LOGGER.addHandler(fileHandler);
			LOGGER.setLevel(Level.INFO);

		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//////////////////////////////////////////////////////////////////////////

	private void initMembers() {

		// if (!dht.containsKey("Angel")) {
		this.services.put(new Client(1, "Angel", 10));
		// }
		// if (!dht.containsKey("Bernardo")) {
		this.services.put(new Client(2, "Bernardo", 20));
		// }
		// if (!dht.containsKey("Carlos")) {
		this.services.put(new Client(3, "Carlos", 30));
		// }
		// if (!dht.containsKey("Daniel")) {
		this.services.put(new Client(4, "Daniel", 40));
		// }
		// if (!dht.containsKey("Eugenio")) {
		this.services.put(new Client(5, "Eugenio", 50));
		// }
		// if (!dht.containsKey("Zamorano")) {
		this.services.put(new Client(6, "Zamorano", 60));
		// }
	}

	//////////////////////////////////////////////////////////////////////////

	private Client readClient(Scanner sc) {
		int accNumber = 0;
		String name = null;
		int balance = 0;

		System.out.print(">>> Enter account number (int) = ");
		if (sc.hasNextInt()) {
			accNumber = sc.nextInt();
		} else {
			System.out.println("The provised text provided is not an integer");
			sc.next();
			return null;
		}

		System.out.print(">>> Enter name (String) = ");
		name = sc.next();

		System.out.print(">>> Enter balance (int) = ");
		if (sc.hasNextInt()) {
			balance = sc.nextInt();
		} else {
			System.out.println("The provised text provided is not an integer");
			sc.next();
			return null;
		}
		return new Client(accNumber, name, balance);
	}

	//////////////////////////////////////////////////////////////////////////

	private void put(Client client) {
		this.services.put(client);
	}

	//////////////////////////////////////////////////////////////////////////

	private Integer get(int accNumber) {

		Integer value = this.services.get(accNumber);

		return value;

	}

	//////////////////////////////////////////////////////////////////////////

	private Integer remove(int accNumber) {

		Integer value = this.services.remove(accNumber);

		return value;

	}

	//////////////////////////////////////////////////////////////////////////

	private Integer update(int accNumber, int balance) {

		Integer value = this.services.update(accNumber, balance);

		return value;
	}

	//////////////////////////////////////////////////////////////////////////

	public void close() {
		this.nodeJG.close();
	}

	//////////////////////////////////////////////////////////////////////////

	public String toString() {
		return "List of values in the bank: \n"
				+ nodeJG.clientDBString();
	}

	//////////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {

		boolean correct = false;
		int menuKey = 0;
		boolean exit = false;
		Scanner sc = new Scanner(System.in);
		Client client;
		Integer value = 0;
		String cluster = "clusterBank";
		Menu menu = new Menu();

		MainBank mainBank = new MainBank(cluster);

		// TODO esto se hacía antes. Supongo que es para esperar a que esté el quorum
		// creado. No claro si es necesario en JGroups. 202510
		// If there is already a quorum
		// if (!bankManager.isCreated()) {
		// System.out.println("Bye. There is already a quorum");
		// return;
		// }

		int accNumber = 0;

		while (!exit) {
			try {

				MenuCommands command = menu.getCommand();

				switch (command.getOperation()) {
					case CREATE_CLIENT: // Put
						client = command.getClient();
						mainBank.put(client);
						break;

					case READ_CLIENT: // Get
						client = command.getClient();
						mainBank.get(client.getAccountNumber());
						break;
					case DELETE_CLIENT: // Remove
						client = command.getClient();
						mainBank.remove(client.getAccountNumber());
						break;
					case UPDATE_CLIENT: // Update
						client = command.getClient();
						mainBank.update(client.getAccountNumber(), client.getBalance());
						break;
					case TO_STRING:
						System.out.println(mainBank.toString());
						break;
					case INIT_DB:
						mainBank.initMembers();
						break;
					case EXIT:
						mainBank.close();
						exit = true;
						break;
					default:
						break;
				}
			} catch (Exception e) {
				LOGGER.severe("Exception at Main. Error read data");
				e.printStackTrace();
			}
		}
		sc.close();
	}
}