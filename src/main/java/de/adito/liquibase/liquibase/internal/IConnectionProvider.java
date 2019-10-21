package de.adito.liquibase.liquibase.internal;

import org.openide.nodes.Node;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.util.function.Supplier;

/**
 * Provides Database-Connections
 *
 * @author w.glanzer, 24.10.2018
 */
public interface IConnectionProvider
{

  /**
   * Returns an ConnectionSupplier, or <tt>null</tt> when no connection
   * can be created with the current nodes
   *
   * @param pNodes Nodes
   * @return an Supplier, or <tt>null</tt>
   */
  @Nullable
  Supplier<Connection> findConnectionInNodes(Node[] pNodes);

}
