package es.upm.dit.cnvr_fcon.bank_2025.common;

import es.upm.dit.cnvr_fcon.bank_2025.interfaces.*;

/**
*
* This class receives operations to update the bank  data base and 
* broadcasts them to the group members.
* 
* @author aalonso
* @since 2025/10/17
*/
public class ServicesBank implements ServiceInterface{

	private SendMessages sender;
	private ClientDB    clientDB; //anadido
	private String   localName;
	
	/** The constructor
	 * @param sender an object for sending messages
	 * @param localName the process identifier
	 */
	public ServicesBank (SendMessages sender, ClientDB clientDB, String localName) {
		this.sender    = sender;
		this.clientDB  = clientDB;
		this.localName = localName;
	}
	
	

	/**
	 * A client object is created and stored in the database
	 * @param client The client object
	 */
	@Override
	public Client put (Client client) {
		
		// TO BE DONE
		// Create an operation and multicast it
		//return null;
		if (client == null) {
            return null;
        }
		        // Crear la operación y difundirla
        OperationsBank op = new OperationsBank(
                ServicesEnum.CREATE_CLIENT,
                client,
                localName
        );
        sender.sendMessage(op);

        // Opcional: devolver el propio cliente
        return client;
	}
	
	/**
	 * Get the information of a client
	 * @param accNumber the account number of the client
	 */
	public Integer get (Integer accNumber) {
		
		// TO BE DONE
		// Create an operation and multicast it
		//return null;
		if (accNumber == null) {
            return null;
        }

		// Crear la operación serializable
        OperationsBank op = new OperationsBank(
                ServicesEnum.READ_CLIENT,
                accNumber,
                localName
        );

		// Enviar la operación a los nodos del grupo
        sender.sendMessage(op);

        // Leemos de la BD local el saldo (o null si no existe)
        Client c = clientDB.readClient(accNumber);
        return (c != null) ? c.getBalance() : null;

	}

	/**
	 * Remove a client
	 * @param accNumber the account number of the client
	 */
	public Integer remove(Integer accNumber) {
		
		// TO BE DONE
		// Create an operation and multicast it
		//return null;
		if (accNumber == null) {
            return null;
        }

        OperationsBank op = new OperationsBank(
                ServicesEnum.DELETE_CLIENT,
                accNumber,
                localName
        );
        sender.sendMessage(op);

        // Devolvemos el número de cuenta solo como confirmación
        return accNumber;
	}

	/**
	 * Update the balance of a client
	 * @param accNumber the account number of the client
	 * @param balance the balance
	 */
	@Override
	public Integer update(Integer accNumber, Integer balance) {
		
		// TO BE DONE
		// Create an operation and multicast it
		// return null;
		if (accNumber == null || balance == null) {
            return null;
        }

        OperationsBank op = new OperationsBank(
                ServicesEnum.UPDATE_CLIENT,
                accNumber,
                balance,
                localName
        );
        sender.sendMessage(op);

        // Devolvemos el nuevo saldo como valor de retorno
        return balance;
	}
	@Override
	public String toString() {
		return clientDB.toString();
	}
}
