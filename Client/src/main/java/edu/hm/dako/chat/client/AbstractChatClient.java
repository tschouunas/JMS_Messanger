package edu.hm.dako.chat.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.hm.dako.chat.common.ChatPDU;
import edu.hm.dako.chat.common.ClientConversationStatus;
import edu.hm.dako.chat.common.ExceptionHandler;
import edu.hm.dako.chat.common.PduType;
import edu.hm.dako.chat.common.SystemConstants;
import edu.hm.dako.chat.communication.RestComImpl;
import edu.hm.dako.chat.connection.Connection;
import edu.hm.dako.chat.connection.ConnectionFactory;
import edu.hm.dako.chat.connection.DecoratingConnectionFactory;
import edu.hm.dako.chat.jms.JMSConnection;
import edu.hm.dako.chat.jms.JMSConnectionFactory;
import edu.hm.dako.chat.tcp.TcpConnectionFactory;

/**
 * Gemeinsame Funktionalitaet fuer alle Client-Implementierungen.
 * 
 * @author Peter Mandl
 *
 */
public abstract class AbstractChatClient implements ClientCommunication {

	private static Log log = LogFactory.getLog(AbstractChatClient.class);

	// Username (Login-Kennung) des Clients
	protected String userName;

	protected String threadName;

	protected int localPort;

	protected int serverPort;
	protected String remoteServerAddress;

	protected ClientUserInterface userInterface;

	// Connection Factory und Verbindung zum Server
	protected ConnectionFactory connectionFactory;
	protected Connection connection;
	protected String serverType;

	// Gemeinsame Daten des Clientthreads und dem Message-Listener-Threads
	protected SharedClientData sharedClientData;

	// Thread, der die ankommenden Nachrichten fuer den Client verarbeitet
	protected Thread messageListenerThread;

	/**
	 * @param userInterface
	 *            GUI-Interface
	 * @param serverPort
	 *            Port des Servers
	 * @param remoteServerAddress
	 *            Adresse des Servers
	 */

	public AbstractChatClient(ClientUserInterface userInterface, int serverPort, String remoteServerAddress,
			String serverType) {

		this.userInterface = userInterface;
		this.serverPort = serverPort;
		this.remoteServerAddress = remoteServerAddress;
		this.serverType = serverType;

		/*
		 * Verbindung zum Server aufbauen
		 */
		if (serverType.equals(SystemConstants.IMPL_JMS)) {
			try {
				connectionFactory = getDecoratedFactory(new JMSConnectionFactory());
				System.out.println(connectionFactory.getClass());

				connection = connectionFactory.connectToServer(remoteServerAddress, serverPort, localPort, 20000,
						20000);

			} catch (Exception e) {
				ExceptionHandler.logException(e);
			}
		} else {
			try {
				connectionFactory = getDecoratedFactory(new TcpConnectionFactory());
				connection = connectionFactory.connectToServer(remoteServerAddress, serverPort, localPort, 20000,
						20000);
			} catch (Exception e) {
				ExceptionHandler.logException(e);
			}
		}
		log.info("Verbindung zum Server steht");

		/*
		 * Gemeinsame Datenstruktur aufbauen
		 */
		sharedClientData = new SharedClientData();
		sharedClientData.messageCounter = new AtomicInteger(0);
		sharedClientData.logoutCounter = new AtomicInteger(0);
		sharedClientData.eventCounter = new AtomicInteger(0);
		sharedClientData.confirmCounter = new AtomicInteger(0);
		sharedClientData.messageCounter = new AtomicInteger(0);
	}

	/**
	 * Ergaenzt ConnectionFactory um Logging-Funktionalitaet
	 * 
	 * @param connectionFactory
	 *            ConnectionFactory
	 * @return Dekorierte ConnectionFactory
	 */
	public static ConnectionFactory getDecoratedFactory(ConnectionFactory connectionFactory) {
		return new DecoratingConnectionFactory(connectionFactory);
	}

	@Override
	public void login(String name) throws IOException {

		userName = name;
		sharedClientData.userName = name;
		sharedClientData.status = ClientConversationStatus.REGISTERING;

		ChatPDU requestPdu = new ChatPDU();
		requestPdu.setPduType(PduType.LOGIN_REQUEST);
		requestPdu.setClientStatus(sharedClientData.status);
		Thread.currentThread().setName("Client-" + userName);
		requestPdu.setClientThreadName(Thread.currentThread().getName());
		requestPdu.setUserName(userName);
		System.out.println(requestPdu.toString());
		try {

			URI restUrl = createUrl(remoteServerAddress, serverPort);

			if (restUrl != null) {
				RestComImpl restHandler = new RestComImpl(restUrl);
				restHandler.login(requestPdu);
			}
			// connection.send(requestPdu);
			log.debug("Login-Request-PDU fuer Client " + userName + " an Server gesendet");
		} catch (Exception e) {
			log.error("Login in AbstractChatClient funktioniert nicht");
			throw new IOException();
		}
	}

	@Override
	public void logout(String name) throws IOException {

		sharedClientData.status = ClientConversationStatus.UNREGISTERING;
		ChatPDU requestPdu = new ChatPDU();
		requestPdu.setPduType(PduType.LOGOUT_REQUEST);
		requestPdu.setClientStatus(sharedClientData.status);
		requestPdu.setClientThreadName(Thread.currentThread().getName());
		requestPdu.setUserName(userName);
		try {

			// Rest etc. (siehe Branch Jonas) login send

			URI restUrl = createUrl(remoteServerAddress, serverPort);

			if (restUrl != null) {
				RestComImpl restHandler = new RestComImpl(restUrl);
				restHandler.logout(requestPdu);
			}
			// connection.send(requestPdu);
			sharedClientData.logoutCounter.getAndIncrement();
			log.debug("Logout-Request von " + requestPdu.getUserName() + " gesendet, LogoutCount = "
					+ sharedClientData.logoutCounter.get());

		} catch (Exception e) {
			log.debug("Senden der Logout-Nachricht nicht moeglich");
			throw new IOException();
		}
	}

	@Override
	public void tell(String name, String text) throws IOException {

		ChatPDU requestPdu = new ChatPDU();
		requestPdu.setPduType(PduType.CHAT_MESSAGE_REQUEST);
		requestPdu.setClientStatus(sharedClientData.status);
		requestPdu.setClientThreadName(Thread.currentThread().getName());
		requestPdu.setUserName(userName);
		requestPdu.setMessage(text);
		sharedClientData.messageCounter.getAndIncrement();
		requestPdu.setSequenceNumber(sharedClientData.messageCounter.get());
		try {
			connection.send(requestPdu);
			log.info("Chat-Message-Request-PDU fuer Client " + name + " an Server gesendet, Inhalt: " + text);
			log.info("MessageCounter: " + sharedClientData.messageCounter.get() + ", SequenceNumber: "
					+ requestPdu.getSequenceNumber());
		} catch (Exception e) {
			log.debug("Senden der Chat-Nachricht nicht moeglich");
			throw new IOException();
		}
	}

	@Override
	public void cancelConnection() {
		try {
			connection.close();
		} catch (Exception e) {
			ExceptionHandler.logException(e);
		}
	}

	@Override
	public boolean isLoggedOut() {
		return (sharedClientData.status == ClientConversationStatus.UNREGISTERED);
	}

	private URI createUrl(String serverName, Integer serverPort) {
		try {
			return new URI("http://" + serverName + ":" + serverPort);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}