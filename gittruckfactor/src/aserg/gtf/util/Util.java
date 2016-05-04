package aserg.gtf.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aserg.gtf.GitTruckFactor;
import aserg.gtf.model.NewFileInfo;

public class Util {

	public static void applyFilterFiles(List<LineInfo> filteredFilesInfo, List<NewFileInfo> files) {
		if (filteredFilesInfo != null ){
			for (LineInfo lineInfo : filteredFilesInfo) {
				String path = lineInfo.getValues().get(0);
				for (NewFileInfo newFileInfo : files) {
					if (newFileInfo.getPath().equals(path)){
						newFileInfo.setFiltered(true);
						newFileInfo.setFilterInfo(lineInfo.getValues().get(1));
					}
					
				}
			}
		}
	}

	public static void setModules(List<LineInfo> modulesInfo,
			List<NewFileInfo> files) {
		Map<String, String> moduleMap =  new HashMap<String, String>();
		for (LineInfo lineInfo : modulesInfo) {
			moduleMap.put(lineInfo.getValues().get(0), lineInfo.getValues().get(1));
		}
		for (NewFileInfo newFileInfo : files) {
			if (moduleMap.containsKey(newFileInfo.getPath()))
				newFileInfo.setModule(moduleMap.get(newFileInfo.getPath()));
			else
				GitTruckFactor.LOGGER.warn("Alert: module not found for file "+newFileInfo.getPath());
		}
		
	}

}
