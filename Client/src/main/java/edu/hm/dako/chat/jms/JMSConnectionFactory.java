package edu.hm.dako.chat.jms;


import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.hm.dako.chat.connection.Connection;
import edu.hm.dako.chat.connection.ConnectionFactory;

/**
 * JMS ConnectionFactory Klasse.
 * 
 * @author wir 
 *
 */



public class JMSConnectionFactory implements ConnectionFactory{

	private static Log log = LogFactory.getLog(JMSConnectionFactory.class);

	// Maximale Anzahl an Verbindungsaufbauversuchen zum Server, die ein Client
	// unternimmt, bevor er abbricht
	private static final int MAX_CONNECTION_ATTEMPTS = 50;

	// Zaehlt die Verbindungsaufbauversuche, bis eine Verbindung vom Server
	// angenommen wird
	private long connectionTryCounter = 0;

	/**
	 * Baut eine Verbindung zum Server auf. Der Verbindungsaufbau wird mehrmals
	 * versucht.
	 */
	public Connection connectToServer(String remoteServerAddress, int serverPort,
			int localPort, int sendBufferSize, int receiveBufferSize) throws IOException {

		JMSConnection connection = null;
		boolean connected = false;

		// Es wird "localhost" fuer die lokale IP-Adresse verwendet
		InetAddress localAddress = null;

		int attempts = 0;
		while ((!connected) && (attempts < MAX_CONNECTION_ATTEMPTS)) {
			try {

				connectionTryCounter++;

				connection = new JMSConnection(remoteServerAddress, serverPort);
				connected = true;


			} catch (Exception e) {

				log.error("IOException beim Verbindungsaufbau: " + e.getMessage());

				// Ein wenig warten und erneut versuchen
				attempts++;
				try {
					Thread.sleep(100);
				} catch (Exception e2) {
				}

			}
			if (attempts >= MAX_CONNECTION_ATTEMPTS) {
				throw new IOException();
			}
		}

		log.debug("Anzahl der Verbindungsaufbauversuche fuer die Verbindung zum Server: "
				+ connectionTryCounter);
		return connection;
	}

}
