package aserg.gtf.task;

public abstract class GitDev {
	private static int id = 1;
	
	public static int getNewId(){
		return id++;
	}	
	protected int devId;
	
	private String name;
	private String email;  
	private String shaExample; 
	private int gitHubId=0;

	public GitDev(String name, String email, String shaExample) {
		this.name = name;
		this.email = email;
		this.shaExample = shaExample;
	}
	
//	public AliasDev addAlias(GitDev dev, String pairStr){
//		AliasDev aliasDev;
//		if (this instanceof SingleDev) {
//			aliasDev = new AliasDev(this, pairStr);
//			aliasDev.addDev(this);
//		}
//		else
//			aliasDev = (AliasDev) this;
//		
//		aliasDev.addDev(dev);
//		aliasDev.normalizeId(this.devId);
//		return aliasDev;	
//	}
	
	
	public int getDevId() {
		return devId;
	}
	
	public void setDevId(int devId) {
		this.devId = devId;
	}

	public abstract void normalizeId(int newId);
	
	public String getName() {
		return name;
	}
	public String getEmail() {
		return email;
	}
	public static String getPairString(String name, String email){
		return name.toUpperCase()+"**"+email.toUpperCase();
	}
	public String getPairString(){
		return getPairString(this.name, this.email);
	}
	
	@Override
	public boolean equals(Object obj) {
		GitDev other = (GitDev)obj;
		return this.getPairString().equals(other.getPairString());
	}
	
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return getPairString().hashCode();
	}
	
	
	public String getShaExample() {
		return shaExample;
	}
	@Override
	public String toString() {
		return name+"**"+email + "(" + getDevId()+ " - "+ gitHubId + ")";
	}
	public int getGitHubId() {
		return gitHubId;
	}
	public void setGitHubId(int gitHubId) {
		this.gitHubId = gitHubId;
	}
}
