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
 * Performs the "Drop All" and "Update" Action in Liquibase
 *
 * @author w.glanzer, 10.08.2020
 */
@NbBundle.Messages("CTL_DropAllAndUpdateAction=Drop All & Update...")
@ActionID(category = "Liquibase", id = "de.adito.liquibase.actions.DropAllAndUpdateAction")
@ActionRegistration(displayName = "#CTL_DropAllAndUpdateAction", lazy = false)
@ActionReference(path = "Plugins/Liquibase/Actions", position = 300)
public class DropAllAndUpdateAction extends AbstractLiquibaseAction
{

  @Override
  protected void performAction0(@NonNull Node[] pNodes) throws CancellationException, LiquibaseException, IOException
  {
    // Drop All & Update
    ILiquibaseExecutorFacade.INSTANCE.executeDropAllAndUpdate(getConnectionProvider(), getChangelogProvider());
  }

  @Override
  public String getName()
  {
    return Bundle.CTL_DropAllAndUpdateAction();
  }

}
