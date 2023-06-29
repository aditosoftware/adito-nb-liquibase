package de.adito.liquibase.actions;


import com.google.common.base.Charsets;
import de.adito.liquibase.nb.NbUtil;
import lombok.NonNull;
import org.apache.commons.io.IOUtils;
import org.openide.awt.*;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

import java.io.*;
import java.util.concurrent.CancellationException;

/**
 * @author w.glanzer, 10.08.2020
 */
@NbBundle.Messages({
    "CTL_NewChangelogAction=New Changelog...",
    "LBL_EnterNewName_changelog=Changelog Name:"
})
@ActionID(category = "Liquibase", id = "de.adito.liquibase.actions.NewChangelogAction")
@ActionRegistration(displayName = "#CTL_NewChangelogAction", lazy = false)
@ActionReference(path = "de/adito/aod/action/aliasDefinition/new", position = 100)
public class NewChangeLogAction extends AbstractFolderBasedAction
{

  private static final String _TEMPLATE;

  static
  {
    try
    {
      _TEMPLATE = IOUtils.toString(NewChangeLogAction.class.getResource("changelog.xml"), Charsets.UTF_8);
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void performAction0(Node @NonNull [] pNodes, @NonNull String pName, @NonNull File pParent) throws CancellationException, IOException
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

    NbUtil.open(target);
  }

  @NonNull
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

