package aserg.gtf.dao;

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import aserg.gtf.model.LogCommitInfo;
import aserg.gtf.model.UserAliasInfo;

public class UserAliasInfoDAO extends GenericDAO<UserAliasInfo>{
	public List<String> getProjectsName(){
		String hql = "SELECT repositoryName FROM logcommitinfo ci GROUP BY repositoryName;";
		Query q = em.createNativeQuery(hql);
		return q.getResultList();

	}

	@Override
	public void persist(UserAliasInfo userAliasInfo) {
		super.persist(userAliasInfo);
	}

	@Override
	public UserAliasInfo find(Object id) {
		return this.em.find(UserAliasInfo.class, id);
	}
	
	@Override
	public List<UserAliasInfo> findAll(Class clazz) {
		// TODO Auto-generated method stub
		return super.findAll(UserAliasInfo.class);
	}
	
	@Override
	public void merge(UserAliasInfo o) {
		super.merge(o);
	}
	

	@Override
	public boolean exist(UserAliasInfo userAlias) {
		return this.find(userAlias.getId())!=null;
	}
	
	PersistThread<UserAliasInfo> thread = null;
	public void persistAll(Collection<UserAliasInfo> userAliases){
		if (thread == null)
			thread = new PersistThread<UserAliasInfo>(userAliases, this);
		else {
			try {
				if (thread.isAlive())
					thread.join();
				thread = new PersistThread<UserAliasInfo>(userAliases, this);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		thread.start();
	}
	
	public void persistAllNoThread(Collection<UserAliasInfo> userAliases){
		EntityTransaction tx = this.em.getTransaction();
		try {
			tx.begin();
			for (UserAliasInfo t : userAliases) {
				this.em.persist(t);
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
	public void clear() {
		this.em.clear();		
	}
	
	
	
}
