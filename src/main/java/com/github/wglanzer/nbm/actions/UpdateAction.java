package com.github.wglanzer.nbm.actions;

import com.github.wglanzer.nbm.liquibase.ILiquibaseProvider;
import org.jetbrains.annotations.NotNull;
import org.openide.awt.*;
import org.openide.util.NbBundle;

/**
 * Execute Command: "Update"
 *
 * @author w.glanzer, 23.10.2018
 */
@NbBundle.Messages({
    "CTL_UpdateAction=Update...",
    "LBL_UpdateAction_Success=Update Successfull"
})
@ActionID(category = "Liquibase", id = "com.github.wglanzer.nbm.actions.UpdateAction")
@ActionRegistration(displayName = "#CTL_UpdateAction", lazy = false)
@ActionReferences({
    @ActionReference(path = "Actions/Project/Liquibase/XML", position = 0),
    //@ActionReference(path = "Toolbars/Liquibase", position = 0)
})
public class UpdateAction extends AbstractLiquibaseAction
{

  @Override
  public void actionPerformed(@NotNull ILiquibaseProvider pProvider) throws Exception
  {
    pProvider.executeWith(pLiquibase -> {
      pLiquibase.update("");
      getNotificationFacade().notify(Bundle.LBL_UpdateAction_Success(), null, true);
    });
  }

  @Override
  public String getName()
  {
    return Bundle.CTL_UpdateAction();
  }

}
