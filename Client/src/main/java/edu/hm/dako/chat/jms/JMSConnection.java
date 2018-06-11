package edu.hm.dako.chat.jms;

import java.io.Serializable;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import edu.hm.dako.chat.connection.EndOfFileException;

import java.util.logging.Logger;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import edu.hm.dako.chat.common.ChatPDU;
import edu.hm.dako.chat.common.PduType;
import edu.hm.dako.chat.connection.Connection;
import com.google.gson.*;

import edu.hm.dako.chat.connection.ConnectionTimeoutException;

/**
 * JMS Connection Klasse.
 * 
 * @author wir 
 *
 */

public class JMSConnection implements Connection {



	private static Logger log = Logger.getLogger(JMSConnection.class.getName());

	private static final String DEFAULT_CONNECTION_FACTORY = "jms/RemoteConnectionFactory";
	private static final String DEFAULT_QUEUE_DESTINATION = "jms/queue/ChatQueue";
	private static final String DEFAULT_TOPIC_DESTINATION = "jms/topic/ChatTopic";

	private static final String DEFAULT_USERNAME = "tester";
	private static final String DEFAULT_PASSWORD = "tester";
	private static final String INITIAL_CONTEXT_FACTORY = "org.jboss.naming.remote.client.InitialContextFactory";
	private String PROVIDER_URL;

	private JMSContext jmsContext = null;
	private JMSProducer queueProducer = null;
	private Queue destinationQueue = null;

	private Topic destinationTopic = null;
	private JMSConsumer topicConsumer = null;

	private Context namingContext = null;

	private Gson gson = new Gson();

	private CountDownLatch receiveRdySignal = new CountDownLatch(1);

	public JMSConnection(String remoteServerAdress, int serverPort) {

		PROVIDER_URL = "http-remoting://" + remoteServerAdress + ":" + serverPort;

		log.log(Level.INFO, Thread.currentThread().getName() + ": Verbindung mit Queue und Topic unter " + PROVIDER_URL
				+ " wird aufgebaut.");

		try {
			String userName = System.getProperty("username", DEFAULT_USERNAME);
			String password = System.getProperty("password", DEFAULT_PASSWORD);

			// Kontext für JNDI lookups vorbereiten
			final Properties env = new Properties();
			env.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
			env.put(Context.PROVIDER_URL, System.getProperty(Context.PROVIDER_URL, PROVIDER_URL));
			env.put(Context.SECURITY_PRINCIPAL, userName);
			env.put(Context.SECURITY_CREDENTIALS, password);
			namingContext = new InitialContext(env);

			// JNDI lookups
			// Perform the JNDI lookups
			String connectionFactoryString = System.getProperty("connection.factory", DEFAULT_CONNECTION_FACTORY);
			log.info("Attempting to acquire connection factory \"" + connectionFactoryString + "\"");
			ConnectionFactory connectionFactory = (ConnectionFactory) namingContext.lookup(connectionFactoryString);
			log.info("Found connection factory \"" + connectionFactoryString + "\" in JNDI");

			String destinationString = System.getProperty("destination", DEFAULT_QUEUE_DESTINATION);
			log.info("Attempting to acquire destination \"" + destinationString + "\"");
			destinationQueue = (Queue) namingContext.lookup(destinationString);
			log.info("Found destination \"" + destinationString + "\" in JNDI");

			String destinationStringTopic = System.getProperty("destination", DEFAULT_TOPIC_DESTINATION);
			log.info("Attempting to acquire destination \"" + destinationStringTopic + "\"");
			destinationTopic = (Topic) namingContext.lookup(destinationStringTopic);
			log.info("Found destination \"" + destinationStringTopic + "\" in JNDI");
	
			
			// Erstellen des JMSContext und queueProducers
			jmsContext = connectionFactory.createContext(userName, password);
			log.info("Context wurde angelegt!");
			queueProducer = jmsContext.createProducer();
			log.log(Level.INFO, Thread.currentThread().getName() + ": Verbindung mit Queue wurde aufgebaut.");

			
			topicConsumer = jmsContext.createConsumer(destinationTopic);


		} catch (NamingException e) {
			log.log(Level.SEVERE, "JNDI Fehler: ", e);
			System.exit(1);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Unerwarteter Fehler bei Erstellung der Connection: ", e);
		}
	}

	/**
	 * Setzt den Usernamen für den Messageselector, startet die Topicconnection und
	 * benachrichtigt den JMSMessageListenerThread darüber, dass nun empfangen
	 * werden kann.
	 * 
	 * @param userName
	 */
	public void startMessageHandling(String userName) {
		try {
			
			topicConsumer = jmsContext.createConsumer(destinationTopic,
					"(destination = 'chat') OR (destination = '" + userName + "')", false);
			jmsContext.start();
			receiveRdySignal.countDown();
			log.log(Level.INFO, Thread.currentThread().getName() + ": Verbindung mit Topic wurde aufgebaut.");
		} catch (Exception e) {
			log.log(Level.SEVERE, "Unerwarteter Fehler bei Erstellung der Connection: ", e);
		}
	}

	@Override
	public Serializable receive(int timeout) throws Exception, JMSException, EndOfFileException {
		System.out.println("Das ist unser topic1: " + destinationTopic);
		if (topicConsumer == null) {
			log.info("Empfangsversuch, obwohl Verbindung nicht steht!");
			throw new EndOfFileException(new Exception());
		}

		try {
			Message m = topicConsumer.receive();

			if (m instanceof TextMessage) {
				TextMessage message = (TextMessage) m;
				String text = message.getText();
				ChatPDU pdu = gson.fromJson(text, ChatPDU.class);
				log.log(Level.INFO, "Methode wurde aufgerufen und Nachricht erhalten!");
				log.log(Level.INFO, "PDU Inhalt: " + pdu.getMessage());
				return (Serializable) pdu;

			} else {
				ObjectMessage objectMessage = (ObjectMessage) m;
				Serializable serMessage = objectMessage.getObject();
				return (Serializable) serMessage;
			}
		} catch (JMSException e) {
			log.info("JMSException bei Empfang: " + e);
			throw e;
		} catch (Exception e) {
			log.info("Unerwartete Exception bei Empfang: " + e);
			throw new EndOfFileException(e);
		}
	}

	@Override
	public Serializable receive() throws Exception, JMSException, EndOfFileException {
		System.out.println("Das ist unser topic: " + destinationTopic);
		if (topicConsumer == null) {
			log.info("Empfangsversuch, obwohl Verbindung nicht steht!");
			throw new EndOfFileException(new Exception());
		}

		try {
			Message m = topicConsumer.receive();

			if (m instanceof TextMessage) {
				TextMessage message = (TextMessage) m;
				String text = message.getText();
				ChatPDU pdu = gson.fromJson(text, ChatPDU.class);
				log.log(Level.INFO, "Methode wurde aufgerufen und Nachricht erhalten!");
				log.log(Level.INFO, "PDU Inhalt: " + pdu.toString());
				return (Serializable) pdu;

			} else {
				ObjectMessage objectMessage = (ObjectMessage) m;
				Serializable serMessage = objectMessage.getObject();
				return (Serializable) serMessage;
			}

		} catch (JMSException e) {
			log.info("JMSException bei Empfang: " + e);
			throw e;
		} catch (Exception e) {
			log.info("Unerwartete Exception bei Empfang: " + e);
			throw new EndOfFileException(e);
		}
	}

	@Override
	public void send(Serializable message) throws Exception {
		String content = gson.toJson(message);
		queueProducer.send(destinationQueue, content);
	}

	@Override
	public synchronized void close() throws Exception {
		if (jmsContext != null) {
			jmsContext.close();
		}

		if (namingContext != null) {
			namingContext.close();
		}
		log.info("Verbindungen mit Queue und Topic geschlossen.");
	}

	public CountDownLatch getReceiveRdySignal() {
		return receiveRdySignal;
	}
	
}
