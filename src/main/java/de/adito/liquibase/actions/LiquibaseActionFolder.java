package de.adito.liquibase.actions;

import org.openide.awt.*;
import org.openide.util.*;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Folder for Liquibase-Actions in Project Menu
 *
 * @author w.glanzer, 31.07.2020
 */
@NbBundle.Messages("CTL_LiquibaseAction=Liquibase")
@ActionID(category = "Liquibase", id = "de.adito.liquibase.actions.LiquibaseActionFolder")
@ActionRegistration(displayName = "#CTL_LiquibaseAction", lazy = false)
@ActionReference(path = "de/adito/aod/action/aliasDefinition", position = 210)
public class LiquibaseActionFolder extends AbstractAction implements Presenter.Popup
{

  @Override
  public void actionPerformed(ActionEvent e)
  {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public JMenuItem getPopupPresenter()
  {
    JMenu main = new JMenu(Bundle.CTL_LiquibaseAction());
    Utilities.actionsForPath("Plugins/Liquibase/Actions").forEach(pAction -> {
      if (pAction != null)
        main.add(pAction);
      else
        main.addSeparator();
    });
    return main;
  }

}
