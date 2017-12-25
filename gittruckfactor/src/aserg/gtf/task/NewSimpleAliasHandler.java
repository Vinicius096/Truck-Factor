package aserg.gtf.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.StringUtils;

import aserg.gtf.model.LogCommitInfo;
import aserg.gtf.util.LineInfo;

public class NewSimpleAliasHandler{
	List<LineInfo> fileAliases;
	Map<String, NewAlias> mapIds = new HashMap<String, NewAlias>();
	public NewSimpleAliasHandler(List<LineInfo> list) {
		this.fileAliases = list;
	}
	public NewSimpleAliasHandler() {
		this.fileAliases = null;
	}
	
	
	public Map<String, Integer> execute(String repositoryName, Map<String, LogCommitInfo> commits){
//		setUsername(commits);
		Map<String, Pair> pairs = getPairs(commits);
		HashMap<String, NewAlias> devEmailMap = new HashMap<String, NewAlias>();
		HashMap<String, NewAlias> devNameMap = new HashMap<String, NewAlias>();
		
		//Group pairs with the same e-mail
		for (Pair pair : pairs.values()) {
			String pairEmailUpper = pair.getEmail().toUpperCase();
			if (!devEmailMap.containsKey(pairEmailUpper))
				devEmailMap.put(pairEmailUpper, new NewAlias());
			devEmailMap.get(pairEmailUpper).addPair(pair);
			pair.setAlias(devEmailMap.get(pairEmailUpper));
		}

		
		//Group pairs with the same name
		for (Pair pair : pairs.values()) {
			String pairNameUpper = pair.getName().toUpperCase();
			if (!devNameMap.containsKey(pairNameUpper))
				devNameMap.put(pairNameUpper, pair.getAlias());

			for (Pair innerPair : pair.getAlias().getPairs()) {
				devNameMap.get(pairNameUpper).addPair(innerPair);
				innerPair.setAlias(devNameMap.get(pairNameUpper));
			}
		}
//		for (Entry<String, NewAlias> entry : devNameMap.entrySet())  System.out.println(entry.getKey()+";"+entry.getValue());
		Map<String, Integer> map = new HashMap<String, Integer>();
		
		
		for (LogCommitInfo commit : commits.values()) {
			String authorPairName = commit.getAuthorName().toUpperCase()+"**"+commit.getAuthorEmail().toUpperCase();
			String committerPairName = commit.getCommitterName().toUpperCase()+"**"+commit.getCommitterEmail().toUpperCase();
			int devId = pairs.get(!authorPairName.isEmpty()?authorPairName:committerPairName).getAlias().getAliasID();
			commit.setAuthorId(devId);
			map.put(commit.getUserName(), devId);
		}
//		for (Entry<String, Pair> entry : pairs.entrySet()) System.out.println(entry.getKey() + ";" + entry.getValue().getUserId());
		return map;
	}
		
	private Map<String, Pair> getPairs(Map<String, LogCommitInfo> commits) {
		Map<String, Pair> pairs = new HashMap<String, Pair>();
//		Set<Pair> pairs = new HashSet<Pair>();
		for (LogCommitInfo commit : commits.values()) {
			Pair authorPair = new Pair(commit.getAuthorName(), commit.getAuthorEmail(), commit.getSha());
			Pair committerPair = new Pair(commit.getCommitterName(), commit.getCommitterEmail(), commit.getSha());
			if(!pairs.containsKey(authorPair.getPairString()))
				pairs.put(authorPair.getPairString(), authorPair);
			if(!pairs.containsKey(committerPair.getPairString()))
				pairs.put(committerPair.getPairString(), committerPair);
		}
//		for (Pair pair : pairs.values()) System.out.println(pair);
		return pairs;
	}
//	private Map<String, Integer> toIntegerId(Map<String, NewAlias> mapIds2) {
//		Map<String, Integer> map = new HashMap<String, Integer>();
//		Map<Integer, NewAlias> printMap = new HashMap<Integer, NewAlias>();
//		for (Entry<String, NewAlias> entry : mapIds2.entrySet()) {
//			map.put(entry.getKey(), entry.getValue().getAliasID());
////			if (entry.getValue().getUsernames().size()>1)
////				printMap.put(entry.getValue().getAliasID(), entry.getValue());
//		}
////		System.out.println(printMap);
//		return map;
//	}
//	private void setAuthorsId(HashMap<String, NewAlias> devEmailMap,
//			Map<String, LogCommitInfo> commits, boolean setDevIds) {
//		Map<String, NewAlias> mapId = new HashMap<String, NewAlias>();
//		int id = 0;
//		for (Entry<String, NewAlias> entry : devEmailMap.entrySet()) {
//			Set<String> usernames = entry.getValue().getPairs();
//			for (String username : usernames) {
//				if (!mapId.containsKey(username))
//					mapId.put(username, entry.getValue());
////				else
////				System.err.println("\n\nERROR in method setUsername of class SimpleAliasHandler!!! Username already exist: " + username);
//			}
//		}
//		if (setDevIds)
//			for (LogCommitInfo commit : commits.values()) {
//				commit.setAuthorId(mapId.get(commit.getUserName()).getAliasID());
//			}
//		mapIds = mapId;
//	}
	
//	private void resetAuthorsId(HashMap<String, Alias> devNameMap,
//			Map<String, LogCommitInfo> commits) {
//		Map<Integer, Alias> mapId = new HashMap<Integer, Alias>();
//		int id = 0;
//		for (Entry<String, Alias> entry : devNameMap.entrySet()) {
//			Set<String> usernames = entry.getValue().getUsernames();
//			for (String username : usernames) {
//				if (!mapId.containsKey(authorId))
//					mapId.put(authorId, id);
////				else
////				System.err.println("\n\nERROR in method resetUsername of class SimpleAliasHandler!!! Username already exist: " + username);
//			}
//		}
//		for (LogCommitInfo commit : commits.values()) {
//			Integer newId = mapId.get(mapIds.get(commit.getUserName()));
//			commit.setAuthorId(newId);
//		}
//		Map<String, Integer> newMapId = new HashMap<String, Integer>();
//		for (LogCommitInfo commit : commits.values()) {
//			assert !newMapId.containsKey(commit.getUserName())||newMapId.get(commit.getUserName())==commit.getAuthorId();
//			newMapId.put(commit.getUserName(), commit.getAuthorId());
//		}
//		mapIds = newMapId;
//	}
	
	private void setUsername(Map<String, LogCommitInfo> commits) {
		for (LogCommitInfo commit : commits.values()) {
			if (!commit.getNormMainEmail().isEmpty())
				commit.setUserName(commit.getNormMainEmail());
			else if (!commit.getMainName().isEmpty())
				commit.setUserName(commit.getNormMainName());
			else{
				System.err.println("Empty usernam in commit  " + commit.getSha());
				commit.setUserName("");
			}
		}
		
	}
}
