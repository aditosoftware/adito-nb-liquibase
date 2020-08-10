package de.adito.liquibase.internal.executors;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.database.IAliasDiffService;
import de.adito.liquibase.internal.base.ILiquibaseProvider;
import de.adito.liquibase.internal.changelog.IChangelogProvider;
import de.adito.liquibase.internal.connection.IConnectionProvider;
import de.adito.liquibase.notification.INotificationFacade;
import liquibase.exception.LiquibaseException;
import org.jetbrains.annotations.NotNull;
import org.netbeans.api.project.*;
import org.openide.*;
import org.openide.util.*;

import java.io.File;
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
  public void executeDropAll(@NotNull IConnectionProvider pConnectionProvider) throws LiquibaseException
  {
    ILiquibaseProvider.getInstance(pConnectionProvider).executeOn(null, pLiquibase -> {
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
  public void executeUpdate(@NotNull IConnectionProvider pConnectionProvider, @NotNull IChangelogProvider pChangeLogProvider) throws LiquibaseException
  {
    ILiquibaseProvider.getInstance(pConnectionProvider).executeOn(pChangeLogProvider, pLiquibase -> {
      // Execute Update
      pLiquibase.update("");

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

}