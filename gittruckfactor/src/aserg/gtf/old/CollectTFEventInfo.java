package aserg.gtf.old;

import aserg.gtf.CommonMethods;
import aserg.gtf.GitTruckFactor;
import aserg.gtf.dao.ProjectInfoDAO;
import aserg.gtf.dao.newstudy.LeaverDAO;
import aserg.gtf.dao.newstudy.MeasureDAO;
import aserg.gtf.model.DeveloperInfo;
import aserg.gtf.model.LogCommitInfo;
import aserg.gtf.model.ProjectInfo;
import aserg.gtf.model.ProjectStatus;
import aserg.gtf.model.newstudy.Leaver;
import aserg.gtf.task.SimpleAliasHandler;
import aserg.gtf.util.FileInfoReader;
import aserg.gtf.util.LineInfo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public class CollectTFEventInfo
{
  private static final Logger LOGGER = Logger.getLogger(GitTruckFactor.class);
  
  public static void main(String[] args)
  {
    MeasureDAO measureDAO = new MeasureDAO();
    ProjectInfoDAO projectDAO = new ProjectInfoDAO();
    List<ProjectInfo> projects = projectDAO.findAll(null);
    String repositoriesPath = "/Users/guilherme/test/github_repositories/";
    String scriptsPath = "./";
    Map<String, List<LineInfo>> aliasInfo;
    try
    {
      aliasInfo = FileInfoReader.getFileInfo("repo_info/alias.txt");
    }
    catch (IOException e)
    {
      LOGGER.warn("Not possible to read repo_info/alias.txt file. Aliases treating step will not be executed!");
      aliasInfo = null;
    }
    if (args.length > 0)
    {
      repositoriesPath = args[0];
      if (repositoriesPath.charAt(repositoriesPath.length() - 1) != '/') {
        repositoriesPath = repositoriesPath + "/";
      }
    }
    if (args.length > 1)
    {
      scriptsPath = args[1];
      if (scriptsPath.charAt(scriptsPath.length() - 1) != '/') {
        scriptsPath = scriptsPath + "/";
      }
    }
    for (ProjectInfo projectInfo : projects) {
      if (projectInfo.getStatus() == ProjectStatus.GETINFO)
      {
        projectInfo.setStatus(ProjectStatus.ANALYZING);
        projectDAO.update(projectInfo);
        
        String repositoryName = projectInfo.getFullName();
        String repositoryPath = repositoriesPath + repositoryName + "/";
        try
        {
          CommonMethods commonMethods = new CommonMethods(repositoryPath, repositoryName);
          
          String stdOut = commonMethods.createAndExecuteCommand(scriptsPath + "reset_repo.sh " + repositoryPath + " " + projectInfo.getDefault_branch());
          System.out.println(stdOut);
          stdOut = commonMethods.createAndExecuteCommand(scriptsPath + "get_git_log.sh " + repositoryPath);
          System.out.println(stdOut);
          if ((aliasInfo != null) && (aliasInfo.containsKey(repositoryName))) {
            commonMethods.replaceNamesInLogCommitFile(aliasInfo.get(repositoryName));
          }
          Map<String, LogCommitInfo> allRepoCommits = commonMethods.gitLogExtractor.execute();
          Map<String, Integer> mapIds = new SimpleAliasHandler().execute(repositoryName, allRepoCommits);
          
          System.out.println("Persisting " + repositoryName + " commits ...");
          commonMethods.gitLogExtractor.persistNoThread(allRepoCommits);
          
          Map<Integer, DeveloperInfo> repositoryDevelopers = commonMethods.getRepositoryDevelopers(allRepoCommits, mapIds);
          
          List<Leaver> leavers = new LeaverDAO().getRepositoryLeavers(repositoryName);
          for (Leaver leaver : leavers)
          {
            DeveloperInfo dev = repositoryDevelopers.get(leaver.getUsername());
            leaver.setCommits(dev.getCommits());
          }
          System.out.println("Persisting Leavers commits ...");
          new LeaverDAO().updateAll(leavers);
          
          projectInfo.setStatus(ProjectStatus.ANALYZED);
          projectDAO.update(projectInfo);
        }
        catch (Exception e)
        {
          e.printStackTrace(System.err);
          projectInfo.setErrorMsg("NewTFStudy error: " + e.getMessage());
          projectInfo.setStatus(ProjectStatus.ERROR);
          projectDAO.update(projectInfo);
        }
      }
    }
  }
}
