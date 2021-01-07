package de.adito.liquibase.internal.base;

import de.adito.liquibase.internal.changelog.IChangelogProvider;
import de.adito.liquibase.internal.connection.IConnectionProvider;
import de.adito.liquibase.notification.INotificationFacade;
import liquibase.*;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.*;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.*;
import liquibase.resource.*;
import org.jetbrains.annotations.*;
import org.netbeans.api.progress.*;
import org.openide.*;
import org.openide.util.NbBundle;

import java.awt.*;
import java.io.*;
import java.sql.Connection;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.*;

/**
 * Provides direct access to Liquibase for a given ChangeLog-File
 *
 * @author w.glanzer, 30.07.2020
 */
class LiquibaseProviderImpl implements ILiquibaseProvider
{
  private static final String CLEAR_CHECKSUMS = "Clear checksums";
  private static final String CANCEL = "Cancel";
  private static final String SKIP = "Skip";
  private IConnectionProvider connectionProvider;
  private Connection connection;

  LiquibaseProviderImpl(@NotNull IConnectionProvider pConnectionProvider)
  {
    connectionProvider = pConnectionProvider;
  }

  LiquibaseProviderImpl(@NotNull Connection pConnection)
  {
    connection = pConnection;
  }

  @NbBundle.Messages("LBL_ActionProgress=Executing Liquibase Action...")
  @Override
  public <Ex extends Exception> void executeOn(@Nullable IChangelogProvider pChangelogProvider, @NotNull ILiquibaseConsumer<Ex> pExecutor)
      throws Ex, LiquibaseException, IOException
  {
    executeOn(false, pChangelogProvider, pExecutor);
  }

  @NbBundle.Messages({
      "LBL_Title_ChangelogRequired=Cannot find the changelog",
      "LBL_Message_ChangelogRequired=The changelog must end with '.xml'"
  })
  @Override
  public <Ex extends Exception> void executeOn(boolean pChangelogRequired, @Nullable IChangelogProvider pChangeLogProvider, @NotNull ILiquibaseConsumer<Ex> pExecutor) throws Ex, LiquibaseException, IOException
  {
    if (pChangelogRequired)
    {
      if (pChangeLogProvider == null || pChangeLogProvider.findCurrentChangeLog() == null)
      {
        INotificationFacade.INSTANCE.notify(Bundle.LBL_Title_ChangelogRequired(), Bundle.LBL_Message_ChangelogRequired(), false, null);
        return;
      }
    }
    AtomicReference<Ex> exRef = new AtomicReference<>();
    if (connectionProvider != null)
      connectionProvider.executeOnCurrentConnection(pCon -> {
                                                      if (pChangelogRequired)
                                                        return _getContexts(pChangeLogProvider, pCon);
                                                      return Set.of();
                                                    },
                                                    (pCon, pStrings) -> {
                                                      _executeOn(pChangeLogProvider, pExecutor, pCon, pStrings, exRef);
                                                      return null;
                                                    });
    else
      _executeOn(pChangeLogProvider, pExecutor, connection, List.of(), exRef);

    // Exception thrown?
    if (exRef.get() != null)
      throw exRef.get();
  }

  /**
   * Extracts all Contexts from the Changesets.
   */
  private Set<String> _getContexts(@Nullable IChangelogProvider pChangelogProvider, @NotNull Connection pConnection)
  {
    try
    {
      JdbcConnection con = new JdbcConnection(pConnection);
      File currentChangeLogFile = pChangelogProvider == null ? null : pChangelogProvider.findCurrentChangeLog();
      Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(con);

      if (currentChangeLogFile != null)
      {
        ProjectResourceAccessor resourceAccessor = new ProjectResourceAccessor(pChangelogProvider);
        String changeLogPath = resourceAccessor.getRelativePath(currentChangeLogFile);
        ADITOLiquibaseImpl instance = new ADITOLiquibaseImpl(changeLogPath, resourceAccessor, database);
        return instance.getDatabaseChangeLog().getChangeSets().stream()
            .flatMap(pChangeSet -> Stream.concat(pChangeSet.getContexts().getContexts().stream(),
                                                 pChangeSet.getInheritableContexts().stream()
                                                     .flatMap(pContextExpr -> pContextExpr.getContexts().stream()))
            )
            .collect(Collectors.toSet());
      }
    }
    catch (LiquibaseException pE)
    {
      INotificationFacade.INSTANCE.error(pE);
    }

    return Set.of();
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
                                                 @NotNull Connection pConnection, List<String> pContexts, @NotNull AtomicReference<Ex> pConsumerExRef)
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
      AbstractADITOLiquibase instance;
      if (pChangelogProvider == null || currentChangeLogFile == null)
      {
        ResourceAccessor resourceAccessor = new FileSystemResourceAccessor(new File(".")); // what should we use here?!
        instance = new ADITOLiquibaseImpl(new DatabaseChangeLog(null), resourceAccessor, database);
      }
      else
      {
        ProjectResourceAccessor resourceAccessor = new ProjectResourceAccessor(pChangelogProvider);
        String changeLogPath = resourceAccessor.getRelativePath(currentChangeLogFile);
        instance = new ADITOLiquibaseImpl(changeLogPath, resourceAccessor, database);
      }

      // validate
      _validate(instance, false);

      try
      {
        // execute
        pExecutor.accept(instance, new Contexts(pContexts));
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
      "LBL_ContinueValidation=Clear CheckSums before Validation?",
      "TITLE_VALIDATION_FAIL=Liquibase validation failed"
  })
  private void _validate(@NotNull Liquibase pLiquibase, boolean pSkipable) throws LiquibaseException
  {
    try
    {
      pLiquibase.validate();
    }
    catch (ValidationFailedException vfe)
    {
      if (pSkipable)
      {
        DialogDescriptor dialogDescriptor = new DialogDescriptor("Error " + vfe.getLocalizedMessage() + " could not be cleared, skip?",
                                                                 Bundle.TITLE_VALIDATION_FAIL(), true, new String[]{SKIP, CANCEL},
                                                                 SKIP, DialogDescriptor.BOTTOM_ALIGN, null, null);
        Dialog dialog = DialogDisplayer.getDefault().createDialog(dialogDescriptor);
        dialog.setResizable(true);
        dialog.setMinimumSize(new Dimension(250, 50));
        dialog.pack();
        dialog.setVisible(true);
        if (!dialogDescriptor.getValue().equals(SKIP))
          throw vfe; //rethrow
      }
      else
      {
        DialogDescriptor dialogDescriptor = new DialogDescriptor(vfe.getLocalizedMessage() + "\n" +
                                                                     Bundle.LBL_ContinueValidation(), Bundle.TITLE_VALIDATION_FAIL(),
                                                                 true, new String[]{CLEAR_CHECKSUMS, CANCEL},
                                                                 CLEAR_CHECKSUMS, DialogDescriptor.BOTTOM_ALIGN, null, null);
        Dialog dialog = DialogDisplayer.getDefault().createDialog(dialogDescriptor);
        dialog.setResizable(true);
        dialog.setMinimumSize(new Dimension(250, 50));
        dialog.pack();
        dialog.setVisible(true);
        if (dialogDescriptor.getValue().equals(CLEAR_CHECKSUMS))
        {
          // Clear
          pLiquibase.clearCheckSums();

          // Validate and Continue
          _validate(pLiquibase, true);
        }
        else
          throw vfe; //rethrow
      }
    }
  }

}
