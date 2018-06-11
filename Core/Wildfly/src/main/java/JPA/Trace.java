package JPA;

import java.io.Serializable;
import javax.persistence.*;

/**
 * Entity implementation class for Entity: Trace
 * @autor wir
 */
@Entity
@Table(name = "trace")

public class Trace implements Serializable {

	
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	@Column(name = "clientThread")
	private String clientThreadName;
	@Column(name = "serverThread")
	private String serverThreadName;
	@Column(name = "message")
	private String message;

	protected Trace() {

	}

	public Trace(String clientThreadName, String serverThreadName, String message) {
		this.clientThreadName = clientThreadName;
		this.serverThreadName = serverThreadName;
		this.message = message;
	}

	public String getClientThreadName() {
		return clientThreadName;
	}

	public String getServerThreadName() {
		return serverThreadName;
	}

	public String getMessage() {
		return message;
	}

   
}
