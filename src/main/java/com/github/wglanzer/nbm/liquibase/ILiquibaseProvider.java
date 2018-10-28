package com.github.wglanzer.nbm.liquibase;

import liquibase.Liquibase;
import liquibase.exception.LiquibaseException;
import org.jetbrains.annotations.NotNull;

/**
 * Execute Actions on a single, valid Liquibase instance
 *
 * @author w.glanzer, 25.10.2018
 */
@FunctionalInterface
public interface ILiquibaseProvider
{

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
