package aserg.gtf.task;

public class SingleDev extends GitDev {
	
	
	public SingleDev(String name, String email, String shaExample) {
		super(name, email, shaExample);
		this.devId = GitDev.getNewId();
	}
	
	public void normalizeId(int newId) {
		this.setDevId(newId);
	}
	
}
