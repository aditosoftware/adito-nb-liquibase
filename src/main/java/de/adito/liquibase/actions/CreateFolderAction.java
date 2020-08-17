package de.adito.liquibase.actions;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.common.IProjectQuery;
import de.adito.liquibase.nb.LiquibaseFolderService;
import de.adito.liquibase.notification.INotificationFacade;
import org.jetbrains.annotations.NotNull;
import org.openide.*;
import org.openide.awt.*;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

import java.io.*;
import java.nio.file.Files;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.stream.Stream;

/**
 * @author w.glanzer, 10.08.2020
 */
@NbBundle.Messages({
    "CTL_CreateFolderAction=Create Folder...",
    "LBL_EnterNewName=Folder Name:"
})
@ActionID(category = "Liquibase", id = "de.adito.liquibase.actions.CreateFolderAction")
@ActionRegistration(displayName = "#CTL_CreateFolderAction", lazy = false)
@ActionReference(path = "Plugins/Liquibase/Actions", position = 400)
public class CreateFolderAction extends AbstractLiquibaseAction
{

  @Override
  protected boolean enable0(@NotNull Node[] pActivatedNodes)
  {
    // only active, if one (and only one) project is selected
    return Stream.of(pActivatedNodes)
        .map(IProjectQuery.getInstance()::findProjects)
        .flatMap(Set::stream)
        .distinct()
        .count() == 1 &&

        // and if an alias could be found
        getChangelogProvider().findAliasName() != null;
  }

  @Override
  protected void performAction0(@NotNull Node[] pActivatedNodes) throws CancellationException
  {
    NotifyDescriptor.InputLine desc = new NotifyDescriptor.InputLine(Bundle.LBL_EnterNewName(), getName());
    Object result = DialogDisplayer.getDefault().notify(desc);
    String input = desc.getInputText().trim();
    String aliasName = getChangelogProvider().findAliasName();
    if (result != DialogDescriptor.OK_OPTION || input.isEmpty() || aliasName == null)
      return;

    // read current project (it has to be only one, because of enable0)
    Stream.of(pActivatedNodes)
        .map(IProjectQuery.getInstance()::findProjects)
        .flatMap(Set::stream)

        // get .liquibase folder
        .map(LiquibaseFolderService::getInstance)
        .map(pService -> pService.getLiquibaseFolderPathForAlias(aliasName))

        // create directory
        .findFirst()
        .ifPresent(pLiquibaseFolder -> {
          try
          {
            Files.createDirectories(new File(pLiquibaseFolder, input).toPath());
          }
          catch (IOException e)
          {
            INotificationFacade.INSTANCE.error(e);
          }
        });
  }

  @Override
  public String getName()
  {
    return Bundle.CTL_CreateFolderAction();
  }

}
