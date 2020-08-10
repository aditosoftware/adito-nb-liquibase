package de.adito.liquibase.actions;

import de.adito.liquibase.internal.base.IConnectionProvider;
import de.adito.liquibase.internal.connection.DialogConnectionProvider;
import de.adito.liquibase.notification.INotificationFacade;
import liquibase.exception.LiquibaseException;
import org.jetbrains.annotations.NotNull;
import org.openide.nodes.Node;
import org.openide.util.*;
import org.openide.util.actions.NodeAction;

import java.util.concurrent.CancellationException;

/**
 * Abstract LiquibaseAction in Context Menu
 *
 * @author w.glanzer, 10.08.2020
 */
abstract class AbstractLiquibaseAction extends NodeAction
{

  private final IConnectionProvider connectionProvider = new DialogConnectionProvider();

  @Override
  protected final void performAction(Node[] activatedNodes)
  {
    RequestProcessor.getDefault().post(() -> {
      try
      {
        performAction0(activatedNodes == null ? new Node[0] : activatedNodes);
      }
      catch (CancellationException cancelled)
      {
        //nothing
      }
      catch (LiquibaseException e)
      {
        INotificationFacade.INSTANCE.error(e);
      }
    });
  }

  @Override
  protected boolean enable(Node[] activatedNodes)
  {
    return connectionProvider.hasConnectionsAvailable();
  }

  @Override
  public HelpCtx getHelpCtx()
  {
    return null;
  }

  @Override
  protected boolean asynchronous()
  {
    return false;
  }

  /**
   * Gets called, if this action should be executed
   *
   * @param pActivatedNodes Currently selected nodes
   * @throws CancellationException Exception, if the user cancels the execution
   * @throws LiquibaseException    Exception that happens, if Liquibase failes somehow
   */
  protected abstract void performAction0(@NotNull Node[] pActivatedNodes) throws CancellationException, LiquibaseException;

  /**
   * @return the connection provider for creating liquibase instances
   */
  @NotNull
  protected IConnectionProvider getConnectionProvider()
  {
    return connectionProvider;
  }

}
