package com.github.wglanzer.nbm.liquibase.internal;

import org.jetbrains.annotations.*;
import org.openide.nodes.Node;

/**
 * Provides access to the current "activated"/"selected" Changelog-File
 *
 * @author w.glanzer, 28.10.2018
 */
public interface IChangelogProvider
{

  /**
   * Returns the current changelog-file or <tt>null</tt>
   *
   * @param pNodes currently selected nodes
   * @return the absolute path to the file
   */
  @Nullable
  String findChangeLogFile(@NotNull Node[] pNodes);

}
