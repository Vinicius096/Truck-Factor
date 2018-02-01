package aserg.gtf.task;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import aserg.gtf.dao.GitHubDeveloperDAO;
import aserg.gtf.model.GitHubDeveloper;
import aserg.gtf.model.LogCommitInfo;
import aserg.gtf.util.LineInfo;


public class GitHubUsersAliasHandler{
	static List<GitHubDeveloper> gitHubDevInfo = null;
	static Map<String, GitHubDeveloper> gitHubDevMap = null;
	List<LineInfo> fileAliases;
	Map<String, NewAlias> mapIds = new HashMap<String, NewAlias>();
	public GitHubUsersAliasHandler(List<LineInfo> list) {
		this.fileAliases = list;
	}
	public GitHubUsersAliasHandler() {
		this.fileAliases = null;
	}
	
	
	public Map<String, Integer> execute(String repositoryName, Map<String, LogCommitInfo> commits){
		if (gitHubDevMap == null){
			gitHubDevInfo = new GitHubDeveloperDAO().findAll(null);
			gitHubDevMap = new HashMap<String, GitHubDeveloper>();
			for (GitHubDeveloper gitHubDev : gitHubDevInfo) {
				Set<String> pairsNameEmail = gitHubDev.getPairsNameEmail();
				for (String nameEmail : pairsNameEmail) {
					String nameEmailAux = StringUtils.stripAccents(nameEmail);
					if (gitHubDevMap.containsKey(nameEmailAux) && gitHubDevMap.get(nameEmailAux).getGitHubId() != gitHubDev.getGitHubId())
						System.err.println("DUPLICATED PAIR_NAME-EMAIL " + nameEmailAux);
					gitHubDevMap.put(nameEmailAux, gitHubDev);
				}
			}
		}
			
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
				devNameMap.put(pairNameUpper, new NewAlias());
			
			devNameMap.get(pairNameUpper).addPair(pair);
			
//			if (!devNameMap.containsKey(pairNameUpper))
//				devNameMap.put(pairNameUpper, pair.getAlias());
//
//			for (Pair innerPair : pair.getAlias().getPairs()) {
//				devNameMap.get(pairNameUpper).addPair(innerPair);
//				innerPair.setAlias(devNameMap.get(pairNameUpper));
//			}
		}
		
		for (NewAlias nameAlias : devNameMap.values()) {
			nameAlias.normalizeId();
		}
		
		// Add GitHub developer id when it exists
		for (Pair pair : pairs.values()) {
			if (gitHubDevMap.containsKey(pair.getPairString())){
				GitHubDeveloper gitHubPairDev = gitHubDevMap.get(pair.getPairString());
				pair.setGitHubId(gitHubPairDev.getGitHubId());
				for (Pair innerPair : pair.getAlias().getPairs()) {
					
					innerPair.setGitHubId(gitHubPairDev.getGitHubId());
				}
			}

			
		}
		
		unifyIds(pairs);
		
//		for (Entry<String, Pair> entry : pairs.entrySet()) System.out.println(entry.getKey() + ";" + entry.getValue().getUserId()+ ";" + entry.getValue().getGitHubId());
//		for (Entry<String, Pair> entry : pairs.entrySet()) System.out.println(entry.getKey() + ";" + entry.getValue().getUserId());
		Map<String, Integer> map = new HashMap<String, Integer>();
		
//		for (Entry<String, Pair> entry : pairs.entrySet()) System.out.println(entry.getKey() + ";" + entry.getValue().getUserId()+ ";" + entry.getValue().getGitHubId());
		
		for (LogCommitInfo commit : commits.values()) {
			String authorPairName = commit.getAuthorName().toUpperCase()+"**"+commit.getAuthorEmail().toUpperCase();
			String committerPairName = commit.getCommitterName().toUpperCase()+"**"+commit.getCommitterEmail().toUpperCase();
			int devId = pairs.get(!authorPairName.isEmpty()?authorPairName:committerPairName).getGitHubId();
			commit.setAuthorId(devId);
			map.put(commit.getUserName(), devId);
		}
//		for (Entry<String, Pair> entry : pairs.entrySet()) System.out.println(entry.getKey() + ";" + entry.getValue().getUserId());
		return map;
	}
		
	private void unifyIds(Map<String, Pair> pairs) {
		Set<Integer> usedIds = new HashSet<Integer>();
		Map<Integer, Set<Pair>> nonGitHubPairs = new HashMap<Integer, Set<Pair>>();
		for (Pair pair : pairs.values()) {
			if (pair.getGitHubId() == 0){
				int userId = pair.getUserId();
				if (!nonGitHubPairs.containsKey(userId))
					nonGitHubPairs.put(userId, new HashSet<Pair>());
				nonGitHubPairs.get(userId).add(pair);
			}
			else{
				usedIds.add(pair.getGitHubId());
			}
		}
		int newId = 1;
		for (Entry<Integer, Set<Pair>> entry : nonGitHubPairs.entrySet()) {
			Set<Pair> values = entry.getValue();
			while (usedIds.contains(newId))
				newId++;
			for (Pair pair : values) {
				pair.setGitHubId(newId);
			}
			newId++;
		}
		
		
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
