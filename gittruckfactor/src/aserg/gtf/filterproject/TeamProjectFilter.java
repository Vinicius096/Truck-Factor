package aserg.gtf.filterproject;

import java.util.ArrayList;
import java.util.List;

import aserg.gtf.dao.ProjectInfoDAO;
import aserg.gtf.model.ProjectInfo;

public class TeamProjectFilter extends ProjectFilter {
	
	private float teamThreshold;
	
	
	
	public TeamProjectFilter(List<ProjectInfo> projects, float teamThreshold, ProjectInfoDAO piDAO) {
		super(projects, "*TEAM*", piDAO);
		this.teamThreshold = teamThreshold;
	}

	@Override
	public List<ProjectInfo> filter() {
		List<ProjectInfo> newList = new ArrayList<ProjectInfo>();
		for (ProjectInfo projectInfo : projects) {
			if (!projectInfo.isFiltered()) {
				if (projectInfo.getNumAuthors() < teamThreshold) {
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
	

	public float getTeamThreshold() {
		return teamThreshold;
	}

	public void setTeamThreshold(float teamThreshold) {
		this.teamThreshold = teamThreshold;
	}


}
