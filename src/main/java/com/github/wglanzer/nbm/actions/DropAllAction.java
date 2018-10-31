package com.github.wglanzer.nbm.actions;

import com.github.wglanzer.nbm.liquibase.ILiquibaseProvider;
import org.jetbrains.annotations.NotNull;
import org.openide.*;
import org.openide.awt.*;
import org.openide.util.NbBundle;

import java.util.concurrent.CancellationException;

/**
 * Execute Command: "Drop All"
 *
 * @author w.glanzer, 23.10.2018
 */
@NbBundle.Messages({
    "CTL_DropAllAction=Drop All...",
    "LBL_DropAllConfirmation=Do you really want to drop all data from the selected database?",
    "LBL_DropAllConfirmation_Title=Drop All Confirmation",
    "BTN_DropAllConfirmation=Drop All",
    "LBL_DropSuccess_Title=Drop Success",
    "LBL_DropSuccess_Message=Dropped all data from database"
})
@ActionID(category = "Liquibase", id = "com.github.wglanzer.nbm.actions.DropAllAction")
@ActionRegistration(displayName = "#CTL_DropAllAction", lazy = false)
@ActionReferences({
    @ActionReference(path = "Actions/Project/Liquibase/XML", position = 1050, separatorBefore = 1000),
    //@ActionReference(path = "Toolbars/Liquibase", position = 1000)
})
public class DropAllAction extends AbstractLiquibaseAction
{

  @Override
  public void actionPerformed(@NotNull ILiquibaseProvider pProvider) throws Exception
  {
    // First Choose Connection
    pProvider.executeWith(pLiquibase -> {
      // Then Display Warning
      NotifyDescriptor descr = new NotifyDescriptor(Bundle.LBL_DropAllConfirmation(), Bundle.LBL_DropAllConfirmation_Title(),
                                                    NotifyDescriptor.OK_CANCEL_OPTION, NotifyDescriptor.QUESTION_MESSAGE,
                                                    new Object[]{Bundle.BTN_DropAllConfirmation(), NotifyDescriptor.CANCEL_OPTION},
                                                    Bundle.BTN_DropAllConfirmation());
      if (DialogDisplayer.getDefault().notify(descr) == Bundle.BTN_DropAllConfirmation())
      {
        // Execute Action
        pLiquibase.dropAll();
        getNotificationFacade().notify(Bundle.LBL_DropSuccess_Title(), Bundle.LBL_DropSuccess_Message(), true);
      }
      else
        throw new CancellationException();
    });
  }

  @Override
  public String getName()
  {
    return Bundle.CTL_DropAllAction();
  }

}
