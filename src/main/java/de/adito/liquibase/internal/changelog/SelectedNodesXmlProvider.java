package de.adito.liquibase.internal.changelog;

import lombok.NonNull;
import org.jetbrains.annotations.*;
import org.openide.filesystems.*;
import org.openide.util.Lookup;

import java.io.File;

/**
 * Reads the currently selected nodes and extracts the selected xml-file
 *
 * @author s.seemann, 04.03.2021
 */
public class SelectedNodesXmlProvider extends SelectedNodesChangelogProvider
{

  /**
   * Returns the first xml-file in a given lookup
   *
   * @param pLookup Lookup
   * @return File or null, if no changelog was found
   */
  @Nullable
  @Override
  protected File findChangelog(@NonNull Lookup pLookup)
  {
    return pLookup.lookupAll(FileObject.class).stream()
        .filter(pFileObject -> pFileObject.getNameExt().endsWith("xml"))
        .map(FileUtil::toFile)
        .findFirst()
        .orElse(null);
  }
}
