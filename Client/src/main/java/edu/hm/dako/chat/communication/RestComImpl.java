package edu.hm.dako.chat.communication;

import java.io.Serializable;
import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.hm.dako.chat.common.ChatPDU;

//TODO write java doc for methods and class
public class RestComImpl implements RestComInterface {

	private URI restURI;
	private Client restClient;

	// Url should contain following:
	// http://uri:serverport
	public RestComImpl(final URI restURI) {
		this.restURI = restURI;
		this.restClient = ClientBuilder.newClient();
	}

	@Override
	public boolean login(Serializable loginPdu) {

		// Build json
		Gson gson = new GsonBuilder().create();
		String json = gson.toJson(loginPdu);

		// send request to target url for specific userName
		WebTarget webTarget = restClient.target(restURI);
		WebTarget loginWebTarget = webTarget.path("Wildfly/rest/users/login");
		Invocation.Builder invocationBuilder = loginWebTarget.request();

		// send userName as json to server and get response
		Response response = invocationBuilder.post(Entity.entity(json, MediaType.APPLICATION_JSON));

		// check responseStatus
		int responseStatus = response.getStatus();
		if (responseStatus == 404) {
			System.out.println("Response status: " + responseStatus + " Endpoint not found");
		}
		// check if no problems occurred
		if (response.getStatus() == Status.OK.getStatusCode()) {
			System.out.println("Status von ReST: " + responseStatus);
			return true;
		}
		// }
		return false;
	}

	@Override
	public boolean logout(Serializable logoutPdu) {

		Gson gson = new GsonBuilder().create();
		String json = gson.toJson(logoutPdu);


		// send request to target url for specific userName
		WebTarget webTarget = restClient.target(restURI);
		WebTarget logoutWebTarget = webTarget.path("Wildfly/rest/users/logout");
		Invocation.Builder invocationBuilder = logoutWebTarget.request();

		Response response = invocationBuilder.post(Entity.entity(json, MediaType.APPLICATION_JSON));

		if (response.getStatus() == Status.OK.getStatusCode()) {
			return true;
		}
		// }
		return false;
	}

}
