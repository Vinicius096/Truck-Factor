package aserg.gtf.dao.newstudy;

import aserg.gtf.dao.GenericDAO;
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
	
	

}
