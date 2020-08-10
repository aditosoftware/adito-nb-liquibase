package de.adito.liquibase.internal.changelog;

import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Provides access to changelogs in the current context
 *
 * @author w.glanzer, 10.08.2020
 */
public interface IChangelogProvider
{

  /**
   * Returns the changelog that should be used to execute liquibase actions
   *
   * @return the changelog, or null if no changelog was found
   */
  @Nullable
  File findCurrentChangeLog();

  /**
   * Tries to extract the alias name, to which this changelog belongs to
   *
   * @return the name of the alias or null, if it cannot be read
   */
  @Nullable
  String findAliasName();

  /**
   * Returns true, if there are some changelogs available in general
   *
   * @return true, if available
   */
  boolean hasChangelogsAvailable();

}
