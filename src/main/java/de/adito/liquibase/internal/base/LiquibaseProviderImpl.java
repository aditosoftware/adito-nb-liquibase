package de.adito.liquibase.internal.base;

import de.adito.liquibase.internal.changelog.IChangelogProvider;
import de.adito.liquibase.internal.connection.IConnectionProvider;
import liquibase.Liquibase;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.*;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.*;
import liquibase.resource.*;
import org.jetbrains.annotations.*;
import org.netbeans.api.progress.*;
import org.openide.*;
import org.openide.util.NbBundle;

import java.io.*;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides direct access to Liquibase for a given ChangeLog-File
 *
 * @author w.glanzer, 30.07.2020
 */
class LiquibaseProviderImpl implements ILiquibaseProvider
{
  private final IConnectionProvider connectionProvider;

  LiquibaseProviderImpl(@NotNull IConnectionProvider pConnectionProvider)
  {
    connectionProvider = pConnectionProvider;
  }

  @NbBundle.Messages("LBL_ActionProgress=Executing Liquibase Action...")
  @Override
  public <Ex extends Exception> void executeOn(@Nullable IChangelogProvider pChangelogProvider, @NotNull ILiquibaseConsumer<Ex> pExecutor)
      throws Ex, LiquibaseException, IOException
  {
    AtomicReference<Ex> exRef = new AtomicReference<>();
    connectionProvider.executeOnCurrentConnection(pCon -> {
      _executeOn(pChangelogProvider, pExecutor, pCon, exRef);
      return null;
    });

    // Exception thrown?
    if (exRef.get() != null)
      throw exRef.get();
  }

  /**
   * Executes something with liquibase (LiquibaseConsumer)
   *
   * @param pChangelogProvider Provider for the changelogs
   * @param pExecutor          Contains the algorithm that should be executed
   * @param pConnection        Execute something on the given connection
   * @param pConsumerExRef     Contains the exception of the given consumer, if any happened
   */
  private <Ex extends Exception> void _executeOn(@Nullable IChangelogProvider pChangelogProvider, @NotNull ILiquibaseConsumer<Ex> pExecutor,
                                                 @NotNull Connection pConnection, @NotNull AtomicReference<Ex> pConsumerExRef)
      throws LiquibaseException
  {
    JdbcConnection con = new JdbcConnection(pConnection);
    ProgressHandle handle = ProgressHandleFactory.createSystemHandle(Bundle.LBL_ActionProgress());

    try
    {
      // show progress
      handle.start();
      handle.switchToIndeterminate();

      // Ressources
      File currentChangeLogFile = pChangelogProvider == null ? null : pChangelogProvider.findCurrentChangeLog();
      Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(con);
      Liquibase instance;
      if (pChangelogProvider == null || currentChangeLogFile == null)
      {
        ResourceAccessor resourceAccessor = new FileSystemResourceAccessor(new File(".")); // what should we use here?!
        instance = new Liquibase(new DatabaseChangeLog(null), resourceAccessor, database);
      }
      else
      {
        ProjectResourceAccessor resourceAccessor = new ProjectResourceAccessor(pChangelogProvider);
        String changeLogPath = resourceAccessor.getRelativePath(currentChangeLogFile);
        instance = new Liquibase(changeLogPath, resourceAccessor, database);
      }

      // validate
      _validate(instance);

      try
      {
        // execute
        pExecutor.accept(instance);
      }
      catch (Exception ex)
      {
        try
        {
          //noinspection unchecked
          pConsumerExRef.set((Ex) ex);
        }
        catch (ClassCastException cce)
        {
          throw new RuntimeException(cce);
        }
      }
    }
    finally
    {
      handle.finish();
    }
  }

  /**
   * Validates the given Liquibase instance
   *
   * @param pLiquibase Instance to validate
   * @throws LiquibaseException if failure, or user cancel
   */
  @NbBundle.Messages({
      "LBL_ContinueValidation=Clear CheckSums before Validation?"
  })
  private void _validate(@NotNull Liquibase pLiquibase) throws LiquibaseException
  {
    try
    {
      pLiquibase.validate();
    }
    catch (ValidationFailedException vfe)
    {
      NotifyDescriptor.Confirmation descr = new DialogDescriptor.Confirmation(vfe.getLocalizedMessage() + "\n" +
                                                                                  Bundle.LBL_ContinueValidation(), NotifyDescriptor.YES_NO_OPTION);
      Object result = DialogDisplayer.getDefault().notify(descr);
      if (result == NotifyDescriptor.YES_OPTION)
      {
        // Clear
        pLiquibase.clearCheckSums();

        // Validate and Continue
        _validate(pLiquibase);
      }
      else
        throw vfe; //rethrow
    }
  }

}
