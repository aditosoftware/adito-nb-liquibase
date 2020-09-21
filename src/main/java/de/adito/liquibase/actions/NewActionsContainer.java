package de.adito.liquibase.actions;

import org.openide.awt.*;
import org.openide.util.*;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Container to display a "New"-Menu
 *
 * @author w.glanzer, 17.09.2020
 */
@NbBundle.Messages("CTL_NewActionsContainer=New")
@ActionID(category = "AliasDefinition", id = "de.adito.liquibase.actions.NewActionsContainer")
@ActionRegistration(displayName = "#CTL_NewActionsContainer", lazy = false)
@ActionReference(path = "de/adito/aod/action/aliasDefinition", position = 0)
public class NewActionsContainer extends AbstractAction implements Presenter.Popup
{

  @Override
  public void actionPerformed(ActionEvent e)
  {
    throw new RuntimeException("A container action can not be performed");
  }

  @Override
  public JMenuItem getPopupPresenter()
  {
    JMenu main = new JMenu(NbBundle.getMessage(NewActionsContainer.class, "CTL_NewActionsContainer"));
    main.setVisible(false); // nur anzeigen, wenn ein Item drin ist

    Utilities.actionsForPath("de/adito/aod/action/aliasDefinition/new").forEach(pAction -> {
      if (pAction != null)
      {
        main.add(pAction);
        main.setVisible(true); // Anzeigen
      }
      else
        main.addSeparator();
    });

    return main;
  }

}
