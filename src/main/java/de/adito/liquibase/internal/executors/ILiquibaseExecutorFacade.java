package de.adito.liquibase.internal.executors;

import de.adito.liquibase.internal.changelog.IChangelogProvider;
import de.adito.liquibase.internal.connection.IConnectionProvider;
import de.adito.liquibase.nb.LiquibaseFolderService;
import liquibase.exception.LiquibaseException;
import org.jetbrains.annotations.*;

import java.io.IOException;
import java.sql.Connection;

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
  void executeDropAll(@NotNull IConnectionProvider pConnectionProvider) throws LiquibaseException, IOException;

  /**
   * Executes an UPDATE command, with potential user interaction
   *
   * @param pConnectionProvider Provider for the connection
   * @param pChangeLogProvider  Provider for the changelogs to apply
   */
  void executeUpdate(@NotNull IConnectionProvider pConnectionProvider, @NotNull IChangelogProvider pChangeLogProvider) throws LiquibaseException, IOException;

  /**
   * Executes an DROP ALL and UPDATE command, with potential user interaction
   *
   * @param pConnectionProvider Provider for the connection
   * @param pChangeLogProvider  Provider for the changelogs to apply
   */
  void executeDropAllAndUpdate(@NotNull IConnectionProvider pConnectionProvider, @NotNull IChangelogProvider pChangeLogProvider) throws LiquibaseException, IOException;

  /**
   * Executes an FUTURE ROLLBACK SQL command, with potential user interaction
   *
   * @param pConnectionProvider Provider for the connection
   * @param pChangeLogProvider  Provider for the changelogs to apply
   */
  void executeFutureRollbackSQL(@NotNull IConnectionProvider pConnectionProvider, @NotNull IChangelogProvider pChangeLogProvider) throws LiquibaseException, IOException;

  /**
   * Executes an UPDATE SQL command, with potential user interaction
   *
   * @param pConnectionProvider Provider for the connection
   * @param pChangeLogProvider  Provider for the changelogs to apply
   */
  void executeUpdateSQL(@NotNull IConnectionProvider pConnectionProvider, @NotNull IChangelogProvider pChangeLogProvider) throws LiquibaseException, IOException;

  /**
   * Executes an UPDATE SQL and an FUTURE ROLLBACK SQL command, with potential user interaction
   *
   * @param pConnectionProvider Provider for the connection
   * @param pChangeLogProvider  Provider for the changelogs to apply
   */
  void executeUpdateAndRollbackSQL(@NotNull IConnectionProvider pConnectionProvider, @NotNull IChangelogProvider pChangeLogProvider) throws LiquibaseException, IOException;

  /**
   * Executes an GENERATE CHANGELOG command, with potential user interaction
   *
   * @param pConnectionProvider Provider for the connection
   * @param pTableName          Name of the table, which create-changelog should be created
   * @param pSubfolderName      Name of the subfolder of {@link LiquibaseFolderService#observeLiquibaseFolder()}, where the changelog should be stored
   */
  void executeGenerateChangelog(@NotNull Connection pConnectionProvider, @NotNull String pTableName, @Nullable String pSubfolderName) throws LiquibaseException, IOException;
}
