package adminservice;

import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import JPA.Count;
import JPA.CountDB;
import JPA.Trace;
import JPA.TraceDB;
import server.JMSMessageBean;
import server.SharedChatClientList;

/**
 * JMS Connection Klasse.
 * 
 * @author wir 
 *
 */

@Singleton
@Lock(LockType.READ)
@Path("/api")
public class AdminService {
	private static final Logger log = Logger.getLogger(JMSMessageBean.class.getName());
    
	
    @Inject
    private TraceDB trace;
    @Inject
    private CountDB count;
    
    
    private SharedChatClientList client = SharedChatClientList.getInstance();
    
    
    private final static int maxMessagesShown = 20;
    

    public AdminService() {}
    
   
    
    /*
     * Erstellen der Liste mit User und Anzahl der Nachrichten
     */
    
    @GET @Path("/data/count")
    @Produces("application/json")
    public String respAllCount () {
    	List<Count> countList = count.getEntityList();
        JsonObjectBuilder builder = Json.createObjectBuilder();
        JsonArrayBuilder data = Json.createArrayBuilder();

            for (int i=0;i<countList.size(); i++) {
                JsonObjectBuilder entry = Json.createObjectBuilder();
                entry.add("client", countList.get(i).getUserName());
                entry.add("count", countList.get(i).getMessageCounter());
                data.add(entry);
            }
            builder.add("data", data);           
            builder.add("success", true);
        return builder.build().toString();
    }
    
    /*
     * Liste begrenzt auf 20 Zeilen, für das Anzeigen der Nachrichten
     */
   
    @GET @Path("/data/trace")
    @Produces("application/json")
    public String respTrace () {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        
        try {
            List<Trace> traceList = trace.getEntityList();
            JsonArrayBuilder data = Json.createArrayBuilder();
            
            for (int i = traceList.size() - 1; i >= Math.max(traceList.size() - maxMessagesShown, 0); i--) {
                JsonObjectBuilder entry = Json.createObjectBuilder();
                Trace trace = traceList.get(i);
                               
                if( trace.getServerThreadName() != null || trace.getMessage() != null ) {
                
                entry.add("client", traceList.get(i).getClientThreadName()); 
                entry.add("server", traceList.get(i).getServerThreadName());
                entry.add("message", traceList.get(i).getMessage());
                data.add(entry);
                } 
            }
            builder.add("data", data);
            builder.add("success", true);    
        } 
        catch (SecurityException | IllegalStateException ex) {
        log.info("Exception");
            Logger.getLogger(AdminService.class.getName()).log(Level.SEVERE, null, ex);
            builder.add("success", false);
        }
        return builder.build().toString();
    }
    
    
    /*
     * Zeigt ale angezeigten User und die Anzahl der gesammten versendeten Nachrichten
     */
    
    @GET @Path("/overview/all")
    @Produces("application/json")
    public String respStats () {
    	int messageSum = client.getTotalNumberOfReceivedMessages();
    Vector<String> users = client.getRegisteredClientNameList();
        
        return Json.createObjectBuilder()
                .add("data", Json.createObjectBuilder()
                .add("User Count", users.size())
                .add("Message Count", messageSum))
                .add("success", true).build().toString();
        
    }
    
    /*
     * Inhalt der Datenbank löschen
     */
    
    @DELETE @Path("/data")
    @Produces("application/json")
    public String resetDB () {
            trace.resetDB();;
            count.resetDB();
            return "{\"success\": true}";
    }
   
}
