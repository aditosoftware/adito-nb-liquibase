package de.adito.liquibase.internal.base;

import liquibase.Liquibase;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.*;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.*;
import liquibase.resource.*;
import org.jetbrains.annotations.*;
import org.netbeans.api.progress.*;
import org.netbeans.api.project.*;
import org.openide.*;
import org.openide.filesystems.FileUtil;
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
  public <Ex extends Exception> void executeOn(@Nullable File pChangeLogFile, @NotNull ILiquibaseConsumer<Ex> pExecutor) throws Ex, LiquibaseException
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

        // create
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(con);
        ResourceAccessor resAcc = _createResourceAccesor(pChangeLogFile);
        String changelogFile = resAcc instanceof ProjectResourceAccessor && pChangeLogFile != null ?
            ((ProjectResourceAccessor) resAcc).getRelativePath(pChangeLogFile) : null;
        Liquibase base = new Liquibase(new DatabaseChangeLog(changelogFile), resAcc, database);

        // validate
        _validate(base);

        // execute
        pExecutor.accept(base);
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
   * Creates the correct resource accessor for the given changelog
   *
   * @param pChangeLogFile File
   * @return the accessor
   */
  @NotNull
  private ResourceAccessor _createResourceAccesor(@Nullable File pChangeLogFile)
  {
    if (pChangeLogFile == null)
      return new FileSystemResourceAccessor(new File(".")); // what should we do?!

    Project project = FileOwnerQuery.getOwner(FileUtil.toFileObject(pChangeLogFile));
    if (project == null)
      throw new RuntimeException("File has to be placed in a valid project (" + pChangeLogFile + ")");
    return new ProjectResourceAccessor(project);
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
