package de.adito.liquibase.internal.connection;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.database.IPossibleConnectionProvider.IPossibleDBConnection.IConnectionFunction;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Provides Database-Connections
 *
 * @author w.glanzer, 24.10.2018
 */
public interface IConnectionProvider
{

  /**
   * Executes something on the current connection.
   *
   * @param pFunction Function that consumes the connection
   * @throws IOException if no current connection exists
   * @throws Ex          if something inside pFunction failed
   */
  @SuppressWarnings("UnusedReturnValue")
  <T, Ex extends Throwable> T executeOnCurrentConnection(@NotNull IConnectionFunction<T, Ex> pFunction) throws IOException, Ex;

  /**
   * Returns true, if there are some connections available in general
   *
   * @return true, if available
   */
  boolean hasConnectionsAvailable();

  /**
   * This resets the chosen connection so that, e.g. the user has to choose another one
   */
  void reset();

}
