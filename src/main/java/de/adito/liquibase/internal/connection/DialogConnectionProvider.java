package de.adito.liquibase.internal.connection;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.common.IProjectQuery;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.database.IPossibleConnectionProvider;
import org.jetbrains.annotations.*;
import org.netbeans.api.project.Project;
import org.openide.*;
import org.openide.nodes.Node;
import org.openide.util.*;

import java.awt.*;
import java.beans.FeatureDescriptor;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;

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
  public <T, Ex extends Throwable> T executeOnCurrentConnection(@NotNull Supplier<Set<String>> pGetContexts,
                                                                @NotNull IConnectionContextFunction<T, Ex> pFunction) throws IOException, Ex
  {
    Pair<IPossibleConnectionProvider.IPossibleDBConnection, List<String>> result = _findPersistedConnection(pGetContexts, true);
    IPossibleConnectionProvider.IPossibleDBConnection con = result.first();
    if (con == null)
      throw new IOException("Failed to get current connection");

    pFunction.before();

    // Execute something
    return con.withJDBCConnection(pCon -> pFunction.apply(new UnclosableConnectionWrapper(pCon), result.second()));
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
  @NotNull
  private Pair<IPossibleConnectionProvider.IPossibleDBConnection, List<String>> _showSelectionDialog(@NotNull Supplier<Set<String>> pGetContexts,
                                                                                                     @NotNull Project pProject) throws CancellationException
  {
    SelectConnectionDialogModel model = new SelectConnectionDialogModel(pProject, _getSelectedAliasDefinitionName(), pGetContexts);
    SelectConnectionDialogPanel panel = new SelectConnectionDialogPanel(model);
    DialogDescriptor desc = new DialogDescriptor(panel, Bundle.MSG_SelectConnection());
    panel.setValidator(desc::setValid);

    Dialog dialog = DialogDisplayer.getDefault().createDialog(desc);
    dialog.getAccessibleContext().setAccessibleDescription(Bundle.ACSD_SelectConnection());
    dialog.pack();
    dialog.setMinimumSize(dialog.getSize());
    dialog.setVisible(true);
    dialog.dispose();
    panel.dispose();

    if (desc.getValue() == DialogDescriptor.OK_OPTION)
      return model.getSelectedConnectionAndContexts();
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

  @NotNull
  private Pair<IPossibleConnectionProvider.IPossibleDBConnection, List<String>> _findPersistedConnection(@NotNull Supplier<Set<String>> pGetContexts,
                                                                                                         @SuppressWarnings("SameParameterValue") boolean pOpenNewConnection)
  {
    IPossibleConnectionProvider.IPossibleDBConnection con = selectedConnectionRef == null ? null : selectedConnectionRef.get();
    synchronized (connectionLock)
    {
      // new connection?
      if (con == null && pOpenNewConnection)
      {
        // read new connection
        Project project = _findCurrentProject();
        if (project != null)
        {
          Pair<IPossibleConnectionProvider.IPossibleDBConnection, List<String>> result = _showSelectionDialog(pGetContexts, project);
          // persist in ref
          if (result.first() != null)
            selectedConnectionRef = new WeakReference<>(result.first());
          return result;
        }
      }
    }
    // finished
    return Pair.of(con, List.of());
  }
}
