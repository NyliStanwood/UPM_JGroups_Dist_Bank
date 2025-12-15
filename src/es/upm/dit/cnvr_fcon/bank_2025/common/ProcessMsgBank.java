package es.upm.dit.cnvr_fcon.bank_2025.common;


import es.upm.dit.cnvr_fcon.bank_2025.bank.MainBank;

/**
 *
 * Receives an operation (OperationBank) an processes it
 *
 * @author aalonso
 * @since 2022/10/17
 */

public class ProcessMsgBank {

	private java.util.logging.Logger LOGGER = MainBank.LOGGER;

	private ClientDB clientDB;
	private String   localName;
	/**
	 * The constructor
	 * @param clientDB the database to be processed
	 */
	public ProcessMsgBank(ClientDB clientDB, String localName) {
		this.clientDB  = clientDB;
		this.localName = localName;
	}

	/**
	 * Operation object for processing a service
	 * @param operation the operation.
	 * @return the client that been processed. null if the operation 
	 * was not feasible
	 */
	public Client processOpn(OperationsBank operation) {
		Client client;
		//ConvertsByte.ByteToOperation(data);

		if (operation == null) {
			return null;		
		}

		try {
			switch (operation.getOperation()) {
			//case CREATE_CLIENT: // Put
				// To be done

				//case XXXX:
				// Process all the operations
			case CREATE_CLIENT:  // Put
            client = operation.getClient();
            if (client == null) {
                LOGGER.warning(localName + " -> CREATE_CLIENT: client is null");
                return null;
            }

            if (clientDB.createClient(client)) {
                LOGGER.info(localName + " -> Client created: " + client);
                return client;
            } else {
                LOGGER.warning(localName + " -> CREATE_CLIENT failed, client already exists: " + client);
                return null;
            }	
			
			
			
			case READ_CLIENT:
            client = clientDB.readClient(operation.getAccountNumber());
            LOGGER.info(localName + " -> READ_CLIENT " + operation.getAccountNumber()
                        + " => " + client);
            return client;
		    

			case UPDATE_CLIENT:
            client = clientDB.updateClient(
                        operation.getAccountNumber(),
                        operation.getBalance()
                     );
            LOGGER.info(localName + " -> UPDATE_CLIENT account=" 
                        + operation.getAccountNumber()
                        + " newBalance=" + operation.getBalance()
                        + " => " + client);
            return client;

            
			case DELETE_CLIENT:
            client = clientDB.deleteClient(operation.getAccountNumber());
            LOGGER.info(localName + " -> DELETE_CLIENT account=" 
                        + operation.getAccountNumber()
                        + " => " + client);
            return client;
			        case CREATE_BANK:   // No lo usamos de verdad, solo para que el switch esté completo
            LOGGER.warning(localName + " -> CREATE_BANK received (not implemented)");
            return null;

            default:
            LOGGER.warning(localName + " -> Unknown operation: " + operation.getOperation());
            return null;

					} 
	    } catch (Exception E) {
			LOGGER.severe("Unexpected expcetion");
			E.printStackTrace();
			return null;
		}
	}


	public java.util.HashMap <Integer, Client> getClientDB() {
		return this.clientDB.getClientDB();
	}

}
