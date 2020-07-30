package de.adito.liquibase.internal.base;

import de.adito.liquibase.internal.Bundle;
import liquibase.Liquibase;
import liquibase.database.*;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.*;
import org.jetbrains.annotations.NotNull;
import org.netbeans.api.project.*;
import org.openide.*;
import org.openide.filesystems.FileUtil;

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
  private final ProjectResourceAccessor resourceAccessor;
  private final String relativeChangeLogFile;

  LiquibaseProviderImpl(@NotNull IConnectionProvider pConnectionProvider, @NotNull File pChangeLogFile)
  {
    Project project = FileOwnerQuery.getOwner(FileUtil.toFileObject(pChangeLogFile));
    if (project == null)
      throw new RuntimeException("File has to be placed in a valid project (" + pChangeLogFile + ")");

    connectionProvider = pConnectionProvider;
    resourceAccessor = new ProjectResourceAccessor(project);
    relativeChangeLogFile = resourceAccessor.getRelativePath(pChangeLogFile);
  }

  @Override
  public <Ex extends Exception> void executeWith(@NotNull ILiquibaseConsumer<Ex> pExecutor) throws Ex, LiquibaseException
  {
    Connection jdbcCon = connectionProvider.findCurrentConnection();
    if (jdbcCon != null)
    {
      JdbcConnection con = new JdbcConnection(jdbcCon);

      try
      {
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(con);
        Liquibase base = new Liquibase(relativeChangeLogFile, resourceAccessor, database);

        // validate
        _validate(base);

        // execute
        pExecutor.accept(base);
      }
      finally
      {
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
