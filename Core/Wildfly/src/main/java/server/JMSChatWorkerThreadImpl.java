package server;

import java.util.Vector;
import java.util.logging.Level;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import JPA.CountDB;
import JPA.TraceDB;
import common.ChatPDU;
import common.ClientConversationStatus;
import common.ClientListEntry;
import common.ExceptionHandler;
import connection.Connection;
import connection.ConnectionTimeoutException;
import connection.EndOfFileException;

/**
 * Worker-Thread zur serverseitigen Bedienung einer Session mit einem Client.
 * Jedem Chat-Client wird serverseitig ein Worker-Thread zugeordnet.
 * 
 * @author wir
 *
 */
public class JMSChatWorkerThreadImpl extends AbstractWorkerThread {
	
	@Inject
	private CountDB countManager;
	
	@Inject
	private TraceDB traceManager;

	private static Log log = LogFactory.getLog(JMSChatWorkerThreadImpl.class);

	public JMSChatWorkerThreadImpl(Connection con, SharedChatClientList clients, SharedServerCounter counter) {

		super(con, clients, counter);
	}

	private JMSMessageBean bean;

	public JMSChatWorkerThreadImpl(JMSMessageBean that) {
		super();

		// set reference to calling bean
		this.bean = that;
	}

	
	@Override
	protected void chatMessageRequestAction(ChatPDU receivedPdu) {
		userName = receivedPdu.getUserName();
		// for benchmarking
		ClientListEntry client = clients.getClient(userName);
		clients.setRequestStartTime(userName, startTime);
		clients.incrNumberOfReceivedChatMessages(userName);
		log.info("Chat-Message-Request-PDU von " + userName + " mit Sequenznummer "
				+ receivedPdu.getSequenceNumber() + " empfangen");

		// Chat-PDU an alle Clients senden
		if (!clients.existsClient(userName)) {
			log.warn("User nicht in Clientliste: " + userName);
		} else {

			ChatPDU pdu = ChatPDU.createChatMessageEventPdu(userName, receivedPdu);


			try {
				bean.sendToAllClients(pdu);
				receivedPdu.setServerThreadName(Thread.currentThread().getName());

				log.info("Chat-Event-PDU gesendet");
				clients.incrNumberOfSentChatEvents(userName);
				log.info(userName + ": Anzahl gesendeter ChatMessages von dem Client = "
						+ receivedPdu.getSequenceNumber());
				
			} catch (Exception e) {
				log.warn("Datenbankverbindung konnte nicht geschlossen werden");
			} 
		}

		// Response-PDU an Client senden
		if (client != null) {
			ChatPDU responsePdu = ChatPDU.createChatMessageResponsePdu(userName, 0, 0, 0, 0,
					client.getNumberOfReceivedChatMessages(), receivedPdu.getClientThreadName(),
					(System.nanoTime() - client.getStartTime()));

			if (responsePdu.getServerTime() / 1000000 > 100) {
				log.info(Thread.currentThread().getName()
						+ ", Benoetigte Serverzeit vor dem Senden der Response-Nachricht > 100 ms: "
						+ responsePdu.getServerTime() + " ns = " + responsePdu.getServerTime() / 1000000 + " ms");
			}

			try {
				bean.sendToClient(responsePdu, userName);
				log.info("Chat-Message-Response-PDU an " + userName + " gesendet");
			} catch (Exception e) {
				log.warn("Senden einer Chat-Message-Response-PDU an " + userName + " nicht moeglich");
			}
		}
		log.info("Aktuelle Laenge der Clientliste: " + clients.size());
	}

	/**
	 * Verbindung zu einem Client ordentlich abbauen
	 */
	private void closeConnection() {

		log.debug("Schliessen der Chat-Connection zum " + userName);

		// Bereinigen der Clientliste falls erforderlich

		if (clients.existsClient(userName)) {
			log.debug("Close Connection fuer " + userName
					+ ", Laenge der Clientliste vor dem bedingungslosen Loeschen: " + clients.size());

			clients.deleteClientWithoutCondition(userName);
			log.debug(
					"Laenge der Clientliste nach dem bedingungslosen Loeschen von " + userName + ": " + clients.size());
		}

		try {
			connection.close();
		} catch (Exception e) {
			log.debug("Exception bei close");
			// ExceptionHandler.logException(e);
		}
	}



	@Override
	protected void handleIncomingMessage() throws Exception {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Methode loginRequestAction in RestService.java
	 */
	@Override
	protected void loginRequestAction(ChatPDU receivedPdu) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Methode logoutRequestAction in RestService.java
	 */
	@Override
	protected void logoutRequestAction(ChatPDU receivedPdu) {
		// TODO Auto-generated method stub
		
	}

}
