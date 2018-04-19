package aserg.gtf.dao.newstudy;

import javax.persistence.EntityTransaction;

import aserg.gtf.dao.GenericDAO;
import aserg.gtf.model.newstudy.TFDeveloper;

public class TFDeveloperDAO
extends GenericDAO<TFDeveloper>
{
  @Override
public TFDeveloper find(Object id)
  {
    return this.em.find(TFDeveloper.class, id);
  }
  
  @Override
public boolean exist(TFDeveloper entity)
  {
    return find(entity.getId()) != null;
  }
  
  public void updateAll(java.util.Collection<TFDeveloper> list)
  {
	  EntityTransaction tx = this.em.getTransaction();
		try {
			tx.begin();
			for (TFDeveloper t : list) {
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
  
}
