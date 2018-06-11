package rest;

import java.util.Vector;

import javax.annotation.Resource;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.Topic;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.Gson;

import common.ChatPDU;
import common.ClientConversationStatus;
import common.ClientListEntry;
import common.ExceptionHandler;
import common.PduType;
import server.JMSChatWorkerThreadImpl;
import server.SharedChatClientList;

/**
 * 
 * This class produces a RESTful service to login/logout user from the chat
 * application.
 * 
 * @autor wir
 */
@Path("/users")
@RequestScoped
public class RestService {

	@Resource(mappedName = "java:jboss/exported/jms/topic/ChatTopic")
	private Topic topic;

	@Resource(mappedName = "java:/JmsXA")
	private ConnectionFactory connectionFactory;

	// JMS properties
	private SharedChatClientList clients = null;
	private String clientThreadName = null;
	private JMSContext jmsContext = null;
	
	String userName = System.getProperty("username", "tester");
	String password = System.getProperty("password", "tester");

	Gson gson = new Gson();

	private static Log log = LogFactory.getLog(RestService.class);

	@POST
	@Path("/login")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response login(String message) {
		// if not, add him and return ok response
		 jmsContext = connectionFactory.createContext(userName, password);
		
		 Gson gson = new Gson();
	     ChatPDU pduIn = gson.fromJson(message, ChatPDU.class);
	     
	     loginRequestAction(pduIn);

		return Response.status(Status.OK).entity(Status.OK.getReasonPhrase()).build();
	}

	@POST
	@Path("logout/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response logout(String message) {

		jmsContext = connectionFactory.createContext(userName, password);
		
		 Gson gson = new Gson();
	     ChatPDU pduIn = gson.fromJson(message, ChatPDU.class);
	     
	     logoutRequestAction(pduIn);

		return Response.status(Status.OK).entity(Status.OK.getReasonPhrase()).build();
	}

	protected void sendLoginListUpdateEvent(ChatPDU pdu) {

		// Liste der eingeloggten bzw. sich einloggenden User ermitteln

		Vector<String> clientList = clients.getRegisteredClientNameList();

		log.info("Aktuelle Clientliste, die an die Clients uebertragen wird: " + clientList);

		pdu.setClients(clientList);

		Vector<String> clientList2 = clients.getClientNameList();
		for (String s : new Vector<String>(clientList2)) {
			log.debug("Fuer " + s + " wird Login- oder Logout-Event-PDU an alle aktiven Clients gesendet");

			ClientListEntry client = clients.getClient(s);
			try {
				if (client != null) {

					send2Topic(pdu);

					log.debug("Login- oder Logout-Event-PDU an " + client.getUserName() + " gesendet");
					clients.incrNumberOfSentChatEvents(client.getUserName());
					
				}
			} catch (Exception e) {
				log.error("Senden einer Login- oder Logout-Event-PDU an " + s + " nicht moeglich");
				ExceptionHandler.logException(e);
			}
		}

	}

	/**
	 * Antwort-PDU fuer den initiierenden Client aufbauen und senden
	 * 
	 * @param eventInitiatorClient
	 *            Name des Clients
	 */
	private void sendLogoutResponse(String eventInitiatorClient) {

		ClientListEntry client = clients.getClient(eventInitiatorClient);

		if (client != null) {
			ChatPDU responsePdu = ChatPDU.createLogoutResponsePdu(eventInitiatorClient, 0, 0, 0, 0,
					client.getNumberOfReceivedChatMessages(), clientThreadName);

			log.debug(eventInitiatorClient + ": SentEvents aus Clientliste: " + client.getNumberOfSentEvents()
					+ ": ReceivedConfirms aus Clientliste: " + client.getNumberOfReceivedEventConfirms());
			try {
				send2Topic(responsePdu);
			} catch (Exception e) {
				log.debug("Senden einer Logout-Response-PDU an " + eventInitiatorClient + " fehlgeschlagen");
				log.debug("Exception Message: " + e.getMessage());
			}

			log.debug("Logout-Response-PDU an Client " + eventInitiatorClient + " gesendet");
		}
	}

	public void loginRequestAction(ChatPDU receivedPdu) {
		System.out.println("Ich komme in den Login Request Case");
		ChatPDU pdu;
		System.out.println("Login-Request-PDU fuer " + receivedPdu.getUserName() + " empfangen");

		if (clients == null)
			clients = SharedChatClientList.getInstance();
		// Neuer Client moechte sich einloggen, Client in Client-Liste
		// eintragen
		if (!clients.existsClient(receivedPdu.getUserName())) {

			log.info("User nicht in Clientliste: " + receivedPdu.getUserName());
			ClientListEntry client = new ClientListEntry(receivedPdu.getUserName());
			client.setLoginTime(System.nanoTime());
			clients.createClient(receivedPdu.getUserName(), client);
			clients.changeClientStatus(receivedPdu.getUserName(), ClientConversationStatus.REGISTERING);
			log.info("User " + receivedPdu.getUserName() + " nun in Clientliste");

			userName = receivedPdu.getUserName();
			clientThreadName = receivedPdu.getClientThreadName();
			Thread.currentThread().setName(receivedPdu.getUserName());
			log.info("Laenge der Clientliste: " + clients.size());

			// Login-Event an alle Clients (auch an den gerade aktuell
			// anfragenden) senden

			Vector<String> clientList = clients.getClientNameList();
			pdu = ChatPDU.createLoginEventPdu(userName, clientList, receivedPdu);
			log.info("Login Event PDU wird gesendet: ");
			log.info("Login Event PDU PDUType: " + pdu.getPduType());
			log.info("Login Event PDU Userliste: " + pdu.getClients());
			sendLoginListUpdateEvent(pdu);

			// Login Response senden
			ChatPDU responsePdu = ChatPDU.createLoginResponsePdu(userName, receivedPdu);
			try {
				log.info("Login Response PDU wird gesendet: ");
				log.info("Login Response PDU PDUType: " + receivedPdu.getPduType());
				log.info("Login Response PDU Status: " + receivedPdu.getClientStatus());
				log.info("Login Response PDU Userliste: " + receivedPdu.getClients());

				send2Topic(responsePdu);

				clients.changeClientStatus(userName, ClientConversationStatus.REGISTERED);

			} catch (Exception e) {

				log.info("Senden einer Login-Response-PDU an " + userName + " fehlgeschlagen");
				log.info("Exception Message: " + e.getMessage());
			}

		} else {
			// User bereits angemeldet, Fehlermeldung an Client senden,
			// Fehlercode an Client senden
			pdu = ChatPDU.createLoginErrorResponsePdu(receivedPdu, ChatPDU.LOGIN_ERROR);

			try {
				send2Topic(pdu);
				log.info("Login-Response-PDU an " + receivedPdu.getUserName() + " mit Fehlercode " + ChatPDU.LOGIN_ERROR
						+ " gesendet");
			} catch (Exception e) {
				log.info("Senden einer Login-Response-PDU an " + receivedPdu.getUserName() + " nicth moeglich");
				ExceptionHandler.logExceptionAndTerminate(e);
			}
		}
	}

	protected void logoutRequestAction(ChatPDU receivedPdu) {

		String userName = receivedPdu.getUserName();
		
		ChatPDU pdu;
		log.info("Logout-Request-PDU von " + receivedPdu.getUserName() + " empfangen");
		
		
		if (clients == null)
			clients = SharedChatClientList.getInstance();
			log.info("Unsere Clients: " + clients);
		// TODO
		if (!clients.existsClient(userName)) {
		log.info("User nicht in Clientliste: " + receivedPdu.getUserName());
		 } else {

		// Event an Client versenden
		Vector<String> clientList = clients.getClientNameList();
		pdu = ChatPDU.createLogoutEventPdu(userName, clientList, receivedPdu);

		clients.changeClientStatus(receivedPdu.getUserName(), ClientConversationStatus.UNREGISTERING);
		sendLoginListUpdateEvent(pdu);

		try {
			Thread.sleep(1000);
		} catch (Exception e) {
			ExceptionHandler.logException(e);
		}

		clients.changeClientStatus(receivedPdu.getUserName(), ClientConversationStatus.UNREGISTERED);

		// Logout Response senden
		sendLogoutResponse(receivedPdu.getUserName());

		// Worker-Thread des Clients, der den Logout-Request gesendet
		// hat, auch gleich zum Beenden markieren
		clients.finish(receivedPdu.getUserName());
		log.info("Laenge der Clientliste beim Vormerken zum Loeschen von " + receivedPdu.getUserName() + ": "
				+ clients.size());
	  }
	}

	private void send2Topic(ChatPDU pdu) {

		String message = gson.toJson(pdu);

		jmsContext = connectionFactory.createContext();
		log.info("CHAT Server send message to topic");

		// Build JMS Byte Message
		try {
			jmsContext.createProducer().send(topic, message);
			log.info("PDU Type der gesendeten PDU" + pdu.getPduType());
			log.info("PDU Client Status der gesendeten PDU" + pdu.getClientStatus());

		} catch (Exception e) {
			log.error(e);
		} finally {
			jmsContext.close();
		}
		log.info("REST Server send message to Topic abgeschlossen:");
	}

}