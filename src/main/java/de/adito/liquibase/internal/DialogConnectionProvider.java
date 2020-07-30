package de.adito.liquibase.internal;

import de.adito.liquibase.internal.base.IConnectionProvider;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;

/**
 * ConnectionProvider that asks the user to select a connection
 *
 * @author w.glanzer, 30.07.2020
 */
public class DialogConnectionProvider implements IConnectionProvider
{

  @Nullable
  @Override
  public Connection findCurrentConnection()
  {
    return null;
  }

}
