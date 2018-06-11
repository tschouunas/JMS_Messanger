package edu.hm.dako.chat.client;

import edu.hm.dako.chat.common.ExceptionHandler;
import edu.hm.dako.chat.common.SystemConstants;
import edu.hm.dako.chat.client.JMSMessageListenerThreadImpl;
import edu.hm.dako.chat.client.SimpleMessageListenerThreadImpl;

/**
 * <p/>
 * Verwaltet eine Verbindung zum Server.
 *
 * @author Mandl
 */
public class ClientImpl extends AbstractChatClient {

	/**
	 * Konstruktor
	 * 
	 * @param userInterface
	 *          Schnittstelle zum User-Interface
	 * @param serverPort
	 *          Portnummer des Servers
	 * @param remoteServerAddress
	 *          IP-Adresse/Hostname des Servers
	 */

	public ClientImpl(ClientUserInterface userInterface, int serverPort,
			String remoteServerAddress, String serverType) {

		super(userInterface, serverPort, remoteServerAddress, serverType);
		this.serverPort = serverPort;
		this.remoteServerAddress = remoteServerAddress;

		Thread.currentThread().setName("Client");
		threadName = Thread.currentThread().getName();

		try {
			// JMS Server erzeugen
			if (serverType.equals(SystemConstants.IMPL_JMS)) {
				messageListenerThread = new JMSMessageListenerThreadImpl(userInterface, connection, sharedClientData);
				
			// Simple TCP Server erzeugen
			} else {
				messageListenerThread = new SimpleMessageListenerThreadImpl(userInterface, connection,
						sharedClientData);
			}
			
			messageListenerThread.start();
		} catch (Exception e) {
			ExceptionHandler.logException(e);
		}
	}
}