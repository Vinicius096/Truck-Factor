package aserg.gtf.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.eclipse.persistence.annotations.Index;

@Entity
public class GitHubDeveloper extends AbstractEntity{
	@Transient
	private static String SEP_STR= "**";
	@Transient
	private static Map<Integer, GitHubDeveloper> gitHubDevs = new HashMap<Integer, GitHubDeveloper>();
	@Id
	private int gitHubId;
	
	@Index(name="GITHUB_LOGIN")
	private String login;

	
	@ElementCollection
	private Set<String> pairsNameEmail;
	private String name;
	private String email;
	private String company;
	
	@Transient
	private boolean updated = false;
	
	public GitHubDeveloper() {
		// TODO Auto-generated constructor stub
	}
//	public GitHubDeveloper(User user) {
//		this.gitHubId = user.getId();
//		this.login = user.getLogin();
//		this.name = user.getName();
//		this.email = user.getEmail();
//		this.company = user.getCompany();
//		this.pairsNameEmail = new HashSet<String>();
//		updated = true;
//	}
	
	public static void initiateGitHubDeveloper(List<GitHubDeveloper> gitHubDevelopers){
		gitHubDevs = new HashMap<Integer, GitHubDeveloper>();
		for (GitHubDeveloper gitHubDeveloper : gitHubDevelopers) {
			gitHubDevs.put(gitHubDeveloper.getGitHubId(), gitHubDeveloper);
		}
	}

//	public static GitHubDeveloper addGitHubDeveloper(User user,
//			CommitUser commitUser, ProjectDevelopers projectDevs) {
//		String userString = createUserString(commitUser);
//		if(user!=null&&user.getLogin()!=null){
//			if (!gitHubDevs.containsKey(user.getId()))
//				gitHubDevs.put(user.getId(), new GitHubDeveloper(user));
//			GitHubDeveloper gitHubDev = gitHubDevs.get(user.getId());
//			if (!gitHubDev.pairsNameEmail.contains(userString)){
//				gitHubDev.pairsNameEmail.add(createUserString(commitUser));
//				gitHubDev.setUpdated(true);
//			}
//			projectDevs.addGitHubDevs(user.getLogin());
//			return gitHubDev;
//		}
//		else{
//			projectDevs.addNotGitHubDevs(userString);
//		}
//		return null;
//	}
	public static String createUserString(String userName, String userEmail) {
		return userName.toUpperCase()+SEP_STR+userEmail.toUpperCase();
	}
	public int getGitHubId() {
		return gitHubId;
	}
	public void setGitHubId(int gitHubId) {
		this.gitHubId = gitHubId;
	}
	public String getLogin() {
		return login;
	}
	public void setLogin(String login) {
		this.login = login;
	}
	public Set<String> getPairsNameEmail() {
		return pairsNameEmail;
	}
	public void setPairsNameEmail(Set<String> pairsNameEmail) {
		this.pairsNameEmail = pairsNameEmail;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getCompany() {
		return company;
	}
	public void setCompany(String company) {
		this.company = company;
	}
	
	public static Map<Integer, GitHubDeveloper> getGitHubDevs() {
		return gitHubDevs;
	}
	
	@Override
	public String toString() {
		String str = String.format("%s;%s;%s;%s", login, name, email, pairsNameEmail);
		return str;
	}
	public boolean isUpdated() {
		return updated;
	}
	public void setUpdated(boolean updated) {
		this.updated = updated;
	}
	
	
}
