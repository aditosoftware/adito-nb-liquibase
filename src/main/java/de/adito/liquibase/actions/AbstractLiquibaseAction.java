package de.adito.liquibase.actions;

import de.adito.liquibase.internal.changelog.*;
import de.adito.liquibase.internal.connection.*;
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
  private final IChangelogProvider changelogProvider = new SelectedNodesChangelogProvider();

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
    boolean connectionOK = connectionProvider.hasConnectionsAvailable();
    boolean changelogOK = !changelogAware() || changelogProvider.hasChangelogsAvailable();
    return connectionOK && changelogOK;
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
   * @return true, if this action is only available, if a changelog is selected
   */
  protected boolean changelogAware()
  {
    return true;
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

  /**
   * @return the changelog provider that provides access to the currently selected changelog
   */
  @NotNull
  protected IChangelogProvider getChangelogProvider()
  {
    return changelogProvider;
  }

}
