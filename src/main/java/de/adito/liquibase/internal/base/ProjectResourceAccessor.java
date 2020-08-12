package de.adito.liquibase.internal.base;

import de.adito.liquibase.internal.changelog.IChangelogProvider;
import de.adito.liquibase.nb.LiquibaseFolderService;
import liquibase.resource.FileSystemResourceAccessor;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.*;
import org.netbeans.api.project.*;
import org.openide.filesystems.FileUtil;

import java.io.*;
import java.util.Optional;

/**
 * Provides access to resources inside a project
 *
 * @author w.glanzer, 30.07.2020
 */
class ProjectResourceAccessor extends FileSystemResourceAccessor
{

  public ProjectResourceAccessor(@NotNull IChangelogProvider pChangelogProvider)
  {
    super(_getResourceAccessorRoots(pChangelogProvider));
  }

  /**
   * Returns the relativized path of the given file, relative to the root
   *
   * @param pFile File
   * @return Path
   */
  @NotNull
  public String getRelativePath(@NotNull File pFile)
  {
    return getRootPaths().stream()
        .filter(pRoot -> {
          try
          {
            return FileUtils.directoryContains(pRoot.toFile(), pFile);
          }
          catch (IOException e)
          {
            return false;
          }
        })
        .map(pRoot -> pRoot.relativize(pFile.toPath()).toFile().getPath())
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Cannot find " + pFile + " in known root paths " + getRootPaths()));
  }

  /**
   * Returns all available roots for this resource accessor
   *
   * @param pChangelogProvider Provider for the currently selected changelog
   * @return the files
   */
  @Nullable
  private static File[] _getResourceAccessorRoots(@NotNull IChangelogProvider pChangelogProvider)
  {
    File changeLog = pChangelogProvider.findCurrentChangeLog();
    if (changeLog == null)
      return null;

    Project project = FileOwnerQuery.getOwner(changeLog.toURI());
    String aliasName = pChangelogProvider.findAliasName();
    if (project == null)
      return null;

    if (aliasName == null)
      // If there is no alias available in the current context,
      // then we should be aware of all aliasses in the project.
      return LiquibaseFolderService.getInstance(project).observeLiquibaseFolder()
          .map(pFolderOpt -> pFolderOpt
              .map(FileUtil::toFile)
              .map(File::listFiles))
          .blockingFirst(Optional.empty())
          .orElse(null);

    // Alias is available
    return LiquibaseFolderService.getInstance(project).observeLiquibaseFolderForAlias(aliasName)
        .map(pFolderOpt -> pFolderOpt
            .map(FileUtil::toFile)
            .map(pFile -> new File[]{pFile}))
        .blockingFirst(Optional.empty())
        .orElse(null);
  }

}
