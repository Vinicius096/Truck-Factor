package aserg.gtf.filterproject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import aserg.gtf.dao.CommitFileDAO;
import aserg.gtf.dao.ProjectInfoDAO;
import aserg.gtf.model.ProjectInfo;

public class NewMigrationProjectFilter extends ProjectFilter {

	private float percFilesThreshold;
	private int nCommitsThreshold;
	private int type;
	
	public NewMigrationProjectFilter(List<ProjectInfo> projects, int type, float percFilesThreshold, int nCommitsThreshold, ProjectInfoDAO piDAO) throws Exception {
		super(projects, ("*NEWMIGRATION-" + (type==1?"BIGGEST*":"FIRSTEST*")), piDAO);
		this.type = type;
		if (type!= 1 && type != 2)
			throw new Exception("Parameter type has a wrong value!");
		this.percFilesThreshold = percFilesThreshold;
		this.nCommitsThreshold = nCommitsThreshold;
	}

	@Override
	public List<ProjectInfo> filter() {
		List<ProjectInfo> newList = new ArrayList<ProjectInfo>();
		CommitFileDAO cfiDAO = new CommitFileDAO();
		System.out.println(new Date());
		for (ProjectInfo projectInfo : projects) {
			if (!projectInfo.isFiltered()) {
				List<Long> listNumAddCommitFiles;
				if (type == 1)
					listNumAddCommitFiles = cfiDAO.newGetAddsCommitFileOrderByNumberOfCFs(projectInfo.getFullName());
				else 
					listNumAddCommitFiles = cfiDAO.newGetAddsCommitFileOrderByDate(projectInfo.getFullName());
				int sum = 0;
				int count = 0;
				long totalCommitFiles = getNumCommitFiles(listNumAddCommitFiles);
				while (count < nCommitsThreshold
						&& count < listNumAddCommitFiles.size()) {
					sum += listNumAddCommitFiles.get(count++);
					if (sum > totalCommitFiles * percFilesThreshold)
						break;
				}
				if (sum > totalCommitFiles * percFilesThreshold) {
					System.out.format("%s %d %d %d %d %s\n",
							projectInfo.getFullName(), sum,
							totalCommitFiles, count, projectInfo.getNumFiles(), projectInfo.getLanguage());
					projectInfo.setFiltered(true);
					String filterInfo = projectInfo.getFilterinfo();
					projectInfo.setFilterinfo(filterInfo == null
							|| filterInfo.isEmpty() ? filterStamp : filterInfo
									+ filterStamp);
				} else
					newList.add(projectInfo);
			}
		}
		System.out.println(new Date());
		return newList;
	}

	private Long getNumCommitFiles(List<Long> listNumAddCommitFiles) {
		long sum= 0;
		for (Long num : listNumAddCommitFiles) {
			sum+=num;
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
	
	

}
