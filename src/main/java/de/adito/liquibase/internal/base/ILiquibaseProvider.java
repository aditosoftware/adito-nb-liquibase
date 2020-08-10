package de.adito.liquibase.internal.base;

import de.adito.liquibase.internal.connection.IConnectionProvider;
import liquibase.Liquibase;
import liquibase.exception.LiquibaseException;
import org.jetbrains.annotations.*;

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
   * @return the provider
   */
  @NotNull
  static ILiquibaseProvider getInstance(@NotNull IConnectionProvider pConnectionProvider)
  {
    return new LiquibaseProviderImpl(pConnectionProvider);
  }

  /**
   * Execute an Action on a single, valid Liquibase instance
   *
   * @param pChangeLogFile ChangeLog, if ressources have to be used
   * @param pExecutor      Function which provides access to liquibase
   */
  <Ex extends Exception> void executeOn(@Nullable File pChangeLogFile, @NotNull ILiquibaseConsumer<Ex> pExecutor) throws Ex, LiquibaseException;

  /**
   * Consumer, to get liquibase instance
   */
  interface ILiquibaseConsumer<Ex extends Throwable>
  {
    void accept(@NotNull Liquibase pLiquibase) throws Ex;
  }

}
