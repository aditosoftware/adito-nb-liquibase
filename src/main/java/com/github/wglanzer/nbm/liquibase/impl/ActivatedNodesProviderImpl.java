package com.github.wglanzer.nbm.liquibase.impl;

import com.github.wglanzer.nbm.liquibase.internal.*;
import com.google.inject.Singleton;
import org.jetbrains.annotations.*;
import org.netbeans.api.db.explorer.*;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;

import java.sql.Connection;
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
    DatabaseConnection con = ConnectionManager.getDefault().getConnection("LIQUIBASE-CON");
    if (con == null)
    {
      con = ConnectionManager.getDefault().getConnections()[0];
      //con = ConnectionManager.getDefault().showAddConnectionDialogFromEventThread(null);
    }

    if (con == null)
      return null;

    DatabaseConnection connection = con;
    return () -> {
      try
      {
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
      FileObject fileObject = node.getLookup().lookup(FileObject.class);
      if (fileObject != null &&
          (fileObject.getExt().equals("xml") || fileObject.getExt().equals("json")))
        return fileObject.getPath();
    }

    return null;
  }

}
