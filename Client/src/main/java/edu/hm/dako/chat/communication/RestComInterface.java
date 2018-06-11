package edu.hm.dako.chat.communication;

import java.io.Serializable;

public interface RestComInterface {
	/**
	 * Lists the methods necessary to handle login/logout to the chat server
	 */
	
	/**
	 * Method needs an user name and registers the user to the chat app
	 * 
	 * @param loginName user name connecting to chat app
	 * @return true if successful 
	 */
	boolean login (Serializable loginPdu);
	
	/**
	 * Method takes registered user and logs him out from the chat app
	 * 
	 * @param logoutName user which gets log out from chat app
	 * @return true if successful
	 */
	boolean logout (Serializable logoutPdu);
}
