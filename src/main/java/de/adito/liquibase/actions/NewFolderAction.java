package de.adito.liquibase.actions;

import lombok.NonNull;
import org.openide.awt.*;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.CancellationException;

/**
 * @author w.glanzer, 10.08.2020
 */
@NbBundle.Messages({
    "CTL_CreateFolderAction=New Folder...",
    "LBL_EnterNewName=Folder Name:"
})
@ActionID(category = "Liquibase", id = "de.adito.liquibase.actions.NewFolderAction")
@ActionRegistration(displayName = "#CTL_CreateFolderAction", lazy = false)
@ActionReference(path = "de/adito/aod/action/aliasDefinition/new", position = 1000)
public class NewFolderAction extends AbstractFolderBasedAction
{

  @Override
  protected void performAction0(@NonNull Node[] pNodes, @NonNull String pName, @NonNull File pParent) throws CancellationException, IOException
  {
    Files.createDirectories(new File(pParent, pName).toPath());
  }

  @NonNull
  @Override
  protected String getInputLineTitle()
  {
    return Bundle.LBL_EnterNewName();
  }

  @Override
  public String getName()
  {
    return Bundle.CTL_CreateFolderAction();
  }

}
