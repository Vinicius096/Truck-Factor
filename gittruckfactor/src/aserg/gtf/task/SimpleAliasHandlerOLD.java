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

public class SimpleAliasHandlerOLD{
	List<LineInfo> fileAliases;
	Map<String, Integer> mapIds = new HashMap<String, Integer>();
	public SimpleAliasHandlerOLD(List<LineInfo> list) {
		this.fileAliases = list;
	}
	public SimpleAliasHandlerOLD() {
		this.fileAliases = null;
	}
	
	
	public Map<String, Integer> execute(String repositoryName, Map<String, LogCommitInfo> commits){
		setUsername(commits);
		HashMap<String, Set<String>> devEmailMap = new HashMap<String, Set<String>>();
		HashMap<String, Set<String>> devNameMap = new HashMap<String, Set<String>>();
		
		for (LogCommitInfo commit : commits.values()) {
			String commitMainEmail = commit.getNormMainEmail();
			if (!devEmailMap.containsKey(commitMainEmail))
				devEmailMap.put(commitMainEmail, new HashSet<String>());
			devEmailMap.get(commitMainEmail).add(commit.getUserName());
		}
		
		setAuthorsId(devEmailMap, commits);
		
		for (LogCommitInfo commit : commits.values()) {
			String commitMainName = commit.getNormMainName();
			//Avoid to group developers with blank name
			if (!commitMainName.isEmpty()){
				if (!devNameMap.containsKey(commitMainName))
					devNameMap.put(commitMainName, new HashSet<String>());
				devNameMap.get(commitMainName).add(commit.getUserName());
			}
		}
		
		resetAuthorsId(devNameMap, commits);
		
		return mapIds;
	}

	private void setAuthorsId(HashMap<String, Set<String>> devMap,
			Map<String, LogCommitInfo> commits) {
		Map<String, Integer> mapId = new HashMap<String, Integer>();
		int id = 0;
		for (Entry<String, Set<String>> entry : devMap.entrySet()) {
			Set<String> usernames = entry.getValue();
			id++;
			for (String username : usernames) {
				if (!mapId.containsKey(username))
					mapId.put(username, id);
//				else
//				System.err.println("\n\nERROR in method resetUsername of class SimpleAliasHandler!!! Username already exist: " + username);
			}
		}
		for (LogCommitInfo commit : commits.values()) {
			commit.setAuthorId(mapId.get(commit.getUserName()));
		}
		mapIds = mapId;
	}
	
	private void resetAuthorsId(HashMap<String, Set<String>> devMap,
			Map<String, LogCommitInfo> commits) {
		Map<Integer, Integer> mapId = new HashMap<Integer, Integer>();
		int id = 0;
		for (Entry<String, Set<String>> entry : devMap.entrySet()) {
			Set<String> usernames = entry.getValue();
			id++;
			for (String username : usernames) {
				Integer authorId = mapIds.get(username);
				if (!mapId.containsKey(authorId))
					mapId.put(authorId, id);
//				else
//				System.err.println("\n\nERROR in method resetUsername of class SimpleAliasHandler!!! Username already exist: " + username);
			}
		}
		for (LogCommitInfo commit : commits.values()) {
			Integer newId = mapId.get(mapIds.get(commit.getUserName()));
			commit.setAuthorId(newId);
		}
		Map<String, Integer> newMapId = new HashMap<String, Integer>();
		for (LogCommitInfo commit : commits.values()) {
			assert !newMapId.containsKey(commit.getUserName())||newMapId.get(commit.getUserName())==commit.getAuthorId();
			newMapId.put(commit.getUserName(), commit.getAuthorId());
		}
		mapIds = newMapId;
	}
	
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
