package aserg.gtf;



import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import aserg.gtf.commands.SystemCommandExecutor;
import aserg.gtf.dao.ProjectInfoDAO;
import aserg.gtf.dao.newstudy.MeasureDAO;
import aserg.gtf.model.DeveloperInfo;
import aserg.gtf.model.LogCommitInfo;
import aserg.gtf.model.NewFileInfo;
import aserg.gtf.model.ProjectInfo;
import aserg.gtf.model.ProjectStatus;
import aserg.gtf.model.authorship.Developer;
import aserg.gtf.model.authorship.Repository;
import aserg.gtf.model.newstudy.Measure;
import aserg.gtf.task.DOACalculator;
import aserg.gtf.task.NewAliasHandler;
import aserg.gtf.task.SimpleAliasHandler;
import aserg.gtf.task.extractor.FileInfoExtractor;
import aserg.gtf.task.extractor.GitLogExtractor;
import aserg.gtf.task.extractor.LinguistExtractor;
import aserg.gtf.truckfactor.GreedyTruckFactor;
import aserg.gtf.truckfactor.TFInfo;
import aserg.gtf.truckfactor.TruckFactor;
import aserg.gtf.util.FileInfoReader;
import aserg.gtf.util.LineInfo;

public class NewTFStudy {
	private static final Logger LOGGER = Logger.getLogger(GitTruckFactor.class);
	private static FileInfoExtractor fileExtractor = null;
	private static LinguistExtractor linguistExtractor =  null;
	private static GitLogExtractor gitLogExtractor = null;	
//	private static Map<String, List<LineInfo>> filesInfo;
//	private static Map<String, List<LineInfo>> aliasInfo;
//	private static Map<String, List<LineInfo>> modulesInfo;
	
	private static int chunckSize = 365;
	
	public static void main(String[] args) {
		GitTruckFactor.loadConfiguration();
		MeasureDAO measureDAO = new MeasureDAO();
		ProjectInfoDAO projectDAO = new ProjectInfoDAO();
		List<ProjectInfo> projects= projectDAO.findAll(null);
		String repositoriesPath = "/Users/guilherme/test/github_repositories/";
		
		if (args.length>0)
			repositoriesPath = args[0];
		if (args.length>1)
			chunckSize = Integer.parseInt(args[1]);
		
		
		for (ProjectInfo projectInfo : projects) {
			if (projectInfo.getStatus() == ProjectStatus.DOWNLOADED || projectInfo.getStatus() == ProjectStatus.RECALC){
				projectInfo.setStatus(ProjectStatus.ANALYZING);
				projectDAO.update(projectInfo);
				// build my command as a list of strings
				try {
					String repositoryName = projectInfo.getFullName();
					String repositoryPath = repositoriesPath+repositoryName+"/";
					Date pushed_at = projectInfo.getPushed_at();
					
					String stdOut = createAndExecuteCommand("./reset_repo.sh "+ repositoryPath + " " + projectInfo.getDefault_branch());
					stdOut = createAndExecuteCommand("./get_git_log.sh "+ repositoryPath);
					System.out.println(stdOut);
					
					initializeExtractors(repositoryPath, repositoryName);	
					
					// GET Repository commits
					Map<String, LogCommitInfo> allRepoCommits = gitLogExtractor.execute();
					Map<String, Integer> mapIds = new SimpleAliasHandler().execute(repositoryName, allRepoCommits);
					Map<String, DeveloperInfo> repositoryDevelopers = getRepositoryDevelopers(allRepoCommits, mapIds);	
					
					
					// Update #authors and tf
					updateRepo(projectDAO, projectInfo, repositoryName,
								repositoryPath, allRepoCommits,
								repositoryDevelopers);
					
					List<LogCommitInfo> sortedCommitList = getSortedCommitList(allRepoCommits);
					LogCommitInfo firstCommit = sortedCommitList.get(0);
					LogCommitInfo lastCommit = sortedCommitList.get(sortedCommitList.size()-1);
					
					if (daysBetween(firstCommit.getMainCommitDate(), pushed_at)<=chunckSize){
						String errorMsg = "Development history too short. Less than " + chunckSize + " days.";
						System.err.println(errorMsg);
						projectInfo.setStatus(ProjectStatus.NOTCOMPUTED);
						projectInfo.setErrorMsg(errorMsg);
						projectDAO.update(projectInfo);
						continue;
					}
						


					// Brake the development history in chuncks and apply the algorithm to identify TF events
					List<Measure> repositoryMeasures = new ArrayList<Measure>();
					Calendar calcDate = Calendar.getInstance(); 
					calcDate.setTime(firstCommit.getMainCommitDate()); 
					calcDate.add(Calendar.DATE, chunckSize);
					Date computedDate = new Date();
					while (calcDate.getTime().before(pushed_at)){
						LogCommitInfo nearCommit = getNearCommit(calcDate.getTime(), sortedCommitList);
						TFInfo tf = getTF(calcDate.getTime(), repositoryName,
								repositoryPath, allRepoCommits,
								repositoryDevelopers, nearCommit);
						
						Measure measure = new Measure(repositoryName, calcDate.getTime(), nearCommit.getSha(), tf, computedDate);
						
						
						List<Developer> tfDevelopers = tf.getTfDevelopers();
						

						calcDate.add(Calendar.DATE, chunckSize);
						int nLeavers = 0; 
						for (Developer developer : tfDevelopers) {
							if (!repositoryDevelopers.containsKey(developer.getNewUserName()))
								System.err.println("TF developer was not found: " + developer.getNewUserName());
							DeveloperInfo devInfo = repositoryDevelopers.get(developer.getNewUserName());
							Date devLastCommitDate = devInfo.getLastCommit().getMainCommitDate();
							if (daysBetween(devLastCommitDate, pushed_at)>=chunckSize && devLastCommitDate.before(calcDate.getTime())){
								measure.addLeaver(devInfo);
								nLeavers++;
								System.out.printf("%s left the project in %s (%d-%d)\n", developer, devInfo.getLastCommit().getMainCommitDate(), nLeavers, tf.getTf());
								if (nLeavers == tf.getTf()){
									System.out.println("\n========TF EVENT: " + repositoryName + "=======\n");
								}
							}
						}
						repositoryMeasures.add(measure);
					}
					
					stdOut = createAndExecuteCommand("./reset_repo.sh "+ repositoryPath + " " + projectInfo.getDefault_branch());
					
					for (Measure measure : repositoryMeasures) {
						measureDAO.persist(measure);
					}
					
				} catch (Exception e) {
					System.err.println("NewTFStudy error: " + e.getStackTrace());
					projectInfo.setErrorMsg("NewTFStudy error: " + e.getStackTrace());
					projectInfo.setStatus(ProjectStatus.ERROR);
					projectDAO.update(projectInfo);
				} 

				projectInfo.setStatus(ProjectStatus.ANALYZED);
				projectDAO.update(projectInfo);
				
			}
		}
	}
	private static TFInfo getTF(Date calcDate, String repositoryName,
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
	private static LogCommitInfo getNearCommit(Date calcDate,
			List<LogCommitInfo> sortedCommitList) {
		LogCommitInfo retCommit = sortedCommitList.get(0);
		for (LogCommitInfo logCommitInfo : sortedCommitList) {
			if (logCommitInfo.getMainCommitDate().before(calcDate))
				retCommit = logCommitInfo;
			else 
				return retCommit;
		}
		return null;
	}
	public static void updateRepo(ProjectInfoDAO projectDAO,
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
	private static int daysBetween(Date d1, Date d2){
        return (int)( (d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
	}
	private static Map<String, LogCommitInfo> filterCommitsByDate(
			Map<String, LogCommitInfo> commits, Date endDate) {
		Map<String, LogCommitInfo> newCommits =  new HashMap<String, LogCommitInfo>(commits);
		for (Entry<String, LogCommitInfo> entry : commits.entrySet()) {
			if (entry.getValue().getMainCommitDate().after(endDate))
				newCommits.remove(entry.getKey());
		}
		return newCommits;
	}

	private static List<LogCommitInfo> getSortedCommitList(
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


	private static int getNAuthors(
			Map<String, DeveloperInfo> repositoryDevelopers) {
		Set<Integer> userIds = new HashSet<Integer>();
		for (DeveloperInfo dev : repositoryDevelopers.values()) {
			userIds.add(dev.getUserId());
		}
		return userIds.size();
	}

	private static Map<String, DeveloperInfo> getRepositoryDevelopers(
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

	private static void print(Map<String, LogCommitInfo> commits) {
		for (LogCommitInfo commit : commits.values()) {
			System.out.printf("%s;%s;%s;%s;%s;%s\n", commit.getSha(), commit.getAuthorName(), commit.getAuthorEmail(), 
														 commit.getCommitterName(), commit.getCommitterEmail(), commit.getUserName());
		}
		
	}
	private static void print2(Map<String, LogCommitInfo> commits) {
		for (LogCommitInfo commit : commits.values()) {
			System.out.printf("%s;%s;%s;%s;%s;%s\n", commit.getSha(), commit.getAuthorName(), commit.getAuthorEmail(), 
														 commit.getCommitterName(), commit.getCommitterEmail(), commit.getAuthorId());
		}
		
	}

	public static void initializeExtractors(String repositoryPath, String repositoryName) {
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

	private static String createAndExecuteCommand(String cmd) throws IOException, Exception {
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
