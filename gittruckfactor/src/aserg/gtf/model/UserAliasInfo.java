package aserg.gtf.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints={
		@UniqueConstraint(columnNames = {"repositoryname, userid" })
})
public class UserAliasInfo extends AbstractEntity{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	protected Long id;
	
	private String repositoryName;
	private Integer userId;
	@ElementCollection
	private Set<String> aliases;
	private int numAliases;
	
	public UserAliasInfo() {
		// TODO Auto-generated constructor stub
	}
	public UserAliasInfo(String repositoryName, Integer userId, Set<String> aliases) {
		super();
		this.repositoryName = repositoryName;
		this.userId = userId;
		this.aliases = aliases;
		this.numAliases = aliases.size();
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
	public Integer getUserId() {
		return userId;
	}
	public void setUserId(Integer userId) {
		this.userId = userId;
	}
	public Set<String> getAliases() {
		return aliases;
	}
	public void setAliases(Set<String> aliases) {
		this.aliases = aliases;
	}
	public int getNumAliases() {
		return numAliases;
	}
	public void setNumAliases(int numAliases) {
		this.numAliases = numAliases;
	}
	
	
	
}
