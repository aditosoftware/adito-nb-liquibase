package de.adito.liquibase.nb;

import de.adito.observables.netbeans.FileObservable;
import de.adito.util.reactive.cache.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.*;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.*;
import org.netbeans.api.project.Project;
import org.openide.filesystems.*;
import org.openide.util.lookup.ServiceProvider;

import java.io.*;
import java.util.Optional;

/**
 * Contains all necessary information about the .liquibase-Folder in a single project
 *
 * @author w.glanzer, 05.08.2020
 */
@ServiceProvider(service = LiquibaseFolderService.class, path = "Projects/de-adito-project/Lookup")
public class LiquibaseFolderService implements Disposable
{

  private static final String _LIQUIBASE_FOLDER = ".liquibase";
  private Project project;

  /**
   * Returns the instance for a specific project
   *
   * @param pProject Project
   * @return the instance
   */
  @NotNull
  public static LiquibaseFolderService getInstance(@NotNull Project pProject)
  {
    LiquibaseFolderService service = pProject.getLookup().lookup(LiquibaseFolderService.class);
    if (service == null)
      throw new RuntimeException("Failed to retrieve liquibase service for project " + pProject + ". " +
                                     "Please reinstall your Liquibase plugin to provide this service.");
    return service;
  }

  private final ObservableCache cache = new ObservableCache();
  private final CompositeDisposable disposable = new CompositeDisposable();

  @SuppressWarnings("unused") // ServiceProvider
  public LiquibaseFolderService()
  {
  }

  @SuppressWarnings("unused") // ServiceProvider with given Project
  public LiquibaseFolderService(@NotNull Project pProject)
  {
    project = pProject;
    disposable.add(new ObservableCacheDisposable(cache));
  }

  @Override
  public void dispose()
  {
    if (!disposable.isDisposed())
      disposable.dispose();
  }

  @Override
  public boolean isDisposed()
  {
    return disposable.isDisposed();
  }

  /**
   * Observes the subfolder in ".liquibase" Folder for a given alias
   *
   * @param pAliasName Name of the alias to be tracked
   * @return Observable with the folder as content. Triggers on Rename/Move etc.
   */
  @NotNull
  public Observable<Optional<FileObject>> observeLiquibaseFolderForAlias(@NotNull String pAliasName)
  {
    return cache.calculate("liquibaseFolderForAlias_" + pAliasName, () -> FileObservable.create(_getLiquibaseFolderFileForAlias(pAliasName))
        .map(pFileOpt -> pFileOpt.map(FileUtil::toFileObject)));
  }

  /**
   * Triggers, that an alias with pOldName should be renamed to pNewName or be deleted
   *
   * @param pOldName Old Name
   * @param pNewName New Name, NULL if it should be deleted
   */
  public void moveAlias(@NotNull String pOldName, @Nullable String pNewName) throws IOException
  {
    // nothing to change
    if (pOldName.equals(pNewName))
      return;

    File oldFolder = _getLiquibaseFolderFileForAlias(pOldName);

    // Old does not exist or is not modifiable
    if (!oldFolder.exists() || !oldFolder.canWrite() || !oldFolder.canRead())
      return;

    if (pNewName != null)
      FileUtils.moveDirectory(oldFolder, _getLiquibaseFolderFileForAlias(pNewName));
    else
      FileUtils.deleteDirectory(oldFolder);
  }

  /**
   * Creates the File-Object that points to the .liquibase-Folder instance for a given alias
   *
   * @param pAliasName Alias
   * @return the file, maybe does not exist
   */
  @NotNull
  private File _getLiquibaseFolderFileForAlias(@NotNull String pAliasName)
  {
    File liquibaseFolder = new File(FileUtil.toFile(project.getProjectDirectory()), _LIQUIBASE_FOLDER);
    return new File(liquibaseFolder, pAliasName);
  }
}
