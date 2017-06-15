package aserg.gtf.filterproject;

import java.util.ArrayList;
import java.util.List;

import aserg.gtf.dao.ProjectInfoDAO;
import aserg.gtf.model.ProjectInfo;

public class SizeProjectFilter extends ProjectFilter {
	
	private int teamThreshold;
	
	
	
	public SizeProjectFilter(List<ProjectInfo> projects, int teamThreshold, ProjectInfoDAO piDAO) {
		super(projects, "*SIZE*", piDAO);
		this.teamThreshold = teamThreshold;
	}

	@Override
	public List<ProjectInfo> filter() {
		List<ProjectInfo> newList = new ArrayList<ProjectInfo>();
		for (ProjectInfo projectInfo : projects) {
			if (!projectInfo.isFiltered()) {
				if (projectInfo.getNumFiles() < teamThreshold) {
					projectInfo.setFiltered(true);
					String filterInfo = projectInfo.getFilterinfo();
					projectInfo.setFilterinfo(filterInfo == null
							|| filterInfo.isEmpty() ? filterStamp : filterInfo
							+ filterStamp);
				} else
					newList.add(projectInfo);
			}
		}
		return newList;
	}
	

	public int getTeamThreshold() {
		return teamThreshold;
	}

	public void setTeamThreshold(int teamThreshold) {
		this.teamThreshold = teamThreshold;
	}


}
