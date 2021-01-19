package de.adito.liquibase.nb;

import org.jetbrains.annotations.NotNull;
import org.netbeans.api.actions.Openable;
import org.openide.filesystems.*;

import java.io.File;

/**
 * Utils for Netbeans-Methods
 *
 * @author s.seemann, 08.01.2021
 */
public class NbUtil
{

  /**
   * Opens the file
   *
   * @param pFile the file, which should be opened
   */
  public static void open(@NotNull File pFile)
  {
    FileObject fo = FileUtil.toFileObject(pFile);
    for (Openable openable : fo.getLookup().lookupAll(Openable.class))
      openable.open();
  }
}
