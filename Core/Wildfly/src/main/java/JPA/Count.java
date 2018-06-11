package JPA;

import java.io.Serializable;
import java.lang.Integer;
import java.lang.String;

import javax.persistence.*;

/**
 * Entity implementation class for Entity: Count
 * @autor wir
 */
@Entity
@Table(name="count2")

public class Count implements Serializable {
	
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "userName")
	private String userName;
	@Column(name = "messageCount")
	private Integer messageCounter;
	
	protected Count() {

	}
	
	public Count(String userName) {
		this.userName = userName;
		this.messageCounter = 1;
	}   
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}   
	public Integer getMessageCounter() {
		return messageCounter;
	}

	public void setMessageCounter(Integer messageCounter) {
		this.messageCounter = messageCounter;
	}
   
}
