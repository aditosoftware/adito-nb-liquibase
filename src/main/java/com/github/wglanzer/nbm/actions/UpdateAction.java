package com.github.wglanzer.nbm.actions;

import com.github.wglanzer.nbm.liquibase.ILiquibaseProvider;
import com.github.wglanzer.nbm.util.Util;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.liquibase.*;
import org.jetbrains.annotations.NotNull;
import org.openide.awt.*;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.*;

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
    @ActionReference(path = LiquiConstants.ACTION_REFERENCE, position = 0),
    //@ActionReference(path = "Toolbars/Liquibase", position = 0)
})
public class UpdateAction extends AbstractLiquibaseAction implements ILiquibaseUpdateAction
{
  @Override
  public void execute(@NotNull ILiquibaseProvider pProvider) throws Exception
  {
    pProvider.executeWith(pLiquibase -> {
      
      //pLiquibase.update("");
      
      String message = null;
      Action action = null;
      Object value = getValue(LiquiConstants.DETAILS_ACTION_KEY);
      if (value instanceof Action)
      {
        action = (Action) value;
        message = (String) action.getValue(Action.SHORT_DESCRIPTION);
        action.putValue(LiquiConstants.CHANGELOG_KEY, pLiquibase.getChangeLogFile());//todo notwendig
        action.putValue("url", pLiquibase.getDatabase().getConnection().getURL());//todo notwendig
      }
      
      getNotificationFacade().notify(Bundle.LBL_UpdateAction_Success(), message, true, action);

    });
  }

  @Override
  public String getName()
  {
    return Bundle.CTL_UpdateAction();
  }

  @Override
  protected void performAction(Node[] activatedNodes)
  {
    perform();
    //actionPerformed((ActionEvent) null);
  }

  @Override
  protected boolean enable(Node[] activatedNodes)//todo warum werden hier keine nodes geliefert?
  {
    Node[] nodes = TopComponent.getRegistry().getActivatedNodes();
    if (nodes.length == 1)
      return Util.existsChangelogFile(nodes[0]) && Util.containsConnection(nodes[0]);

    return false;
  }
}
