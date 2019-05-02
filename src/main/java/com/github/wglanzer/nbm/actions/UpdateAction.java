package com.github.wglanzer.nbm.actions;

import com.github.wglanzer.nbm.liquibase.ILiquibaseProvider;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.liquibase.*;
import org.jetbrains.annotations.NotNull;
import org.openide.awt.*;
import org.openide.util.*;

import java.awt.event.*;

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
@ActionReference(path = LiquiConstants.ACTION_REFERENCE, position = 0)
public class UpdateAction extends AbstractLiquibaseAction implements ILiquibaseUpdateAction
{
  @Override
  public void execute(@NotNull ILiquibaseProvider pProvider) throws Exception
  {
    pProvider.executeWith(pLiquibase -> {

      pLiquibase.update("");

      String message = null;
      ActionListener action = null;

      IDiffService service = Lookup.getDefault().lookup(IDiffService.class);
      if (service != null)
      {
        action = new _DetailsAction(service);
        message = service.getMessage();
      }

      getNotificationFacade().notify(Bundle.LBL_UpdateAction_Success(), message, true, action);
    });
  }

  @Override
  public String getName()
  {
    return Bundle.CTL_UpdateAction();
  }

  private class _DetailsAction implements ActionListener
  {
    private IDiffService service;

    _DetailsAction(IDiffService pService)
    {
      service = pService;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
      service.perform();
    }
  }
}
