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
import aserg.gtf.truckfactor.TFInfo;

@Entity
public class Measure extends AbstractEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	protected Long id;
	
	private String repositoryName;
	@Temporal(TemporalType.TIMESTAMP)
	private Date repositoryDate;
	private String commitSha;
	private int tf;
	private String tfInfo;
	private int nLeavers;
	private String leaversInfo;
	private boolean isTFEvent;
	@Temporal(TemporalType.TIMESTAMP)
	private Date lastTFLeaverDate;
	

	public Measure() {
		// TODO Auto-generated constructor stub
	}
	
	
	
	public Measure(String repositoryName, Date date, String commitSha, TFInfo tf) {
		this.repositoryName = repositoryName;
		this.repositoryDate = date;
		this.commitSha = commitSha;
		this.tf = tf.getTf();
		this.tfInfo = tf.getFormatedInfo(repositoryName);
		this.nLeavers = 0;
		this.leaversInfo = new String();
		this.isTFEvent = false;
		this.lastTFLeaverDate = null;
	}

	public void addLeaver(DeveloperInfo devInfo) {
		this.nLeavers++;
		this.leaversInfo+= String.format("%s;%s;%s;%s;%s;%d\n", 
				devInfo.getName(), 
				devInfo.getEmail(), 
				devInfo.getUserName(), 
				devInfo.getFirstCommit().getMainCommitDate(), 
				devInfo.getLastCommit().getMainCommitDate(),
				devInfo.getCommits().size());
		
		if (lastTFLeaverDate == null || lastTFLeaverDate.before(devInfo.getLastCommit().getMainCommitDate()))
			lastTFLeaverDate = devInfo.getLastCommit().getMainCommitDate();
		
		if (nLeavers == tf)
			this.isTFEvent = true;
		
	}


	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getRepositoryName() {
		return repositoryName;
	}
	public void setRepositoryName(String repositoryName) {
		this.repositoryName = repositoryName;
	}
	public Date getMeasureDate() {
		return repositoryDate;
	}
	public void setMeasureDate(Date measureDate) {
		this.repositoryDate = measureDate;
	}
	public String getCommitSha() {
		return commitSha;
	}
	public void setCommitSha(String commitSha) {
		this.commitSha = commitSha;
	}
	public int getTf() {
		return tf;
	}
	public void setTf(int tf) {
		this.tf = tf;
	}
	public String getTfInfo() {
		return tfInfo;
	}
	public void setTfInfo(String tfInfo) {
		this.tfInfo = tfInfo;
	}
	public int getnLeavers() {
		return nLeavers;
	}
	public void setnLeavers(int nLeavers) {
		this.nLeavers = nLeavers;
	}
	public String getLeaversInfo() {
		return leaversInfo;
	}
	public void setLeaversInfo(String leaversInfo) {
		this.leaversInfo = leaversInfo;
	}
	public boolean isTFEvent() {
		return isTFEvent;
	}
	public void setTFEvent(boolean isTFEvent) {
		this.isTFEvent = isTFEvent;
	}
	public Date getRepositoryDate() {
		return repositoryDate;
	}
	public void setRepositoryDate(Date repositoryDate) {
		this.repositoryDate = repositoryDate;
	}

	public Date getLastTFLeaverDate() {
		return lastTFLeaverDate;
	}
	public void setLastTFLeaverDate(Date lastTFLeaverDate) {
		this.lastTFLeaverDate = lastTFLeaverDate;
	}

	
	
	
	
}
