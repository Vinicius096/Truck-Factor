package aserg.gtf.gitstudy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Dev {
	private String username;
	private String name;
	private String email;
	private Set<String> files;
	
	public Dev(String username, String name, String email) {
		super();
		this.username = username;
		this.name = name;
		this.email = email;
		files = new HashSet<String>();
	}

	public void addFile(String file){
		files.add(file);
	}
	
	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
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

	public Set<String> getFiles() {
		return files;
	}

	public void setFiles(Set<String> files) {
		this.files = files;
	}
	
	
	
	
}
