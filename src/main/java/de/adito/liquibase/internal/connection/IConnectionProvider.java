package de.adito.liquibase.internal.connection;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.Connection;
import java.util.*;
import java.util.function.Supplier;

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
  <T, Ex extends Throwable> T executeOnCurrentConnection(@NotNull Supplier<Set<String>> pGetContexts,
                                                         @NotNull IConnectionContextFunction<T, Ex> pFunction) throws IOException, Ex;

  /**
   * This resets the chosen connection so that, e.g. the user has to choose another one
   */
  void reset();

  interface IConnectionContextFunction<T, Ex extends Throwable>
  {
    /**
     * Functions that gets executed before the connection is opened and the liquibase instance created
     */
    default void before()
    {
    }

    T apply(@NotNull Connection pConnection, @NotNull List<String> pContexts) throws Ex;
  }

}
