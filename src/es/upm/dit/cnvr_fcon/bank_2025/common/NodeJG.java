package es.upm.dit.cnvr_fcon.bank_2025.common;

import org.jgroups.Message;
import org.jgroups.JChannel;
import org.jgroups.Receiver;
import org.jgroups.Address;
import org.jgroups.View;
import org.jgroups.util.Util;

import es.upm.dit.cnvr_fcon.bank_2025.bank.MainBank;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * This class manages the counter and connects in the cluster group.
 * It receives and processes multicast messages. For this purpose, it implements 
 * the org.jgroups.Receiver interface 
 * 
 * Purpose: Cluster management and message processing

    Joins the process to the JGroups cluster
    Creates necessary objects
    Broadcasts service invocations from the menu
    Processes received messages
    Implements: org.jgroups.Receiver interface
 
 * 
 * @author aalonso
 * @since 2025/11/12 
 */
public class NodeJG implements Receiver{


	// private static final long serialVersionUID = 1L;

	// Create a JGroups node
	private JChannel channel; 
	private String user_name=System.getProperty("user.name", "n/a"); 
	public static Logger LOGGER = Logger.getLogger(MainBank.class.getName());

	//private int key;
	private Address        addr;
	private String         localName;
	private SendMessages sender;
	private ServicesBank services;
	// Create a database
	final ClientDB         stateDB = new ClientDB();
	//final ClientDB stateDB = new ClientDB(new Client(13, "13", 13));

	//	final List<String> state = new LinkedList<>();

	private ProcessMsgBank  processMsg;

	/**
	 * The constructor. It adds the process to the group, by connecting to a 
	 * channel. It creates all the objects for managing the distributed counter. 
	 * Initially, it gets the system counter state. 
	 * @param cluster the name of the group cluster
	 */
	public NodeJG(String cluster) {
		// IN this case, the channel has to be created. It is only be used
		// for having information from remote nodes
		try {

			// TO BE DONE MIRARRR
        // 1. Crear el canal JGroups (puede ser con config XML )
        // Si no, el por defecto:
        channel = new JChannel();

        // 2. Este objeto (NodeJG) será el receptor de mensajes y de estado
        channel.setReceiver(this);

        // 3. Conectarse al cluster (nombre que viene por parámetro)
        channel.connect(cluster);

        // 4. Guardar dirección local (opcional pero útil para logs)
        addr = channel.getAddress();
        localName = addr.toString();
        LOGGER.info("Joined cluster " + cluster + " with address " + localName);

        // 5. Crear el objeto que envía mensajes
        sender = new SendMessages(channel);

        // 6. Crear la capa de servicios del banco
		services = new ServicesBank(sender, stateDB, localName);

        // 7. Crear el procesador de mensajes entrantes
		processMsg = new ProcessMsgBank(stateDB, localName);



			
		} catch (Exception e) {
			System.out.println("Error to create the JGroups channel");
			e.printStackTrace();
			System.out.println(e);
		}

		try {
			// Configure the state when working the rest system
			//this.channel.getState(null, 10000);
		    // 8. Pedir el estado actual al cluster (si ya hay algún nodo)
             channel.getState(null, 10000);
		} catch (Exception e) {
			System.out.println("Error get the State");
			e.printStackTrace();
			System.out.println(e);
		}

	}

	/**
	 * This method implements in the org.jgroups.receiver interface
	 * Called when a change in membership has occurred. (JGroups)
	 * View - the received view
	 */
	public void viewAccepted(View new_view) {
		System.out.println("** view: " + new_view);
	}

	/**
	 * This method implements in the org.jgroups.receiver interface
	 * Called when a message is received (JGroups).
	 * msg - the message received
	 */
	public void receive(Message msg) {
		try {
		//Sacar el objeto del Message. 
		LOGGER.fine("Message received from " + msg.getSrc());

        Object obj = msg.getObject();

        // Aseguramos que el mensaje es del tipo esperado
        if (!(obj instanceof OperationsBank)) {
            LOGGER.severe("Received object is not an OperationsBank: ");
            return;
        }
		//Hacer cast a OperationsBank.
        OperationsBank op = (OperationsBank) obj;
		
		
		// usar synchronized para acceso a la BD compartida
        synchronized (stateDB) {
            // Procesar la operación sobre la BD
            Client result = processMsg.processOpn(op);

            // Opcional: logs para depuración
            if (result != null) {
                LOGGER.fine("Operation " + op.getOperation() + " processed for client: " + result);
            } else {
                LOGGER.fine("Operation " + op.getOperation() + " could not be processed");
            }
        }

    } catch (Exception e) {
        System.out.println("Error processing received message");
        e.printStackTrace();
        System.out.println(e);
    }
  }
		

	/**
	 * This method implements in the org.jgroups.receiver interface
	 * Allows an application to write a state through a provided OutputStream. 
	 * After the state has been written the OutputStream doesn't need to 
	 * be closed as stream closing is automatically done when a calling thread 
	 * returns from this callback (JGroups).
	 * @param output - the OutputStream
	 */
	public void getState(OutputStream output) throws Exception {
		LOGGER.finest("Invocation to getState");
		synchronized(stateDB) {
			try {
				// Configure the state when working the rest system    		
			   // Enviar el estado completo de la BD de clientes al nodo que lo pide
                Util.objectToStream(stateDB, new DataOutputStream(output));
			} catch (Exception e) {
				System.err.println(e);
				e.printStackTrace();
			}
		}
	}

	/**
	 * This method implements in the org.jgroups.receiver interface
	 * Allows an application to read a state through a provided InputStream. 
	 * After the state has been read the InputStream doesn't need to be 
	 * closed as stream closing is automatically done when a calling 
	 * thread returns from this callback (JGroups).
	 * 
	 *  @param input - the InputStream
	 */
	public void setState(InputStream input) throws Exception {
		LOGGER.finest("Invocation to setState");
		//List<String> list=Util.objectFromStream(new DataInputStream(input));
		ClientDB clientDBinput = Util.objectFromStream(new DataInputStream(input));
		synchronized(stateDB) {
			try {
				// Configure the state when working the rest system		
				// TO BE DONE
				// Copiar el estado recibido en nuestra BD local
                stateDB.createBank(clientDBinput);

			} catch (Exception e) {
				System.out.println("Error to set the state");
				e.printStackTrace();
				System.out.println(e);
			}
		}
		//System.out.println("received state (" + list.size() + " messages in chat history):");
		//list.forEach(System.out::println);

		System.out.println("received state (" + " messages in chat history):");
	}

	/**
	 * Close the channel
	 */
	public void close() {
		channel.close();
	}

	// The MainCounter requires this object
	/** 
	 * This method returns the service object in this class. It is used by
	 * MainCounter for invoking the system counter, from the client menu.
	 * @return Counter system services.
	 */
	public ServicesBank getServices() {
		return this.services;
	}

	
	/**
	 * Get a String with the contents of the DB
	 * @return the content of the DB
	 */
	public String clientDBString() {
		return stateDB.toString();
	}

}
