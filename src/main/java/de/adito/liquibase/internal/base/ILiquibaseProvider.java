package de.adito.liquibase.internal.base;

import liquibase.Liquibase;
import liquibase.exception.LiquibaseException;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Execute Actions on a single, valid Liquibase instance
 *
 * @author w.glanzer, 25.10.2018
 */
public interface ILiquibaseProvider
{

  /**
   * Provides a new ILiquibaseProvider instance
   *
   * @param pConnectionProvider Provider for the current connection
   * @param pChangeLogFile      ChangeLog-File
   * @return the provider
   */
  @NotNull
  static ILiquibaseProvider getInstance(@NotNull IConnectionProvider pConnectionProvider, @NotNull File pChangeLogFile)
  {
    return new LiquibaseProviderImpl(pConnectionProvider, pChangeLogFile);
  }

  /**
   * Execute an Action on a single, valid Liquibase instance
   *
   * @param pExecutor Function which provides access to liquibase
   */
  <Ex extends Exception> void executeWith(@NotNull ILiquibaseConsumer<Ex> pExecutor) throws Ex, LiquibaseException;

  /**
   * Consumer, to get liquibase instance
   */
  interface ILiquibaseConsumer<Ex extends Throwable>
  {
    void accept(@NotNull Liquibase pLiquibase) throws Ex;
  }

}
