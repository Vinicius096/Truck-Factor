package aserg.gtf.old;



import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import aserg.gtf.GitTruckFactor;
import aserg.gtf.dao.ProjectInfoDAO;
import aserg.gtf.dao.newstudy.MeasureDAO;
import aserg.gtf.model.ProjectInfo;
import aserg.gtf.model.ProjectStatus;
import aserg.gtf.util.FileInfoReader;
import aserg.gtf.util.LineInfo;


// Compute the TF in a moment t and verify if the TF developers does not commit after t
public class NewTFStudy_Threads {
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
		String computationInfo = "Computation (Alg2 - GitHub ids - Survive - Threads)- " + computationDate + " - Chunk size = " + chunckSize+ " - Leaver size = " + leaverSize;
		if (args.length>4)
			computationInfo = args[4];

		ExecutorService tpes =
	            Executors.newFixedThreadPool(10);
		
		for (ProjectInfo projectInfo : projects) {
			if (projectInfo.getStatus() == ProjectStatus.DOWNLOADED || projectInfo.getStatus() == ProjectStatus.RECALC){
				tpes.execute(new TFEventExecuter(projectInfo, measureDAO, projectDAO, repositoriesPath, 
						scriptsPath, chunckSize, leaverSize, computationDate, computationInfo, aliasInfo.get(projectInfo.getFullName())));
				
			}
		}
		System.out.println("All threads was created");
		tpes.shutdown();
	}

}
