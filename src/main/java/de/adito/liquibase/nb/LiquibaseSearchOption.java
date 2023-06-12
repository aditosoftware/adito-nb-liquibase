package de.adito.liquibase.nb;

import lombok.NonNull;
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
  @NonNull
  @Override
  public List<SearchFilterDefinition> getFilters()
  {
    return Collections.singletonList(new SearchFilterDefinition()
    {
      @Override
      public boolean searchFile(@NonNull FileObject file) throws IllegalArgumentException
      {
        return true;
      }

      @NonNull
      @Override
      public FolderResult traverseFolder(@NonNull FileObject folder) throws IllegalArgumentException
      {
        if (folder.getNameExt().equalsIgnoreCase(".liquibase"))
          return FolderResult.TRAVERSE_ALL_SUBFOLDERS;
        return FolderResult.DO_NOT_TRAVERSE;
      }
    });
  }
}