package de.adito.liquibase.internal.connection;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.common.IProjectQuery;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.database.IPossibleConnectionProvider;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.database.IPossibleConnectionProvider.IPossibleDBConnection.IConnectionFunction;
import org.jetbrains.annotations.*;
import org.netbeans.api.project.Project;
import org.openide.*;
import org.openide.nodes.Node;
import org.openide.util.*;

import java.awt.*;
import java.beans.FeatureDescriptor;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.concurrent.CancellationException;

/**
 * ConnectionProvider that asks the user to select a connection
 *
 * @author w.glanzer, 30.07.2020
 */
public class DialogConnectionProvider implements IConnectionProvider
{

  private final Object connectionLock = new Object();
  private WeakReference<IPossibleConnectionProvider.IPossibleDBConnection> selectedConnectionRef;

  @Override
  public <T, Ex extends Throwable> T executeOnCurrentConnection(@NotNull IConnectionFunction<T, Ex> pFunction) throws IOException, Ex
  {
    IPossibleConnectionProvider.IPossibleDBConnection con = _findPersistedConnection(true);
    if (con == null)
      throw new IOException("Failed to get current connection");

    // Execute something
    return con.withJDBCConnection(pCon -> pFunction.apply(new UnclosableConnectionWrapper(pCon)));
  }

  @Override
  public boolean hasConnectionsAvailable()
  {
    if (_findPersistedConnection(false) != null)
      return true;

    Project project = _findCurrentProject();
    if (project != null)
      return new SelectConnectionDialogModel(project, null).hasConnectionsAvailable();
    return false;
  }

  @Override
  public void reset()
  {
    synchronized (connectionLock)
    {
      selectedConnectionRef = null;
    }
  }

  /**
   * shows a dialog to ask the user, which connection to use
   *
   * @param pProject Project as environment
   * @return the connection or null / error, if the user did not select any
   */
  @NbBundle.Messages({
      "MSG_SelectConnection=Select Database Connection",
      "ACSD_SelectConnection=Select the database connection for generating connection code."
  })
  @Nullable
  private IPossibleConnectionProvider.IPossibleDBConnection _showSelectionDialog(@NotNull Project pProject) throws CancellationException
  {
    SelectConnectionDialogModel model = new SelectConnectionDialogModel(pProject, _getSelectedAliasDefinitionName());
    SelectConnectionDialogPanel panel = new SelectConnectionDialogPanel(model);
    DialogDescriptor desc = new DialogDescriptor(panel, Bundle.MSG_SelectConnection());

    Dialog dialog = DialogDisplayer.getDefault().createDialog(desc);
    dialog.getAccessibleContext().setAccessibleDescription(Bundle.ACSD_SelectConnection());
    dialog.pack();
    dialog.setMinimumSize(dialog.getSize());
    dialog.setVisible(true);
    dialog.dispose();
    panel.dispose();

    if (desc.getValue() == DialogDescriptor.OK_OPTION)
      return model.getSelectedConnection();
    else
      throw new CancellationException();
  }

  /**
   * @return the current project environment
   */
  @Nullable
  private Project _findCurrentProject()
  {
    return Optional.ofNullable(IProjectQuery.getInstance().findProjects(Utilities.actionsGlobalContext(), IProjectQuery.ReturnType.MULTIPLE_TO_SET))
        .flatMap(pProjects -> pProjects.stream().findFirst())
        .orElse(null);
  }

  /**
   * @return the currently selected alias definition name or null, if nothing is selected
   */
  @Nullable
  private String _getSelectedAliasDefinitionName()
  {
    return Utilities.actionsGlobalContext().lookupAll(Node.class)
        .stream()
        .map(FeatureDescriptor::getDisplayName)
        .findFirst()
        .orElse(null);
  }

  @Nullable
  private IPossibleConnectionProvider.IPossibleDBConnection _findPersistedConnection(boolean pOpenNewConnection)
  {
    synchronized (connectionLock)
    {
      IPossibleConnectionProvider.IPossibleDBConnection con = selectedConnectionRef == null ? null : selectedConnectionRef.get();

      // new connection?
      if (con == null && pOpenNewConnection)
      {
        // read new connection
        Project project = _findCurrentProject();
        if (project != null)
          con = _showSelectionDialog(project);

        // persist in ref
        if (con != null)
          selectedConnectionRef = new WeakReference<>(con);
      }

      // finished
      return con;
    }
  }

}
