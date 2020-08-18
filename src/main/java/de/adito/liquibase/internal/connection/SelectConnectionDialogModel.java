package de.adito.liquibase.internal.connection;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.database.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import org.jetbrains.annotations.*;
import org.netbeans.api.db.explorer.*;
import org.netbeans.api.project.Project;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.io.IOException;
import java.sql.Connection;
import java.util.*;

/**
 * Model for the "SelectConnection" Dialog
 *
 * @author w.glanzer, 05.08.2020
 * @see SelectConnectionDialogPanel
 */
class SelectConnectionDialogModel extends DefaultComboBoxModel<Object>
{
  private final Set<_Item> availableData = new HashSet<>();
  private final List<_Item> shownData = new ArrayList<>();
  private final BehaviorSubject<Boolean> showAllConnections = BehaviorSubject.create();

  SelectConnectionDialogModel(@NotNull Project pProject, @Nullable String pPreselectedSourceName)
  {
    for (IPossibleConnectionProvider.IPossibleDBConnection c : AditoConnectionManager.getPossibleConnections(pProject))
      availableData.add(new _Item(c));

    // Add all unnamed connections
    for (DatabaseConnection c : ConnectionManager.getDefault().getConnections())
      availableData.add(new _Item(c));

    // init connections
    setShowAllConnections(false);

    // Preselect
    if (pPreselectedSourceName != null)
      _tryPreselect(pPreselectedSourceName);
  }

  @Override
  public int getSize()
  {
    return shownData.size();
  }

  @Override
  public _Item getElementAt(int index)
  {
    return shownData.get(index);
  }

  /**
   * Sets, if all connections should be shown
   *
   * @param pShowAll true, if all should be shown
   */
  public void setShowAllConnections(boolean pShowAll)
  {
    if (Objects.equals(showAllConnections.getValue(), pShowAll))
      return;

    showAllConnections.onNext(pShowAll);
    setSelectedItem(null);
    shownData.clear();
    availableData.stream()
        .filter(pItem -> pShowAll || pItem.getOwner() != null)
        .sorted(Comparator.comparing(_Item::toString, String.CASE_INSENSITIVE_ORDER))
        .forEachOrdered(shownData::add);

    // If no connection can be found, then all connections should be shown
    if (shownData.isEmpty() && !pShowAll)
      setShowAllConnections(true);
    else
    {
      // Selection
      if (!shownData.isEmpty())
        setSelectedItem(shownData.get(0));
      fireContentsChanged(this, 0, shownData.size());
    }
  }

  /**
   * @return true, if all connections should be shown
   */
  public Observable<Boolean> isShowAllConnections()
  {
    return showAllConnections;
  }

  /**
   * @return true, if there are connections available and useable
   */
  public boolean hasConnectionsAvailable()
  {
    return !availableData.isEmpty();
  }

  /**
   * @return the selected connection or null if no connection was selected
   */
  @Nullable
  public IPossibleConnectionProvider.IPossibleDBConnection getSelectedConnection()
  {
    _Item item = (_Item) getSelectedItem();
    if (item != null)
      return item.connection;

    return null;
  }

  /**
   * Tries to preselect the source with the given name
   *
   * @param pSourceName Name of the source to preselect
   */
  private void _tryPreselect(@NotNull String pSourceName)
  {
    // Something 100% equal?
    for (_Item data : shownData)
    {
      if (pSourceName.equals(data.getOwner()))
      {
        setSelectedItem(data);
        return;
      }
    }

    // We assume, that the data-owners are in the format "system / owner"
    for (_Item data : shownData)
    {
      String owner = data.getOwner();
      if (owner != null)
      {
        String[] split = owner.split("/");
        if (split.length >= 2 && pSourceName.equals(split[1].trim()))
        {
          setSelectedItem(data);
          return;
        }
      }
    }
  }

  /**
   * A single database connection entry
   */
  private static class _Item
  {
    private final IPossibleConnectionProvider.IPossibleDBConnection connection;

    _Item(@NotNull IPossibleConnectionProvider.IPossibleDBConnection pConnection)
    {
      connection = pConnection;
    }

    _Item(@NotNull DatabaseConnection pConnection)
    {
      connection = new IPossibleConnectionProvider.IPossibleDBConnection()
      {
        @NotNull
        @Override
        public String getURL()
        {
          return pConnection.getDatabaseURL();
        }

        @Nullable
        @Override
        public String getSourceName()
        {
          return null;
        }

        @Override
        public <T, Ex extends Throwable> T withJDBCConnection(@NotNull IConnectionFunction<T, Ex> pFunction) throws IOException, Ex
        {
          ConnectionManager.getDefault().showConnectionDialog(pConnection);
          Connection jdbcCon = pConnection.getJDBCConnection();
          if (jdbcCon == null)
            // should not happen
            throw new IOException("Connection could not be read. Maybe it is not connected?");
          return pFunction.apply(jdbcCon);
        }
      };
    }

    @Nullable
    public String getOwner()
    {
      return connection.getSourceName();
    }

    @NbBundle.Messages("UnknownOwner=Unknown owner")
    @Override
    public String toString()
    {
      String name = getOwner();
      return "[" + (name != null ? name : Bundle.UnknownOwner()) + "] " + connection.getURL();
    }

    @Override
    public boolean equals(Object pO)
    {
      if (this == pO)
        return true;
      if (pO == null || getClass() != pO.getClass())
        return false;
      _Item item = (_Item) pO;
      return Objects.equals(connection.getURL(), item.connection.getURL()) &&
          Objects.equals(connection.getSourceName(), item.connection.getSourceName());
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(connection.getURL(), connection.getSourceName());
    }
  }
}
