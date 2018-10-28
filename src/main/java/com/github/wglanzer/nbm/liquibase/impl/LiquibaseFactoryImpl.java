package com.github.wglanzer.nbm.liquibase.impl;

import com.github.wglanzer.nbm.liquibase.ILiquibaseProvider;
import com.github.wglanzer.nbm.liquibase.internal.ILiquibaseFactory;
import com.google.inject.Singleton;
import liquibase.Liquibase;
import liquibase.database.*;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.util.function.Supplier;

/**
 * @author w.glanzer, 24.10.2018
 */
@Singleton
class LiquibaseFactoryImpl implements ILiquibaseFactory
{

  @NotNull
  @Override
  public ILiquibaseProvider create(@NotNull Supplier<Connection> pConnectionSupplier, @NotNull String pChangeLogFile)
  {
    return new _Provider(pConnectionSupplier, pChangeLogFile);
  }

  private static class _Provider implements ILiquibaseProvider
  {
    private final Supplier<Connection> connectionSupplier;
    private final String changeLogFile;

    public _Provider(Supplier<Connection> pConnectionSupplier, String pChangeLogFile)
    {
      connectionSupplier = pConnectionSupplier;
      changeLogFile = pChangeLogFile;
    }

    @Override
    public <Ex extends Exception> void executeWith(@NotNull ILiquibaseProvider.ILiquibaseConsumer<Ex> pExecutor) throws Ex, LiquibaseException
    {
      JdbcConnection con = null;
      try
      {
        con = new JdbcConnection(connectionSupplier.get());
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(con);
        Liquibase base = new Liquibase(changeLogFile, new FileSystemResourceAccessor(), database);
        base.validate();
        pExecutor.accept(base);
      }
      finally
      {
        if(con != null)
          con.close();
      }
    }
  }

}
