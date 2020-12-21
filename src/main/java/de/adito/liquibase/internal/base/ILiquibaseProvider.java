package de.adito.liquibase.internal.base;

import de.adito.liquibase.internal.changelog.IChangelogProvider;
import de.adito.liquibase.internal.connection.IConnectionProvider;
import liquibase.Contexts;
import liquibase.exception.LiquibaseException;
import org.jetbrains.annotations.*;

import java.io.IOException;

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
   * @param pChangeLogProvider ChangeLogProvider, if ressources have to be used
   * @param pExecutor          Function which provides access to liquibase
   */
  <Ex extends Exception> void executeOn(@Nullable IChangelogProvider pChangeLogProvider, @NotNull ILiquibaseConsumer<Ex> pExecutor) throws Ex, LiquibaseException, IOException;

  <Ex extends Exception> void executeOn(boolean pChangelogRequired, @Nullable IChangelogProvider pChangeLogProvider, @NotNull ILiquibaseConsumer<Ex> pExecutor) throws Ex, LiquibaseException, IOException;

  /**
   * Consumer, to get liquibase instance
   */
  interface ILiquibaseConsumer<Ex extends Throwable>
  {
    void accept(@NotNull AbstractADITOLiquibase pLiquibase, @NotNull Contexts pContexts) throws Ex;
  }

}
