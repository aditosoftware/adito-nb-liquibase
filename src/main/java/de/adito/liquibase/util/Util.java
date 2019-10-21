package de.adito.liquibase.util;

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
  
  private static String _getChangelogPath(Node pNode)
  {
    String folderPath = getFolderPath(pNode);
    if (folderPath != null)
      return folderPath + File.separator + LiquiConstants.CHANGELOG_FILE;

    return null;
  }

  /**
   * Returns the path for this node relative to
   * the Liquibase home folder.
   * @param pNode needed for project location.
   * @return a path or null.
   */
  public static String getFolderPath(Node pNode)
  {
    final String sep = File.separator;

    Project project = pNode.getLookup().lookup(Project.class);
    if (project != null)
    {
      String projectPath = FileUtil.toFile(project.getProjectDirectory()).getPath();
      String lqPath = projectPath + sep + LiquiConstants.LIQUIBASE_FOLDER;

      DataFolder f = pNode.getLookup().lookup(DataFolder.class);
      if (f != null)
        return lqPath + sep + f.getPrimaryFile().getName();
    }

    return null;
  }

  /**
   * Returns true if the changelog.xml file exists
   * @param pNode needed for project location.
   * @return true if changelog.xml exists, els false.
   */
  public static boolean existsChangelogFile(Node pNode)
  {
    String changelogPath = _getChangelogPath(pNode);
    if (changelogPath != null)
      return Files.exists(new File(changelogPath).toPath());

    return false;
  }

  /**
   * Returns true if a DatabaseConnection object resides
   * in node's lookup.
   * @param pNode we need its lookup.
   */
  public static boolean containsConnection(Node pNode)
  {
    return pNode.getLookup().lookup(DatabaseConnection.class) != null;
  }

}

