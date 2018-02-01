package aserg.gtf;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import aserg.gtf.dao.ProjectInfoDAO;
import aserg.gtf.dao.newstudy.MeasureDAO;
import aserg.gtf.model.DeveloperInfo;
import aserg.gtf.model.LogCommitInfo;
import aserg.gtf.model.ProjectInfo;
import aserg.gtf.model.ProjectStatus;
import aserg.gtf.model.authorship.Developer;
import aserg.gtf.model.newstudy.Measure;
import aserg.gtf.task.GitHubUsersAliasHandler;
import aserg.gtf.truckfactor.TFInfo;
import aserg.gtf.util.LineInfo;

public class TFEventExecuter implements Runnable {
	private ProjectInfo projectInfo;
	private MeasureDAO measureDAO;
	private ProjectInfoDAO projectDAO;
	private String repositoriesPath;
	private String scriptsPath;
	private int chunckSize;
	private int leaverSize;
	private Date computationDate;
	private String computationInfo;
	private List<LineInfo> repAliasInfo;
	private int id; 
	static int nThreads = 0;
	
	
	public TFEventExecuter(ProjectInfo projectInfo, MeasureDAO measureDAO,
			ProjectInfoDAO projectDAO, String repositoriesPath,
			String scriptsPath, int chunckSize, int leaverSize,
			Date computationDate, String computationInfo,
			List<LineInfo> repAliasInfo) {
		super();
		this.projectInfo = projectInfo;
		this.measureDAO = measureDAO;
		this.projectDAO = projectDAO;
		this.repositoriesPath = repositoriesPath;
		this.scriptsPath = scriptsPath;
		this.chunckSize = chunckSize;
		this.leaverSize = leaverSize;
		this.computationDate = computationDate;
		this.computationInfo = computationInfo;
		this.repAliasInfo = repAliasInfo;
		this.id = ++nThreads;
	}
	@Override
	public void run() {
		projectInfo.setStatus(ProjectStatus.ANALYZING);
		projectDAO.update(projectInfo);
		
		String repositoryName = projectInfo.getFullName();
		String repositoryPath = repositoriesPath+repositoryName+"/";
		String stdOut;

		System.out.println("Executing thread-"+id);
		try {
			CommonMethods commonMethods = new CommonMethods(repositoryPath, repositoryName);


			stdOut = commonMethods.createAndExecuteCommand(scriptsPath+"reset_repo.sh "+ repositoryPath + " " + projectInfo.getDefault_branch());
			System.out.println(stdOut);
			stdOut = commonMethods.createAndExecuteCommand(scriptsPath+"get_git_log.sh "+ repositoryPath);
			System.out.println(stdOut);

			if (repAliasInfo!= null)
				commonMethods.replaceNamesInLogCommitFile(repAliasInfo);

			// GET Repository commits
			Map<String, LogCommitInfo> allRepoCommits = commonMethods.gitLogExtractor.execute();
			//			Map<String, Integer> mapIds = new NewSimpleAliasHandler().execute(repositoryName, allRepoCommits);
			Map<String, Integer> mapIds = new GitHubUsersAliasHandler().execute(repositoryName, allRepoCommits);
			Map<Integer, DeveloperInfo> repositoryDevelopers = commonMethods.getRepositoryDevelopers(allRepoCommits, mapIds);	


			// Update #authors and tf
			//			commonMethods.updateRepo(projectDAO, projectInfo, allRepoCommits,
			//						repositoryDevelopers);

			List<LogCommitInfo> sortedCommitList = commonMethods.getSortedCommitList(allRepoCommits);
			LogCommitInfo firstCommit = sortedCommitList.get(0);
			LogCommitInfo lastCommit = sortedCommitList.get(sortedCommitList.size()-1);

			if (CommonMethods.daysBetween(firstCommit.getMainCommitDate(), lastCommit.getMainCommitDate())<=2*chunckSize){
				String errorMsg = "Development history too short. Less than " + chunckSize + " days.";
				System.err.println(errorMsg);
				projectInfo.setStatus(ProjectStatus.NOTCOMPUTED);
				projectInfo.setErrorMsg(errorMsg);
				projectDAO.update(projectInfo);
			}
			else{


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
					//				if (lastTFEventMeasure!=null)
					//					measure.setLastTFEventMeasure(lastTFEventMeasure);

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
					if (nLeavers == tf.getTf()){
						if (equalSets(lastTFEventDevelopers, tfDevelopers)){
							measure.setTFEvent(false);
							measure.setSurviveEvent(false);
						}
						else{
							measure.setTFEvent(true);
							measure.setSurviveEvent(false);
							lastTFEventDevelopers = tfDevelopers;
							lastTFEventMeasure = measure;
						}
					}
					else{
						if(equalSets(lastTFEventDevelopers, tfDevelopers)){
							measure.setTFEvent(false);
							measure.setSurviveEvent(false);
						}
						else{
							measure.setTFEvent(false);
							measure.setSurviveEvent(false);
							if (lastTFEventMeasure!=null){
								measure.setSurviveEvent(true);
								lastTFEventMeasure.setSurviveMeasure(measure);
								lastTFEventDevelopers = null;
								lastTFEventMeasure = null;
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
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
			projectInfo.setErrorMsg("NewTFStudy error: " + e.getMessage());
			projectInfo.setStatus(ProjectStatus.ERROR);
			projectDAO.update(projectInfo);
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
