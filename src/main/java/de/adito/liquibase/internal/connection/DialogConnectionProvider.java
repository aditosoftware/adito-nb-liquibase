package de.adito.liquibase.internal.connection;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.database.IPossibleConnectionProvider;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.database.IPossibleConnectionProvider.IPossibleDBConnection.IConnectionFunction;
import org.jetbrains.annotations.*;
import org.netbeans.api.project.Project;
import org.openide.*;
import org.openide.util.*;

import java.awt.*;
import java.io.IOException;
import java.lang.ref.WeakReference;
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
      return new SelectConnectionDialogModel(project).hasConnectionsAvailable();
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
  private IPossibleConnectionProvider.IPossibleDBConnection _showSelectionDialog(@NotNull Project pProject)
  {
    SelectConnectionDialogModel model = new SelectConnectionDialogModel(pProject);
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
    return Utilities.actionsGlobalContext().lookup(Project.class);
  }

  @Nullable
  private IPossibleConnectionProvider.IPossibleDBConnection _findPersistedConnection(boolean pOpenNewConnection)
  {
    try
    {
      synchronized (connectionLock)
      {
        IPossibleConnectionProvider.IPossibleDBConnection con = selectedConnectionRef == null ? null : selectedConnectionRef.get();
        if (con != null)
          return con;

        if (pOpenNewConnection)
        {
          // read new connection
          Project project = _findCurrentProject();
          if (project != null)
            con = _showSelectionDialog(project);

          // persist in ref
          if (con != null)
            selectedConnectionRef = new WeakReference<>(con);

          // finished
          return con;
        }
      }
    }
    catch (Exception e)
    {
      // nothing, just return null
    }

    return null;
  }

}
