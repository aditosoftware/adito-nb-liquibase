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

import java.io.File;
import java.sql.Connection;

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
      throws Ex, LiquibaseException
  {
    Connection jdbcCon = connectionProvider.findCurrentConnection();
    if (jdbcCon != null)
    {
      JdbcConnection con = new JdbcConnection(jdbcCon);
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

        // execute
        pExecutor.accept(instance);
      }
      finally
      {
        handle.finish();
        if (!con.isClosed())
          con.close();
      }
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
