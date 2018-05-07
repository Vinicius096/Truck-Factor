package aserg.gtf.task;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class Alias{
	private static int id=1;
	private static Map<Integer, Alias> mapAlias = new HashMap<Integer, Alias>();
	private Set<String> usernames;
	private int aliasID;
	public Alias() {
		this.aliasID = id++;
		this.usernames = new HashSet<String>();
		Alias.mapAlias.put(aliasID, this);
	}
	public void addUsername(String username){
		this.usernames.add(username);
	}
	public boolean contains(String username){
		return this.usernames.contains(username);
	}
	
	public Set<String> getUsernames() {
		return usernames;
	}
	public int getAliasID() {
		return aliasID;
	}
	static public Alias getAlias(int aliasId){
		return mapAlias.get(aliasId);
	}
	@Override
	public String toString() {
		return aliasID + "=" + usernames.toString();
	}
}