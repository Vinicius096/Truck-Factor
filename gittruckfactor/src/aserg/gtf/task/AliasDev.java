package aserg.gtf.task;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AliasDev extends GitDev {
//	private static Map<Integer, GitDev> mapAlias = new HashMap<Integer, GitDev>();
	private Set<GitDev> devs;
	
	public AliasDev(GitDev dev, String pairName) {
		super(dev.getName(), dev.getEmail(), dev.getShaExample());
		this.devs = new HashSet<GitDev>();
		this.devId = dev.getDevId();
//		this.mapAlias.put(this.getDevId(), this);
	}
	
	public void normalizeId(int newId){
		this.setDevId(newId);
		for (GitDev dev : devs) {
			if (dev.getDevId()!=newId)
				dev.normalizeId(newId);
		}
	}
	public void addDev(GitDev dev){
		this.devs.add(dev);
	}
	public boolean contains(GitDev dev){
		return this.devs.contains(dev);
	}
	
	public Set<GitDev> getDevs() {
		return devs;
	}
	
	
//	static public GitDev getAliasDev(int devId){
//		return mapAlias.get(devId);
//	}
	@Override
	public String toString() {
		return this.getDevId() + "=" + devs.toString();
	}

	
	@Override
	public void setGitHubId(int gitHubId) {
		super.setGitHubId(gitHubId);
		for (GitDev dev : devs) {
			dev.setGitHubId(gitHubId);
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		AliasDev other = (AliasDev)obj;
		return super.equals(other)&&this.devs.toString()==other.devs.toString();
	}
	
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return (getPairString()+this.devs.toString()).hashCode();
	}
}
