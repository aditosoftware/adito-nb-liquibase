package com.github.wglanzer.nbm.liquibase.impl;

import com.github.wglanzer.nbm.liquibase.INotificationFacade;
import com.google.common.base.Strings;
import com.google.inject.Singleton;
import org.jetbrains.annotations.*;
import org.openide.awt.*;
import org.openide.util.Exceptions;

import javax.swing.*;
import java.awt.event.ActionListener;

/**
 * @author w.glanzer, 31.10.2018
 */
@Singleton
public class NotificationFacadeImpl implements INotificationFacade
{


  @Override
  public void notify(String pTitle, String pMessage, boolean pAutoDispose, @Nullable ActionListener pListener)
  {
    _notify(pTitle, pMessage, pAutoDispose, NotificationDisplayer.Priority.NORMAL, pListener);
  }

  @Override
  public void error(@NotNull Throwable pThrowable)
  {
    _notify(pThrowable.getClass().getSimpleName(), _getRootMessage(pThrowable), false, NotificationDisplayer.Priority.HIGH, null);
    Exceptions.printStackTrace(pThrowable);
  }

  private void _notify(String pTitle, String pMessage, boolean pAutoDispose, @NotNull NotificationDisplayer.Priority pPriority,
                       @Nullable ActionListener pActionListener)
  {
    Icon icon = pPriority.getIcon();
    Notification n = NotificationDisplayer.getDefault().notify(Strings.nullToEmpty(pTitle), icon,
                                                               Strings.nullToEmpty(pMessage), pActionListener);
    if (pAutoDispose)
    {
      Timer timer = new Timer(6500, e -> n.clear());
      timer.setRepeats(false);
      timer.start();
    }
  }

  private String _getRootMessage(Throwable pThrowable)
  {
    Throwable cause = pThrowable.getCause();
    if (cause != null)
      return _getRootMessage(cause);
    String msg = pThrowable.getLocalizedMessage();
    if (msg == null)
      msg = pThrowable.getMessage();
    if (msg == null)
      msg = "exception";
    return msg;
  }

}
