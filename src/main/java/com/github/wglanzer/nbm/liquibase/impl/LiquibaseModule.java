package com.github.wglanzer.nbm.liquibase.impl;

import com.github.wglanzer.nbm.liquibase.*;
import com.github.wglanzer.nbm.liquibase.internal.*;
import com.google.inject.AbstractModule;

/**
 * Guice: LiquibaseModule
 *
 * @author w.glanzer, 23.10.2018
 */
public class LiquibaseModule extends AbstractModule
{

  @Override
  protected void configure()
  {
    bind(IConnectionProvider.class).to(ActivatedNodesProviderImpl.class);
    bind(IChangelogProvider.class).to(ActivatedNodesProviderImpl.class);
    bind(IActionEnvironment.class).to(ActionEnvironmentImpl.class);
    bind(ILiquibaseFactory.class).to(LiquibaseFactoryImpl.class);
    bind(INotificationFacade.class).to(NotificationFacadeImpl.class);
  }

}
