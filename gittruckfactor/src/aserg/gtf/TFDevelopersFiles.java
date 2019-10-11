package aserg.gtf;



import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import aserg.gtf.dao.ProjectInfoDAO;
import aserg.gtf.dao.newstudy.MeasureDAO;
import aserg.gtf.model.DeveloperInfo;
import aserg.gtf.model.LogCommitInfo;
import aserg.gtf.model.ProjectInfo;
import aserg.gtf.model.ProjectStatus;
import aserg.gtf.model.authorship.Developer;
import aserg.gtf.model.newstudy.Measure;
import aserg.gtf.task.NewGitHubUsersAliasHandler;
import aserg.gtf.truckfactor.TFInfo;
import aserg.gtf.util.FileInfoReader;
import aserg.gtf.util.LineInfo;


// Compute the TF in a moment t and verify if the TF developers does not commit after t
public class TFDevelopersFiles {
	private static final Logger LOGGER = Logger.getLogger(GitTruckFactor.class);
//	private static Map<String, List<LineInfo>> filesInfo;
//	private static Map<String, List<LineInfo>> aliasInfo;
//	private static Map<String, List<LineInfo>> modulesInfo;

	private static int chunckSize = 365;
	private static int leaverSize = 365;
	
	public static void main(String[] args) {
		GitTruckFactor.loadConfiguration();
		MeasureDAO measureDAO = new MeasureDAO();
		ProjectInfoDAO projectDAO = new ProjectInfoDAO();
		List<ProjectInfo> projects= projectDAO.findAll(null);
		String repositoriesPath = "/Users/guilherme/test/github_repositories/";
		String scriptsPath = "./";
		
		Map<String, List<LineInfo>> aliasInfo;
		try {
			aliasInfo = FileInfoReader.getFileInfo("repo_info/alias.txt");
		} catch (IOException e) {
			LOGGER.warn("Not possible to read repo_info/alias.txt file. Aliases treating step will not be executed!");
			aliasInfo = null;
		}
		
		if (args.length>0){
			repositoriesPath = args[0];
			if (repositoriesPath.charAt(repositoriesPath.length()-1) != '/')
				repositoriesPath+="/";
		}
		if (args.length>1){
			scriptsPath = args[1];
			if (scriptsPath.charAt(scriptsPath.length()-1) != '/')
				scriptsPath+="/";
		}
		if (args.length>2)
			chunckSize = Integer.parseInt(args[2]);
		if (args.length>3)
			leaverSize = Integer.parseInt(args[3]);
		Date computationDate = new Date();
		String computationInfo = "Computation (Alg2 - GitHub ids usernames or names or emails)- " + computationDate + " - Chunk size = " + chunckSize+ " - Leaver size = " + leaverSize;
		if (args.length>4)
			computationInfo = args[4];
		for (ProjectInfo projectInfo : projects) {
			if (projectInfo.getStatus() == ProjectStatus.ANALYZED)
				System.out.println("ANALYZED = " + projectInfo.getFullName());
			if (projectInfo.getStatus() == ProjectStatus.DOWNLOADED || projectInfo.getStatus() == ProjectStatus.RECALC){
				projectInfo.setStatus(ProjectStatus.ANALYZING);
				projectDAO.update(projectInfo);
				
				String stdOut;
				String repositoryName = projectInfo.getFullName();
				String repositoryPath = repositoriesPath+repositoryName+"/";
				
				try {
					CommonMethods commonMethods = new CommonMethods(repositoryPath, repositoryName);
					
					
					stdOut = commonMethods.createAndExecuteCommand(scriptsPath+"reset_repo.sh "+ repositoryPath + " " + projectInfo.getDefault_branch());
					System.out.println(stdOut);
					stdOut = commonMethods.createAndExecuteCommand(scriptsPath+"get_git_log.sh "+ repositoryPath);
					System.out.println(stdOut);
					
					if (aliasInfo!= null  && aliasInfo.containsKey(repositoryName))
						commonMethods.replaceNamesInLogCommitFile(aliasInfo.get(repositoryName));
					
					// GET Repository commits
					Map<String, LogCommitInfo> allRepoCommits = commonMethods.gitLogExtractor.execute();
//					Map<String, Integer> mapIds = new NewSimpleAliasHandler().execute(repositoryName, allRepoCommits);
					Map<String, Integer> mapIds = new NewGitHubUsersAliasHandler().execute(repositoryName, allRepoCommits, false);
					Map<Integer, DeveloperInfo> repositoryDevelopers = commonMethods.getRepositoryDevelopers(allRepoCommits, mapIds);	
					
//					// Save logs 
//					stdOut = commonMethods.createAndExecuteCommand(scriptsPath+"save_logs.sh "+ repositoryPath + " " + repositoryName.replace("/", "-"));
//					System.out.println(stdOut);

					
//					
					
					List<LogCommitInfo> sortedCommitList = commonMethods.getSortedCommitList(allRepoCommits);
					LogCommitInfo firstCommit = sortedCommitList.get(0);
					LogCommitInfo lastCommit = sortedCommitList.get(sortedCommitList.size()-1);
					
					// Update #authors and tf, and firstCommit date 
					commonMethods.updateRepo(projectDAO, projectInfo, allRepoCommits,
								repositoryDevelopers, firstCommit, lastCommit);
					
					if (CommonMethods.daysBetween(firstCommit.getMainCommitDate(), lastCommit.getMainCommitDate())<=2*chunckSize){
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
					
					// Set created_at as date to start the algorithm
					calcDate.setTime(projectInfo.getCreated_at()); 
					calcDate.add(Calendar.DATE, chunckSize);
					
					
					Measure lastTFEventMeasure = null;
					List<Developer> lastTFEventDevelopers = null;
					// No problem to compute the TF next to last commit because the last commit of a leavers must be at least LEAVERSIZE days
					while (CommonMethods.daysBetween(calcDate.getTime(), lastCommit.getMainCommitDate()) > 0){
						LogCommitInfo nearCommit = commonMethods.getNearCommit(calcDate.getTime(), sortedCommitList);
						TFInfo tf = commonMethods.getTF(calcDate.getTime(), allRepoCommits, nearCommit);
						
						Measure measure = new Measure(repositoryName, calcDate.getTime(), nearCommit.getSha(), tf, computationDate, computationInfo);
//						if (lastTFEventMeasure!=null)
//							measure.setLastTFEventMeasure(lastTFEventMeasure);
						
						List<Developer> tfDevelopers = tf.getTfDevelopers();

						int nLeavers = 0; 
						for (Developer developer : tfDevelopers) {
							if (!repositoryDevelopers.containsKey(developer.getAuthorId()))
								System.err.println("TF developer was not found: " + developer);
							DeveloperInfo devInfo = repositoryDevelopers.get(developer.getAuthorId());
							Date devLastCommitDate = devInfo.getLastCommit().getMainCommitDate();
							if (CommonMethods.daysBetween(devLastCommitDate, lastCommit.getMainCommitDate())>=leaverSize && devLastCommitDate.before(calcDate.getTime())){
								measure.addLeaver(devInfo);
								nLeavers++;
								System.out.printf("%s left the project in %s (%d-%d)\n", developer, devInfo.getLastCommit().getMainCommitDate(), nLeavers, tf.getTf());
								
							}
							measure.addTFDeveloper(devInfo);
						}
						
						if (tf.getTf()>0) {
							if (nLeavers == tf.getTf()) {
								if (equalSets(lastTFEventDevelopers,
										tfDevelopers) ||
										(lastTFEventMeasure!=null && !measure.getLastTFLeaverDate().after(lastTFEventMeasure.getLastTFLeaverDate()))) {
									measure.setTFEvent(false);
									measure.setSurviveEvent(false);
								} else {
									measure.setTFEvent(true);
									measure.setSurviveEvent(false);
									lastTFEventDevelopers = tfDevelopers;
									lastTFEventMeasure = measure;
								}
							} else {
								if (equalSets(lastTFEventDevelopers,
										tfDevelopers)) {
									measure.setTFEvent(false);
									measure.setSurviveEvent(false);
								} else {
									measure.setTFEvent(false);
									measure.setSurviveEvent(false);
									if (lastTFEventMeasure != null) {
										measure.setSurviveEvent(true);
										lastTFEventMeasure
												.setSurviveMeasure(measure);
										lastTFEventDevelopers = null;
										lastTFEventMeasure = null;
									}
								}
							}
						}
						if (measure.isTFEvent())
							System.out.println("\n========TF EVENT: " + repositoryName + "=======\n");
						
						if (measure.isSurviveEvent())
							System.out.println("\n========Survive TF EVENT: " + repositoryName + "=======\n");
						
						repositoryMeasures.add(measure);
						calcDate.add(Calendar.DATE, chunckSize);
					}
					
					stdOut = commonMethods.createAndExecuteCommand(scriptsPath+"reset_repo.sh "+ repositoryPath + " " + projectInfo.getDefault_branch());
					
					for (Measure measure : repositoryMeasures) {
						if (measure.isTFEvent())
							commonMethods.insertAdditionalInfo(measure, scriptsPath, allRepoCommits, sortedCommitList);
						measureDAO.persistOrUpdate(measure);
					}
					
					projectInfo.setStatus(ProjectStatus.ANALYZED);
					projectDAO.update(projectInfo);
					
				} catch (Exception e) {
					e.printStackTrace(System.err);
					projectInfo.setErrorMsg("NewTFStudy error: " + e.getMessage());
					projectInfo.setStatus(ProjectStatus.ERROR);
					projectDAO.update(projectInfo);
				} 
			}
		}
	}

	private static boolean equalSets(List<Developer> eventTfDevelopers,
			List<Developer> tfDevelopers) {
		if (eventTfDevelopers==null)
			return false;
		if (eventTfDevelopers.size()!=tfDevelopers.size())
			return false;
		Set<Integer> tfIds = new HashSet<Integer>();
		for (Developer developer : tfDevelopers) {
			tfIds.add(developer.getAuthorId());
		}
		for (Developer developer : eventTfDevelopers) {
			if (!tfIds.contains(developer.getAuthorId()))
				return false;
		}
		return true;
	}

}
