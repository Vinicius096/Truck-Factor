package aserg.gtf;



import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.common.io.LineReader;

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
public class CommitsCounter {
	private static final Logger LOGGER = Logger.getLogger(GitTruckFactor.class);

	public static void main(String[] args) {

		String logPath = "/Users/guilherme/git/Truck-Factor/gittruckfactor/log/";
		String projectsListFile = "./tfSystems.info";

		if (args.length>0){
			logPath = args[0];
			if (logPath.charAt(logPath.length()-1) != '/')
				logPath+="/";
		}
		if (args.length>1){
			projectsListFile = args[1];
			if (projectsListFile.charAt(projectsListFile.length()-1) != '/')
				projectsListFile+="/";
		}
		
		
		List<ProjectInfo> projects= new CommitsCounter().getProjects(logPath, projectsListFile);
		


		for (ProjectInfo projectInfo : projects) {
			
			String stdOut;
			String repositoryName = projectInfo.getName();
			Date eventDate = projectInfo.getEventDate();
			String repositoryPath = logPath+repositoryName.replace("/", "-")+"/";

			try {
				CommonMethods commonMethods = new CommonMethods(repositoryPath, repositoryName);


				// GET Repository commits
				Map<String, LogCommitInfo> allRepoCommits = commonMethods.gitLogExtractor.execute();
				List<LogCommitInfo> oldCommits =  new ArrayList<LogCommitInfo>();
				List<LogCommitInfo> newCommits =  new ArrayList<LogCommitInfo>();
						
				for (LogCommitInfo commit : allRepoCommits.values()) {
					if(commit.getCommitterDate().before(eventDate))
						oldCommits.add(commit);
					else if (commit.getCommitterDate().after(eventDate))
						newCommits.add(commit);
				}
				projectInfo.setnCommitsBefore(oldCommits.size());
				projectInfo.setnCommitsAfter(newCommits.size());
				System.out.println(projectInfo.toString()+";"+allRepoCommits.size());
				

			} catch (Exception e) {
				e.printStackTrace(System.err);
			} 

		}
	}

	private List<ProjectInfo> getProjects(String repositoriesPath,
			String systemsFile) {
		BufferedReader br;
		List<ProjectInfo> projects = new ArrayList<CommitsCounter.ProjectInfo>();
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(systemsFile), "UTF8"));
			LineReader lineReader = new LineReader(br);
			String sCurrentLine;
			String[] values;
			int countcfs = 0;
			while ((sCurrentLine = lineReader.readLine()) != null) {
				countcfs++;
				if (sCurrentLine.startsWith("#"))
					continue;
				;
				values = sCurrentLine.split(";");
				if (values.length<3)
					System.err.println("Erro na linha " + countcfs);
				SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Date data = formato.parse(values[2]);
				projects.add(new ProjectInfo(values[0], values[1], data));
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return projects;
	}

	public class ProjectInfo{
		private String name;
		private String status;
		private Date eventDate;
		private int nCommitsBefore;
		private int nCommitsAfter;

		public ProjectInfo(String name, String status, Date eventDate) {
			super();
			this.name = name;
			this.status = status;
			this.eventDate = eventDate;
		}

		public String getName() {
			return name;
		}
		public Date getEventDate() {
			return eventDate;
		}
		public void setnCommitsAfter(int nCommitsAfter) {
			this.nCommitsAfter = nCommitsAfter;
		}

		public void setnCommitsBefore(int nCommitsBefore) {
			this.nCommitsBefore = nCommitsBefore;
		}

		@Override
		public String toString() {
			return name+";"+status+";"+eventDate+";"+nCommitsBefore+";"+nCommitsAfter;
		}
	}

}
