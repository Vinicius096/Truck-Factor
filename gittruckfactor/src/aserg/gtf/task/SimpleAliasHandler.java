package aserg.gtf.task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import aserg.gtf.model.LogCommitInfo;
import aserg.gtf.util.LineInfo;

public class SimpleAliasHandler{
	List<LineInfo> fileAliases;
	Map<String, Alias> mapIds = new HashMap<String, Alias>();
	public SimpleAliasHandler(List<LineInfo> list) {
		this.fileAliases = list;
	}
	public SimpleAliasHandler() {
		this.fileAliases = null;
	}
	
	
	public Map<String, Integer> execute(String repositoryName, Map<String, LogCommitInfo> commits){
		setUsername(commits);
		HashMap<String, Alias> devEmailMap = new HashMap<String, Alias>();
		HashMap<String, Alias> devNameMap = new HashMap<String, Alias>();
		
		for (LogCommitInfo commit : commits.values()) {
			String commitMainEmail = commit.getNormMainEmail();
			if (!devEmailMap.containsKey(commitMainEmail))
				devEmailMap.put(commitMainEmail, new Alias());
			devEmailMap.get(commitMainEmail).addUsername(commit.getUserName());
		}
		
		setAuthorsId(devEmailMap, commits, false);
		
		for (LogCommitInfo commit : commits.values()) {
			String commitMainName = commit.getNormMainName();
			if(commitMainName.isEmpty()){
				commit.fixEmptyName();
				commitMainName = commit.getNormMainName();
			}
				
			//Avoid to group developers with blank name
			if (!commitMainName.isEmpty()){
				if (!devNameMap.containsKey(commitMainName))
					devNameMap.put(commitMainName, mapIds.get(commit.getUserName()));
				devNameMap.get(commitMainName).addUsername(commit.getUserName());
			}
		}
		
		setAuthorsId(devNameMap, commits, true);
		
		return toIntegerId(mapIds);
	}

	private Map<String, Integer> toIntegerId(Map<String, Alias> mapIds2) {
		Map<String, Integer> map = new HashMap<String, Integer>();
		Map<Integer, Alias> printMap = new HashMap<Integer, Alias>();
		for (Entry<String, Alias> entry : mapIds2.entrySet()) {
			map.put(entry.getKey(), entry.getValue().getAliasID());
//			if (entry.getValue().getUsernames().size()>1)
//				printMap.put(entry.getValue().getAliasID(), entry.getValue());
		}
//		System.out.println(printMap);
		return map;
	}
	private void setAuthorsId(HashMap<String, Alias> devEmailMap,
			Map<String, LogCommitInfo> commits, boolean setDevIds) {
		Map<String, Alias> mapId = new HashMap<String, Alias>();
		int id = 0;
		for (Entry<String, Alias> entry : devEmailMap.entrySet()) {
			Set<String> usernames = entry.getValue().getUsernames();
			for (String username : usernames) {
				if (!mapId.containsKey(username))
					mapId.put(username, entry.getValue());
//				else
//				System.err.println("\n\nERROR in method setUsername of class SimpleAliasHandler!!! Username already exist: " + username);
			}
		}
		if (setDevIds)
			for (LogCommitInfo commit : commits.values()) {
				commit.setAuthorId(mapId.get(commit.getUserName()).getAliasID());
			}
		mapIds = mapId;
	}
	
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
