package aserg.gtf.model.newstudy;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import aserg.gtf.CommonMethods;
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
	@OneToMany(cascade = { CascadeType.ALL })
	private List<TFDeveloper> tfDevelopers;
	@Lob
	private String tfInfo;
	private int nLeavers;
	@OneToMany(cascade = { CascadeType.ALL })
	private List<Leaver> leavers;
	@Lob
	private String leaversInfo;
	private boolean isTFEvent;
	@Temporal(TemporalType.TIMESTAMP)
	private Date firstTFLeaverDate;
	@Temporal(TemporalType.TIMESTAMP)
	private Date lastTFLeaverDate;
	private int daysBetweenLeavers; 
	@Temporal(TemporalType.TIMESTAMP)
	private Date computedDate;
	
	private int eventNCommits;
	private int eventNDevs;
	private int eventNAllFiles;
	private int eventNSourceFiles;
	
	private String computationInfo;
	
	private boolean isSurviveEvent = false;
	
	@ManyToOne(cascade = { CascadeType.ALL })
	private Measure surviveMeasure = null;
//	
//	@ManyToOne
//	private Measure lastTFEventMeasure = null;
	

	public Measure(String repositoryName, Date date, String commitSha, TFInfo tf, Date computedDate, String computationInfo) {
		this.repositoryName = repositoryName;
		this.repositoryDate = date;
		this.commitSha = commitSha;
		this.tf = tf.getTf();
//		this.tfInfo = tf.getFormatedInfo(repositoryName);
		this.tfInfo = tf.getSimpleFormatedInfo(repositoryName);
		this.nLeavers = 0;
		this.leaversInfo = new String();
		this.isTFEvent = false;
		this.lastTFLeaverDate = null;
		this.computedDate = computedDate;
		this.computationInfo = computationInfo;
		this.leavers = new ArrayList<Leaver>();
		this.tfDevelopers = new ArrayList<TFDeveloper>();
	}
	

	
	
	



	public Measure() {
		// TODO Auto-generated constructor stub
	}
	
	
	

	public void addLeaver(DeveloperInfo devInfo) {
		this.leavers.add(new Leaver(devInfo));
		this.nLeavers++;
		this.leaversInfo+= String.format("%s;%s;%s;%s;%s;%s;%d\n", 
				devInfo.getName(), 
				devInfo.getEmail(), 
				devInfo.getUserName(), 
				fDate(devInfo.getFirstCommit().getMainCommitDate()), 
				fDate(devInfo.getLastCommit().getMainCommitDate()),
				devInfo.getLastCommit().getSha(),
				devInfo.getCommits().size());
		
		boolean changed = false;
		if (lastTFLeaverDate == null || lastTFLeaverDate.before(devInfo.getLastCommit().getMainCommitDate())){
			lastTFLeaverDate = devInfo.getLastCommit().getMainCommitDate();
			changed = true;
		}
		
		if (firstTFLeaverDate == null || firstTFLeaverDate.after(devInfo.getLastCommit().getMainCommitDate())){
			firstTFLeaverDate = devInfo.getLastCommit().getMainCommitDate();
			changed = true;
		}
		if (changed)
			daysBetweenLeavers = CommonMethods.daysBetween(firstTFLeaverDate, lastTFLeaverDate);
		
		if (nLeavers == tf)
			this.isTFEvent = true;
		
	}
	
	public void addTFDeveloper(DeveloperInfo devInfo) {
		this.tfDevelopers.add(new TFDeveloper(devInfo));
	}
	
	@Override
	public String toString() {
		return String.format("%s;%s;%s;%s;%d;%d;%s;%s;%s;%s;%d;%d;%d;%d", repositoryName, fDate(repositoryDate), 
				commitSha, isTFEvent, tf, nLeavers, fDate(lastTFLeaverDate), fDate(computedDate), 
				leaversInfo.replace(";", " - ").replace("\n", "**"), tfInfo, eventNCommits, eventNDevs, eventNAllFiles, eventNSourceFiles);
		
	}
	private String fDate(Date date){
		if (date == null) 
			return "";
		return (new SimpleDateFormat("dd-MM-yyyy")).format(date);
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


	public String getComputationInfo() {
		return computationInfo;
	}


	public void setComputationInfo(String computationInfo) {
		this.computationInfo = computationInfo;
	}

	public Date getFirstTFLeaverDate() {
		return firstTFLeaverDate;
	}




	public void setFirstTFLeaverDate(Date firstTFLeaverDate) {
		this.firstTFLeaverDate = firstTFLeaverDate;
	}




	public int getEventNCommits() {
		return eventNCommits;
	}



	public void setEventNCommits(int eventNCommits) {
		this.eventNCommits = eventNCommits;
	}



	public int getEventNDevs() {
		return eventNDevs;
	}



	public void setEventNDevs(int eventNDevs) {
		this.eventNDevs = eventNDevs;
	}



	public int getEventNAllFiles() {
		return eventNAllFiles;
	}



	public void setEventNAllFiles(int eventNAllFiles) {
		this.eventNAllFiles = eventNAllFiles;
	}



	public int getEventNSourceFiles() {
		return eventNSourceFiles;
	}



	public void setEventNSourceFiles(int eventNSourceFiles) {
		this.eventNSourceFiles = eventNSourceFiles;
	}

	public boolean isSurviveEvent() {
		return isSurviveEvent;
	}

	public void setSurviveEvent(boolean isSurviveEvent) {
		this.isSurviveEvent = isSurviveEvent;
	}

	public Measure getSurviveMeasure() {
		return surviveMeasure;
	}

	public void setSurviveMeasure(Measure surviveMeasure) {
		this.surviveMeasure = surviveMeasure;
	}
	
//	public Measure getLastTFEventMeasure() {
//		return lastTFEventMeasure;
//	}
//	
//	public void setLastTFEventMeasure(Measure lastTFEventMeasure) {
//		this.lastTFEventMeasure = lastTFEventMeasure;
//	}

}
