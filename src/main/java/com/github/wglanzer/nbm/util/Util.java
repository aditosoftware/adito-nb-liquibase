package com.github.wglanzer.nbm.util;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.liquibase.LiquiConstants;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.nodes.Node;

import java.io.File;
import java.nio.file.Files;


public final class Util
{
  public static String getChangelogPath(Node pNode)
  {
    String folderPath = getFolderPath(pNode);
    if (folderPath != null)
      return folderPath + File.separator + LiquiConstants.CHANGELOG_FILE;

    return null;
  }

  public static String getFolderPath(Node pNode)
  {
    final String sep = File.separator;

    Project project = pNode.getLookup().lookup(Project.class);
    if (project != null)
    {
      String projectPath = FileUtil.toFile(project.getProjectDirectory()).getPath();
      String lqPath = projectPath + sep + LiquiConstants.LIQUIBASE_FOLDER;

      DataFolder f = pNode.getLookup().lookup(DataFolder.class);
      return lqPath + sep + f.getPrimaryFile().getName();
    }

    return null;
  }
  
  
  public static boolean existsChangelogFile(Node pNode)
  {
    String changelogPath = getChangelogPath(pNode);
    if (changelogPath != null)
      return Files.exists(new File(changelogPath).toPath());


    return false;
  }


  public static boolean containsConnection(Node pNode)
  {
    return pNode.getLookup().lookup(DatabaseConnection.class) != null;
  }

}

