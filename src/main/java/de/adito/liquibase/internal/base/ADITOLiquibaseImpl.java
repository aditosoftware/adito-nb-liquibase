package de.adito.liquibase.internal.base;

import liquibase.*;
import liquibase.changelog.*;
import liquibase.changelog.filter.*;
import liquibase.changelog.visitor.ListVisitor;
import liquibase.database.*;
import liquibase.exception.*;
import liquibase.executor.*;
import liquibase.lockservice.*;
import liquibase.resource.ResourceAccessor;

import java.io.*;
import java.util.*;

/**
 * @author s.seemann, 17.12.2020
 */
public class ADITOLiquibaseImpl extends AbstractADITOLiquibase
{
  public ADITOLiquibaseImpl(String changeLogFile, ResourceAccessor resourceAccessor, DatabaseConnection conn) throws LiquibaseException
  {
    super(changeLogFile, resourceAccessor, conn);
  }

  public ADITOLiquibaseImpl(String changeLogFile, ResourceAccessor resourceAccessor, Database database)
  {
    super(changeLogFile, resourceAccessor, database);
  }

  public ADITOLiquibaseImpl(DatabaseChangeLog changeLog, ResourceAccessor resourceAccessor, Database database)
  {
    super(changeLog, resourceAccessor, database);
  }

  /**
   * Skips the {@link NotRanChangeSetFilter}, because we want that the SQL is generated anyway. Therefor 4 lines of the method are changed.
   */
  @Override
  public void futureRollbackSQL(Integer count, String tag, Contexts contexts, LabelExpression labelExpression, Writer output, boolean checkLiquibaseTables) throws LiquibaseException
  {
    runInScope(new Scope.ScopedRunner()
    {
      @Override
      public void run() throws Exception
      {


        LoggingExecutor outputTemplate = new LoggingExecutor(Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor(database),
                                                             output, database);
        Executor oldTemplate = getAndReplaceJdbcExecutor(output);
        Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor(database, outputTemplate);

        outputHeader("SQL to roll back currently unexecuted changes");

        LockService lockService = LockServiceFactory.getInstance().getLockService(database);
        lockService.waitForLock();

        try
        {
          DatabaseChangeLog changeLog = getDatabaseChangeLog();
          if (checkLiquibaseTables)
          {
            checkLiquibaseTables(false, changeLog, contexts, labelExpression);
          }
          ChangeLogHistoryServiceFactory.getInstance().getChangeLogService(database).generateDeploymentId();

          changeLog.validate(database, contexts, labelExpression);

          ChangeLogIterator logIterator;
          if ((count == null) && (tag == null))
          {
            logIterator = new ChangeLogIterator(changeLog, // ADITO changed
                                                new ContextChangeSetFilter(contexts),
                                                new LabelChangeSetFilter(labelExpression),
                                                new IgnoreChangeSetFilter(),
                                                new DbmsChangeSetFilter(database));
          }
          else if (count != null)
          {
            ChangeLogIterator forwardIterator = new ChangeLogIterator(changeLog, // ADITO changed
                                                                      new ContextChangeSetFilter(contexts),
                                                                      new LabelChangeSetFilter(labelExpression),
                                                                      new DbmsChangeSetFilter(database),
                                                                      new IgnoreChangeSetFilter(),
                                                                      new CountChangeSetFilter(count));
            final ListVisitor listVisitor = new ListVisitor();
            forwardIterator.run(listVisitor, new RuntimeEnvironment(database, contexts, labelExpression));

            logIterator = new ChangeLogIterator(changeLog, // ADITO changed
                                                new ContextChangeSetFilter(contexts),
                                                new LabelChangeSetFilter(labelExpression),
                                                new DbmsChangeSetFilter(database),
                                                new IgnoreChangeSetFilter(),
                                                new ChangeSetFilter()
                                                {
                                                  @Override
                                                  public ChangeSetFilterResult accepts(ChangeSet changeSet)
                                                  {
                                                    return new ChangeSetFilterResult(
                                                        listVisitor.getSeenChangeSets().contains(changeSet), null, null
                                                    );
                                                  }
                                                });
          }
          else
          {
            List<RanChangeSet> ranChangeSetList = database.getRanChangeSetList();
            ChangeLogIterator forwardIterator = new ChangeLogIterator(changeLog, // ADITO changed
                                                                      new ContextChangeSetFilter(contexts),
                                                                      new LabelChangeSetFilter(labelExpression),
                                                                      new DbmsChangeSetFilter(database),
                                                                      new IgnoreChangeSetFilter(),
                                                                      new UpToTagChangeSetFilter(tag, ranChangeSetList));
            final ListVisitor listVisitor = new ListVisitor();
            forwardIterator.run(listVisitor, new RuntimeEnvironment(database, contexts, labelExpression));

            logIterator = new ChangeLogIterator(changeLog, // ADITO changed
                                                new ContextChangeSetFilter(contexts),
                                                new LabelChangeSetFilter(labelExpression),
                                                new DbmsChangeSetFilter(database),
                                                new IgnoreChangeSetFilter(),
                                                new ChangeSetFilter()
                                                {
                                                  @Override
                                                  public ChangeSetFilterResult accepts(ChangeSet changeSet)
                                                  {
                                                    return new ChangeSetFilterResult(
                                                        listVisitor.getSeenChangeSets().contains(changeSet), null, null
                                                    );
                                                  }
                                                });
          }

          logIterator.run(createRollbackVisitor(),
                          new RuntimeEnvironment(database, contexts, labelExpression)
          );
        }
        finally
        {
          try
          {
            lockService.releaseLock();
          }
          catch (LockException e)
          {
            getLog().severe(MSG_COULD_NOT_RELEASE_LOCK, e);
          }
          Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor("jdbc", database, oldTemplate);
          resetServices();
        }

        flushOutputWriter(
            output);
      }
    });
  }

  private void runInScope(Scope.ScopedRunner scopedRunner) throws LiquibaseException
  {
    Map<String, Object> scopeObjects = new HashMap<>();
    scopeObjects.put(Scope.Attr.database.name(), getDatabase());
    scopeObjects.put(Scope.Attr.resourceAccessor.name(), getResourceAccessor());

    try
    {
      Scope.child(scopeObjects, scopedRunner);
    }
    catch (Exception e)
    {
      if (e instanceof LiquibaseException)
      {
        throw (LiquibaseException) e;
      }
      else
      {
        throw new LiquibaseException(e);
      }
    }
  }

  private void flushOutputWriter(Writer output) throws LiquibaseException
  {
    try
    {
      output.flush();
    }
    catch (IOException e)
    {
      throw new LiquibaseException(e);
    }
  }

  private Executor getAndReplaceJdbcExecutor(Writer output)
  {
    /* We have no other choice than to save the current Executor here. */
    @SuppressWarnings("squid:S1941")
    Executor oldTemplate = Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor("jdbc", database);
    final LoggingExecutor loggingExecutor = new LoggingExecutor(oldTemplate, output, database);
    Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor("logging", database, loggingExecutor);
    Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor("jdbc", database, loggingExecutor);
    return oldTemplate;
  }
}
