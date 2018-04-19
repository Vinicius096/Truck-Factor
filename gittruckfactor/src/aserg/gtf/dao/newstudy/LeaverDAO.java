package aserg.gtf.dao.newstudy;

import aserg.gtf.dao.GenericDAO;
import aserg.gtf.model.newstudy.Leaver;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityTransaction;
import javax.persistence.Query;

public class LeaverDAO
  extends GenericDAO<Leaver>
{
  @Override
public Leaver find(Object id)
  {
    return this.em.find(Leaver.class, id);
  }
  
  @Override
public boolean exist(Leaver entity)
  {
    return find(entity.getId()) != null;
  }
  
  /* Error */
  public void updateAll(java.util.Collection<Leaver> list)
  {
	  EntityTransaction tx = this.em.getTransaction();
		try {
			tx.begin();
			for (Leaver t : list) {
				this.em.merge(t);
			}
			tx.commit();
		} catch (RuntimeException e) {
			if(tx != null && tx.isActive()) 
				tx.rollback();
			throw e;
		} 
		finally{
			this.em.clear();
		}
  }
  
  public List<Leaver> getRepositoryLeavers(String repositoryName)
  {
    String hql = "SELECT DISTINCT l.id FROM measure m\tJOIN measure_leaver ml ON ml.measure_id = m.id JOIN leaver l ON ml.leavers_id = l.id WHERE repositoryname = '" + 
    
      repositoryName + "' ;";
    Query q = this.em.createNativeQuery(hql);
    List resultList = q.getResultList();
    List<Leaver> leavers = new ArrayList();
    for (Object object : resultList) {
      leavers.add(find(object));
    }
    return leavers;
  }
}
