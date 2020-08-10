package de.adito.liquibase.internal.base;

import org.jetbrains.annotations.Nullable;

import java.sql.Connection;

/**
 * Provides Database-Connections
 *
 * @author w.glanzer, 24.10.2018
 */
public interface IConnectionProvider
{

  /**
   * Returns the connection that should be used to execute liquibase actions
   *
   * @return the connection
   */
  @Nullable
  Connection findCurrentConnection();

  /**
   * Returns true, if there are some connections available in general
   *
   * @return true, if available
   */
  boolean hasConnectionsAvailable();

}
