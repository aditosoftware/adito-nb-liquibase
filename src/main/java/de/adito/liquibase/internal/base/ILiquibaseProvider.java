package de.adito.liquibase.internal.base;

import de.adito.liquibase.internal.changelog.IChangelogProvider;
import de.adito.liquibase.internal.connection.IConnectionProvider;
import liquibase.Contexts;
import liquibase.exception.LiquibaseException;
import lombok.NonNull;
import org.jetbrains.annotations.*;

import java.io.IOException;
import java.sql.Connection;

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
  @NonNull
  static ILiquibaseProvider getInstance(@NonNull IConnectionProvider pConnectionProvider)
  {
    return new LiquibaseProviderImpl(pConnectionProvider);
  }

  /**
   * Provides a new ILiquibaseProvider instance
   *
   * @param pConnection Current connection
   * @return the provider
   */
  @NonNull
  static ILiquibaseProvider getInstance(@NonNull Connection pConnection)
  {
    return new LiquibaseProviderImpl(pConnection);
  }

  /**
   * Execute an Action on a single, valid Liquibase instance
   *
   * @param pChangeLogProvider ChangeLogProvider, if ressources have to be used
   * @param pExecutor          Function which provides access to liquibase
   */
  <Ex extends Exception> void executeOn(@Nullable IChangelogProvider pChangeLogProvider, @NonNull ILiquibaseConsumer<Ex> pExecutor) throws Ex, LiquibaseException, IOException;

  <Ex extends Exception> void executeOn(boolean pChangelogRequired, @Nullable IChangelogProvider pChangeLogProvider, @NonNull ILiquibaseConsumer<Ex> pExecutor) throws Ex, LiquibaseException, IOException;

  /**
   * Consumer, to get liquibase instance
   */
  interface ILiquibaseConsumer<Ex extends Throwable>
  {
    /**
     * Functions that gets executed before the connection is opened and the liquibase instance created
     */
    default void before()
    {
    }

    void accept(@NonNull AbstractADITOLiquibase pLiquibase, @NonNull Contexts pContexts) throws Ex;
  }

}
