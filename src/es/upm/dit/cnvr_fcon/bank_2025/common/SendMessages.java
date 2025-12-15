package es.upm.dit.cnvr_fcon.bank_2025.common;

import org.jgroups.*;
import org.jgroups.ObjectMessage;

/**
 * This class sends messages to the cluster group.
 * @author aalonso
 * @since 2025/10/17
 */
public class SendMessages{

	private JChannel channel;
	
	/**
	 * The constructor
	 * @param channel the JGroup channel for sending messages
	 */
	public SendMessages (JChannel channel) {
		this.channel = channel;		
	}
	
	/**
	 * Multicast an operation
	 * @param operation the operation 
	 */
	public void sendMessage(OperationsBank operation) {
		    try {
		// TO BE DONE
        // Mensaje multicast (destino null = todos los nodos)
        Message msg = new ObjectMessage(null, operation);

        channel.send(msg);

        System.out.println(">> Sent operation: " + operation);

    } catch (Exception e) {
        System.out.println("Error sending operation: " + operation);
        e.printStackTrace();
    }
	}
}

