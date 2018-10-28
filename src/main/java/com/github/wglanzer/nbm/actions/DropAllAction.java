package com.github.wglanzer.nbm.actions;

import com.github.wglanzer.nbm.liquibase.ILiquibaseProvider;
import liquibase.Liquibase;
import org.jetbrains.annotations.NotNull;
import org.openide.awt.*;
import org.openide.util.NbBundle;

/**
 * Execute Command: "Drop All"
 *
 * @author w.glanzer, 23.10.2018
 */
@NbBundle.Messages("CTL_DropAllAction=Drop All")
@ActionID(category = "Liquibase", id = "com.github.wglanzer.nbm.actions.DropAllAction")
@ActionRegistration(displayName = "#CTL_DropAllAction", lazy = false)
@ActionReferences({
    @ActionReference(path = "Actions/Project/Liquibase/XML", position = 1050, separatorBefore = 1000),
    //@ActionReference(path = "Toolbars/Liquibase", position = 1000)
})
public class DropAllAction extends AbstractLiquibaseAction
{

  @Override
  public void actionPerformed(@NotNull ILiquibaseProvider pProvider)
  {
    try
    {
      pProvider.executeWith(Liquibase::dropAll);
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getName()
  {
    return Bundle.CTL_DropAllAction();
  }

}
