package com.github.wglanzer.nbm.liquibase;

import org.jetbrains.annotations.*;

/**
 * Facade to display Notifications as a balloon
 *
 * @author w.glanzer, 31.10.2018
 */
public interface INotificationFacade
{

  /**
   * Shows a simple balloon to display a pMessage to the user.
   *
   * @param pTitle       Title
   * @param pMessage     Message
   * @param pAutoDispose <tt>true</tt> if the balloon should dispose automatically after a couple of seconds
   */
  void notify(@Nullable String pTitle, @Nullable String pMessage, boolean pAutoDispose);

  /**
   * Displays an error as a balloon
   *
   * @param pThrowable Exception
   */
  void error(@NotNull Throwable pThrowable);

}
