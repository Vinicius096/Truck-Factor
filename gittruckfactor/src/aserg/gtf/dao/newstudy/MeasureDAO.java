package aserg.gtf.dao.newstudy;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Query;

import aserg.gtf.dao.GenericDAO;
import aserg.gtf.model.newstudy.Leaver;
import aserg.gtf.model.newstudy.Measure;

public class MeasureDAO extends GenericDAO<Measure>{

	@Override
	public Measure find(Object id) {
		return this.em.find(Measure.class, id);
	}

	@Override
	public boolean exist(Measure entity) {
		return this.find(entity.getId())!=null;
	}
	
	public void persistOrUpdate(Measure o) {
		synchronized (this) {
			if (o.getId()==null)
				super.persist(o);
			else
				super.merge(o);
		}
	}

	public String getNumberOfCommitsData(String repositoryName, String timeframe)
	{
		String strRes="";
		String hql = " WITH "
				+ "repocommits AS (SELECT * FROM logcommitinfo WHERE  repositoryname = \'"+ repositoryName + "\'), "
				+ "maxDate AS (SELECT max(committerdate) FROM repocommits), "
				+ "grid AS ( SELECT start_time, lead(start_time) OVER (ORDER BY start_time) AS end_time FROM  "
				+ "(SELECT generate_series(min(committerdate), max(committerdate) + interval \'1 " + timeframe +"\', interval \'1 " + timeframe +"\') AS start_time "
						+ "FROM   repocommits  ) x ) "
				+ "SELECT TO_CHAR(start_time, 'DD-MM-YYYY'), count(DISTINCT e.sha) AS events FROM grid g "
					+ "LEFT JOIN repocommits e ON e.committerdate >= g.start_time "
					+ "AND e.committerdate <  g.end_time "
					+ "WHERE g.start_time <= (SELECT max FROM maxDate) "
					+ "GROUP  BY 1 ORDER  BY 1;";
		Query q = this.em.createNativeQuery(hql);
		List resultList = q.getResultList();
		for (Object object : resultList) {
			Object o[] = (Object[]) object;
			strRes+=repositoryName+";"+o[0]+";"+o[1]+"\n";
		}
		return strRes;
	}
	
	public String getNumberOfCommitsDataAll(String timeframe, String computedDate)
	{
		String strRes="repository;date;ncommits\n";
		String hql;
		if (computedDate == null)
			hql = "SELECT repositoryname FROM measure m "
				+ "WHERE istfevent "
				+ "GROUP BY repositoryname;";
		else
			hql = "SELECT repositoryname FROM measure m "
				+ "WHERE istfevent and computeddate = \'" + computedDate + "\' "
				+ "GROUP BY repositoryname;";
		Query q = this.em.createNativeQuery(hql);
		List resultList = q.getResultList();
		for (Object object : resultList) {
			strRes+=getNumberOfCommitsData((String)object, timeframe);
		}
		return strRes;
	}
	
	public String getNumberOfLeaversCommits(String repositoryName, String leaverName, String timeframe)
	{
		String strRes="";
		String hql = " WITH "
				+ "leavercommits AS (SELECT lci.* FROM measure m "
					+ "JOIN measure_leaver ml ON ml.measure_id = m.id "
					+ "JOIN leaver l ON ml.leavers_id = l.id "
					+ "JOIN leaver_logcommitinfo ll ON ll.leaver_id = l.id "
					+ "JOIN logcommitinfo lci ON ll.commits_id = lci.id "
					+ "WHERE istfevent AND m.repositoryname = \'"+ repositoryName + "\' "
							+ "AND l.username = \'"+ leaverName + "\'), "
				+ "maxDate AS (SELECT max(committerdate) FROM leavercommits), "
				+ "grid AS ( SELECT start_time, lead(start_time) OVER (ORDER BY start_time) AS end_time FROM  "
				+ "(SELECT generate_series(min(committerdate), max(committerdate) + interval \'1 " + timeframe +"\', interval \'1 " + timeframe +"\') AS start_time "
						+ "FROM   leavercommits  ) x ) "
				+ "SELECT TO_CHAR(start_time, 'DD-MM-YYYY'), count(DISTINCT e.sha) AS events FROM grid g "
					+ "LEFT JOIN leavercommits e ON e.committerdate >= g.start_time "
					+ "AND e.committerdate <  g.end_time "
					+ "WHERE g.start_time <= (SELECT max FROM maxDate) "
					+ "GROUP  BY 1 ORDER  BY 1;";
		Query q = this.em.createNativeQuery(hql);
		List resultList = q.getResultList();
		for (Object object : resultList) {
			Object o[] = (Object[]) object;
			strRes+=repositoryName+";"+leaverName+";"+o[0]+";"+o[1]+"\n";
		}
		return strRes;
	}
	
	public String getNumberOfCommitsDataAllLeavers(String timeframe, String computedDate)
	{
		String strRes="repository;leaver;date;ncommits\n";
		String hql;
		if (computedDate == null)
			hql = "SELECT DISTINCT m.repositoryname, l.username FROM measure m "
					+ "JOIN measure_leaver ml ON ml.measure_id = m.id "
					+ "JOIN leaver l ON ml.leavers_id = l.id "
					+ "JOIN leaver_logcommitinfo ll ON ll.leaver_id = l.id "
					+ "JOIN logcommitinfo lci ON ll.commits_id = lci.id "
						+ "WHERE istfevent  ORDER BY m.repositoryname, l.username;";
		else
			hql = "SELECT DISTINCT m.repositoryname, l.username FROM measure m "
					+ "JOIN measure_leaver ml ON ml.measure_id = m.id "
					+ "JOIN leaver l ON ml.leavers_id = l.id "
					+ "JOIN leaver_logcommitinfo ll ON ll.leaver_id = l.id "
					+ "JOIN logcommitinfo lci ON ll.commits_id = lci.id "
						+ "WHERE istfevent and computeddate = \'" + computedDate + "\' ORDER BY m.repositoryname, l.username;";
		Query q = this.em.createNativeQuery(hql);
		List resultList = q.getResultList();
		for (Object object : resultList) {
			Object o[] = (Object[]) object;
			strRes+=getNumberOfLeaversCommits((String)o[0], (String)o[1], timeframe);
		}
		return strRes;
	}

	
}
