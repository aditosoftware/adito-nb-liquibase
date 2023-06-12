package de.adito.liquibase.internal.connection;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.database.*;
import de.adito.aditoweb.nbm.vaadinicons.IVaadinIconsProvider;
import de.adito.swing.icon.IconAttributes;
import info.clearthought.layout.TableLayout;
import io.reactivex.rxjava3.disposables.*;
import lombok.NonNull;
import org.openide.util.*;
import org.openide.windows.WindowManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

/**
 * Panel for a dialog to select a database connection
 *
 * @author w.glanzer, 05.08.2020
 */
class SelectConnectionDialogPanel extends JPanel implements Disposable, IConnectionLoaderStateListener
{
  private static final Color _ERROR_COLOR = UIManager.getColor("nb.errorForeground") == null ? Color.RED
      : UIManager.getColor("nb.errorForeground");
  private static final Color _WARNING_COLOR = UIManager.getColor("nb.warningForeground") == null ? Color.YELLOW
      : UIManager.getColor("nb.warningForeground");

  private final SelectConnectionDialogModel model;
  private final CompositeDisposable disposable = new CompositeDisposable();
  private JPanel contextsPanel;
  private JLabel messageLabel;
  private final JLabel loadingIconLabel = new JLabel();
  private ImageIcon loadingIcon;
  private ImageIcon warningIcon;
  private ImageIcon foundPreselectedIcon;
  private Icon foundPreselectedIconFallback;

  @NbBundle.Messages({"LoadingIconTooltip=Looking for further available connections",
                      "PreselectedConnectionFound=Found a connection that matches the preselection criteria in the newly loaded connections"})
  SelectConnectionDialogPanel(@NonNull SelectConnectionDialogModel pModel)
  {
    model = pModel;
    model.addConnectionStateListener(this);
    _initIcons();


    double fill = TableLayout.FILL;
    double pref = TableLayout.PREFERRED;
    double gap = 8;

    double[] cols = {gap, fill, gap, pref, gap};
    double[] rows = {gap,
                     pref,
                     4,
                     pref,
                     gap,
                     pref,
                     gap,
                     pref,
                     4,
                     pref,
                     4,
                     pref,
                     gap
    };

    setLayout(new TableLayout(cols, rows));
    add(_createMessageLabel(), "1,5");
    add(_createComboBox(), "1,1");
    add(_initLoadingIconLabel(), "3,1");
    add(_createCheckBox(), "1,3");
    add(_createContextLabel(), "1,7");
    add(_createContextsPanel(), "1,9");

    setPreferredSize(new Dimension(650, 130));
    setMinimumSize(getPreferredSize());
  }

  @Override
  public void dispose()
  {
    disposable.dispose();
  }

  @Override
  public boolean isDisposed()
  {
    return disposable.isDisposed();
  }

  /**
   * @return the new ComboBox to select a connection
   */
  @NonNull
  private JComponent _createComboBox()
  {
    JComboBox<Object> combo = new JComboBox<>(model);
    combo.addItemListener(e -> _updateMessageLabel());
    return combo;
  }

  @NbBundle.Messages("AvailableContexts=Available Contexts:")
  private JComponent _createContextLabel()
  {
    return new JLabel(Bundle.AvailableContexts());
  }

  private JComponent _createContextsPanel()
  {
    disposable.add(model.observeContexts().subscribe(contexts -> contexts.ifPresent(this::_showContexts)));
    contextsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    contextsPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
    return contextsPanel;
  }

  /**
   * Shows the contexts as Checkboxes. Also adds an default-Checkbox
   */
  @NbBundle.Messages("DefaultContext=default")
  private void _showContexts(Set<String> pContexts)
  {
    if (contextsPanel != null)
    {
      contextsPanel.removeAll();

      JCheckBox defaultChbx = new JCheckBox(Bundle.DefaultContext());
      defaultChbx.setEnabled(false);
      defaultChbx.setSelected(true);
      defaultChbx.setBorder(new EmptyBorder(0, -3, 0, 0));
      model.contextSelected("", true);

      contextsPanel.add(defaultChbx);

      pContexts.forEach(pContextName -> {
        JCheckBox chbx = new JCheckBox(pContextName);
        chbx.addActionListener(al -> model.contextSelected(pContextName, chbx.isSelected()));
        contextsPanel.add(chbx);
      });
    }

    WindowManager.getDefault().invokeWhenUIReady(() -> {
      revalidate();
      contextsPanel.repaint();
      repaint();
    });
  }

  /**
   * Sets the validator. The validator is called, if something changed.
   *
   * @param pValidator the validator
   */
  public void setValidator(@NonNull Consumer<Boolean> pValidator)
  {
    pValidator.accept(false);
    disposable.add(model.observeIsValid().subscribe(pBoolean -> pValidator.accept(pBoolean != null && pBoolean)));
  }

  /**
   * @return the checkbox to determine, if the user wants to show all connections
   */
  @NbBundle.Messages("ShowAllConnections=Show all connections")
  @NonNull
  private JCheckBox _createCheckBox()
  {
    JCheckBox cb = new JCheckBox(Bundle.ShowAllConnections());
    // align checkbox with the combobox and the messageLabel
    cb.setBorder(new EmptyBorder(0, -3, 0, 0));
    disposable.add(model.isShowAllConnections().subscribe(cb::setSelected));
    cb.addActionListener(e -> model.setShowAllConnections(cb.isSelected()));
    return cb;
  }

  /**
   * @return the message label to show more information, if for example an "external" connection was selected
   */
  @NonNull
  private JLabel _createMessageLabel()
  {
    messageLabel = new JLabel();
    return messageLabel;
  }


  private void _initIcons()
  {
    warningIcon = new ImageIcon(SelectConnectionDialogPanel.class.getResource("/de/adito/liquibase/actions/icons/sign_warning16.png"));
    loadingIcon = new ImageIcon(SelectConnectionDialogPanel.class.getResource("/de/adito/liquibase/actions/icons/loading16.gif"));
    foundPreselectedIconFallback = UIManager.getIcon("OptionPane.questionIcon");
    IVaadinIconsProvider vaadinIconsProvider = Lookup.getDefault().lookup(IVaadinIconsProvider.class);
    foundPreselectedIcon = null;
    if (vaadinIconsProvider != null)
    {
      Image vaadinIcon = vaadinIconsProvider.findImage(IVaadinIconsProvider.VaadinIcon.QUESTION,
                                                       new IconAttributes.Builder().setColor(Color.green).create());
      if (vaadinIcon != null)
        foundPreselectedIcon = new ImageIcon(vaadinIcon);
    }
  }

  private JLabel _initLoadingIconLabel()
  {
    loadingIconLabel.setIcon(loadingIcon);
    loadingIconLabel.setToolTipText(Bundle.LoadingIconTooltip());
    return loadingIconLabel;
  }

  /**
   * Updates the message label to the current model selection
   */
  @NbBundle.Messages({
      "PotentiallyRemote=The connection potentially points to an external database! ",
      "Remote=The connection points to an external database! ",
      "AuditActive=Audit is active, the audit layer is ignored by liquibase! ",
      "OfflineActive=Offline sync is active, manual adjusting in ASYS_AUDIT necessary! "
  })
  private void _updateMessageLabel()
  {
    SwingUtilities.invokeLater(() -> {
      messageLabel.setForeground(new JLabel().getForeground()); // reset to default

      // new message
      String message = "";
      IPossibleConnectionProvider.IPossibleDBConnection connection = model.getSelectedConnectionAndContexts().first();
      IJDBCURLTester tester = Lookup.getDefault().lookup(IJDBCURLTester.class);
      if (connection != null && tester != null)
      {
        IJDBCURLTester.EResult result = tester.test(connection.getURL());
        switch (result)
        {
          case POTENTIALLY_REMOTE:
            message = Bundle.PotentiallyRemote();
            break;
          case REMOTE:
            message = Bundle.Remote();
            break;
        }

        List<IPossibleConnectionProvider.IPossibleDBConnection.ITableMetaInfo> metaInfos = connection.getTableMetaInfos();
        boolean auditActive = metaInfos.stream().anyMatch(IPossibleConnectionProvider.IPossibleDBConnection.ITableMetaInfo::isAuditActive);
        boolean offlineActive = metaInfos.stream().anyMatch(IPossibleConnectionProvider.IPossibleDBConnection.ITableMetaInfo::isOfflineActive);

        if (auditActive)
        {
          message += Bundle.AuditActive();
          messageLabel.setForeground(_WARNING_COLOR);
        }

        if (offlineActive)
        {
          if (!message.isEmpty())
            message += "<br/>";
          message += Bundle.OfflineActive();
          messageLabel.setForeground(_ERROR_COLOR);
        }

        messageLabel.setText("<html>" + message + "</html>");
        if (message.equals(""))
          messageLabel.setIcon(null);
        else
          messageLabel.setIcon(warningIcon);
      }
    });
  }

  @Override
  public void stateChanged(ELoadingState pNewLoadingState)
  {
    if (pNewLoadingState == ELoadingState.LOADING)
      loadingIconLabel.setIcon(loadingIcon);
    else
    {

      loadingIconLabel.setIcon(null);
      if (pNewLoadingState == ELoadingState.FINISHED_FOUND_SELECTED)
      {
        messageLabel.setIcon(foundPreselectedIcon == null ? foundPreselectedIconFallback : foundPreselectedIcon);
        messageLabel.setText(Bundle.PreselectedConnectionFound());
      }
    }
  }
}