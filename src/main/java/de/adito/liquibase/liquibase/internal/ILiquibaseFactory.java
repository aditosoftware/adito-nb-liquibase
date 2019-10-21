package de.adito.liquibase.liquibase.internal;

import de.adito.liquibase.liquibase.ILiquibaseProvider;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.util.function.Supplier;

/**
 * Creates Liquibase Instances
 *
 * @author w.glanzer, 24.10.2018
 */
public interface ILiquibaseFactory
{

  /**
   * Creates a new Liquibase instance, based on the connection and a changelogfile
   *
   * @param pConnectionSupplier Supplier for a connection (java.sql)
   * @param pChangeLogFile      Absolute Path to a Changelog
   * @return LiquibaseProvider to provide access to liquibase-object
   */
  @NotNull
  ILiquibaseProvider create(@NotNull Supplier<Connection> pConnectionSupplier, @NotNull String pChangeLogFile);

}
