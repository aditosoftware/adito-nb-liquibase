package com.github.wglanzer.nbm;

import com.github.wglanzer.nbm.liquibase.impl.LiquibaseModule;
import com.google.inject.*;

/**
 * Constants for general Liquibase Integration
 *
 * @author w.glanzer, 27.10.2018
 */
public interface ILiquibaseConstants
{

  Injector INJECTOR = Guice.createInjector(new LiquibaseModule());

}
