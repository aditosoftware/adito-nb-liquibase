package com.github.wglanzer.nbm.actions;

import com.github.wglanzer.nbm.liquibase.ILiquibaseProvider;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.liquibase.LiquiConstants;
import liquibase.exception.LiquibaseException;
import org.jetbrains.annotations.NotNull;
import org.openide.awt.*;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;
import org.openide.util.actions.SystemAction;
import org.openide.windows.TopComponent;

import java.awt.event.ActionEvent;
import java.util.*;

/**
 * Execute Command: "Drop All" and "Update"
 *
 * @author w.glanzer, 23.10.2018
 */
@NbBundle.Messages("CTL_DropAllAndUpdateAction=Drop All & Update...")
@ActionID(category = "Liquibase", id = "com.github.wglanzer.nbm.actions.DropAllAndUpdateAction")
@ActionRegistration(displayName = "#CTL_DropAllAndUpdateAction", lazy = false)
@ActionReferences({
    @ActionReference(path = LiquiConstants.ACTION_REFERENCE, position = 1100),
    //@ActionReference(path = "Toolbars/Liquibase", position = 0)
})
public class DropAllAndUpdateAction extends AbstractLiquibaseAction
{
  @Override
  public void execute(@NotNull ILiquibaseProvider pProvider) throws Exception
  {
    _DelegateProvider provider = new _DelegateProvider(pProvider);
    SystemAction.get(DropAllAction.class).execute(provider); //Execute DropAll
    SystemAction.get(UpdateAction.class).execute(provider); //Execute Update
    provider.execute();
  }

  @Override
  protected void performAction(Node[] activatedNodes)
  {
    perform();
  }

  @Override
  protected boolean enable(Node[] activatedNodes)
  {
    Node[] nodes = TopComponent.getRegistry().getActivatedNodes();

    boolean dropAllenabled = SystemAction.get(DropAllAction.class).enable(nodes);
    boolean updateEnabled = SystemAction.get(UpdateAction.class).enable(nodes);
    return dropAllenabled & updateEnabled;
  }

  @Override
  public String getName()
  {
    return Bundle.CTL_DropAllAndUpdateAction();
  }

  /**
   * Provider that tracks, if an action was executed
   */
  private static class _DelegateProvider implements ILiquibaseProvider
  {
    private ILiquibaseProvider delegate;
    private List<ILiquibaseConsumer<? extends Exception>> consumers = new ArrayList<>();

    public _DelegateProvider(ILiquibaseProvider pDelegate)
    {
      delegate = pDelegate;
    }

    @Override
    public <Ex extends Exception> void executeWith(@NotNull ILiquibaseConsumer<Ex> pExecutor) throws Ex, LiquibaseException
    {
      consumers.add(pExecutor);
    }

    public void execute() throws Exception
    {
      try
      {
        delegate.executeWith(pLiquibase -> {
          for (ILiquibaseConsumer<? extends Exception> consumer : consumers)
            consumer.accept(pLiquibase); // throws CancellationException, if cancelled
        });
      }
      finally
      {
        consumers.clear();
      }
    }
  }

}
