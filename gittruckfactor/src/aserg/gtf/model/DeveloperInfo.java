package aserg.gtf.model;

import java.util.ArrayList;
import java.util.List;

public class DeveloperInfo {
	private String name;
	private String email;
	private String userName;
	private Integer userId;
	
	private LogCommitInfo firstCommit;
	private LogCommitInfo lastCommit;
	private List<LogCommitInfo> commits;
	
	public DeveloperInfo(String name, String email, String userName, Integer userId) {
		super();
		this.name = name;
		this.email = email;
		this.userName = userName;
		this.userId = userId;
		commits = new ArrayList<LogCommitInfo>();
	}
	
	public void addCommit(LogCommitInfo commit){
		if (firstCommit == null){
			firstCommit = commit;
			lastCommit = commit;
		}
		else{
			if (commit.getMainCommitDate().before(firstCommit.getMainCommitDate()))
				firstCommit = commit;
			if (commit.getMainCommitDate().after(lastCommit.getMainCommitDate()))
				lastCommit = commit;
		}
		commits.add(commit);
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getUserName() {
		return userName;
	}

	public LogCommitInfo getFirstCommit() {
		return firstCommit;
	}

	public LogCommitInfo getLastCommit() {
		return lastCommit;
	}

	public List<LogCommitInfo> getCommits() {
		return commits;
	}
	
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return this.userId.hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		DeveloperInfo other = (DeveloperInfo)obj;
		return this.userId.equals(other.userId);
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}
	@Override
	public String toString() {
		return userName+"="+userId;
	}
}
