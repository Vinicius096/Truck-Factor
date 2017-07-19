package aserg.gtf.model.newstudy;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import aserg.gtf.model.AbstractEntity;
import aserg.gtf.model.DeveloperInfo;

@Entity
public class Leaver extends AbstractEntity{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	protected Long id;
	
	private String name;
	private String email;
	private String username;
	private String lastCommitSha;
	@Temporal(TemporalType.TIMESTAMP)
	private Date lastCommitDate;
	private String firstCommitSha;
	@Temporal(TemporalType.TIMESTAMP)
	private Date firstCommitDate;
	private int nCommits;
	
	
	
	public Leaver() {
		// TODO Auto-generated constructor stub
	}
	
	
	
	public Leaver(DeveloperInfo devInfo) {
		this.name = devInfo.getName();
		this.email = devInfo.getEmail().toLowerCase();
		this.username = devInfo.getUserName();
		this.firstCommitDate = devInfo.getFirstCommit().getMainCommitDate();
		this.firstCommitSha = devInfo.getFirstCommit().getSha();
		this.lastCommitDate = devInfo.getLastCommit().getMainCommitDate();
		this.lastCommitSha = devInfo.getLastCommit().getSha();
		this.nCommits = devInfo.getCommits().size();
	}



	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
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
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getLastCommitSha() {
		return lastCommitSha;
	}
	public void setLastCommitSha(String lastCommitSha) {
		this.lastCommitSha = lastCommitSha;
	}
	public Date getLastCommitDate() {
		return lastCommitDate;
	}
	public void setLastCommitDate(Date lastCommitDate) {
		this.lastCommitDate = lastCommitDate;
	}
	public String getFirstCommitSha() {
		return firstCommitSha;
	}
	public void setFirstCommitSha(String firstCommitSha) {
		this.firstCommitSha = firstCommitSha;
	}
	public Date getFirstCommitDate() {
		return firstCommitDate;
	}
	public void setFirstCommitDate(Date firstCommitDate) {
		this.firstCommitDate = firstCommitDate;
	}
	public int getnCommits() {
		return nCommits;
	}
	public void setnCommits(int nCommits) {
		this.nCommits = nCommits;
	}
	
	
	
}
