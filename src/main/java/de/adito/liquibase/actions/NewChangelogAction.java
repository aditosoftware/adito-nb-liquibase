package de.adito.liquibase.actions;


import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.openide.awt.*;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

import java.io.*;
import java.util.concurrent.CancellationException;

/**
 * @author w.glanzer, 10.08.2020
 */
@NbBundle.Messages({
    "CTL_NewChangelogAction=New ChangeLog...",
    "LBL_EnterNewName_changelog=ChangeLog Name:"
})
@ActionID(category = "Liquibase", id = "de.adito.liquibase.actions.NewChangelogAction")
@ActionRegistration(displayName = "#CTL_NewChangelogAction", lazy = false)
@ActionReference(path = "de/adito/aod/action/aliasDefinition/new", position = 100)
public class NewChangelogAction extends AbstractFolderBasedAction
{

  private static final String _TEMPLATE;

  static
  {
    try
    {
      _TEMPLATE = IOUtils.toString(NewChangelogAction.class.getResource("changelog.xml"), Charsets.UTF_8);
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void performAction0(@NotNull Node[] pNodes, @NotNull String pName, @NotNull File pParent) throws CancellationException, IOException
  {
    if (!pName.endsWith(".xml"))
      pName += ".xml";

    File target = new File(pParent, pName);
    if (target.exists())
      return; // do nothing, maybe dialog?

    try (FileOutputStream fos = new FileOutputStream(target))
    {
      IOUtils.write(_TEMPLATE, fos, Charsets.UTF_8);
    }
  }

  @NotNull
  @Override
  protected String getInputLineTitle()
  {
    return Bundle.LBL_EnterNewName_changelog();
  }

  @Override
  public String getName()
  {
    return Bundle.CTL_NewChangelogAction();
  }

}

