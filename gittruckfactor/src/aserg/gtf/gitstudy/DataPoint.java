package aserg.gtf.gitstudy;

public class DataPoint {
	private Dev dev;
	private String file;
	
	public DataPoint(Dev dev, String file) {
		super();
		this.dev = dev;
		this.file = file;
	}
	
	@Override
	public String toString() {
		return "(" + dev.getName()+ ","+file+")";
	}

	public Dev getDev() {
		return dev;
	}

	public void setDev(Dev dev) {
		this.dev = dev;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}
}
