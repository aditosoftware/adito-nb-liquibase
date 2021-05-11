package de.adito.liquibase.actions;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.common.IProjectQuery;
import de.adito.liquibase.nb.LiquibaseFolderService;
import de.adito.notification.INotificationFacade;
import org.jetbrains.annotations.*;
import org.netbeans.api.project.Project;
import org.openide.*;
import org.openide.filesystems.*;
import org.openide.nodes.Node;

import java.io.*;
import java.util.concurrent.CancellationException;
import java.util.stream.Stream;

/**
 * @author w.glanzer, 17.09.2020
 */
public abstract class AbstractFolderBasedAction extends AbstractLiquibaseAction
{

  @Override
  protected boolean enable0(@NotNull Node[] pActivatedNodes)
  {
    // if .liquibase-folder could be found
    return findLiquibaseFolder(pActivatedNodes[0]) != null;
  }

  @Override
  protected void performAction0(@NotNull Node[] pActivatedNodes) throws CancellationException
  {
    NotifyDescriptor.InputLine desc = new NotifyDescriptor.InputLine(getInputLineTitle(), getName());
    Object result = DialogDisplayer.getDefault().notify(desc);
    String input = desc.getInputText().trim();
    if (result != DialogDescriptor.OK_OPTION || input.isEmpty())
      return;

    Stream.of(pActivatedNodes)

        // get .liquibase folder
        .map(this::findLiquibaseFolder)

        // create directory
        .findFirst()
        .ifPresent(pParentFolder -> {
          try
          {
            performAction0(pActivatedNodes, input, pParentFolder);
          }
          catch (IOException e)
          {
            INotificationFacade.INSTANCE.error(e);
          }
        });
  }

  @Nullable
  protected File findLiquibaseFolder(@NotNull Node pNode)
  {
    String aliasName = getChangelogProvider().findAliasName();

    // FolderProvider, if sub-folder selected
    AbstractFolderBasedAction.IFolderProvider prov = pNode.getLookup().lookup(AbstractFolderBasedAction.IFolderProvider.class);
    if (prov != null)
    {
      FileObject folder = prov.getFolder();
      if (folder != null)
        return FileUtil.toFile(folder);
    }

    // read current project (it has to be only one, because of enable0)
    if (aliasName != null)
    {
      Project project = IProjectQuery.getInstance().findProjects(pNode, IProjectQuery.ReturnType.MULTIPLE_TO_NULL);
      if (project != null)
        return LiquibaseFolderService.getInstance(project).getLiquibaseFolderPathForAlias(aliasName);
    }

    return null;
  }

  protected abstract void performAction0(@NotNull Node[] pActivatedNodes,
                                         @NotNull String pName, @NotNull File pParent) throws CancellationException, IOException;

  /**
   * @return Title for the inputLine dialog
   */
  @NotNull
  protected abstract String getInputLineTitle();

  /**
   * Interface to provide a single folder for selection in the activated nodes
   */
  public interface IFolderProvider
  {
    /**
     * @return the folder instacne or NULL, if it could not be found
     */
    @Nullable
    FileObject getFolder();
  }
}
