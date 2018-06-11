package JPA;

	import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Stateless;
	import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceUnit;
import javax.persistence.TypedQuery;
	import javax.persistence.criteria.CriteriaBuilder;
	import javax.persistence.criteria.CriteriaQuery;
	import javax.persistence.criteria.Root;
	import JPA.*;
	
	import common.ChatPDU;
import server.JMSMessageBean;

	/**
	 * Session Bean für Zugriff auf CountDB.
	 * 
	 * @author wir
	 */

@Stateless
	public class CountDB {
	
	private static final Logger log = Logger.getLogger(JMSMessageBean.class.getName());
	
	@PersistenceUnit(unitName = "CountPU")
	EntityManagerFactory entityManagerFactory;	
		@PersistenceContext(unitName = "CountPU", type=PersistenceContextType.TRANSACTION)
		private EntityManager em;
		
		public void count(String userName) throws Exception {
			log.log(Level.SEVERE, "Schreibe in die Datenbank");
			Count countE = em.find(Count.class, userName);
			
			
			if (countE == null) {
				countE = new Count(userName);
				em.persist(countE);
			} else {
				countE.setMessageCounter(countE.getMessageCounter() + 1);
			}
		}
		
		 public List<Count> getEntityList() {
		        CriteriaBuilder cb = em.getCriteriaBuilder();
		        CriteriaQuery<Count> cq = cb.createQuery(Count.class);
		        Root<Count> rootEntry = cq.from(Count.class);
		        CriteriaQuery<Count> all = cq.select(rootEntry);
		        TypedQuery<Count> allQuery = em.createQuery(all);
		        return allQuery.getResultList();
		 }
		 
		 public void resetDB() {
				List<Count> countList = getEntityList();
				for (Count count : countList) {
					em.remove(count);
				}
			}
		
	

	


}