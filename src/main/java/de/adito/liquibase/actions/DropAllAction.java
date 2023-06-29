package de.adito.liquibase.actions;

import de.adito.liquibase.internal.executors.ILiquibaseExecutorFacade;
import liquibase.exception.LiquibaseException;
import lombok.NonNull;
import org.openide.awt.*;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

import java.io.IOException;
import java.util.concurrent.CancellationException;

/**
 * Performs the "Drop All" Action in Liquibase
 *
 * @author w.glanzer, 30.07.2020
 */
@NbBundle.Messages("CTL_DropAllAction=Drop All...")
@ActionID(category = "Liquibase", id = "de.adito.liquibase.actions.DropAllAction")
@ActionRegistration(displayName = "#CTL_DropAllAction", lazy = false)
@ActionReference(path = "Plugins/Liquibase/Actions", position = 200)
public class DropAllAction extends AbstractLiquibaseAction
{

  @Override
  protected void performAction0(Node @NonNull [] pNodes) throws CancellationException, LiquibaseException, IOException
  {
    ILiquibaseExecutorFacade.INSTANCE.executeDropAll(getConnectionProvider());
  }

  @Override
  public String getName()
  {
    return Bundle.CTL_DropAllAction();
  }

  @Override
  protected boolean changelogAware()
  {
    return false;
  }

}
