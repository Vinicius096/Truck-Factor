package aserg.gtf.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class ProjectDevelopers extends AbstractEntity{
	@Id
	private String projectName;

	@ElementCollection
	private Set<String> notGitHubDevs;

	@ElementCollection
	private Set<String> gitHubDevs;

	private int nGitHubDevs=0;
	private int nNotGitHubDevs=0;
	
	private int nCommits;
	private int nCommitsGitHubAuthor;
	private int nCommitsGitHubAuthorOrCommitter;
	
	public ProjectDevelopers() {
		// TODO Auto-generated constructor stub
	}
	public ProjectDevelopers(String projectName) {
		this.projectName = projectName;
		notGitHubDevs = new HashSet<String>();
		gitHubDevs = new HashSet<String>();
	}
	
	
	public Set<String> getNotGitHubDevs() {
		return notGitHubDevs;
	}
	
	public void addNotGitHubDevs(String nameEmailStr){
		this.notGitHubDevs.add(nameEmailStr);
		this.nNotGitHubDevs = notGitHubDevs.size();
	}
	
	public Set<String> getGitHubDevs() {
		return gitHubDevs;
	}
	
	public void addGitHubDevs(String login){
		this.gitHubDevs.add(login);
		this.nGitHubDevs = gitHubDevs.size();
	}
	public String getProjectName() {
		return projectName;
	}
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	public void setGitHubDevs(Set<String> gitHubDevs) {
		this.gitHubDevs = gitHubDevs;
	}
	public void setNotGitHubDevs(Set<String> notGitHubDevs) {
		this.notGitHubDevs = notGitHubDevs;
	}
	public int getnCommits() {
		return nCommits;
	}
	public void setnCommits(int nCommits) {
		this.nCommits = nCommits;
	}
	public int getnCommitsGitHubAuthor() {
		return nCommitsGitHubAuthor;
	}
	public void setnCommitsGitHubAuthor(int nCommitsGitHubAuthor) {
		this.nCommitsGitHubAuthor = nCommitsGitHubAuthor;
	}
	public int getnCommitsGitHubAuthorOrCommitter() {
		return nCommitsGitHubAuthorOrCommitter;
	}
	public void setnCommitsGitHubAuthorOrCommitter(
			int nCommitsGitHubAuthorOrCommitter) {
		this.nCommitsGitHubAuthorOrCommitter = nCommitsGitHubAuthorOrCommitter;
	}
	public int getnGitHubDevs() {
		return nGitHubDevs;
	}
	public void setnGitHubDevs(int nGitHubDevs) {
		this.nGitHubDevs = nGitHubDevs;
	}
	public int getnNotGitHubDevs() {
		return nNotGitHubDevs;
	}
	public void setnNotGitHubDevs(int nNotGitHubDevs) {
		this.nNotGitHubDevs = nNotGitHubDevs;
	}
	
}
