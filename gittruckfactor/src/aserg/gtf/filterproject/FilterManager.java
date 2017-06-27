package aserg.gtf.filterproject;

import java.util.ArrayList;
import java.util.List;

import aserg.gtf.dao.ProjectInfoDAO;
import aserg.gtf.model.ProjectInfo;

public class FilterManager {
	List<ProjectInfo> projects;
	List<ProjectFilter> filters;
	ProjectFilter mainFilter;
	
	
	
	public static void main(String[] args) throws Exception {
		String repositoriesPath = "/Users/guilherme/test/github_repositories/";
		String scriptsPath = "./";
		if (args.length>0)
			repositoriesPath = args[0];
		if (args.length>1)
			scriptsPath = args[1];
		
		ProjectInfoDAO projectInfoDAO = new ProjectInfoDAO();
		FilterManager filterManager =  new FilterManager(projectInfoDAO.findAll(null));
		
//		List<ProjectInfo> projectsC = filterManager.getProjectsByLanguage("c/c++");
//		filterManager.addFilter(new HistoryProjectFilter(projectsC, 327, projectInfoDAO));
//		filterManager.addFilter(new TeamProjectFilter(projectsC, 16, projectInfoDAO));
//		filterManager.addFilter(new SizeProjectFilter(projectsC, 146, projectInfoDAO));
//
//		List<ProjectInfo> projectsJava = filterManager.getProjectsByLanguage("java");
//		filterManager.addFilter(new HistoryProjectFilter(projectsJava, 115, projectInfoDAO));
//		filterManager.addFilter(new TeamProjectFilter(projectsJava, 7, projectInfoDAO));
//		filterManager.addFilter(new SizeProjectFilter(projectsJava, 80, projectInfoDAO));
//
//		List<ProjectInfo> projectsJavascript = filterManager.getProjectsByLanguage("javascript");
//		filterManager.addFilter(new HistoryProjectFilter(projectsJavascript, 367, projectInfoDAO));
//		filterManager.addFilter(new TeamProjectFilter(projectsJavascript, 32, projectInfoDAO));
//		filterManager.addFilter(new SizeProjectFilter(projectsJavascript, 57, projectInfoDAO));
//
//		List<ProjectInfo> projectsPHP = filterManager.getProjectsByLanguage("php");
//		filterManager.addFilter(new HistoryProjectFilter(projectsPHP, 225, projectInfoDAO));
//		filterManager.addFilter(new TeamProjectFilter(projectsPHP, 18, projectInfoDAO));
//		filterManager.addFilter(new SizeProjectFilter(projectsPHP, 44, projectInfoDAO));
//
//		List<ProjectInfo> projectsPython = filterManager.getProjectsByLanguage("python");
//		filterManager.addFilter(new HistoryProjectFilter(projectsPython, 227, projectInfoDAO));
//		filterManager.addFilter(new TeamProjectFilter(projectsPython, 15, projectInfoDAO));
//		filterManager.addFilter(new SizeProjectFilter(projectsPython, 44, projectInfoDAO));
//
//		List<ProjectInfo> projectsRuby = filterManager.getProjectsByLanguage("ruby");
//		filterManager.addFilter(new HistoryProjectFilter(projectsRuby, 335, projectInfoDAO));
//		filterManager.addFilter(new TeamProjectFilter(projectsRuby, 31, projectInfoDAO));
//		filterManager.addFilter(new SizeProjectFilter(projectsRuby, 48, projectInfoDAO));
		

		filterManager.setMainFilter(new MigrationProjectFilter(filterManager.getProjects(), 2, 0.5f, 20, projectInfoDAO, repositoriesPath, scriptsPath));
		
		filterManager.addFilter(filterManager.getMainFilter());
		
		filterManager.cleanAndFilter();
		filterManager.persistFiltredProjects(projectInfoDAO);
	}
	
	public FilterManager(List<ProjectInfo> projects) {
		this.projects = projects;
		this.filters = new ArrayList<ProjectFilter>(); 
	}
	
	public void addFilter(ProjectFilter filter){
		this.filters.add(filter);
	}

	public List<ProjectInfo> cleanAndFilter(){
		List<ProjectInfo> filteredList = new ArrayList<>();
		for (ProjectInfo projectInfo : projects) {
			projectInfo.setFiltered(false);
			projectInfo.setFilterinfo("");
		}		
//		for (ProjectFilter projectFilter : filters) {
//			projectFilter.clean();
//		}
		for (ProjectFilter projectFilter : filters) {
			filteredList.addAll(projectFilter.filter());
		}
		return filteredList;
	}
	
	public void persistFiltredProjects(ProjectInfoDAO projectInfoDAO){
		for (ProjectInfo projectInfo : projects) {
			projectInfoDAO.update(projectInfo);
		}
	}

	public List<ProjectInfo> getProjects() {
		return projects;
	}
	
	private List<ProjectInfo> getProjectsByLanguage(String language) {
		List<ProjectInfo> newProjects = new ArrayList<>();
		for (ProjectInfo projectInfo : projects) {
			if (language.equalsIgnoreCase("c/c++")){
				if (projectInfo.getLanguage().equalsIgnoreCase("c")||projectInfo.getLanguage().equalsIgnoreCase("c++"))
					newProjects.add(projectInfo);
			}
			else if (projectInfo.getLanguage().equalsIgnoreCase(language))
				newProjects.add(projectInfo);
		}
		return newProjects;
	}

	public void setProjects(List<ProjectInfo> projects) {
		this.projects = projects;
	}

	public List<ProjectFilter> getFilters() {
		return filters;
	}

	public void setFilters(List<ProjectFilter> filters) {
		this.filters = filters;
	}

	public ProjectFilter getMainFilter() {
		if (mainFilter == null)
			mainFilter = filters.get(0);
		return mainFilter;
	}

	public void setMainFilter(ProjectFilter mainFilter) {
		this.mainFilter = mainFilter;
	}
	
}
