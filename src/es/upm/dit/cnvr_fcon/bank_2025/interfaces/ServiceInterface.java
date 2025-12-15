package es.upm.dit.cnvr_fcon.bank_2025.interfaces;

import es.upm.dit.cnvr_fcon.bank_2025.common.Client;

public interface ServiceInterface {

	public Client put (Client client);
	
	public Integer get (Integer accNumber);

	public Integer remove(Integer accNumber);

	public Integer update(Integer accNumber, Integer balance);

	//public Integer GetClientDB();

}