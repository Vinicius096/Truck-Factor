package aserg.gtf.task;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import aserg.gtf.dao.GitHubDeveloperDAO;
import aserg.gtf.dao.UserAliasInfoDAO;
import aserg.gtf.model.GitHubDeveloper;
import aserg.gtf.model.LogCommitInfo;
import aserg.gtf.model.UserAliasInfo;
import aserg.gtf.util.LineInfo;


public class NewGitHubUsersAliasHandler{
	static List<GitHubDeveloper> gitHubDevInfo = null;
	static Map<String, GitHubDeveloper> gitHubDevMap = null;
	List<LineInfo> fileAliases;
	Map<String, GitDev> mapIds = new HashMap<String, GitDev>();
	public NewGitHubUsersAliasHandler(List<LineInfo> list) {
		this.fileAliases = list;
	}
	public NewGitHubUsersAliasHandler() {
		this.fileAliases = null;
	}
	
	
	public Map<String, Integer> execute(String repositoryName, Map<String, LogCommitInfo> commits, boolean saveAliasInfo){
		if (gitHubDevMap == null){
			System.out.println("Getting GitHub users info");
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
			System.out.println("Collected " + gitHubDevMap.size() + " GitHub users");
		}
			
		Map<Integer, Set<GitDev>> mapIds = new HashMap<Integer, Set<GitDev>>();
		Map<String, GitDev> devs = getDevs(commits);
		for (GitDev dev : devs.values()) {
			int devId = dev.getDevId();
			if (!mapIds.containsKey(devId))
				mapIds.put(devId, new HashSet<GitDev>());
			mapIds.get(devId).add(dev);
		}
		
		
		//Group pairs with the same e-mail
		HashMap<String, Set<Integer>> devEmailMap = new HashMap<String, Set<Integer>>();
		for (Entry<String, GitDev> entry : devs.entrySet()) {
		String[] split = entry.getKey().split("\\*\\*");
		if (split.length ==2 && !split[1].isEmpty()){
			String pairEmailUpper = split[1];
			if (!devEmailMap.containsKey(pairEmailUpper))
				devEmailMap.put(pairEmailUpper, new HashSet<Integer>());
			devEmailMap.get(pairEmailUpper).add(entry.getValue().getDevId());	
		}
		}
		setNewIds(mapIds, devEmailMap);
		
		//Group pairs with the same name
		HashMap<String, Set<Integer>> devNameMap = new HashMap<String, Set<Integer>>();
		for (Entry<String, GitDev> entry : devs.entrySet()) {
			String[] split = entry.getKey().split("\\*\\*");
			if (split.length >=1 && split[0].length()>2 && !split[0].equalsIgnoreCase("ROOT")){
				String pairNameUpper = split[0];
				if (!devNameMap.containsKey(pairNameUpper))
					devNameMap.put(pairNameUpper, new HashSet<Integer>());
				devNameMap.get(pairNameUpper).add(entry.getValue().getDevId());
			}
		}
		setNewIds(mapIds, devNameMap);

		// Add GitHub developer id when it exists
		for (GitDev dev : devs.values()) {
			if (gitHubDevMap.containsKey(dev.getPairString())){
				GitHubDeveloper gitHubPairDev = gitHubDevMap.get(dev.getPairString());
				
				// Merge only pairs mapped to an GitHub userName
//				dev.setGitHubId(gitHubPairDev.getGitHubId());
				
				// Merge pairs mapped to an GitHub username and all name and e-mail aliases
				setGitHubId(mapIds, dev.getDevId(), gitHubPairDev.getGitHubId());
			}
		}
		unifyIds(devs);
		
		if (saveAliasInfo)
			saveAlias(repositoryName, devs);
		
//		for (Entry<String, GitDev> entry : devs.entrySet()) System.out.println(entry.getKey() + ";" + entry.getValue().getDevId()+ ";" + entry.getValue().getGitHubId());
//		for (Entry<String, Pair> entry : pairs.entrySet()) System.out.println(entry.getKey() + ";" + entry.getValue().getUserId()+ ";" + entry.getValue().getGitHubId());
//		for (Entry<String, Pair> entry : pairs.entrySet()) System.out.println(entry.getKey() + ";" + entry.getValue().getUserId());
		Map<String, Integer> map = new HashMap<String, Integer>();
		
//		for (Entry<String, Pair> entry : pairs.entrySet()) System.out.println(entry.getKey() + ";" + entry.getValue().getUserId()+ ";" + entry.getValue().getGitHubId());
		
		for (LogCommitInfo commit : commits.values()) {
			String authorPairName = commit.getAuthorName().toUpperCase()+"**"+commit.getAuthorEmail().toUpperCase();
			String committerPairName = commit.getCommitterName().toUpperCase()+"**"+commit.getCommitterEmail().toUpperCase();
			int devId = devs.get(!authorPairName.isEmpty()?authorPairName:committerPairName).getGitHubId();
			commit.setAuthorId(devId);
			map.put(commit.getUserName(), devId);
		}
//		for (Entry<String, Integer> entry : map.entrySet()) System.out.println(entry.getKey() + ";" + entry.getValue());
		return map;
	}
	private void saveAlias(String repositoryName, Map<String, GitDev> devs) {
		Map<Integer, Set<String>> mapAlias =  new HashMap<Integer, Set<String>>();
		Set<UserAliasInfo> aliasesInfo =  new HashSet<UserAliasInfo>();
		for (Entry<String, GitDev> entry : devs.entrySet()) {
			GitDev gitDev = entry.getValue();
			int gitHubId = gitDev.getGitHubId();
			if (!mapAlias.containsKey(gitHubId))
					mapAlias.put(gitHubId, new HashSet<String>());
			mapAlias.get(gitHubId).add(entry.getKey());
		}
		for (Entry<Integer, Set<String>> entry : mapAlias.entrySet()) {
			Integer userId = entry.getKey();
			Set<String> aliases = entry.getValue();
			aliasesInfo.add(new UserAliasInfo(repositoryName, userId, aliases));
		}
		new UserAliasInfoDAO().persistAll(aliasesInfo);
		
	}
	private void setGitHubId(Map<Integer, Set<GitDev>> mapIds, int devId,
			int gitHubId) {
		Set<GitDev> devs = mapIds.get(devId);
		for (GitDev dev : devs) {
			dev.setGitHubId(gitHubId);
		}
		
	}
	private void setNewIds(Map<Integer, Set<GitDev>> mapIds,
			HashMap<String, Set<Integer>> aliasMap) {
		Map<Integer, Integer> shiftId = new HashMap<Integer, Integer>();
		// Initialize ids as they are in mapIds
		for (Integer id : mapIds.keySet()) {
			shiftId.put(id, id);
		}	
		for (Entry<String, Set<Integer>> entry : aliasMap.entrySet()) {
			Set<Integer> ids = entry.getValue();
				
			if (ids.size()>1){
				int newId = GitDev.getNewId();
				mapIds.put(newId, new HashSet<GitDev>());
				for (Integer id : ids) {
					int oldId = shiftId.get(id);
					Set<GitDev> aliasDevs = mapIds.get(oldId);
					for (GitDev dev : aliasDevs) {
						dev.setDevId(newId);
						mapIds.get(newId).add(dev);
					}
					shiftId.put(oldId, newId);
				}
			}
		}
	}
		
	private void unifyIds(Map<String, GitDev> devs) {
		Set<Integer> usedIds = new HashSet<Integer>();
		Map<Integer, Set<GitDev>> nonGitHubPairs = new HashMap<Integer, Set<GitDev>>();
		for (GitDev dev : devs.values()) {
			if (dev.getGitHubId() == 0){
				int userId = dev.getDevId();
				if (!nonGitHubPairs.containsKey(userId))
					nonGitHubPairs.put(userId, new HashSet<GitDev>());
				nonGitHubPairs.get(userId).add(dev);
			}
			else{
				usedIds.add(dev.getGitHubId());
			}
		}
		int newId = -1;
		for (Entry<Integer, Set<GitDev>> entry : nonGitHubPairs.entrySet()) {
			Set<GitDev> values = entry.getValue();
			while (usedIds.contains(newId))
				newId--;
			for (GitDev dev : values) {
				dev.setGitHubId(newId);
			}
			newId--;
		}
		
		
	}
	
	private Map<String, GitDev> getDevs(Map<String, LogCommitInfo> commits) {
		Map<String, GitDev> devs = new HashMap<String, GitDev>();
//		Set<Pair> pairs = new HashSet<Pair>();
		for (LogCommitInfo commit : commits.values()) {
			String authorPairString = GitDev.getPairString(commit.getAuthorName(), commit.getAuthorEmail());
			String committerPairString = GitDev.getPairString(commit.getCommitterName(), commit.getCommitterEmail());
			if(!devs.containsKey(authorPairString))
				devs.put(authorPairString, new SingleDev(commit.getAuthorName(), commit.getAuthorEmail(), commit.getSha()));
			if(!devs.containsKey(committerPairString))
				devs.put(committerPairString, new SingleDev(commit.getCommitterName(), commit.getCommitterEmail(), commit.getSha()));
		}
		return devs;
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
	
	
//	BACKUP
//	public Map<String, Integer> execute(String repositoryName, Map<String, LogCommitInfo> commits){
//		if (gitHubDevMap == null){
//			gitHubDevInfo = new GitHubDeveloperDAO().findAll(null);
//			gitHubDevMap = new HashMap<String, GitHubDeveloper>();
//			for (GitHubDeveloper gitHubDev : gitHubDevInfo) {
//				Set<String> pairsNameEmail = gitHubDev.getPairsNameEmail();
//				for (String nameEmail : pairsNameEmail) {
//					String nameEmailAux = StringUtils.stripAccents(nameEmail);
//					if (gitHubDevMap.containsKey(nameEmailAux) && gitHubDevMap.get(nameEmailAux).getGitHubId() != gitHubDev.getGitHubId())
//						System.err.println("DUPLICATED PAIR_NAME-EMAIL " + nameEmailAux);
//					gitHubDevMap.put(nameEmailAux, gitHubDev);
//				}
//			}
//		}
//			
////		setUsername(commits);
//		Map<Integer, Set<GitDev>> mapIds = new HashMap<Integer, Set<GitDev>>();
//		Map<String, GitDev> devs = getDevs(commits);
//		for (GitDev dev : devs.values()) {
//			int devId = dev.getDevId();
//			if (!mapIds.containsKey(devId))
//				mapIds.put(devId, new HashSet<GitDev>());
//			mapIds.get(devId).add(dev);
//		}
//		
//		HashMap<String, GitDev> devEmailMap = new HashMap<String, GitDev>();
//		HashMap<String, GitDev> devNameMap = new HashMap<String, GitDev>();
//		
//		//Group pairs with the same e-mail
//		for (Entry<String, GitDev> entry : devs.entrySet()) {
//			GitDev dev = entry.getValue();
//			GitDev tempDev = dev;
//			String pairEmailUpper = entry.getKey().split("\\*\\*")[1];
//			if (!devEmailMap.containsKey(pairEmailUpper))
//				devEmailMap.put(pairEmailUpper, dev);
//			else{
//				tempDev = devEmailMap.get(pairEmailUpper).addAlias(dev, entry.getKey());
//				devEmailMap.put(pairEmailUpper, tempDev);
//				
//			}
////			devsAux.put(entry.getKey(), tempDev);
////			devs.put(entry.getKey(), tempDev);
//		}
//		for (Entry<String, GitDev> entry : devs.entrySet()) {
//			String pairEmailUpper = entry.getKey().split("\\*\\*")[1];
//			devs.put(entry.getKey(), devEmailMap.get(pairEmailUpper));
//		}
//		
//		for (Entry<String, GitDev> entry : devs.entrySet()) {
//			GitDev dev = entry.getValue();
//			GitDev tempDev = dev;
//			String pairNameUpper = entry.getKey().split("\\*\\*")[0];
//			if (!devNameMap.containsKey(pairNameUpper))
//				devNameMap.put(pairNameUpper, dev);
//			else{
//				tempDev = devNameMap.get(pairNameUpper).addAlias(dev, entry.getKey());
//				devNameMap.put(pairNameUpper, tempDev);
//			}
////			devsNew.put(entry.getKey(), tempDev);
////			devs.put(entry.getKey(), tempDev);
//		}
//		
//		for (Entry<String, GitDev> entry : devs.entrySet()) {
//			String pairNameUpper = entry.getKey().split("\\*\\*")[0];
//			devs.put(entry.getKey(), devNameMap.get(pairNameUpper));
//		}
//		
//		// Add GitHub developer id when it exists
//		for (GitDev dev : devs.values()) {
//			if (gitHubDevMap.containsKey(dev.getPairString())){
//				GitHubDeveloper gitHubPairDev = gitHubDevMap.get(dev.getPairString());
//				dev.setGitHubId(gitHubPairDev.getGitHubId());
//			}
//
//			
//		}
//		for (Entry<String, GitDev> entry : devs.entrySet()) System.out.println(entry.getKey() + ";" + entry.getValue().getDevId()+ ";" + entry.getValue().getGitHubId());
////		for (Entry<String, Pair> entry : pairs.entrySet()) System.out.println(entry.getKey() + ";" + entry.getValue().getUserId()+ ";" + entry.getValue().getGitHubId());
////		for (Entry<String, Pair> entry : pairs.entrySet()) System.out.println(entry.getKey() + ";" + entry.getValue().getUserId());
//		Map<String, Integer> map = new HashMap<String, Integer>();
//		
////		for (Entry<String, Pair> entry : pairs.entrySet()) System.out.println(entry.getKey() + ";" + entry.getValue().getUserId()+ ";" + entry.getValue().getGitHubId());
//		
//		for (LogCommitInfo commit : commits.values()) {
//			String authorPairName = commit.getAuthorName().toUpperCase()+"**"+commit.getAuthorEmail().toUpperCase();
//			String committerPairName = commit.getCommitterName().toUpperCase()+"**"+commit.getCommitterEmail().toUpperCase();
//			int devId = devs.get(!authorPairName.isEmpty()?authorPairName:committerPairName).getGitHubId();
//			commit.setAuthorId(devId);
//			map.put(commit.getUserName(), devId);
//		}
////		for (Entry<String, Pair> entry : pairs.entrySet()) System.out.println(entry.getKey() + ";" + entry.getValue().getUserId());
//		return map;
//	}
}
