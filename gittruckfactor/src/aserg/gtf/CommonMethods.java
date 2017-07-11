package aserg.gtf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import aserg.gtf.commands.SystemCommandExecutor;
import aserg.gtf.dao.ProjectInfoDAO;
import aserg.gtf.model.DeveloperInfo;
import aserg.gtf.model.LogCommitInfo;
import aserg.gtf.model.NewFileInfo;
import aserg.gtf.model.ProjectInfo;
import aserg.gtf.model.ProjectStatus;
import aserg.gtf.model.authorship.Repository;
import aserg.gtf.model.newstudy.Measure;
import aserg.gtf.task.DOACalculator;
import aserg.gtf.task.SimpleAliasHandler;
import aserg.gtf.task.extractor.FileInfoExtractor;
import aserg.gtf.task.extractor.GitLogExtractor;
import aserg.gtf.task.extractor.LinguistExtractor;
import aserg.gtf.truckfactor.GreedyTruckFactor;
import aserg.gtf.truckfactor.TFInfo;
import aserg.gtf.truckfactor.TruckFactor;

public class CommonMethods {
	protected FileInfoExtractor fileExtractor;
	protected LinguistExtractor linguistExtractor;
	protected GitLogExtractor gitLogExtractor;	
	private String repositoryPath;
	private String repositoryName;

	public CommonMethods(String repositoryPath, String repositoryName) {
		this.repositoryName = repositoryName;
		this.repositoryPath = repositoryPath;

		//		try {
		//			filesInfo = FileInfoReader.getFileInfo("repo_info/filtered-files.txt");
		//		} catch (IOException e) {
		//			LOGGER.warn("Not possible to read repo_info/filtered-files.txt file. File filter step will not be executed!");
		//			filesInfo = null;
		//		}		
		//		try {
		//			aliasInfo = FileInfoReader.getFileInfo("repo_info/alias.txt");
		//		} catch (IOException e) {
		//			LOGGER.warn("Not possible to read repo_info/alias.txt file. Aliases treating step will not be executed!");
		//			aliasInfo = null;
		//		}
		//		try {
		//			modulesInfo = FileInfoReader.getFileInfo("repo_info/modules.txt");
		//		} catch (IOException e) {
		//			LOGGER.warn("Not possible to read repo_info/modules.txt file. No modules info will be setted!");
		//			modulesInfo = null;
		//		}


		fileExtractor = new FileInfoExtractor(repositoryPath, repositoryName);
		linguistExtractor =  new LinguistExtractor(repositoryPath, repositoryName);
		//		aliasHandler =  aliasInfo == null ? null : new NewAliasHandler(aliasInfo.get(repositoryName));
		gitLogExtractor = new GitLogExtractor(repositoryPath, repositoryName);

	}

	public void insertAdditionalInfo(Measure measure, String repositoryPath, String repositoryName, String scriptsPath, Map<String, LogCommitInfo> allRepoCommits, List<LogCommitInfo> sortedCommitList) throws IOException, Exception {



		String commitSha = getNearCommit(measure.getLastTFLeaverDate(), sortedCommitList).getSha();

		// GET Repository commits
		Map<String, LogCommitInfo> partialRepoCommits = removeCommitsAfter(allRepoCommits, commitSha);
		Map<String, Integer> mapIds = new SimpleAliasHandler().execute(repositoryName, partialRepoCommits);
		Map<String, DeveloperInfo> repositoryDevelopers = getRepositoryDevelopers(partialRepoCommits, mapIds);	

		//Extract file info at the new moment
		String stdOut = createAndExecuteCommand(scriptsPath+"getInfoAtSpecifcCommit.sh "+ repositoryPath + " " + commitSha);
		//		System.out.println(stdOut);

		//		initializeExtractors(repositoryPath, repositoryName);	
		// GET Repository files
		List<NewFileInfo> files = fileExtractor.execute();
		files = linguistExtractor.setNotLinguist(files);	

		measure.setEventNCommits(partialRepoCommits.size());
		measure.setEventNDevs(getNAuthors(repositoryDevelopers));
		measure.setEventNAllFiles(files.size());
		int count=0;
		for (NewFileInfo newFileInfo : files) {
			if(newFileInfo.getFiltered()==false)
				count++;
		}
		measure.setEventNSourceFiles(count);

	}


	public Map<String, LogCommitInfo> removeCommitsAfter(
			Map<String, LogCommitInfo> commits, String sha) {
		Date endDate =  commits.get(sha).getMainCommitDate();

		return filterCommitsByDate(commits, endDate);
	}

	public TFInfo getTF(Date calcDate, String repositoryName,
			String repositoryPath, Map<String, LogCommitInfo> allRepoCommits,
			Map<String, DeveloperInfo> repositoryDevelopers, LogCommitInfo nearCommit) throws IOException, Exception {

		Map<String, LogCommitInfo> partialRepoCommits = filterCommitsByDate(allRepoCommits, calcDate);

		//Extract file info at the new moment
		String stdOut = createAndExecuteCommand("./getInfoAtSpecifcCommit.sh "+ repositoryPath + " " + nearCommit.getSha());

		//			initializeExtractors(repositoryPath, repositoryName);	
		// GET Repository files
		List<NewFileInfo> files = fileExtractor.execute();
		files = linguistExtractor.setNotLinguist(files);	

		// GET Repository DOA results
		DOACalculator doaCalculator = new DOACalculator(repositoryPath, repositoryName, partialRepoCommits.values(), files);
		Repository repository = doaCalculator.execute();

		// GET Repository TF
		TruckFactor truckFactor = new GreedyTruckFactor();
		TFInfo tf = truckFactor.getTruckFactor(repository);

		return tf;
	}
	public LogCommitInfo getNearCommit(Date calcDate,
			List<LogCommitInfo> sortedCommitList) {
		LogCommitInfo retCommit = sortedCommitList.get(0);
		for (LogCommitInfo logCommitInfo : sortedCommitList) {
			if (logCommitInfo.getMainCommitDate().before(calcDate) || logCommitInfo.getMainCommitDate().equals(calcDate))
				retCommit = logCommitInfo;
			else 
				return retCommit;
		}
		return retCommit;
	}
	public void updateRepo(ProjectInfoDAO projectDAO,
			ProjectInfo projectInfo, String repositoryName,
			String repositoryPath, Map<String, LogCommitInfo> allRepoCommits,
			Map<String, DeveloperInfo> repositoryDevelopers) throws Exception,
			IOException {
		projectInfo.setNumAuthors(getNAuthors(repositoryDevelopers)); 		

		// GET Repository files
		List<NewFileInfo> files = fileExtractor.execute();
		files = linguistExtractor.setNotLinguist(files);	

		// GET Repository DOA results
		DOACalculator doaCalculator = new DOACalculator(repositoryPath, repositoryName, allRepoCommits.values(), files);
		Repository repository = doaCalculator.execute();

		// GET Repository TF
		TruckFactor truckFactor = new GreedyTruckFactor();
		TFInfo tf = truckFactor.getTruckFactor(repository);

		projectInfo.setTf(tf.getTf());
		System.out.println(tf);
		projectInfo.setStatus(ProjectStatus.TF_COMPUTED);
		projectDAO.update(projectInfo);
	}
	public int daysBetween(Date d1, Date d2){
		return (int)( (d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
	}
	public Map<String, LogCommitInfo> filterCommitsByDate(
			Map<String, LogCommitInfo> commits, Date endDate) {
		Map<String, LogCommitInfo> newCommits =  new HashMap<String, LogCommitInfo>(commits);
		for (Entry<String, LogCommitInfo> entry : commits.entrySet()) {
			if (entry.getValue().getMainCommitDate().after(endDate))
				newCommits.remove(entry.getKey());
		}
		return newCommits;
	}

	public List<LogCommitInfo> getSortedCommitList(
			Map<String, LogCommitInfo> commits) {
		List<LogCommitInfo> newListOfCommits = new ArrayList<LogCommitInfo>(commits.values());
		Collections.sort(newListOfCommits, new Comparator<LogCommitInfo>() {

			@Override
			public int compare(LogCommitInfo lhs, LogCommitInfo rhs) {
				return  lhs.getMainCommitDate().compareTo(rhs.getMainCommitDate());
			}
		});
		return newListOfCommits;
	}



	public int getNAuthors(
			Map<String, DeveloperInfo> repositoryDevelopers) {
		Set<Integer> userIds = new HashSet<Integer>();
		for (DeveloperInfo dev : repositoryDevelopers.values()) {
			userIds.add(dev.getUserId());
		}
		return userIds.size();
	}

	public Map<String, DeveloperInfo> getRepositoryDevelopers(
			Map<String, LogCommitInfo> commits, Map<String, Integer> mapIds) {
		Map<Integer, DeveloperInfo> tempMap = new HashMap<Integer, DeveloperInfo>();
		for (LogCommitInfo commit : commits.values()) {
			Integer userId = mapIds.get(commit.getUserName());
			if (!tempMap.containsKey(userId))
				tempMap.put(userId, new DeveloperInfo(commit.getNormMainName(), commit.getNormMainEmail(), commit.getUserName(), userId));
			tempMap.get(userId).addCommit(commit);	
		}
		Map<String, DeveloperInfo> repositoryDevelopers = new HashMap<String, DeveloperInfo>();
		for (Entry<String, Integer> entry : mapIds.entrySet()) {
			repositoryDevelopers.put(entry.getKey(), tempMap.get(entry.getValue()));
		}
		return repositoryDevelopers;
	}

	public void print(Map<String, LogCommitInfo> commits) {
		for (LogCommitInfo commit : commits.values()) {
			System.out.printf("%s;%s;%s;%s;%s;%s\n", commit.getSha(), commit.getAuthorName(), commit.getAuthorEmail(), 
					commit.getCommitterName(), commit.getCommitterEmail(), commit.getUserName());
		}

	}
	public void print2(Map<String, LogCommitInfo> commits) {
		for (LogCommitInfo commit : commits.values()) {
			System.out.printf("%s;%s;%s;%s;%s;%s\n", commit.getSha(), commit.getAuthorName(), commit.getAuthorEmail(), 
					commit.getCommitterName(), commit.getCommitterEmail(), commit.getAuthorId());
		}

	}



	public String createAndExecuteCommand(String cmd) throws IOException, Exception {
		SystemCommandExecutor commandExecutor = createCommand(cmd);
		int result = commandExecutor.executeCommand();
		return commandExecutor.getStandardOutputFromCommand().toString();
	}

	private static SystemCommandExecutor createCommand(String cmd) {
		List<String> command = new ArrayList<String>();
		for (String str : cmd.split(" ")) {
			command.add(str);
		}

		// execute my command
		SystemCommandExecutor commandExecutor = new SystemCommandExecutor(command);
		return commandExecutor;
	}
}
