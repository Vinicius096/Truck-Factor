package aserg.gtf.gitstudy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import aserg.gtf.GitTruckFactor;
import aserg.gtf.model.LogCommitFileInfo;
import aserg.gtf.model.LogCommitInfo;
import aserg.gtf.model.NewFileInfo;
import aserg.gtf.task.NewAliasHandler;
import aserg.gtf.task.extractor.FileInfoExtractor;
import aserg.gtf.task.extractor.GitLogExtractor;
import aserg.gtf.task.extractor.LinguistExtractor;
import aserg.gtf.util.EmailService;
import aserg.gtf.util.FileInfoReader;
import aserg.gtf.util.LineInfo;
import aserg.gtf.util.Util;

public class Main {
	private static final Logger LOGGER = Logger.getLogger(GitTruckFactor.class);
	private static InputStream input = null;
	
	public static void main(String[] args) {
		LOGGER.trace("GitStudy starts");
		String repositoryPath = "";
		String repositoryName = "";
		if (args.length>0)
			repositoryPath = args[0];
		if (args.length>1)
			repositoryName = args[1];
		
		repositoryPath = (repositoryPath.charAt(repositoryPath.length()-1) == '/') ? repositoryPath : (repositoryPath + "/");
		if (repositoryName.isEmpty())
			repositoryName = repositoryPath.split("/")[repositoryPath.split("/").length-1];
		

		Map<String, List<LineInfo>> filesInfo;
		Map<String, List<LineInfo>> aliasInfo;
		Map<String, List<LineInfo>> modulesInfo;
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
		
		
		FileInfoExtractor fileExtractor = new FileInfoExtractor(repositoryPath, repositoryName);
		LinguistExtractor linguistExtractor =  new LinguistExtractor(repositoryPath, repositoryName);
		NewAliasHandler aliasHandler =  aliasInfo == null ? null : new NewAliasHandler(aliasInfo.get(repositoryName));
		GitLogExtractor gitLogExtractor = new GitLogExtractor(repositoryPath, repositoryName);	
		
		
		
		try {
			produceAndsendSurvey(repositoryPath, repositoryName, filesInfo, modulesInfo,	fileExtractor, linguistExtractor, gitLogExtractor,aliasHandler);
		} catch (Exception e) {
			LOGGER.error("GitStudy calculation aborted!",e);
		}
		

		
		
		LOGGER.trace("OrGitStudyacle end");
	}
	
	private static void produceAndsendSurvey(String repositoryPath,
			String repositoryName, 
			Map<String, List<LineInfo>> filesInfo, 
			Map<String, List<LineInfo>> modulesInfo,
			FileInfoExtractor fileExtractor,
			LinguistExtractor linguistExtractor,
			GitLogExtractor gitLogExtractor, NewAliasHandler aliasHandler) throws Exception {
		
			Map<String, LogCommitInfo> commits = gitLogExtractor.execute();
 			if (aliasHandler != null)
 					commits = aliasHandler.execute(repositoryName, commits);
 			 				

 			//Persist commit info
// 			gitLogExtractor.persist(commits);
 			
			List<NewFileInfo> files = fileExtractor.execute();
			files = linguistExtractor.setNotLinguist(files);	
			if(filesInfo != null && filesInfo.size()>0) 
				if(filesInfo.containsKey(repositoryName))
					Util.applyFilterFiles(filesInfo.get(repositoryName), files);
				else
					LOGGER.warn("No filesInfo for " + repositoryName);
			
			Set<String> fileNames = getFileNames(files); 
			//Persist file info
//			fileExtractor.persist(files);
			
			List<Dev> devs = getDevelopers(commits, fileNames);
			List<DataPoint> dataPoints = getDataPoints(devs);
			
			
			List<DataPoint> selectedDataPoints = selectDataPoints(dataPoints, 20);
			
			System.out.println("Population: " + dataPoints.size() +"\n" + "Sample: " + selectedDataPoints.size() +"\n");
			for (DataPoint dataPoint : selectedDataPoints) {
				System.out.println(dataPoint);
			}
			sendMail(selectedDataPoints, 6, repositoryName);
	}

	private static void sendMail(List<DataPoint> dataPoints, int nFilesMax, String repositoryName) {
		String initialText = "Dear developer of the system "+ repositoryName +"\n\n...\nWe are developing a study about code ownership on software systems. "
				+ "Could you help us to improve our metric to identify code ownership "
				+ " assessing how well is your knowledge about the files listed bellow? The files were randomly selected "
				+ "among files that you have already modified. \nPlease, for each file give a rate between 1 and 5, where five "
				+ "meant that the you are able to reproduce the code without looking at it, a three meant that you  "
				+ "would need to perform some investigations before reproducing the code, and a one meant that you "
				+ "had no knowledge of the code."
				+ "\n\nFiles: \n\n";
		String endText = "\nEnd of the text.\n\nThanks. \n";
		String bodyMessage; 
		Map<Dev, List<String>> map = groupBydev(dataPoints);
		for (Entry<Dev, List<String>> entry : map.entrySet()) {
			Dev dev = entry.getKey(); 
			List<String> files = entry.getValue();
			String filesListStr = "";
			int count = 0;
			for (String file : files) {
				if (count<nFilesMax)
					filesListStr+=file + "\nRate: \n\n";
				count++;				
			}
			bodyMessage = initialText + filesListStr + endText;
			if (true){
//			if (dev.getName().equals("Leonardo Passos")){
				EmailService service = new EmailService("aserg.authorship.research", "password");
				service.sendMessage("Pilot Survey - Test", bodyMessage, dev.getEmail());
			}
				
		}
		
		
	}

	private static Map<Dev, List<String>> groupBydev(List<DataPoint> dataPoints) {
		Map<Dev, List<String>> map = new HashMap<Dev, List<String>>();
		for (DataPoint dataPoint : dataPoints) {
			Dev dev = dataPoint.getDev();
			List<String> files = null;
			if (!map.containsKey(dev))
				map.put(dev, new ArrayList<String>());
			map.get(dev).add(dataPoint.getFile());				
		}
		return map;
	}

	private static List<DataPoint> selectDataPoints(List<DataPoint> dataPoints, int n) {
		List<DataPoint> auxList = new ArrayList(dataPoints);
		List<DataPoint> resultDataPoints = new ArrayList();
		Random randomGenerator =  new Random();
		n = (n>dataPoints.size())?dataPoints.size():n;
		for (int j = 0; j < n; j++) {
			DataPoint dataPoint = auxList.get(randomGenerator.nextInt(auxList.size()));
			auxList.remove(dataPoint);
			resultDataPoints.add(dataPoint);
		}
		return resultDataPoints;
	}

	private static List<DataPoint> getDataPoints(List<Dev> devs) {
		List<DataPoint> dataPoints= new ArrayList<DataPoint>();
		for (Dev dev : devs) {
			for (String file : dev.getFiles()) {
				dataPoints.add(new DataPoint(dev, file));
			}
		}
		return dataPoints;
	}

	private static Set<String> getFileNames(List<NewFileInfo> files) {
		Set<String> fileNames = new HashSet<String>();
		for (NewFileInfo newFileInfo : files) {
			if (!newFileInfo.getFiltered())
				fileNames.add(newFileInfo.getPath());
		}
		return fileNames;
	}

	private static List<Dev> getDevelopers(Map<String, LogCommitInfo> commits, Set<String> fileNames) {
		Map<String, Dev> devsMap = new HashMap<String, Dev>();
		for (Entry<String, LogCommitInfo> entry : commits.entrySet()) {
			LogCommitInfo commitInfo = entry.getValue();
			List<LogCommitFileInfo> commitfiles = commitInfo.getLogCommitFiles();
			Dev dev =  null;
			if (devsMap.containsKey(commitInfo.getUserName()))
				dev = devsMap.get(commitInfo.getUserName());
			else{
				dev =  new Dev(commitInfo.getUserName(), commitInfo.getAuthorName(), commitInfo.getAuthorEmail());
				devsMap.put(commitInfo.getUserName(), dev);
			}
			if (commitfiles != null) {
				// TODO: tratar rename, ou pelo menos remover arquivos não mais presentes
				for (LogCommitFileInfo commitFile : commitfiles) {
					if (fileNames.contains(commitFile.getNewFileName()))
						dev.addFile(commitFile.getNewFileName());
				} 
			}
				
			
			
		}
		return new ArrayList<Dev>(devsMap.values());
	}
}
