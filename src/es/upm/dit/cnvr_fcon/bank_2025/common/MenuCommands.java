package es.upm.dit.cnvr_fcon.bank_2025.common;

import es.upm.dit.cnvr_fcon.bank_2025.interfaces.MenuEnum;

/**
 * This class provides a command that encapsulates the information in 
 * a client invoking
* @author aalonso
* @since 2025/10/17
*/

public class MenuCommands {

	private MenuEnum operation;
	private Client       client;
	private boolean      exit;
		
	/**
	 * Create a command for managing the information of a client
	 * in a bank
	 * @param operation the operation 
	 * @param client the client information 
	 * @param exit true if the process has to finish
	 */
	public MenuCommands(MenuEnum operation, Client client, boolean exit) {
		this.operation = operation;
		this.client    = client;
		this.exit      = exit;
	}

	/**
	 * Create a command for the get and reset operations 
	 * @param operation the operation 
	 * @param exit true if the process has to finish
	 */
	public MenuEnum getOperation() {
		return operation;
	}

	public void setOperation(MenuEnum operation) {
		this.operation = operation;
	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public boolean isExit() {
		return exit;
	}

	public void setExit(boolean exit) {
		this.exit = exit;
	}

	@Override
	public String toString() {
		return "Command [operation=" + operation + ", client=" + client + ", exit=" + exit + "]";
	}
	
	
}
