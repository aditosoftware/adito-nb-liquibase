package de.adito.liquibase.internal.connection;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import org.jetbrains.annotations.*;
import org.netbeans.api.db.explorer.*;
import org.netbeans.api.project.Project;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.util.*;

/**
 * Model for the "SelectConnection" Dialog
 *
 * @author w.glanzer, 05.08.2020
 * @see SelectConnectionDialogPanel
 */
class SelectConnectionDialogModel extends DefaultComboBoxModel<Object>
{
  private final List<_Item> availableData = new ArrayList<>();
  private final List<_Item> shownData = new ArrayList<>();
  private final BehaviorSubject<Boolean> showAllConnections = BehaviorSubject.create();

  SelectConnectionDialogModel(@NotNull Project pProject)
  {
    //todo get connections from project
    for (DatabaseConnection c : ConnectionManager.getDefault().getConnections())
      availableData.add(new _Item(null, c));

    // init connections
    setShowAllConnections(false);
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
  public DatabaseConnection getSelectedConnection()
  {
    _Item item = (_Item) getSelectedItem();
    if (item != null)
      return item.connection;

    return null;
  }

  /**
   * A single database connection entry
   */
  private static class _Item
  {
    private final String name;
    private final DatabaseConnection connection;

    _Item(String pName, DatabaseConnection pConnection)
    {
      name = pName;
      connection = pConnection;
    }

    @Nullable
    public String getOwner()
    {
      return name;
    }

    @NbBundle.Messages("UnknownOwner=Unknown owner")
    @Override
    public String toString()
    {
      return "[" + (name != null ? name : Bundle.UnknownOwner()) + "] " + connection.getDisplayName();
    }
  }
}
