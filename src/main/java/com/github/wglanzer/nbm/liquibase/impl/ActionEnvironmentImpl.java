package com.github.wglanzer.nbm.liquibase.impl;

import com.github.wglanzer.nbm.liquibase.*;
import com.github.wglanzer.nbm.liquibase.internal.*;
import com.github.wglanzer.nbm.util.NetBeansObservables;
import com.google.inject.*;
import io.reactivex.Observable;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * ActionEnvironment, which is bound to NetBeans-activatedNodes
 *
 * @author w.glanzer, 24.10.2018
 */
@Singleton
class ActionEnvironmentImpl implements IActionEnvironment
{

  private final Observable<Optional<ILiquibaseProvider>> activeLiquibase;

  @Inject
  public ActionEnvironmentImpl(@NotNull IConnectionProvider pConnectionProvider, @NotNull ILiquibaseFactory pLiquibaseFactory,
                               @NotNull IChangelogProvider pChangelogProvider)
  {
    activeLiquibase = NetBeansObservables.activatedNodesObservable() // BehaviorSubject
        .map(pNodes -> {
          Supplier<Connection> connectionSupplier = pConnectionProvider.findConnectionInNodes(pNodes);
          String changeLogFile = pChangelogProvider.findChangeLogFile(pNodes);
          if(connectionSupplier == null || changeLogFile == null)
            return Optional.empty();
          return Optional.of(pLiquibaseFactory.create(connectionSupplier, changeLogFile));
        });
  }

  @NotNull
  @Override
  public Observable<Optional<ILiquibaseProvider>> activeLiquibase()
  {
    return activeLiquibase;
  }

}
