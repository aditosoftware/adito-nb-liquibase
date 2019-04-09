package com.github.wglanzer.nbm.actions;

import com.github.wglanzer.nbm.ILiquibaseConstants;
import com.github.wglanzer.nbm.liquibase.INotificationFacade;
import com.github.wglanzer.nbm.util.Util;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.liquibase.LiquiConstants;
import org.openide.awt.*;
import org.openide.nodes.Node;
import org.openide.util.*;
import org.openide.util.actions.NodeAction;
import org.openide.windows.TopComponent;

import java.io.*;
import java.nio.file.Files;


@ActionID(category = "Liquibase", id = "com.github.wglanzer.nbm.actions.CreateChangelogAction")
@ActionRegistration(displayName = "--Create " + LiquiConstants.CHANGELOG_FILE, lazy = false)
@ActionReferences({
    @ActionReference(path = LiquiConstants.ACTION_REFERENCE, position = 1200, separatorBefore = 1199),
})
public class CreateChangelogAction extends NodeAction
{

  @Override
  protected void performAction(Node[] activatedNodes)
  {
    Node[] nodes = TopComponent.getRegistry().getActivatedNodes();
    if (nodes.length == 1)
    {
      String changelogPath = Util.getChangelogPath(nodes[0]);
      if (changelogPath != null)
      {
        try
        {
          Files.createFile(new File(changelogPath).toPath());
        }
        catch (IOException pE)
        {
          AbstractLiquibaseAction._NOTIFICATION_FACADE.error(pE);
        }
      }
    }

  }


  @Override
  protected boolean enable(Node[] activatedNodes)
  {
    Node[] nodes = TopComponent.getRegistry().getActivatedNodes();
    if (nodes.length == 1)
      return !Util.existsChangelogFile(nodes[0]);

    return false;
  }

  @Override
  public HelpCtx getHelpCtx()
  {
    return HelpCtx.DEFAULT_HELP;
  }


  @Override
  public String getName()
  {
    return NbBundle.getMessage(CreateChangelogAction.class, "Create_changelog")+ LiquiConstants.CHANGELOG_FILE;
  }


}
