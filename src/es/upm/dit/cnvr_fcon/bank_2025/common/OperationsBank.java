package es.upm.dit.cnvr_fcon.bank_2025.common;

import es.upm.dit.cnvr_fcon.bank_2025.interfaces.*;

import java.io.Serializable;

/**
 * This class allows to encapsulate the operation and parameters 
 * of an object.
 * It is intended for multicast the operation.
 * @author aalonso
 * @since 2025/11/12
 */
public class OperationsBank implements Serializable {

	private static final long serialVersionUID = 1L;
	
	
	/**
	 * The identifier of the operation
	 */
	private ServicesEnum  operation  = null;

	/**
	 * Information of a client 
	 */
	private Client        client     = null;
	/**
	 * Account number of the client
	 */
	private Integer       accountNumber        = null;
	/**
	 * Balance of the client
	 */
	private Integer       balance    = null;
	
	/**
	 * Local name of the sending process. It is only required for 
	 * printing purposes.  
	 */
	private String        localName;
	
	// Create (Put)
	
	/**
	 * Encapsulate the information for the operation put
	 * @param operation The operation identifier
	 * @param client The client
	 * @param localName The name of the sending process
	 */
	public OperationsBank (ServicesEnum operation, Client client, String localName) {
		this.operation = operation;
		this.client    = client;
		this.localName = localName;
	}

	// Get, Delete
	/**
	 * Encapsulate the information for the operation get and remove
	 * @param operation The operation identifier
	 * @param accountNumber The account number of the client
	 * @param localName  The name of the sending process
	 */
	public OperationsBank (ServicesEnum operation, Integer accountNumber, String localName){
		this.operation     = operation;
		this.accountNumber = accountNumber;
		this.localName     = localName;
	}

	// Update

	/**
	 * Encapsulate the information for the operation update
	 * @param operation The operation identifier
	 * @param accountNumber The account number of the client
	 * @param balance The balance of the client
	 * @param localName The name of the sending process
	 */
	public OperationsBank (ServicesEnum operation, Integer accountNumber, Integer balance, String localName){
		this.operation      = operation;
		this.accountNumber  = accountNumber;
		this.balance        = balance;
		this.localName      = localName;

	}
	
	/**
	 * Get the operation
	 * @return the operation
	 */
	public ServicesEnum getOperation() {
		return this.operation;
	}

	/**
	 * Get the client object
	 * @return the client
	 */
	public Client getClient() {
		return this.client;
	}

	/**
	 * Get the account number
	 * @return the account number
	 */
	public Integer getAccountNumber(){
		return this.accountNumber;
	}

	/**
	 * Get the balance
	 * @return the balance
	 */
	public Integer getBalance() {
		return this.balance;
	}

	/**
	 * Get the local name
	 * @return the local name
	 */
	public String getLocalName() {
		return this.localName;
	}
	
	public String toString() {
		if (client != null) {
			return "Operation: " + this.operation + client.toString();
		} else {
			return "Operation: " + this.operation + " " + this.accountNumber + " " + this.balance;			
		}
	}

}


