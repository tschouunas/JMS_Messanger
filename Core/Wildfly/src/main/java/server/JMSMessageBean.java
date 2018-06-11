package server;

import java.util.logging.Logger;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.logging.Level;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.*;
import javax.inject.Inject;
import javax.jms.*;

import common.ChatPDU;
import common.ClientListEntry;
import common.ExceptionHandler;

import com.google.gson.*;

import JPA.CountDB;
import JPA.TraceDB;

/**
 * Klasse wie SimpleChatWorkerThreadImpl. mit ChatPDU
 * 
 * @auto wir
 */

@MessageDriven(mappedName = "jms/queue/ChatQueue", activationConfig = {
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/queue/ChatQueue"),
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue") })

@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class JMSMessageBean implements MessageListener {
	
	@Inject
	private CountDB countManager;
	
	@Inject
	private TraceDB traceManager;

	private static final String DEFAULT_TOPIC_DESTINATION = "jms/topic/ChatTopic";
	private static final String DEFAULT_CONNECTION_FACTORY = "jms/RemoteConnectionFactory";

	private static final Logger log = Logger.getLogger(JMSMessageBean.class.getName());

	private SharedChatClientList clients;
	private String userName = "tester";

	@Resource(mappedName = "java:jboss/exported/jms/topic/ChatTopic")
	private Topic topic;

	@Resource(mappedName = "java:/JmsXA")
	private ConnectionFactory connectionFactory;

	@Resource
	private MessageDrivenContext mdc;
	
	@Inject
	private JMSContext context;
	
	private long startTime;
	
	Gson gson = new Gson();
	
	JMSChatWorkerThreadImpl jmsThread = new JMSChatWorkerThreadImpl(this);

	/**
	 * Wird automatisch beim Empfang einer Nachricht aufgerufen.
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void onMessage(Message message) {

		try {
			if (message instanceof javax.jms.TextMessage) {
				//Gson:
				String text = ((javax.jms.TextMessage) message).getText();
				log.log(Level.INFO, "Die CPU hat folgende Klasse: " + message.getClass());
				
				ChatPDU receivedPDU = gson.fromJson(text, ChatPDU.class);
				log.log(Level.INFO, "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
				log.log(Level.INFO, "ChatPDU empfangen.");
				log.log(Level.INFO, "Deine Empfangene Datei hat den PDU Type " + receivedPDU.getPduType());
				log.log(Level.INFO, "Die Größe der empfangenden Clientliste: " + receivedPDU.getClients());
				
				
				handleIncomingMessage(receivedPDU);
				countManager.count(receivedPDU.getUserName());
				traceManager.trace(receivedPDU);
				
			} else {

				System.out.println(message);
				ObjectMessage objectMessage = (ObjectMessage) message;
				Serializable ser = objectMessage.getObject();
				ChatPDU pdu = (ChatPDU) ser;
				log.log(Level.INFO, "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
				log.log(Level.INFO, "ChatPDU empfangen.");
				

				handleIncomingMessage(pdu);
			}
		} catch (Exception jex) {
			log.log(Level.SEVERE, "Fehler beim Empfangen einer Nachricht: ", jex);
		}
	}

	
	protected void handleIncomingMessage(ChatPDU pdu) throws Exception {
		
		startTime = System.nanoTime();

		ChatPDU receivedPdu = pdu;
		userName = receivedPdu.getUserName();

		log.log(Level.INFO, "Ankommender PDU Type:  "+ receivedPdu.getPduType());
		log.log(Level.INFO, "Username: " +receivedPdu.getUserName());
		log.log(Level.INFO, "Gesendete Nachricht: " +receivedPdu.getMessage());
		log.log(Level.INFO, "Number of sendEvents: " + receivedPdu.getNumberOfSentEvents());
		// Empfangene Nachricht bearbeiten
		try {
			switch (receivedPdu.getPduType()) {

			case CHAT_MESSAGE_REQUEST:
				// Chat-Nachricht angekommen, an alle verteilen
				jmsThread.chatMessageRequestAction(receivedPdu);
				break;

			default:
				log.log(java.util.logging.Level.WARNING,
						"Falsche PDU empfangen von Client: " + userName + ", PduType: " + receivedPdu.getPduType());
				break;
			}
		} catch (Exception e) {
			log.log(java.util.logging.Level.SEVERE, "Exception bei der Nachrichtenverarbeitung", e);
		}
	}


	/**
	 * Sendet ChatPDU an alle registrierten Chatclients.
	 * 
	 * @param pdu
	 *            Zu sendende PDU.
	 */
	void sendToTopic(ChatPDU pdu) {
		JMSContext jmsContext = connectionFactory.createContext();
		jmsContext.createProducer().setProperty("destination", "chat").send(topic, pdu);
		jmsContext.close();
	}
	
	
	protected void sendToAllClients(ChatPDU pdu) {
		
		String message = gson.toJson(pdu);
		JMSProducer jmsProducer = this.context.createProducer();
		jmsProducer.send(topic, message);
		log.log(Level.INFO, "Chat-Message-Response-PDU gesendet");

	}
	
	
	protected void sendToClient(ChatPDU pdu, String clientName) {
		
		String message = gson.toJson(pdu);
		JMSProducer jmsProducer = this.context.createProducer();
		jmsProducer.setProperty("destination", clientName).send(topic, message);
		log.log(Level.INFO, "Chat-Message-Response-PDU an einzelne User gesendet");
		
	}
	
	
	
}
