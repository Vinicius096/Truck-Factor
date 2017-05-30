package aserg.gtf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import aserg.gtf.commands.SystemCommandExecutor;
import aserg.gtf.dao.ProjectInfoDAO;
import aserg.gtf.model.LogCommitInfo;
import aserg.gtf.model.NewFileInfo;
import aserg.gtf.model.ProjectInfo;
import aserg.gtf.model.ProjectStatus;
import aserg.gtf.model.authorship.Repository;
import aserg.gtf.task.DOACalculator;
import aserg.gtf.task.NewAliasHandler;
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
	private static NewAliasHandler aliasHandler =  null;
	private static GitLogExtractor gitLogExtractor = null;	
	private static Map<String, List<LineInfo>> filesInfo;
	private static Map<String, List<LineInfo>> aliasInfo;
	private static Map<String, List<LineInfo>> modulesInfo;
	public static void main(String[] args) {
		GitTruckFactor.loadConfiguration();
		ProjectInfoDAO projectDAO = new ProjectInfoDAO();
		List<ProjectInfo> projects= projectDAO.findAll(null);
		String repositoriesPath = "/Users/guilherme/test/github_repositories/";
		for (ProjectInfo projectInfo : projects) {
			if (projectInfo.getStatus() == ProjectStatus.DOWNLOADED){
				// build my command as a list of strings
				try {
					String repositoryName = projectInfo.getFullName();
					String repositoryPath = repositoriesPath+repositoryName+"/";
					String stdOut = createAndExecuteCommand("./get_git_log.sh "+ repositoryPath);
					System.out.println(stdOut);
					
					initializeExtractors(repositoryPath, repositoryName);	
					
					// GET Repository commits
					Map<String, LogCommitInfo> commits = gitLogExtractor.execute();
		 			if (aliasHandler != null)
		 					commits = aliasHandler.execute(repositoryName, commits);
		 			projectInfo.setNumAuthors(getNAuthors(commits)); 		
		 			
		 			// GET Repository files
					List<NewFileInfo> files = fileExtractor.execute();
					files = linguistExtractor.setNotLinguist(files);	
					
					// GET Repository DOA results
					DOACalculator doaCalculator = new DOACalculator(repositoryPath, repositoryName, commits.values(), files);
					Repository repository = doaCalculator.execute();
					
					
					
					
					// GET Repository TF
					TruckFactor truckFactor = new GreedyTruckFactor();
					TFInfo tf = truckFactor.getTruckFactor(repository);
					
					projectInfo.setTf(tf.getTf());
					projectInfo.setStatus(ProjectStatus.TF_COMPUTED);
					projectDAO.update(projectInfo);
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					projectInfo.setErrorMsg("NewTFStudy error: " + e.toString());
					projectInfo.setStatus(ProjectStatus.ERROR);
					projectDAO.update(projectInfo);
				} 

				// get the output from the command
				
			}
		}
	}

	private static int getNAuthors(Map<String, LogCommitInfo> commits) {
		Set<String> authors = new HashSet<String>();
		for (Entry<String, LogCommitInfo> entry : commits.entrySet()) {
			LogCommitInfo commitInfo = entry.getValue();
			authors.add(commitInfo.getUserName());
		}
		
		return authors.size();
	}

	public static void initializeExtractors(String repositoryPath, String repositoryName) {
		try {
			filesInfo = FileInfoReader.getFileInfo("repo_info/filtered-files.txt");
		} catch (IOException e) {
			LOGGER.warn("Not possible to read repo_info/filtered-files.txt file. File filter step will not be executed!");
			filesInfo = null;
		}		
		try {
			aliasInfo = FileInfoReader.getFileInfo("repo_info/alias.txt");
		} catch (IOException e) {
			LOGGER.warn("Not possible to read repo_info/alias.txt file. Aliases treating step will not be executed!");
			aliasInfo = null;
		}
		try {
			modulesInfo = FileInfoReader.getFileInfo("repo_info/modules.txt");
		} catch (IOException e) {
			LOGGER.warn("Not possible to read repo_info/modules.txt file. No modules info will be setted!");
			modulesInfo = null;
		}
		
		
		fileExtractor = new FileInfoExtractor(repositoryPath, repositoryName);
		linguistExtractor =  new LinguistExtractor(repositoryPath, repositoryName);
		aliasHandler =  aliasInfo == null ? null : new NewAliasHandler(aliasInfo.get(repositoryName));
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
