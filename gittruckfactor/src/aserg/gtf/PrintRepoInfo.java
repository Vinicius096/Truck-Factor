package aserg.gtf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import aserg.gtf.dao.newstudy.MeasureDAO;

public class PrintRepoInfo {
	static final String INVESTIGATION_DATE = "2017-07-18 06:52:59.771";
	public static void main(String[] args) throws IOException {
		String result;
//		result = new MeasureDAO().getNumberOfCommitsDataAll("month", null);
//		result = new MeasureDAO().getNumberOfCommitsDataAll("month", INVESTIGATION_DATE);
//		result = new MeasureDAO().getNumberOfCommitsDataAllLeavers("month", null);
//		System.out.println(result);
//		Files.write(Paths.get("./leaverscommits_weekly.csv"), new MeasureDAO().getNumberOfCommitsDataAllLeavers("week", INVESTIGATION_DATE).getBytes());
//		System.out.println("SAVE leaverscommits_weekly,csv file");
		
		Files.write(Paths.get("./leaverscommits_daily.csv"), new MeasureDAO().getNumberOfCommitsDataAllLeavers("day", INVESTIGATION_DATE).getBytes());
		System.out.println("SAVE leaverscommits_daily,csv file");
		
		Files.write(Paths.get("./commits_daily.csv"), new MeasureDAO().getNumberOfCommitsDataAll("day", INVESTIGATION_DATE).getBytes());
		System.out.println("SAVE commits_daily,csv file");
				
//		Files.write(Paths.get("./commits_weekly.csv"), new MeasureDAO().getNumberOfCommitsDataAll("week", INVESTIGATION_DATE).getBytes());
	}

}
