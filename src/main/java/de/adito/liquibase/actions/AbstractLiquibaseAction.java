package de.adito.liquibase.actions;

import de.adito.liquibase.liquibase.*;
import de.adito.liquibase.liquibase.impl.*;
import de.adito.liquibase.liquibase.internal.ILiquibaseFactory;
import de.adito.liquibase.util.Util;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.liquibase.*;
import liquibase.exception.LiquibaseException;
import org.jetbrains.annotations.*;
import org.netbeans.api.progress.*;
import org.openide.awt.*;
import org.openide.nodes.Node;
import org.openide.util.*;
import org.openide.util.actions.*;
import org.openide.windows.TopComponent;

import javax.swing.*;
import java.awt.event.*;
import java.sql.Connection;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Abstract Action for those, which require an Liquibase Instance
 *
 * @author w.glanzer, 25.10.2018
 */
@NbBundle.Messages("LBL_ActionProgress=Executing Liquibase Action...")
public abstract class AbstractLiquibaseAction extends NodeAction
{

  @Override
  protected void performAction(Node[] activatedNodes)
  {
    ActivatedNodesProviderImpl impl = new ActivatedNodesProviderImpl();
    Supplier<Connection> connSup = impl.findConnectionInNodes(getActivatedNodes(activatedNodes));
    String changeLogFile = impl.findChangeLogFile(getActivatedNodes(activatedNodes));

    ILiquibaseFactory f = new LiquibaseFactoryImpl();
    ILiquibaseProvider provider = f.create(connSup, changeLogFile);

    RequestProcessor.getDefault().post(() -> {
      try
      {
        ILiquibaseProvider _provider = new _ProgressLiquibaseProvider(() -> provider);
        execute(_provider);
      }
      catch (CancellationException cancelled)
      {
        //nothing
      }
      catch (Exception e)
      {
        getNotificationFacade().error(e);
      }
    });
  }


  Node[] getActivatedNodes(@Nullable Node[] pActivatedNodes)
  {
    if ((pActivatedNodes != null) && (pActivatedNodes.length > 0))
      return pActivatedNodes;

    return TopComponent.getRegistry().getActivatedNodes();
  }

  @Override
  protected boolean enable(Node[] activatedNodes)
  {
    Node[] nodes = getActivatedNodes(activatedNodes);//TopComponent.getRegistry().getActivatedNodes();
    if (nodes.length == 1)
      return Util.existsChangelogFile(nodes[0]) && Util.containsConnection(nodes[0]);

    return false;
  }

  @Override
  public HelpCtx getHelpCtx()
  {
    return null;
  }

  /**
   * @return NotificationFacade to display notifications with a balloon
   */
  @NotNull
  final INotificationFacade getNotificationFacade()
  {
    return new NotificationFacadeImpl();
  }

  protected abstract void execute(@NotNull ILiquibaseProvider pLiquibase) throws Exception;

  /**
   * Folder for Liquibase-Actions in Project Menu
   */
  @NbBundle.Messages("CTL_LiquibaseAction=Liquibase")
  @ActionID(category = "Liquibase", id = "com.github.wglanzer.nbm.actions.AbstractLiquibaseAction.LiquibaseActionFolder")
  @ActionRegistration(displayName = "#CTL_LiquibaseAction", lazy = false)
  @ActionReference(path = LiquiConstants.ACTION_FOLDER, position = 1650)
  public static final class LiquibaseActionFolder extends AbstractAction implements ILiquibaseActionFolder, ActionListener, Presenter.Popup
  {

    @Override
    public void actionPerformed(ActionEvent e)
    {
      throw new RuntimeException("Not implemented");
    }

    @Override
    public JMenuItem getPopupPresenter()
    {
      JMenu main = new JMenu(Bundle.CTL_LiquibaseAction());
      Stream.of(Utilities.actionsToPopup(Utilities.actionsForPath(LiquiConstants.ACTION_REFERENCE)
                                             .toArray(new Action[0]), Lookup.EMPTY)
                    .getComponents())
          .forEach(main::add);
      return main;
    }

  }

  /**
   * LiquibaseProvider delegate to show progressbar
   */
  public static final class _ProgressLiquibaseProvider implements ILiquibaseProvider
  {
    private final Supplier<ILiquibaseProvider> delegate;

    _ProgressLiquibaseProvider(Supplier<ILiquibaseProvider> pDelegate)
    {
      delegate = pDelegate;
    }

    @Override
    public <Ex extends Exception> void executeWith(@NotNull ILiquibaseConsumer<Ex> pExecutor) throws Ex, LiquibaseException
    {
      ILiquibaseProvider provider = delegate.get();
      if (provider == null)
        return;

      ProgressHandle progressHandle = ProgressHandleFactory.createSystemHandle(Bundle.LBL_ActionProgress());
      progressHandle.start();

      try
      {
        progressHandle.switchToIndeterminate();
        provider.executeWith(pExecutor);
      }
      finally
      {
        progressHandle.finish();
      }
    }
  }

}
