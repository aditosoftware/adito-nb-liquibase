package de.adito.liquibase.internal.executors;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.database.IAliasDiffService;
import de.adito.liquibase.internal.base.*;
import de.adito.liquibase.internal.changelog.IChangelogProvider;
import de.adito.liquibase.internal.connection.IConnectionProvider;
import de.adito.liquibase.notification.INotificationFacade;
import liquibase.*;
import liquibase.exception.*;
import org.jetbrains.annotations.NotNull;
import org.netbeans.api.project.*;
import org.openide.*;
import org.openide.util.*;

import java.io.*;
import java.util.concurrent.CancellationException;

/**
 * @author w.glanzer, 30.07.2020
 */
class LiquibaseExecutorFacadeImpl implements ILiquibaseExecutorFacade
{

  @NbBundle.Messages({
      "LBL_DropAllConfirmation=Do you really want to drop all tables from the selected database?",
      "LBL_DropAllConfirmation_Title=Drop All Confirmation",
      "BTN_DropAllConfirmation=Drop All",
      "LBL_DropSuccess_Title=Drop Success",
      "LBL_DropSuccess_Message=Dropped all data from database"
  })
  @Override
  public void executeDropAll(@NotNull IConnectionProvider pConnectionProvider) throws LiquibaseException, IOException
  {
    ILiquibaseProvider.getInstance(pConnectionProvider).executeOn(null, (pLiquibase, pContexts) -> {
      // Then Display Warning
      NotifyDescriptor descr = new NotifyDescriptor(Bundle.LBL_DropAllConfirmation(), Bundle.LBL_DropAllConfirmation_Title(),
                                                    NotifyDescriptor.OK_CANCEL_OPTION, NotifyDescriptor.QUESTION_MESSAGE,
                                                    new Object[]{Bundle.BTN_DropAllConfirmation(), NotifyDescriptor.CANCEL_OPTION},
                                                    NotifyDescriptor.CANCEL_OPTION);
      if (DialogDisplayer.getDefault().notify(descr) == Bundle.BTN_DropAllConfirmation())
      {
        // Execute Action
        pLiquibase.dropAll();

        // Finished
        INotificationFacade.INSTANCE.notify(Bundle.LBL_DropSuccess_Title(), Bundle.LBL_DropSuccess_Message(), true, null);
      }
      else
        throw new CancellationException();
    });
  }

  @NbBundle.Messages({
      "LBL_UpdateSuccess=Update Succesfull",
      "LBL_DiffWithDBTables=Diff with DB tables"
  })
  @Override
  public void executeUpdate(@NotNull IConnectionProvider pConnectionProvider, @NotNull IChangelogProvider pChangeLogProvider) throws LiquibaseException, IOException
  {
    ILiquibaseProvider.getInstance(pConnectionProvider).executeOn(true, pChangeLogProvider, (pLiquibase, pContexts) -> {
      // Execute Update
      pLiquibase.update(pContexts);

      // Finished
      INotificationFacade.INSTANCE.notify(Bundle.LBL_UpdateSuccess(), Bundle.LBL_DiffWithDBTables(), false, e -> {
        // Perform Diff on click
        File changeLog = pChangeLogProvider.hasChangelogsAvailable() ? pChangeLogProvider.findCurrentChangeLog() : null;
        String aliasName = pChangeLogProvider.findAliasName();
        if (changeLog != null && aliasName != null)
        {
          Project owner = FileOwnerQuery.getOwner(changeLog.toURI());
          if (owner != null)
          {
            IAliasDiffService aliasDiffService = Lookup.getDefault().lookup(IAliasDiffService.class);
            aliasDiffService.executeDiffWithDB(owner, aliasName);
          }
        }
      });
    });
  }

  @Override
  public void executeUpdateSQL(@NotNull IConnectionProvider pConnectionProvider, @NotNull IChangelogProvider pChangeLogProvider) throws LiquibaseException, IOException
  {
    ILiquibaseProvider.getInstance(pConnectionProvider).executeOn(true, pChangeLogProvider, (pLiquibase, pContexts) -> {
      Writer writer = new StringWriter();
      _getUpdateSql(pLiquibase, pContexts, writer);

      // Finished
      INotificationFacade.INSTANCE.showSql(writer.toString());
    });
  }

  @NbBundle.Messages({
      "LBL_TitleRollbackImpossible=Cannot create future rollback SQL",
      "LBL_MessageRollbackImpossible=Maybe in the changelog is a insert-tag or sql-tag defined, but no rollback-tag"
  })
  @Override
  public void executeFutureRollbackSQL(@NotNull IConnectionProvider pConnectionProvider, @NotNull IChangelogProvider pChangeLogProvider) throws LiquibaseException, IOException
  {
    ILiquibaseProvider.getInstance(pConnectionProvider).executeOn(true, pChangeLogProvider, (pLiquibase, pContexts) -> {
      Writer writer = new StringWriter();
      if (!_getFutureRollbackSql(pLiquibase, pContexts, writer))
        return;

      // Finished
      INotificationFacade.INSTANCE.showSql(writer.toString());
    });
  }

  @Override
  public void executeUpdateAndRollbackSQL(@NotNull IConnectionProvider pConnectionProvider, @NotNull IChangelogProvider pChangeLogProvider) throws LiquibaseException, IOException
  {
    ILiquibaseProvider.getInstance(pConnectionProvider).executeOn(true, pChangeLogProvider, (pLiquibase, pContexts) -> {
      Writer writer = new StringWriter();
      _getUpdateSql(pLiquibase, pContexts, writer);
      if (!_getFutureRollbackSql(pLiquibase, pContexts, writer))
        return;

      // Finished
      INotificationFacade.INSTANCE.showSql(writer.toString());
    });
  }

  /**
   * Executes the UPDATE SQL command. It does not check, if the changelog is already run or not.
   */
  private void _getUpdateSql(@NotNull AbstractADITOLiquibase pLiquibase, @NotNull Contexts pContexts, @NotNull Writer pOutput) throws LiquibaseException
  {
    try
    {
      pLiquibase.setSkipFilter(true);
      pLiquibase.update(pContexts, pOutput);
    }
    finally
    {
      pLiquibase.setSkipFilter(false);
    }
  }

  /**
   * Executes the FUTURE ROLLBACK SQL command. If an {@link RollbackImpossibleException} occures, it isn't shown as error, but as information.
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean _getFutureRollbackSql(@NotNull AbstractADITOLiquibase pLiquibase, @NotNull Contexts pContexts, @NotNull Writer pOutput) throws LiquibaseException
  {
    try
    {
      pLiquibase.futureRollbackSQL(pContexts, new LabelExpression(), pOutput);
    }
    catch (Throwable pE)
    {
      Throwable temp = pE.getCause();
      if (temp != null)
        temp = temp.getCause();

      // special handling, because the user receives additional information about the cause
      if (temp instanceof RollbackImpossibleException)
      {
        INotificationFacade.INSTANCE.notify(Bundle.LBL_TitleRollbackImpossible(), Bundle.LBL_MessageRollbackImpossible(), false, null);
        return false;
      }
      throw pE;
    }

    return true;
  }
}