package de.adito.liquibase.internal.base;

import liquibase.*;
import liquibase.changelog.*;
import liquibase.changelog.filter.*;
import liquibase.database.*;
import liquibase.exception.*;
import liquibase.resource.ResourceAccessor;

import java.io.Writer;

/**
 * Own Implementation of {@link Liquibase}.
 *
 * @author s.seemann, 17.12.2020
 */
public abstract class AbstractADITOLiquibase extends Liquibase
{
  protected boolean skipFilter = false;

  public AbstractADITOLiquibase(String changeLogFile, ResourceAccessor resourceAccessor, DatabaseConnection conn) throws LiquibaseException
  {
    super(changeLogFile, resourceAccessor, conn);
  }

  public AbstractADITOLiquibase(String changeLogFile, ResourceAccessor resourceAccessor, Database database)
  {
    super(changeLogFile, resourceAccessor, database);
  }

  public AbstractADITOLiquibase(DatabaseChangeLog changeLog, ResourceAccessor resourceAccessor, Database database)
  {
    super(changeLog, resourceAccessor, database);
  }

  /**
   * If true, the {@link ShouldRunChangeSetFilter} in {@link #getStandardChangelogIterator(Contexts, LabelExpression, DatabaseChangeLog)} is skipped.
   */
  public void setSkipFilter(boolean pSkipFilter)
  {
    skipFilter = pSkipFilter;
  }

  /**
   * Returns the ChangeLogIterator. If {@link #skipFilter} is true, {@link ShouldRunChangeSetFilter} is excluded.
   *
   * @see #setSkipFilter(boolean)
   */
  @Override
  protected ChangeLogIterator getStandardChangelogIterator(Contexts contexts, LabelExpression labelExpression, DatabaseChangeLog changeLog) throws DatabaseException
  {
    if (skipFilter)
      return new ChangeLogIterator(changeLog,
                                   new ContextChangeSetFilter(contexts),
                                   new LabelChangeSetFilter(labelExpression),
                                   new DbmsChangeSetFilter(database),
                                   new IgnoreChangeSetFilter());

    return super.getStandardChangelogIterator(contexts, labelExpression, changeLog);
  }

  @Override
  public abstract void futureRollbackSQL(Integer count, String tag, Contexts contexts, LabelExpression labelExpression,
                                         Writer output, boolean checkLiquibaseTables) throws LiquibaseException;
}
