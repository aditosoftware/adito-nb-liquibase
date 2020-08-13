package de.adito.liquibase.internal.connection;

import org.jetbrains.annotations.*;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.project.Project;
import org.openide.*;
import org.openide.util.*;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.util.concurrent.CancellationException;

/**
 * ConnectionProvider that asks the user to select a connection
 *
 * @author w.glanzer, 30.07.2020
 */
public class DialogConnectionProvider implements IConnectionProvider
{

  private final Object connectionLock = new Object();
  private WeakReference<Connection> selectedConnectionRef;

  @Nullable
  @Override
  public Connection findCurrentConnection()
  {
    Connection con = _findPersistedConnection(true);
    if (con != null)
      return new UnclosableConnectionWrapper(con);
    return null;
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
   * @return the connection or null, if the user did not select any
   */
  @NbBundle.Messages({
      "MSG_SelectConnection=Select Database Connection",
      "ACSD_SelectConnection=Select the database connection for generating connection code."
  })
  @Nullable
  private Connection _showSelectionDialog(@NotNull Project pProject)
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
    {
      DatabaseConnection con = model.getSelectedConnection();
      if (con != null)
        return con.getJDBCConnection();
    }
    else
      throw new CancellationException();

    return null;
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
  private Connection _findPersistedConnection(boolean pAskUserToChoose)
  {
    try
    {
      synchronized (connectionLock)
      {
        Connection con = selectedConnectionRef == null ? null : selectedConnectionRef.get();
        if (con != null && con.isValid(500))
          return con;

        if (pAskUserToChoose)
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
