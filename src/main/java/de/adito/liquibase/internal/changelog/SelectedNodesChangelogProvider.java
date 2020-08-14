package de.adito.liquibase.internal.changelog;

import de.adito.liquibase.nb.LiquibaseFolderService;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.*;
import org.netbeans.api.project.*;
import org.openide.filesystems.*;
import org.openide.nodes.Node;
import org.openide.util.*;

import java.io.*;
import java.util.*;

/**
 * Reads the currently selected nodes and extracts the selected changelog file
 *
 * @author w.glanzer, 10.08.2020
 */
public class SelectedNodesChangelogProvider implements IChangelogProvider
{

  @Nullable
  @Override
  public File findCurrentChangeLog()
  {
    Lookup context = Utilities.actionsGlobalContext();

    File inContext = _findChangelog(context);
    if (inContext != null)
      return inContext;

    for (Node node : context.lookupAll(Node.class))
    {
      File inNode = _findChangelog(node.getLookup());
      if (inNode != null)
        return inNode;

      // single recursion stage
      for (Node childNode : node.getChildren().getNodes())
      {
        File inChildNode = _findChangelog(childNode.getLookup());
        if (inChildNode != null)
          return inChildNode;
      }
    }

    return null;
  }

  @Nullable
  @Override
  public String findAliasName()
  {
    File currentChangeLog = findCurrentChangeLog();
    if (currentChangeLog == null)
      return null;

    Project project = FileOwnerQuery.getOwner(currentChangeLog.toURI());
    if (project == null)
      return null;

    // Read the liquibase folder from a project, extract all available aliases
    // and find the correct one by searching the selected changelog
    return LiquibaseFolderService.getInstance(project).observeLiquibaseFolder()
        .blockingFirst(Optional.empty())
        .map(FileUtil::toFile)
        .flatMap(pFile -> {
          File[] children = pFile.listFiles();
          if (children == null)
            return Optional.empty();

          // All children have the same name as an alias -> correct alias found, if paths match
          return Arrays.stream(children)
              .filter(pLiquibaseFolder -> {
                try
                {
                  return FileUtils.directoryContains(pLiquibaseFolder, currentChangeLog);
                }
                catch (IOException e)
                {
                  return false;
                }
              })
              .map(File::getName)
              .findFirst();
        })
        .orElse(null);
  }

  @Override
  public boolean hasChangelogsAvailable()
  {
    return findCurrentChangeLog() != null;
  }

  /**
   * Returns the changelog file in a given lookup
   *
   * @param pLookup Lookup
   * @return File or null, if no changelog was found
   */
  @Nullable
  private File _findChangelog(@NotNull Lookup pLookup)
  {
    return pLookup.lookupAll(FileObject.class).stream()
        .filter(pFileObject -> pFileObject.getNameExt().equals("changelog.xml")) //todo change search
        .map(FileUtil::toFile)
        .findFirst()
        .orElse(null);
  }

}
