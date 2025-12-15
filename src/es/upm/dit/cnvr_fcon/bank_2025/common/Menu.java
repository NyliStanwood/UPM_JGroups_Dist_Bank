package es.upm.dit.cnvr_fcon.bank_2025.common;

import java.util.Scanner;
import java.util.logging.Logger;
import es.upm.dit.cnvr_fcon.bank_2025.bank.MainBank;
import es.upm.dit.cnvr_fcon.bank_2025.interfaces.*;

/**
 * This class implements the client interface for invoking the counter services 
 * @author aalonso
 * @since 2025/10/17
*/

public class Menu {

	boolean correct = false;
	int     menuKey = 0;
	boolean exit    = false;
	Scanner sc      = new Scanner(System.in);
	Client  client;
	Integer value   = 0;
	
	static {
		System.setProperty("java.util.logging.SimpleFormatter.format",
				"[%1$tF %1$tT][%4$-7s] [%5$s] [%2$-7s] %n");

		//    		"[%1$tF %1$tT] [%2$-7s] %3$s %n");
		//           "[%1$tF %1$tT] [%4$-7s] %5$s %n");
		//   "%4$s: %5$s [%1$tc]%n");
		//    "%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS %1$Tp %2$s%n%4$s: %5$s%n");
	}

	public static Logger LOGGER = Logger.getLogger(MainBank.class.getName());
	
	public Menu() {
		
	}
	/**
	 * This methos waits for receiving an invoking from a client and encapsulates
	 * the related information.
	 * @return The command invoked
	 */
	private Client readClient(Scanner sc) {
		int accNumber = 0;
		String name   = null;
		int balance   = 0;

		System. out .print(">>> Enter account number (int) = ");
		if (sc.hasNextInt()) {
			accNumber = sc.nextInt();
		} else {
			System.out.println("The provised text provided is not an integer");
			sc.next();
			return null;
		}

		System. out .print(">>> Enter name (String) = ");
		name = sc.next();

		System. out .print(">>> Enter balance (int) = ");
		if (sc.hasNextInt()) {
			balance = sc.nextInt();
		} else {
			System.out.println("The provised text provided is not an integer");
			sc.next();
			return null;
		}
		return new Client(accNumber, name, balance);
	}
	
	public MenuCommands getCommand() {
		
		int accNumber = 0;
		
		try {
			correct = false;
			menuKey = 0;
			while (!correct) {
				System. out .println(">>> Enter option: 1) Put. 2) Get. 3) Remove. 4) Update  5) Values 7) Init 0) Exit");				
				if (sc.hasNextInt()) {
					menuKey = sc.nextInt();
					correct = true;
				} else {
					sc.next();
					System.out.println("The provised text provided is not an integer");
				}

			}
			
			// TODO esto se hacía antes. Supongo que es para esperar a que esté el quorum 
			// creado. No claro si es necesario en JGroups. 202510
			/*if (!bankManager.isQuorum()) {
				System.out.println("No hay quorum. No es posible ejecutar su elección");
				continue;
			}*/

			switch (menuKey) {
			case 1: // Put
				LOGGER.finest("Main: put");
				client = readClient(sc);
				return new MenuCommands(MenuEnum.CREATE_CLIENT, client, false);

			case 2: // Get
				System. out .print(">>> Enter account number (int) = ");
				if (sc.hasNextInt()) {
					accNumber = sc.nextInt();
					client = new Client(accNumber, null, -1);
					return new MenuCommands(MenuEnum.READ_CLIENT, client, false);
				} else {
					System.out.println("The provised text provided is not an integer");
					sc.next();
				}									
				break;
			case 3: // Remove
				System. out .print(">>> Enter account number (int) = ");
				if (sc.hasNextInt()) {
					accNumber = sc.nextInt();
					client = new Client(accNumber, null, -1);
					return new MenuCommands(MenuEnum.DELETE_CLIENT, client, false);
				} else {
					System.out.println("The provised text provided is not an integer");
					sc.next();
				}									
				break;
			case 4: // Update
				int balance = 0;
				System. out .print(">>> Enter account number (int) = ");
				if (sc.hasNextInt()) {
					accNumber = sc.nextInt();
				} else {
					System.out.println("The provised text provided is not an integer");
					sc.next();
				}
				System. out .print(">>> Enter balance (int) = ");
				if (sc.hasNextInt()) {
					balance = sc.nextInt();
				} else {
					System.out.println("The provised text provided is not an integer");
					sc.next();
				}
				client = new Client(accNumber, null, balance);
				return new MenuCommands(MenuEnum.UPDATE_CLIENT, client, false);
			case 5:
				//System.out.println("List of values in the bank:");
				client = new Client (-1, null, -1);
				return new MenuCommands(MenuEnum.TO_STRING, client, false);
			case 6:
				System.out.println("The option is not available");
				break;
			case 7:
				client = new Client (-1, null, -1);
				return new MenuCommands(MenuEnum.INIT_DB, client, false);
			case 0:
				exit = true;
				sc.close();
				return new MenuCommands(MenuEnum.EXIT, null, true);
			/*default:
				break;*/
			}
		} catch (Exception e) {
			LOGGER.severe("Exception at Main. Error read data");
			e.printStackTrace();
		}
		// If fail, notify program
		return new MenuCommands(MenuEnum.TO_STRING, null, true);
	}
	
}
