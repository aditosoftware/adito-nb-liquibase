package de.adito.liquibase.actions;

import de.adito.liquibase.internal.connection.IConnectionProvider;
import de.adito.liquibase.internal.executors.ILiquibaseExecutorFacade;
import liquibase.exception.LiquibaseException;
import org.jetbrains.annotations.NotNull;
import org.openide.awt.*;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

import java.util.concurrent.CancellationException;

/**
 * Performs the "Drop All" and "Update" Action in Liquibase
 *
 * @author w.glanzer, 10.08.2020
 */
@NbBundle.Messages("CTL_DropAllAndUpdateAction=Drop All & Update...")
@ActionID(category = "Liquibase", id = "de.adito.liquibase.actions.DropAllAndUpdateAction")
@ActionRegistration(displayName = "#CTL_DropAllAndUpdateAction", lazy = false)
@ActionReference(path = "Plugins/Liquibase/Actions", position = 300, separatorAfter = 350)
public class DropAllAndUpdateAction extends AbstractLiquibaseAction
{

  @Override
  protected void performAction0(@NotNull Node[] pNodes) throws CancellationException, LiquibaseException
  {
    // get connection provider to persist user selection
    // and only show selection dialog once
    IConnectionProvider connectionProvider = getConnectionProvider();

    // Drop All
    ILiquibaseExecutorFacade.INSTANCE.executeDropAll(connectionProvider);

    // Update
    ILiquibaseExecutorFacade.INSTANCE.executeUpdate(connectionProvider, getChangelogProvider());
  }

  @Override
  public String getName()
  {
    return Bundle.CTL_DropAllAndUpdateAction();
  }

}
