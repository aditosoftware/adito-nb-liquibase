package de.adito.liquibase.internal.executors;

import de.adito.liquibase.internal.changelog.IChangelogProvider;
import de.adito.liquibase.internal.connection.IConnectionProvider;
import liquibase.exception.LiquibaseException;
import org.jetbrains.annotations.NotNull;

/**
 * Provides easy access to all "complex" actions, like "drop all", "update", etc.
 *
 * @author w.glanzer, 30.07.2020
 */
public interface ILiquibaseExecutorFacade
{

  ILiquibaseExecutorFacade INSTANCE = new LiquibaseExecutorFacadeImpl();

  /**
   * Executes an DROP ALL command, with potential user interaction
   *
   * @param pConnectionProvider Provider for the connection
   */
  void executeDropAll(@NotNull IConnectionProvider pConnectionProvider) throws LiquibaseException;

  /**
   * Executes an UPDATE command, with potential user interaction
   *
   * @param pConnectionProvider Provider for the connection
   * @param pChangeLogProvider  Provider for the changelogs to apply
   */
  void executeUpdate(@NotNull IConnectionProvider pConnectionProvider, @NotNull IChangelogProvider pChangeLogProvider) throws LiquibaseException;

}
