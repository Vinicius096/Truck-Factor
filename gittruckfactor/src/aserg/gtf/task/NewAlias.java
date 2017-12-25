package aserg.gtf.task;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class NewAlias{
	private static int id=1;
	private static Map<Integer, NewAlias> mapAlias = new HashMap<Integer, NewAlias>();
	private Set<Pair> pair;
	private int aliasID;
	
	public NewAlias() {
		this.aliasID = id++;
		this.pair = new HashSet<Pair>();
		this.mapAlias.put(aliasID, this);
	}
	public void addPair(Pair pair){
		this.pair.add(pair);
	}
	public boolean contains(Pair pair){
		return this.pair.contains(pair);
	}
	
	public Set<Pair> getPairs() {
		return pair;
	}
	public int getAliasID() {
		return aliasID;
	}
	static public NewAlias getAlias(int aliasId){
		return mapAlias.get(aliasId);
	}
	@Override
	public String toString() {
		return aliasID + "=" + pair.toString();
	}
}