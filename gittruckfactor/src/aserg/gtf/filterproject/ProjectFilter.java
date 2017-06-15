package aserg.gtf.filterproject;

import java.util.List;

import aserg.gtf.dao.ProjectInfoDAO;
import aserg.gtf.model.ProjectInfo;

public abstract class ProjectFilter {
	List<ProjectInfo> projects;
	String filterStamp;
	ProjectInfoDAO piDAO;
	public ProjectFilter(List<ProjectInfo> projects, String filterStamp, ProjectInfoDAO piDAO) {
		this.projects = projects;
		this.filterStamp = filterStamp;
		this.piDAO = piDAO;
	}
	public abstract List<ProjectInfo> filter();
	
	public void persistFilterInformations() {
		for (ProjectInfo projectInfo : projects) {
			piDAO.update(projectInfo);
		}

	}
	public void clean() {
		for (ProjectInfo projectInfo : projects) {
			if (projectInfo.isFiltered()){
				projectInfo.getFilterinfo().replace(filterStamp, "");
				if (projectInfo.getFilterinfo().isEmpty())
					projectInfo.setFiltered(false);
			}
		}
	}
}
