package de.adito.liquibase.actions;

import de.adito.actions.AbstractAsyncNodeAction;
import de.adito.liquibase.internal.changelog.*;
import de.adito.liquibase.internal.connection.*;
import de.adito.liquibase.notification.INotificationFacade;
import liquibase.exception.LiquibaseException;
import org.jetbrains.annotations.NotNull;
import org.openide.LifecycleManager;
import org.openide.nodes.Node;
import org.openide.util.*;

import java.io.IOException;
import java.util.concurrent.CancellationException;

/**
 * Abstract LiquibaseAction in Context Menu
 *
 * @author w.glanzer, 10.08.2020
 */
public abstract class AbstractLiquibaseAction extends AbstractAsyncNodeAction
{

  private final IConnectionProvider connectionProvider = new DialogConnectionProvider();
  private final IChangelogProvider changelogProvider;

  public AbstractLiquibaseAction()
  {
    this(new SelectedNodesChangelogProvider());
  }

  public AbstractLiquibaseAction(@NotNull IChangelogProvider pChangelogProvider)
  {
    changelogProvider = pChangelogProvider;
  }

  @Override
  protected final void performAction(Node[] pNodes)
  {
    RequestProcessor.getDefault().post(() -> {
      try
      {
        // First save all
        LifecycleManager.getDefault().saveAll();

        // then execute
        performAction0(pNodes == null ? new Node[0] : pNodes);
      }
      catch (CancellationException cancelled)
      {
        //nothing
      }
      catch (LiquibaseException | IOException e)
      {
        INotificationFacade.INSTANCE.error(e);
      }
    });
  }

  @Override
  protected boolean enable0(@NotNull Node[] pNodes)
  {
    // disabled for now, as this can take a very long time due to sequential processing and a possible timeout of 30s per connection
    //boolean connectionOK = connectionProvider.hasConnectionsAvailable();
    boolean changelogOK = !changelogAware() || changelogProvider.hasChangelogsAvailable();
    //return connectionOK && changelogOK;
    return changelogOK;
  }

  @Override
  public final boolean isEnabled()
  {
    // do not cache this value, because connections can be
    // changed without chaning the relevant nodes
    putProperty(PROP_ENABLED, null);
    return super.isEnabled();
  }

  @Override
  public HelpCtx getHelpCtx()
  {
    return null;
  }

  @Override
  protected boolean asynchronous()
  {
    return true;
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
   * @throws IOException           Exception that happens, if the connection could not be created
   */
  protected abstract void performAction0(@NotNull Node[] pActivatedNodes) throws CancellationException, LiquibaseException, IOException;

  /**
   * @return the connection provider for creating liquibase instances
   */
  @NotNull
  protected IConnectionProvider getConnectionProvider()
  {
    connectionProvider.reset();
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
