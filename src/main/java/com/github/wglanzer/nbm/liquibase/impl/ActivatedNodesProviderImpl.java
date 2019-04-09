package com.github.wglanzer.nbm.liquibase.impl;

import com.github.wglanzer.nbm.actions.CreateFolderAction;
import com.github.wglanzer.nbm.liquibase.internal.*;
import com.github.wglanzer.nbm.util.SelectConnectionPanel;
import com.google.inject.Singleton;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.liquibase.LiquiConstants;
import info.clearthought.layout.TableLayout;
import org.jetbrains.annotations.*;
import org.netbeans.api.db.explorer.*;
import org.openide.*;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.*;
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
class ActivatedNodesProviderImpl implements IConnectionProvider, IChangelogProvider
{

  @Override
  public Supplier<Connection> findConnectionInNodes(Node[] pNodes)
  {
    return () -> {
      try
      {
        if (pNodes.length != 1)
          return null;

        DatabaseConnection connection = _startDialog(pNodes[0]);//SelectConnectionPanel.selectConnection(true);
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

  private DatabaseConnection _startDialog(Node pNode)
  {
    ConnectionPanel panel = new ConnectionPanel(pNode);
    DialogDescriptor desc = new DialogDescriptor(panel, NbBundle.getMessage(SelectConnectionPanel.class, "MSG_SelectConnection"));

    Dialog dialog = DialogDisplayer.getDefault().createDialog(desc);
    dialog.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(SelectConnectionPanel.class, "ACSD_SelectConnection"));
    dialog.setVisible(true);
    dialog.dispose();

    if (desc.getValue() == DialogDescriptor.OK_OPTION)
      return panel.getConnection();

    return null;
  }

  private class ConnectionPanel extends JPanel
  {
    private ConnectionModel model;

    ConnectionPanel(Node pNode)
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
                       };


      setLayout(new TableLayout(cols, rows));
      add(_createComboBox(pNode), "1,1");
      add(_createCheckBox(), "1,3");

      setPreferredSize(new Dimension(520, 120));
    }

    private JComponent _createComboBox(Node pNode)
    {
      model = new ConnectionModel(pNode);
      return new JComboBox(model);
    }

    private JCheckBox _createCheckBox()
    {
      
      JCheckBox cb = new JCheckBox(NbBundle.getMessage(CreateFolderAction.class, "ShowAllConnections"));
      cb.setSelected(false);
      model.showAllConnections(cb.isSelected());

      cb.addActionListener(e -> model.showAllConnections(cb.isSelected()));

      return cb;
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
        private final String unknown = NbBundle.getMessage(CreateFolderAction.class, "UnknownOwner");
        final String name;
        final DatabaseConnection connection;

        Row(String pName, DatabaseConnection pConnection)
        {
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
