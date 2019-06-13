package com.github.wglanzer.nbm.liquibase.impl;

import com.github.wglanzer.nbm.actions.CreateFolderAction;
import com.github.wglanzer.nbm.liquibase.internal.*;
import com.google.inject.Singleton;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.liquibase.*;
import info.clearthought.layout.TableLayout;
import org.jetbrains.annotations.*;
import org.netbeans.api.db.explorer.*;
import org.openide.*;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.sql.Connection;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;

/**
 * Implements all "utility"-methods based on the currently selected nodes
 *
 * @author w.glanzer, 24.10.2018
 */
@Singleton
public class ActivatedNodesProviderImpl implements IConnectionProvider, IChangelogProvider
{

  @Override
  public Supplier<Connection> findConnectionInNodes(Node[] pNodes)
  {
    return () -> {
      try
      {
        if (pNodes.length != 1)
          return null;

        DatabaseConnection connection = _startDialog(pNodes[0]);
        if (connection == null)
          return null;
        ConnectionManager.getDefault().connect(connection);
        return connection.getJDBCConnection();
      }
      catch (DatabaseException e)
      {
        throw new RuntimeException("Failed to connect with DB", e);
      }
    };
  }

  @Nullable
  @Override
  public String findChangeLogFile(@NotNull Node[] pNodes)
  {
    for (Node node : pNodes)
    {
      String path = _findFileObject(node);
      if (path != null)
        return path;

      for (Node child : node.getChildren().getNodes())
      {
        path = _findFileObject(child);
        if (path != null)
          return path;
      }
    }

    return null;
  }

  private String _findFileObject(Node pNode)
  {
    FileObject fileObject = pNode.getLookup().lookup(FileObject.class);
    if (fileObject != null && (fileObject.getNameExt().equals(LiquiConstants.CHANGELOG_FILE)))
      return fileObject.getPath();

    return null;
  }

  /**
   * Opens a dialog for selecting a database connection.
   *
   * @param pNode contains a database connection.
   * @return the selected connection or null.
   */
  private DatabaseConnection _startDialog(Node pNode)
  {
    _ConnectionPanel panel = new _ConnectionPanel(pNode);
    DialogDescriptor desc = new DialogDescriptor(panel, NbBundle.getMessage(ActivatedNodesProviderImpl.class, "MSG_SelectConnection"));

    Dialog dialog = DialogDisplayer.getDefault().createDialog(desc);
    dialog.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(ActivatedNodesProviderImpl.class, "ACSD_SelectConnection"));
    dialog.setVisible(true);
    dialog.dispose();

    if (desc.getValue() == DialogDescriptor.OK_OPTION)
      return panel.getConnection();

    return null;
  }

  private class _ConnectionPanel extends JPanel
  {
    private ConnectionModel model;
    private JLabel messageLabel;

    _ConnectionPanel(Node pNode)
    {
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
      add(_createComboBox(pNode), "1,1");
      add(_createMessageLabel(), "1,5");
      add(_createCheckBox(), "1,3");

      setPreferredSize(new Dimension(520, 120));
    }

    private JComponent _createComboBox(Node pNode)
    {
      IJDBCURLTester tester = Lookup.getDefault().lookup(IJDBCURLTester.class);
      model = new ConnectionModel(pNode);
      JComboBox combo = new JComboBox(model);
      combo.addItemListener(e -> {
        messageLabel.setText(" ");
        if (e.getStateChange() == ItemEvent.SELECTED)
        {
          DatabaseConnection connection = model.getConnection();
          if ((connection != null) & (tester != null))
          {
            IJDBCURLTester.EResult result = tester.test(connection.getDatabaseURL());
            switch (result)
            {
              case POTENTIALLY_REMOTE:
                messageLabel.setText(NbBundle.getMessage(ActivatedNodesProviderImpl.class, "PotentiallyRemote"));
                break;
              case REMOTE:
                messageLabel.setText(NbBundle.getMessage(ActivatedNodesProviderImpl.class, "Remote"));
                break;
            }
          }
        }
      });
      return combo;
    }

    private JCheckBox _createCheckBox()
    {
      JCheckBox cb = new JCheckBox(NbBundle.getMessage(CreateFolderAction.class, "ShowAllConnections"));
      cb.setSelected(false);
      model.showAllConnections(cb.isSelected());

      cb.addActionListener(e -> model.showAllConnections(cb.isSelected()));

      return cb;
    }

    private JLabel _createMessageLabel()
    {
      messageLabel = new JLabel();
      messageLabel.setFont(messageLabel.getFont().deriveFont(Font.BOLD));
      messageLabel.setForeground(Color.YELLOW);
      return messageLabel;
    }

    DatabaseConnection getConnection()
    {
      return model.getConnection();
    }

    private class ConnectionModel extends DefaultComboBoxModel<ConnectionModel.Row>
    {
      private List<Row> data = new ArrayList<>();
      private List<Row> namedConnections = new ArrayList<>();
      private List<Row> unnamedConnections = new ArrayList<>();

      ConnectionModel(Node pNode)
      {
        String name = pNode.getDisplayName();
        for (DatabaseConnection c : pNode.getLookup().lookupAll(DatabaseConnection.class))
        {
          namedConnections.add(new Row(name, c));
        }

        for (DatabaseConnection c : ConnectionManager.getDefault().getConnections())
        {
          unnamedConnections.add(new Row(null, c));
        }
      }

      void showAllConnections(boolean pShow)
      {
        data.clear();
        setSelectedItem(null);

        data.addAll(namedConnections);
        if (pShow)
          data.addAll(unnamedConnections);

        if (data.size() > 0)
          setSelectedItem(data.get(0));

        fireContentsChanged(this, 0, data.size());
      }

      @Override
      public int getSize()
      {
        return data.size();
      }

      @Override
      public Row getElementAt(int index)
      {
        return data.get(index);
      }

      DatabaseConnection getConnection()
      {
        Row row = (Row) getSelectedItem();
        if (row != null)
          return row.connection;

        return null;
      }

      private class Row
      {
        final String name;
        final DatabaseConnection connection;

        Row(String pName, DatabaseConnection pConnection)
        {
          String unknown = NbBundle.getMessage(CreateFolderAction.class, "UnknownOwner");
          name = (pName != null) ? pName : unknown;
          connection = pConnection;
        }

        @Override
        public String toString()
        {
          return "  [" + name + "]    " + connection.getDisplayName();
        }
      }
    }
  }
}
