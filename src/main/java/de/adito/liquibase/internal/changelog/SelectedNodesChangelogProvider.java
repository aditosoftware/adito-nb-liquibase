package de.adito.liquibase.internal.changelog;

import de.adito.liquibase.nb.LiquibaseFolderService;
import io.reactivex.rxjava3.core.Observable;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.*;
import org.netbeans.api.project.*;
import org.openide.filesystems.*;
import org.openide.nodes.Node;
import org.openide.util.*;

import java.io.*;
import java.util.*;

/**
 * Reads the currently selected nodes and extracts the selected changelog.xml-file
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

    File inContext = findChangelog(context);
    if (inContext != null)
      return inContext;

    for (Node node : context.lookupAll(Node.class))
    {
      File inNode = findChangelog(node.getLookup());
      if (inNode != null)
        return inNode;

      // single recursion stage
      for (Node childNode : node.getChildren().getNodes())
      {
        File inChildNode = findChangelog(childNode.getLookup());
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
    return LiquibaseFolderService.observe(project)
        .switchMap(pServiceOpt -> pServiceOpt
            .map(LiquibaseFolderService::observeLiquibaseFolder)
            .orElseGet(() -> Observable.just(Optional.empty())))
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
   * Returns the changelog.xml-file in a given lookup
   *
   * @param pLookup Lookup
   * @return File or null, if no changelog was found
   */
  @Nullable
  protected File findChangelog(@NonNull Lookup pLookup)
  {
    return pLookup.lookupAll(FileObject.class).stream()
        .filter(pFileObject -> pFileObject.getNameExt().equalsIgnoreCase("changelog.xml"))
        .map(FileUtil::toFile)
        .findFirst()
        .orElse(null);
  }
}