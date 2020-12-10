package de.adito.liquibase.actions.changelog;

import org.openide.awt.*;
import org.openide.util.*;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Container for Changelog-File-Actions
 *
 * @author s.seemann, 10.12.2020
 */
@NbBundle.Messages("CTL_ChangelogActionsContainer=Liquibase")
@ActionID(category = "AliasDefinition", id = "de.adito.liquibase.actions.changelog.ChangelogActionsContainer")
@ActionRegistration(displayName = "#CTL_ChangelogActionsContainer", lazy = false)
@ActionReference(path = "Plugins/Liquibase/Changelog/Container", position = 0)
public class ChangelogActionsContainer extends AbstractAction implements Presenter.Popup
{
  @Override
  public void actionPerformed(ActionEvent e)
  {
    throw new RuntimeException("A container action can not be performed");
  }

  @Override
  public JMenuItem getPopupPresenter()
  {
    JMenu main = new JMenu(NbBundle.getMessage(ChangelogActionsContainer.class, "CTL_ChangelogActionsContainer"));
    main.setVisible(false); // show only if item is present

    Utilities.actionsForPath("Plugins/Liquibase/Changelog/Actions").forEach(pAction -> {
      if (pAction != null)
      {
        main.add(pAction);
        main.setVisible(true); // show the menu
      }
      else
        main.addSeparator();
    });

    return main;
  }
}
