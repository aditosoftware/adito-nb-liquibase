package de.adito.liquibase.internal.base;

import liquibase.resource.FileSystemResourceAccessor;
import org.jetbrains.annotations.NotNull;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileUtil;

import java.io.File;

/**
 * Provides access to resources inside a project
 *
 * @author w.glanzer, 30.07.2020
 */
class ProjectResourceAccessor extends FileSystemResourceAccessor
{

  private final Project project;

  public ProjectResourceAccessor(@NotNull Project pProject)
  {
    super(FileUtil.toFile(pProject.getProjectDirectory()));
    project = pProject;
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
    return FileUtil.toFile(project.getProjectDirectory()).toPath()
        .relativize(pFile.toPath())
        .toFile()
        .getPath();
  }

}
