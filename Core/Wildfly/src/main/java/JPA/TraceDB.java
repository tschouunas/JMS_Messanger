package JPA;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;



import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceUnit;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import common.ChatPDU;

/**
 * Zugriff auf TraceDB
 * 
 * @author wir
 *
 */
@Stateless
public class TraceDB {

	@PersistenceUnit(unitName = "TracePU")
	EntityManagerFactory entityMF;
	@PersistenceContext(unitName = "TracePU", type=PersistenceContextType.TRANSACTION)
	private EntityManager eM;
	

	public void trace(ChatPDU pdu) throws Exception {
		Trace traceE = new Trace(pdu.getClientThreadName(), pdu.getServerThreadName(), pdu.getMessage());
		eM.persist(traceE);
	}
	
	
	
	 public List<Trace> getEntityList() {
	        CriteriaBuilder criterialB = eM.getCriteriaBuilder();
	        CriteriaQuery<Trace> criterialQ = criterialB.createQuery(Trace.class);
	        Root<Trace> rootEntry = criterialQ.from(Trace.class);
	        CriteriaQuery<Trace> all = criterialQ.select(rootEntry);
	        TypedQuery<Trace> allQuery = eM.createQuery(all);
	        return allQuery.getResultList();
	 }
	 
	 
	 public void resetDB() {
			List<Trace> traceList = getEntityList();
			for (Trace trace : traceList) {
				eM.remove(trace);
			}
		}

}

