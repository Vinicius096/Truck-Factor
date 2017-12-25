package aserg.gtf;



import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import aserg.gtf.model.DeveloperInfo;
import aserg.gtf.model.LogCommitInfo;
import aserg.gtf.model.NewFileInfo;
import aserg.gtf.model.ProjectInfo;
import aserg.gtf.model.ProjectStatus;
import aserg.gtf.model.authorship.Developer;
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
import aserg.gtf.util.FileInfoReader;
import aserg.gtf.util.LineInfo;

public class TFInvestigator {
	private static final Logger LOGGER = Logger.getLogger(GitTruckFactor.class);
	//	private static Map<String, List<LineInfo>> filesInfo;
	//	private static Map<String, List<LineInfo>> aliasInfo;
	//	private static Map<String, List<LineInfo>> modulesInfo;

	private static int chunckSize = 365;
	private static int leaverSize = 365;

	public static void main(String[] args) {
		GitTruckFactor.loadConfiguration();
		String repositoryPath = "";
		String repositoryName = "";
		String scriptsPath = "./";
		String defaultBranch = "master";
		Date updated_at = new Date();  
		
		Map<String, List<LineInfo>> aliasInfo;
		try {
			aliasInfo = FileInfoReader.getFileInfo("repo_info/alias.txt");
		} catch (IOException e) {
			LOGGER.warn("Not possible to read repo_info/alias.txt file. Aliases treating step will not be executed!");
			aliasInfo = null;
		}


		if (args.length>0){
			scriptsPath = args[0];
			if (scriptsPath.charAt(scriptsPath.length()-1) != '/')
				scriptsPath+="/";
		}
		if (args.length>1){
			repositoryPath = args[1];
			if (repositoryPath.charAt(repositoryPath.length()-1) != '/')
				repositoryPath+="/";
		}
		if (args.length>2)
			repositoryName = args[2];
		if (args.length>3)
			defaultBranch = args[3];
		if (args.length>4){
			DateFormat df = new SimpleDateFormat("dd/MM/yyyy"); 
			try {
				updated_at = df.parse(args[4]);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (args.length>5)
			chunckSize = Integer.parseInt(args[5]);

		Date computationDate = new Date();
		String computationInfo = "Computation - " + computationDate + " - Chunk size = " + chunckSize;
		
		String stdOut;
		try {
			CommonMethods commonMethods = new CommonMethods(repositoryPath, repositoryName);
			
			stdOut = commonMethods.createAndExecuteCommand(scriptsPath+"reset_repo.sh "+ repositoryPath + " " + defaultBranch);
			stdOut = commonMethods.createAndExecuteCommand(scriptsPath+"get_git_log.sh "+ repositoryPath);
			System.out.println(stdOut);
			
			if (aliasInfo!= null  && aliasInfo.containsKey(repositoryName))
				commonMethods.replaceNamesInLogCommitFile(aliasInfo.get(repositoryName));

			// GET Repository commits
			Map<String, LogCommitInfo> allRepoCommits = commonMethods.gitLogExtractor.execute();
			Map<String, Integer> mapIds = new SimpleAliasHandler().execute(repositoryName, allRepoCommits);
			Map<Integer, DeveloperInfo> repositoryDevelopers = commonMethods.getRepositoryDevelopers(allRepoCommits, mapIds);	



			List<LogCommitInfo> sortedCommitList = commonMethods.getSortedCommitList(allRepoCommits);
			LogCommitInfo firstCommit = sortedCommitList.get(0);
			LogCommitInfo lastCommit = sortedCommitList.get(sortedCommitList.size()-1);

			if (commonMethods.daysBetween(firstCommit.getMainCommitDate(), lastCommit.getMainCommitDate())<=chunckSize){
				String errorMsg = "Error in " + repositoryName+";Development history too short. Less than " + chunckSize + " days.";
				System.err.println(errorMsg);
			}
			else{
				// Brake the development history in chunks and apply the algorithm to identify TF events
				List<Measure> repositoryMeasures = new ArrayList<Measure>();
				Calendar calcDate = Calendar.getInstance(); 
				calcDate.setTime(firstCommit.getMainCommitDate()); 
				calcDate.add(Calendar.DATE, chunckSize);
				while (commonMethods.daysBetween(calcDate.getTime(), lastCommit.getMainCommitDate()) >= chunckSize){
					LogCommitInfo nearCommit = commonMethods.getNearCommit(calcDate.getTime(), sortedCommitList);
					TFInfo tf = commonMethods.getTF(calcDate.getTime(), allRepoCommits, nearCommit);

					Measure measure = new Measure(repositoryName, calcDate.getTime(), nearCommit.getSha(), tf, computationDate, computationInfo);


					List<Developer> tfDevelopers = tf.getTfDevelopers();


					calcDate.add(Calendar.DATE, chunckSize);
					int nLeavers = 0; 
					for (Developer developer : tfDevelopers) {
						if (!repositoryDevelopers.containsKey(developer.getAuthorId()))
							System.err.println("Error in " + repositoryName+";TF developer was not found: " + developer);
						DeveloperInfo devInfo = repositoryDevelopers.get(developer.getAuthorId());
						Date devLastCommitDate = devInfo.getLastCommit().getMainCommitDate();
						if (commonMethods.daysBetween(devLastCommitDate, lastCommit.getMainCommitDate())>=leaverSize && devLastCommitDate.before(calcDate.getTime())){
							measure.addLeaver(devInfo);
							nLeavers++;
//							System.out.printf("%s left the project in %s (%d-%d)\n", developer, devInfo.getLastCommit().getMainCommitDate(), nLeavers, tf.getTf());
//							if (nLeavers == tf.getTf()){
//								System.out.println("\n========TF EVENT: " + repositoryName + "=======\n");
//							}
						}
					}
					repositoryMeasures.add(measure);
				}

				stdOut = commonMethods.createAndExecuteCommand(scriptsPath+"reset_repo.sh "+ repositoryPath + " " + defaultBranch);

				for (Measure measure : repositoryMeasures) {
					if (measure.isTFEvent())
						commonMethods.insertAdditionalInfo(measure, scriptsPath, allRepoCommits, sortedCommitList);
					System.out.println(measure);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	


}
