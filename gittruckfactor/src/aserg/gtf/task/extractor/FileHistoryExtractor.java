package aserg.gtf.task.extractor;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import aserg.gtf.model.LogCommitFileInfo;
import aserg.gtf.model.LogCommitInfo;
import aserg.gtf.model.NewFileInfo;
import aserg.gtf.task.AbstractTask;

public class FileHistoryExtractor extends AbstractTask<Map<String, LogCommitInfo>>{
	 static final Logger LOGGER = Logger.getLogger(FileHistoryExtractor.class);
	 Map<String, NewFileInfo> mapCodeElements = new HashMap<String, NewFileInfo>();
	public FileHistoryExtractor(String repositoryPath, String repositoryName) {
		super(repositoryName+".csv", repositoryPath, repositoryName);
	}
	@Override
	public Map<String, LogCommitInfo> execute() throws Exception {
		Map<String, LogCommitInfo> mapCommits = new HashMap<String, LogCommitInfo>();
		int countcfs = 0;
		try{	
			LOGGER.info("Extracting logCommits...  "+repositoryPath);
			BufferedReader br = new BufferedReader(new FileReader(
					repositoryPath + fileName));
			String sCurrentLine;
			String[] values;
			while ((sCurrentLine = br.readLine()) != null) {
				values = sCurrentLine.split(";");
				if (values.length!=6)
					LOGGER.error("Problem in line  " + countcfs + ". Too much columns.");
//				Date authorDate = !values[0].isEmpty() ? new Timestamp(Long.parseLong(values[0]) * 1000L) : null;
//				Date commiterDate = !values[0].isEmpty() ? new Timestamp(Long.parseLong(values[0]) * 1000L) : null;
				Date authorDate =  null;
				Date commiterDate = null;
				if (!mapCommits.containsKey(values[0])){
					mapCommits.put(values[0],
							new LogCommitInfo(repositoryName,
									values[0], values[1], "",
									authorDate, "", "",
									commiterDate, ""));
				}
				LogCommitInfo commit = mapCommits.get(values[0]);
				// TODO: mudar o ultimo parametro para considerar classes [3] e pacote [2]
				String commitElementName = values[4];
				commit.addCommitFile(new LogCommitFileInfo(commit, values[5], "", commitElementName));
				if (!mapCodeElements.containsKey(commitElementName))
					mapCodeElements.put(commitElementName, new NewFileInfo(repositoryName, commitElementName));
				countcfs++;
			}
			br.close();
		}
		catch(FileNotFoundException e ){
			throw new Exception("File not found: " + repositoryPath + fileName, e);
		}
		catch(Exception e ){
			throw new Exception(String.format("Error in file %s, line %d%", repositoryName, countcfs));
		}
				
		return mapCommits;
	}

	@Override
	public void persist(Map<String, LogCommitInfo> objects) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	public List<NewFileInfo> getCodeElements(){
		List<NewFileInfo> resultList = new ArrayList<NewFileInfo>();
		for (Entry<String, NewFileInfo> entry : mapCodeElements.entrySet()) {
			resultList.add(entry.getValue());
		}
		return resultList;
	}

}
