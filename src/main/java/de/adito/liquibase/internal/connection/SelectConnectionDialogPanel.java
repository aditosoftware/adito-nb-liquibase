package de.adito.liquibase.internal.connection;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.database.*;
import info.clearthought.layout.TableLayout;
import io.reactivex.rxjava3.disposables.*;
import org.jetbrains.annotations.NotNull;
import org.openide.util.*;

import javax.swing.*;
import java.awt.*;

/**
 * Panel for a dialog to select a database connection
 *
 * @author w.glanzer, 05.08.2020
 */
class SelectConnectionDialogPanel extends JPanel implements Disposable
{
  private final SelectConnectionDialogModel model;
  private final CompositeDisposable disposable = new CompositeDisposable();
  private JLabel messageLabel;

  SelectConnectionDialogPanel(@NotNull SelectConnectionDialogModel pModel)
  {
    model = pModel;

    double fill = TableLayout.FILL;
    double pref = TableLayout.PREFERRED;
    double gap = 8;

    double[] cols = {gap, fill, gap};
    double[] rows = {gap,
                     pref,
                     4,
                     pref,
                     gap,
                     pref,
                     gap
    };


    setLayout(new TableLayout(cols, rows));
    add(_createComboBox(), "1,1");
    add(_createMessageLabel(), "1,5");
    add(_createCheckBox(), "1,3");

    setPreferredSize(new Dimension(550, 80));
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
  @NotNull
  private JComponent _createComboBox()
  {
    JComboBox<Object> combo = new JComboBox<>(model);
    combo.addItemListener(e -> _updateMessageLabel());
    return combo;
  }

  /**
   * @return the checkbox to determine, if the user wants to show all connections
   */
  @NbBundle.Messages("ShowAllConnections=Show all connections")
  @NotNull
  private JCheckBox _createCheckBox()
  {
    JCheckBox cb = new JCheckBox(Bundle.ShowAllConnections());
    disposable.add(model.isShowAllConnections().subscribe(cb::setSelected));
    cb.addActionListener(e -> model.setShowAllConnections(cb.isSelected()));
    return cb;
  }

  /**
   * @return the message label to show more information, if for example an "external" connection was selected
   */
  @NotNull
  private JLabel _createMessageLabel()
  {
    messageLabel = new JLabel();
    messageLabel.setFont(messageLabel.getFont().deriveFont(Font.BOLD));
    messageLabel.setForeground(Color.YELLOW);
    return messageLabel;
  }

  /**
   * Updates the message label to the current model selection
   */
  @NbBundle.Messages({
      "PotentiallyRemote=The connection points potentially to an external database!",
      "Remote=The connection points to an external database!"
  })
  private void _updateMessageLabel()
  {
    // clear
    messageLabel.setText("");

    // new message
    IPossibleConnectionProvider.IPossibleDBConnection connection = model.getSelectedConnection();
    IJDBCURLTester tester = Lookup.getDefault().lookup(IJDBCURLTester.class);
    if (connection != null && tester != null)
    {
      IJDBCURLTester.EResult result = tester.test(connection.getURL());
      switch (result)
      {
        case POTENTIALLY_REMOTE:
          messageLabel.setText(Bundle.PotentiallyRemote());
          break;
        case REMOTE:
          messageLabel.setText(Bundle.Remote());
          break;
      }
    }
  }
}