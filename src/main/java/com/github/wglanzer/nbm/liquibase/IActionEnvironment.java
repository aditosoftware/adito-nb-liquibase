package com.github.wglanzer.nbm.liquibase;

import io.reactivex.Observable;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Provides a global access to currently "selected"/"activated" Liquibase-Instance
 *
 * @author w.glanzer, 25.10.2018
 */
public interface IActionEnvironment
{

  /**
   * @return Observable, which contains a LiquibaseProvider to get Liquibase Access
   */
  @NotNull
  Observable<Optional<ILiquibaseProvider>> activeLiquibase();

}
