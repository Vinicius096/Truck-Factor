package aserg.gtf.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.eclipse.persistence.annotations.Index;

@Entity
public class LogCommitInfo extends AbstractEntity{



	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	protected Long id;
	
	@Index(name="COMMITREPOSITORYNAMEINDEX")
	private String repositoryName;
	private String sha;

	private String authorName;
	private String authorEmail;
	private Integer authorId;
	private Integer authorGitHubId;
	
	@Temporal(TemporalType.TIMESTAMP)
	private Date authorDate;
	
	private String committerName;
	private String committerEmail;
	@Temporal(TemporalType.TIMESTAMP)
	private Date committerDate;
	
	
	@Lob
	private String userName;
	
	
	private String userSource = null;
	

	@Lob
	private String message;
	

	@OneToMany(cascade = { CascadeType.ALL })
	private List<LogCommitFileInfo> logCommitFiles;

	public LogCommitInfo() {
		// TODO Auto-generated constructor stub
	}
	
	public Date getMainCommitDate(){
		if (committerDate != null)
			return committerDate;
		else
			return authorDate;
	}

	public LogCommitInfo(String repositoryName, String sha, String authorName,
			String authorEmail, Date authorDate, String commiterName,
			String commiterEmail, Date commiterDate, String message) {
		super();
		this.repositoryName = repositoryName;
		this.sha = sha;
		this.authorName = authorName;
		this.authorEmail = authorEmail;
		this.authorDate = authorDate;
		this.committerName = commiterName;
		this.committerEmail = commiterEmail;
		this.committerDate = commiterDate;
		this.message = message;
		this.userName = createUserName(authorName, authorEmail, commiterName, commiterEmail);
		this.logCommitFiles = new ArrayList<LogCommitFileInfo>();
		this.getUserSource();
	}
	
	
	public void addCommitFile(LogCommitFileInfo commitFile) {
		logCommitFiles.add(commitFile);
		
	}

	public static String createUserName(String authorName, String authorEmail, String commiterName, String commiterEmail){
		String userName;
		if (!authorEmail.isEmpty())
			userName = authorEmail;	
		else if (!authorName.isEmpty())
			userName = authorName + "[NoAuthorEmail]";
		else if (!commiterEmail.isEmpty())
			userName = commiterEmail + "[NoAuthor]";
		else if (!commiterName.isEmpty())
			userName = commiterName + "[NoAuthor-NoEmail]";
		else 
			userName = "[NoAuthor-NoCommiter]";	
		
		return userName.toUpperCase();
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String getAuthorName() {
		return authorName;
	}




	public String getAuthorEmail() {
		return authorEmail;
	}





	private Date getAuthorDate() {
		return authorDate;
	}




	public void setAuthorDate(Date authorDate) {
		this.authorDate = authorDate;
	}




	public String getCommitterName() {
		return committerName;
	}





	public String getCommitterEmail() {
		return committerEmail;
	}






	public Date getCommitterDate() {
		return committerDate;
	}




	public void setCommitterDate(Date commiterDate) {
		this.committerDate = commiterDate;
	}




	public String getMessage() {
		return message;
	}




	public void setMessage(String message) {
		this.message = message;
	}




	public void setId(Long id) {
		this.id = id;
	}

	

	public Long getId() {
		return id;
	}


	public String getRepositoryName() {
		return repositoryName;
	}


	public void setRepositoryName(String repositoryName) {
		this.repositoryName = repositoryName;
	}


	public String getSha() {
		return sha;
	}


	public void setSha(String sha) {
		this.sha = sha;
	}




	public List<LogCommitFileInfo> getLogCommitFiles() {
		return logCommitFiles;
	}




	public void setLogCommitFiles(List<LogCommitFileInfo> logCommitFiles) {
		this.logCommitFiles = logCommitFiles;
	}



	public String getUserName() {
		return userName;
	}



	public void setUserName(String userName) {
		this.userName = userName;
	}



	public String getMainName() {
		if (getUserSource() == null)
			return "";
		return getUserSource().equals("author") ? authorName : committerName;
	}
	
	public String getNormMainName() {
		String name = getMainName();
		return name.toUpperCase();
	}
	
	private String getMainEmail() {
		if (getUserSource() == null)
			return "";
		return getUserSource().equals("author") ? authorEmail : committerEmail;
	}
	
	public String getNormMainEmail() {
		String email =  getMainEmail();
		return email.toUpperCase();
	}



	public String getUserSource() {
		if (userSource == null)
			userSource = (authorName.isEmpty()&&authorEmail.isEmpty())?(committerName.isEmpty()&&committerEmail.isEmpty()?null:"committer"):"author";
		if (userSource == null)
			System.err.println("Commit without developer information");
		return userSource;
	}

	
	@Override
	public String toString() {
		return getUserName() + "-" + getSha();
	}



	public Integer getAuthorId() {
		return authorId;
	}



	public void setAuthorId(Integer authorId) {
		this.authorId = authorId;
	}


	public LogCommitInfo getClone(){
		LogCommitInfo clone = new LogCommitInfo(repositoryName, sha, authorName, authorEmail, authorDate, committerName, committerEmail, committerDate, message);
		clone.setAuthorId(authorId);
		clone.setUserName(userName);
		if (logCommitFiles != null)
			for (LogCommitFileInfo logCommitFileInfo : logCommitFiles) {
				clone.addCommitFile(logCommitFileInfo.getClone(clone));
			}
		return clone;
	}

	public void fixEmptyName() {
		if (authorName.isEmpty())
			authorName = authorEmail;
		if(committerName.isEmpty())
			committerName = committerEmail;
		
	}

	public void setAuthorGitHubId(Integer authorGitHubId) {
		this.authorGitHubId = authorGitHubId;
	}
	
	public Integer getAuthorGitHubId() {
		return authorGitHubId;
	}


}
