package aserg.gtf.dao;

import java.util.Collection;
import java.util.List;

import aserg.gtf.model.GitHubDeveloper;

public class GitHubDeveloperDAO extends GenericDAO<GitHubDeveloper>{

	@Override
	public GitHubDeveloper find(Object id) {
		return this.em.find(GitHubDeveloper.class, id);
	}

	@Override
	public boolean exist(GitHubDeveloper entity) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void persist(GitHubDeveloper o) {
		GitHubDeveloper persistedGitHubDev = this.em.find(GitHubDeveloper.class, o.getGitHubId());
		if (persistedGitHubDev == null)
			super.persist(o);
		
	}

	
	@Override
	public List<GitHubDeveloper> findAll(Class clazz) {
		// TODO Auto-generated method stub
		return super.findAll(GitHubDeveloper.class);
	}
	
	PersistThread<GitHubDeveloper> thread = null;
	public void persistAll(){
		Collection<GitHubDeveloper> gitHubDevs = GitHubDeveloper.getGitHubDevs().values();
		if (thread == null)
			thread = new PersistThread<GitHubDeveloper>(gitHubDevs, this);
		else {
			try {
				if (thread.isAlive())
					thread.join();
				thread = new PersistThread<>(gitHubDevs, this);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		thread.start();
	}
	
	public void persistOrUpdateAll(){
		Collection<GitHubDeveloper> gitHubDevs = GitHubDeveloper.getGitHubDevs().values();
		for (GitHubDeveloper gitHubDeveloper : gitHubDevs) {
			if (gitHubDeveloper.isUpdated())
				persistOrUpdate(gitHubDeveloper);
		}
	}
	
	public void persistOrUpdate(GitHubDeveloper o){
		o.setUpdated(false);
		GitHubDeveloper persistedDev = this.em.find(GitHubDeveloper.class, o.getGitHubId());
		if (persistedDev != null){
			persistedDev.setCompany(o.getCompany());
			persistedDev.setEmail(o.getEmail());
			persistedDev.setGitHubId(o.getGitHubId());
			persistedDev.setName(o.getName());
			persistedDev.setPairsNameEmail(o.getPairsNameEmail());
			persistedDev.setUpdated(o.isUpdated());
			super.merge(persistedDev);
		}
		else
			this.persist(o);
		

		
		
	}
	
}
