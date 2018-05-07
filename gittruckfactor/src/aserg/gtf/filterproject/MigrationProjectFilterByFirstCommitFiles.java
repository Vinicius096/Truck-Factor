package aserg.gtf.filterproject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import aserg.gtf.commands.SystemCommandExecutor;
import aserg.gtf.dao.LogCommitFileDAO;
import aserg.gtf.dao.ProjectInfoDAO;
import aserg.gtf.model.LogCommitFileInfo;
import aserg.gtf.model.LogCommitInfo;
import aserg.gtf.model.ProjectInfo;
import aserg.gtf.model.Status;
import aserg.gtf.task.extractor.GitLogExtractor;

public class MigrationProjectFilterByFirstCommitFiles extends ProjectFilter {

	private float percFilesThreshold;
	private int nCommitsThreshold;
	private int type;
	private String repositoriesPath;
	private String scriptsPath;

	public MigrationProjectFilterByFirstCommitFiles(List<ProjectInfo> projects,
			int type, float percFilesThreshold, int nCommitsThreshold, ProjectInfoDAO piDAO, String repositoriesPath, String scriptsPath)
			throws Exception {
		super(projects,
				("*MIGRATION-FIRST_FILES_COMMIT*"), piDAO);
		this.type = type;
		if (type != 1 && type != 2)
			throw new Exception("Parameter type has a wrong value!");
		this.percFilesThreshold = percFilesThreshold;
		this.nCommitsThreshold = nCommitsThreshold;
		this.repositoriesPath = repositoriesPath;
		this.scriptsPath = scriptsPath;
	}

	@Override
	public List<ProjectInfo> filter() {
		List<ProjectInfo> newList = new ArrayList<ProjectInfo>();
		LogCommitFileDAO lcfiDAO = new LogCommitFileDAO();
		System.out.println(new Date());
		for (ProjectInfo projectInfo : projects) {
//			if (projectInfo.getStatus()!=ProjectStatus.NULL&&projectInfo.getStatus()!=ProjectStatus.ERROR) {
			if (projectInfo.getFullName().equals("spinnaker/spinnaker")) {
				try {
					System.out.println("Processing "+ projectInfo.getFullName() + " ...");
					String repositoryName = projectInfo.getFullName();
					String repositoryPath  = this.repositoriesPath + repositoryName +"/";
					String stdOut = createAndExecuteCommand(scriptsPath+"reset_repo.sh "+ repositoryPath + " " + projectInfo.getDefault_branch());
					System.out.println(stdOut);
					stdOut = createAndExecuteCommand(scriptsPath+"get_git_log.sh "+ repositoryPath);
					System.out.println(stdOut);

					List<LogCommitInfo> sortedCommitList = getProjectSortedCommitList(projectInfo, type);
					Map<String, Set<String>> commitFilesMap = getCommitFilesMap(projectInfo, type);
					int numFiles = getNumFiles(commitFilesMap); 
					
					int sum = 0;
					int count = 0;
					for (LogCommitInfo logCommitInfo : sortedCommitList) {
						count++;
						if (commitFilesMap.containsKey(logCommitInfo.getSha()))
							sum += commitFilesMap.get(logCommitInfo.getSha()).size();
						if (sum > numFiles * percFilesThreshold || count >= nCommitsThreshold)
							break;
					}
					
					if (sum > numFiles * percFilesThreshold) {
						System.out.format("%s %d %d %d %d %s\n",
								projectInfo.getFullName(), sum, numFiles,
								count, projectInfo.getNumFiles(),
								projectInfo.getLanguage());
						projectInfo.setFiltered(true);
						String filterInfo = projectInfo.getFilterinfo();
						projectInfo.setFilterinfo(filterInfo == null
								|| filterInfo.isEmpty() ? filterStamp : filterInfo
								+ filterStamp);
					} else
						newList.add(projectInfo);
					
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				} 
				
			}
		}
		System.out.println(new Date());
		return newList;
	}

	private int getNumFiles(Map<String, Set<String>> commitFilesMap) {
		int  count = 0;
		for (Set<String> files : commitFilesMap.values()) {
			count+=files.size();
		}
		return count;
	}

	private List<Float> getListAddsByCommit(List<LogCommitInfo> sortedCommitList) {
		List<Float> addsList = new ArrayList<Float>();
		for (LogCommitInfo commit : sortedCommitList) 
			addsList.add((float)getNumAdds(commit));
		return addsList;
	}

	private static int getNumAdds(LogCommitInfo commit) {
		int count = 0;
		for (LogCommitFileInfo commitFile : commit.getLogCommitFiles()) {
			if (commitFile.getStatus() == Status.ADDED)
				count++;
		}
		return count;
	}

	private Map<String, Set<String>> getCommitFilesMap(
			ProjectInfo projectInfo, int type) throws IOException, Exception {
		String repositoryName = projectInfo.getFullName();
		String repositoryPath  = this.repositoriesPath + repositoryName +"/";
		String stdOut = createAndExecuteCommand(scriptsPath+"get_first_files_commits.sh "+ repositoryPath);
		System.out.println(stdOut);
		
		Map<String, Set<String>> commitFilesMap = new HashMap<String, Set<String>>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(repositoryPath + "files_comit_sha.log"), "UTF8"));
		String sCurrentLine;
		String[] values;
		while ((sCurrentLine = br.readLine()) != null) {
			values = sCurrentLine.split(";");
			String fileName = values[0];
			String sha = values[1];
			if (!commitFilesMap.containsKey(sha))
				commitFilesMap.put(sha, new HashSet<String>());
			commitFilesMap.get(sha).add(fileName);
		}
			
		return commitFilesMap;
	}
	
	private List<LogCommitInfo> getProjectSortedCommitList(
			ProjectInfo projectInfo, int type) throws IOException, Exception {
		String repositoryName = projectInfo.getFullName();
		String repositoryPath  = this.repositoriesPath + repositoryName +"/";
		GitLogExtractor gitLogExtractor = new GitLogExtractor(repositoryPath, repositoryName);
		
		// GET Repository commits
		Map<String, LogCommitInfo> allRepoCommits = gitLogExtractor.execute();
		
		List<LogCommitInfo> sortedCommitList = type==1? getSortedCommitListByNFiles(allRepoCommits) : getSortedCommitListByDate(allRepoCommits);
		
		return sortedCommitList;
	}

	private static List<LogCommitInfo> getSortedCommitListByDate(
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
	
	private static List<LogCommitInfo> getSortedCommitListByNFiles(
			Map<String, LogCommitInfo> commits) {
		List<LogCommitInfo> newListOfCommits = new ArrayList<LogCommitInfo>(commits.values());
		Collections.sort(newListOfCommits, new Comparator<LogCommitInfo>() {

			@Override
            public int compare(LogCommitInfo lhs, LogCommitInfo rhs) {
                return  Integer.compare(getNumAdds(lhs), getNumAdds(rhs));
            }
		});
		Collections.reverse(newListOfCommits);
		return newListOfCommits;
	}
	
	private Long getNumCommitFiles(List<Float> listNumAddCommitFiles) {
		long sum = 0;
		for (Float num : listNumAddCommitFiles) {
			sum += num;
		}
		return sum;
	}

	public float getPercFilesThreshold() {
		return percFilesThreshold;
	}

	public void setPercFilesThreshold(float percFilesThreshold) {
		this.percFilesThreshold = percFilesThreshold;
	}

	public int getnCommitsThreshold() {
		return nCommitsThreshold;
	}

	public void setnCommitsThreshold(int nCommitsThreshold) {
		this.nCommitsThreshold = nCommitsThreshold;
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
