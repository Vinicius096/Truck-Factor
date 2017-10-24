package aserg.gtf.dao;

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import aserg.gtf.model.LogCommitInfo;

public class LogCommitDAO extends GenericDAO<LogCommitInfo>{
	public List<String> getProjectsName(){
		String hql = "SELECT repositoryName FROM logcommitinfo ci GROUP BY repositoryName;";
		Query q = em.createNativeQuery(hql);
		return q.getResultList();

	}

	@Override
	public void persist(LogCommitInfo logCFiles) {
		super.persist(logCFiles);
	}

	@Override
	public LogCommitInfo find(Object id) {
		return this.em.find(LogCommitInfo.class, id);
	}
	
	@Override
	public List<LogCommitInfo> findAll(Class clazz) {
		// TODO Auto-generated method stub
		return super.findAll(LogCommitInfo.class);
	}
	
	@Override
	public void merge(LogCommitInfo o) {
		super.merge(o);
	}
	

	@Override
	public boolean exist(LogCommitInfo commit) {
		return this.find(commit.getId())!=null;
	}
	
	PersistThread<LogCommitInfo> thread = null;
	public void persistAll(Collection<LogCommitInfo> logCommits){
		if (thread == null)
			thread = new PersistThread<LogCommitInfo>(logCommits, this);
		else {
			try {
				if (thread.isAlive())
					thread.join();
				thread = new PersistThread<LogCommitInfo>(logCommits, this);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		thread.start();
	}
	
	public void persistAllNoThread(Collection<LogCommitInfo> logCommits){
		EntityTransaction tx = this.em.getTransaction();
		try {
			tx.begin();
			for (LogCommitInfo t : logCommits) {
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
