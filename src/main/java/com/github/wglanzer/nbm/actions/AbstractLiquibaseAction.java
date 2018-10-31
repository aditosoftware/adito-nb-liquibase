package com.github.wglanzer.nbm.actions;

import com.github.wglanzer.nbm.ILiquibaseConstants;
import com.github.wglanzer.nbm.liquibase.*;
import io.reactivex.disposables.Disposable;
import liquibase.exception.LiquibaseException;
import org.jetbrains.annotations.NotNull;
import org.netbeans.api.progress.*;
import org.openide.awt.*;
import org.openide.util.*;
import org.openide.util.actions.*;

import javax.swing.*;
import java.awt.event.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Abstract Action for those, which require an Liquibase Instance
 *
 * @author w.glanzer, 25.10.2018
 */
@NbBundle.Messages("LBL_ActionProgress=Executing Liquibase Action...")
public abstract class AbstractLiquibaseAction extends SystemAction
{

  private static final IActionEnvironment _ENVIRONMENT = ILiquibaseConstants.INJECTOR.getInstance(IActionEnvironment.class);
  private static final INotificationFacade _NOTIFICATION_FACADE = ILiquibaseConstants.INJECTOR.getInstance(INotificationFacade.class);
  private final Disposable enabledDisposable;
  private final AtomicReference<ILiquibaseProvider> liquibaseProvider = new AtomicReference<>();

  AbstractLiquibaseAction()
  {
    enabledDisposable = _ENVIRONMENT.activeLiquibase()
        .subscribe(pL -> {
          setEnabled(pL.isPresent());
          liquibaseProvider.set(pL.orElse(null));
        });
  }

  @Override
  public void actionPerformed(ActionEvent ev)
  {
    RequestProcessor.getDefault().post(() -> {
      try
      {
        ILiquibaseProvider provider = new _ProgressLiquibaseProvider(liquibaseProvider::get);
        actionPerformed(provider);
      }
      catch(CancellationException cancelled)
      {
        //nothing
      }
      catch (Exception e)
      {
        getNotificationFacade().error(e);
      }
    });
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
  protected final INotificationFacade getNotificationFacade()
  {
    return _NOTIFICATION_FACADE;
  }

  protected abstract void actionPerformed(@NotNull ILiquibaseProvider pLiquibase) throws Exception;

  /**
   * Folder for Liquibase-Actions in Project Menu
   */
  @NbBundle.Messages("CTL_LiquibaseAction=Liquibase")
  @ActionID(category = "Liquibase", id = "com.github.wglanzer.nbm.actions.AbstractLiquibaseAction.LiquibaseActionFolder")
  @ActionRegistration(displayName = "#CTL_LiquibaseAction", lazy = false)
  @ActionReference(path = "Loaders/text/xml-mime/Actions", position = 1650)
  public static final class LiquibaseActionFolder extends AbstractAction implements ActionListener, Presenter.Popup
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
      Stream.of(Utilities.actionsToPopup(Utilities.actionsForPath("Actions/Project/Liquibase/XML").toArray(new Action[0]), Lookup.EMPTY)
          .getComponents())
          .forEach(main::add);
      return main;
    }

  }

  /**
   * LiquibaseProvider delegate to show progressbar
   */
  private static final class _ProgressLiquibaseProvider implements ILiquibaseProvider
  {
    private final Supplier<ILiquibaseProvider> delegate;

    public _ProgressLiquibaseProvider(Supplier<ILiquibaseProvider> pDelegate)
    {
      delegate = pDelegate;
    }

    @Override
    public <Ex extends Exception> void executeWith(@NotNull ILiquibaseConsumer<Ex> pExecutor) throws Ex, LiquibaseException
    {
      ILiquibaseProvider provider = delegate.get();
      if(provider == null)
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
