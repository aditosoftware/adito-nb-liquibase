package de.adito.liquibase.nb;

import org.jetbrains.annotations.NotNull;
import org.netbeans.spi.search.*;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

import java.util.*;

/**
 * SearchOption to define, that ".liquibase" folder can be traversed by search
 *
 * @author w.glanzer, 03.08.2020
 */
@ServiceProvider(service = SubTreeSearchOptions.class, path = "Projects/de-adito-project/SearchOptions")
public class LiquibaseSearchOption extends SubTreeSearchOptions
{
  @NotNull
  @Override
  public List<SearchFilterDefinition> getFilters()
  {
    return Collections.singletonList(new SearchFilterDefinition()
    {
      @Override
      public boolean searchFile(@NotNull FileObject file) throws IllegalArgumentException
      {
        return true;
      }

      @NotNull
      @Override
      public FolderResult traverseFolder(@NotNull FileObject folder) throws IllegalArgumentException
      {
        if (folder.getNameExt().equalsIgnoreCase(".liquibase"))
          return FolderResult.TRAVERSE_ALL_SUBFOLDERS;
        return FolderResult.DO_NOT_TRAVERSE;
      }
    });
  }
}