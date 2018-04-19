package aserg.gtf.task;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class NewAlias{
	private static int id=1;
	private static Map<Integer, NewAlias> mapAlias = new HashMap<Integer, NewAlias>();
	private Set<Pair> pairs;
	private int aliasID;
	
	public NewAlias() {
		this.aliasID = id++;
		this.pairs = new HashSet<Pair>();
		NewAlias.mapAlias.put(aliasID, this);
	}
	
	public void normalizeId(){
		for (Pair pair : pairs) {
			if (pair.getUserId()!=this.aliasID){
				pair.getAlias().setAliasID(this.aliasID);
			}
		}
	}
	public void addPair(Pair pair){
		this.pairs.add(pair);
	}
	public boolean contains(Pair pair){
		return this.pairs.contains(pair);
	}
	
	public Set<Pair> getPairs() {
		return pairs;
	}
	public int getAliasID() {
		return aliasID;
	}
	static public NewAlias getAlias(int aliasId){
		return mapAlias.get(aliasId);
	}
	@Override
	public String toString() {
		return aliasID + "=" + pairs.toString();
	}
	
	public void setAliasID(int aliasID) {
		this.aliasID = aliasID;
	}
}