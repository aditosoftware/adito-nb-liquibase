package de.adito.liquibase.internal.executors.generate;

import liquibase.structure.DatabaseObject;
import liquibase.structure.core.*;
import org.jetbrains.annotations.NotNull;
import org.openide.util.NbBundle;

import java.util.*;

/**
 * The types for the GENERATE CHANGELOG command.
 *
 * @author s.seemann, 23.12.2020
 */
@NbBundle.Messages({
    "LBL_Data=Data",
    "LBL_TableStructure=Structure"
})
public enum EGenerateType
{
  /**
   * Generates the table structure with all indixes and constraints are included
   */
  TABLE(DatabaseObjectFactory.getInstance().getStandardTypes(), Bundle.LBL_TableStructure(), true),

  /**
   * Generate the insert statements
   */
  DATA(List.of(Data.class), Bundle.LBL_Data(), false);

  EGenerateType(@NotNull Collection<Class<? extends DatabaseObject>> pSnapshotTypes, String pDisplayName, boolean pDefaultType)
  {
    snapshotTypes = pSnapshotTypes;
    displayName = pDisplayName;
    defaultType = pDefaultType;
  }

  private final Collection<Class<? extends DatabaseObject>> snapshotTypes;
  private final String displayName;
  private final boolean defaultType;

  public Collection<Class<? extends DatabaseObject>> getSnapshotTypes()
  {
    return snapshotTypes;
  }

  public String getDisplayName()
  {
    return displayName;
  }

  public boolean isDefaultType()
  {
    return defaultType;
  }
}
