package aserg.gtf;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import org.apache.log4j.Logger;

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
import aserg.gtf.truckfactor.PrunedGreedyTruckFactor;
import aserg.gtf.truckfactor.TFInfo;
import aserg.gtf.truckfactor.TruckFactor;
import aserg.gtf.util.LineInfo;

public class CommonMethods {
	private static final Logger LOGGER = Logger.getLogger(GitTruckFactor.class);
	protected FileInfoExtractor fileExtractor;
	protected LinguistExtractor linguistExtractor;
	public GitLogExtractor gitLogExtractor;	
	private String repositoryPath;
	private String repositoryName;
	private static Map<String, List<LineInfo>> aliasInfo;

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

	public void replaceNamesInLogCommitFile(
			List<LineInfo> repoAliasInfo) {
		Path path = Paths.get(repositoryPath+"commitinfo.log");
		Charset charset = StandardCharsets.UTF_8;
		try {
			String content = new String(Files.readAllBytes(path), charset);
			for (LineInfo lineInfo : repoAliasInfo) {
				
				content = content.replace(";-"+lineInfo.getValues().get(1)+"-;", ";-"+lineInfo.getValues().get(0)+"-;");
			}
			
			Files.write(path, content.getBytes(charset));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
	}

	public void insertAdditionalInfo(Measure measure, String scriptsPath, Map<String, LogCommitInfo> allRepoCommits, List<LogCommitInfo> sortedCommitList) throws IOException, Exception {



		String commitSha = getNearCommit(measure.getLastTFLeaverDate(), sortedCommitList).getSha();

		// GET Repository commits
		Map<String, LogCommitInfo> partialRepoCommits = removeCommitsAfter(allRepoCommits, commitSha);
		Map<String, Integer> mapIds = new SimpleAliasHandler().execute(repositoryName, partialRepoCommits);
		Map<Integer, DeveloperInfo> repositoryDevelopers = getRepositoryDevelopers(partialRepoCommits, mapIds);	

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

	public TFInfo getTF(Date calcDate, Map<String, LogCommitInfo> allRepoCommits, LogCommitInfo nearCommit) throws IOException, Exception {
		
		Map<String, LogCommitInfo> partialRepoCommits = filterCommitsByDate(allRepoCommits, calcDate);

		//Extract file info at the new moment
		String stdOut = createAndExecuteCommand("./getInfoAtSpecifcCommit.sh "+ repositoryPath + " " + nearCommit.getSha());
		System.out.println(stdOut);
		//			initializeExtractors(repositoryPath, repositoryName);	
		
		if (aliasInfo!= null  && aliasInfo.containsKey(repositoryName))
			this.replaceNamesInLogCommitFile(aliasInfo.get(repositoryName));
		// GET Repository files
		List<NewFileInfo> files = fileExtractor.execute();
		files = linguistExtractor.setNotLinguist(files);	

		// GET Repository DOA results
		DOACalculator doaCalculator = new DOACalculator(repositoryPath, repositoryName, partialRepoCommits.values(), files);
		Repository repository = doaCalculator.execute();

		// GET Repository TF
		TruckFactor truckFactor = new PrunedGreedyTruckFactor(0.1f);
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
			ProjectInfo projectInfo, Map<String, LogCommitInfo> allRepoCommits,
			Map<Integer, DeveloperInfo> repositoryDevelopers, LogCommitInfo firstCommit, LogCommitInfo lastCommit) throws Exception,
			IOException {
		projectInfo.setFirstCommit(firstCommit.getMainCommitDate());
		projectInfo.setLastCommit(lastCommit.getMainCommitDate());
		projectInfo.setNumAuthors(getNAuthors(repositoryDevelopers)); 		

		// GET Repository files
		List<NewFileInfo> files = fileExtractor.execute();
		files = linguistExtractor.setNotLinguist(files);	
		
		projectInfo.setNumFiles(getNumFiles(files));

		// GET Repository DOA results
		DOACalculator doaCalculator = new DOACalculator(repositoryPath, repositoryName, allRepoCommits.values(), files);
		Repository repository = doaCalculator.execute();

		// GET Repository TF
		TruckFactor truckFactor = new PrunedGreedyTruckFactor(0.1f);
		TFInfo tf = truckFactor.getTruckFactor(repository);

		projectInfo.setTf(tf.getTf());
		System.out.println(tf);
		projectInfo.setStatus(ProjectStatus.TF_COMPUTED);
		projectDAO.update(projectInfo);
	}
	private int getNumFiles(List<NewFileInfo> files) {
		int n=0;
		for (NewFileInfo newFileInfo : files) {
			if (!newFileInfo.getFiltered())
				n++;
		}
		return n;
	}

	public static int daysBetween(Date d1, Date d2){
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
			Map<Integer, DeveloperInfo> repositoryDevelopers) {
		Set<Integer> userIds = new HashSet<Integer>();
		for (DeveloperInfo dev : repositoryDevelopers.values()) {
//			System.out.println(dev);
			userIds.add(dev.getUserId());
		}
		return userIds.size();
	}

	public Map<Integer, DeveloperInfo> getRepositoryDevelopers(
			Map<String, LogCommitInfo> commits, Map<String, Integer> mapIds) {
		Map<Integer, DeveloperInfo> tempMap = new HashMap<Integer, DeveloperInfo>();
		for (LogCommitInfo commit : commits.values()) {
			Integer userId = mapIds.get(commit.getUserName());
			if (!tempMap.containsKey(userId))
				tempMap.put(userId, new DeveloperInfo(commit.getNormMainName(), commit.getNormMainEmail(), commit.getUserName(), userId));
			tempMap.get(userId).addCommit(commit);	
		}
		Map<Integer, DeveloperInfo> repositoryDevelopers = new HashMap<Integer, DeveloperInfo>();
		for (Entry<String, Integer> entry : mapIds.entrySet()) {
			if (tempMap.get(entry.getValue())!=null)
					repositoryDevelopers.put(entry.getValue(), tempMap.get(entry.getValue()));
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
