package de.adito.liquibase.internal.connection;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.database.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import lombok.NonNull;
import org.jetbrains.annotations.*;
import org.netbeans.api.project.Project;
import org.openide.util.*;

import javax.swing.*;
import java.util.*;
import java.util.function.Supplier;

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
  private final BehaviorSubject<Optional<Set<String>>> contextsLoaded = BehaviorSubject.create();
  private final BehaviorSubject<Optional<_Item>> connections = BehaviorSubject.create();
  private final BehaviorSubject<Optional<Pair<String, Boolean>>> selectedContextsChanged = BehaviorSubject.create(); // name of context and disabled/enabled
  private final Set<String> selectedContexts = new HashSet<>();
  private final List<IConnectionLoaderStateListener> connectionStateListeners = new ArrayList<>();

  SelectConnectionDialogModel(@NonNull Project pProject, @Nullable String pPreselectedSourceName, @NonNull Supplier<Set<String>> pGetContexts)
  {
    contextsLoaded.onNext(Optional.empty());
    selectedContextsChanged.onNext(Optional.empty());
    SwingUtilities.invokeLater(() -> {
      _loadAsync(pProject, pPreselectedSourceName);

      // init connections
      setShowAllConnections(true);

      // Preselect
      if (pPreselectedSourceName != null)
        _tryPreselect(pPreselectedSourceName);
    });

    _loadContexts(pGetContexts);
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
   * @return Creates an Observable, that checks, if the current selection in the dialog is valid
   */
  @NonNull
  public Observable<Boolean> observeIsValid()
  {
    return Observable.combineLatest(selectedContextsChanged, connections, contextsLoaded, (pContexts, pItem, pContextsLoaded) ->
        pContextsLoaded.isPresent() && (pContextsLoaded.get().isEmpty() || !selectedContexts.isEmpty()) && pItem.isPresent());
  }

  /**
   * Does load all connections whose loading time is potentially long - because of this, the loading is done asynchronously
   *
   * @param pProject    Project for which to load connections
   * @param pSourceName name of the connection to preselect
   */
  private void _loadAsync(@NonNull Project pProject, @Nullable String pSourceName)
  {
    new Thread(() -> {
      for (IPossibleConnectionProvider.IPossibleDBConnection c : AditoConnectionManager.getPossibleConnections(pProject))
        availableData.add(new _Item(c));
      finishedLoading();
      IConnectionLoaderStateListener.ELoadingState loadingState = IConnectionLoaderStateListener.ELoadingState.FINISHED;
      if (pSourceName != null && _findItemToSelect(pSourceName) != null)
      {
        // if there is no item selected yet, preselect the suggested item
        if (getSelectedItem() == null)
          _tryPreselect(pSourceName);
          // the user already selected an item, changing the selection is unwise - however, notify the user that a connection matching the
          // suggested one was found
        else
          loadingState = IConnectionLoaderStateListener.ELoadingState.FINISHED_FOUND_SELECTED;
      }
      if (contextsLoaded.getValue().isPresent())
        fireConnectionStateChanged(loadingState);
    }).start();
  }

  private void finishedLoading()
  {
    // remember selected item
    _Item selectedItem = (_Item) getSelectedItem();
    // clear and add new connections
    shownData.clear();
    availableData.stream()
        .filter(pItem -> showAllConnections.blockingFirst(true) || pItem.getOwner() != null)
        .sorted(Comparator.comparing(_Item::toString, String.CASE_INSENSITIVE_ORDER))
        .forEachOrdered(shownData::add);
    fireContentsChanged(this, 0, shownData.size());
    // set the selected item again
    if (selectedItem != null && shownData.contains(selectedItem))
      setSelectedItem(selectedItem);
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

  public void contextSelected(@NonNull String pContextName, boolean pEnabled)
  {
    if (pEnabled)
      selectedContexts.add(pContextName);
    else
      selectedContexts.remove(pContextName);
    selectedContextsChanged.onNext(Optional.of(Pair.of(pContextName, pEnabled)));
  }

  /**
   * @return the selected connection or null if no connection was selected
   */
  @NonNull
  public Pair<IPossibleConnectionProvider.IPossibleDBConnection, List<String>> getSelectedConnectionAndContexts()
  {
    List<String> selected;
    if (!contextsLoaded.getValue().isPresent())
      selected = new ArrayList<>();
    else
      selected = new ArrayList<>(selectedContexts);

    _Item item = (_Item) getSelectedItem();
    if (item != null)
      return Pair.of(item.connection, selected);

    return Pair.of(null, selected);
  }

  @Override
  public void setSelectedItem(Object anObject)
  {
    if (anObject == null)
      connections.onNext(Optional.empty());
    else
      connections.onNext(Optional.of((_Item) anObject));
    if (Objects.equals(anObject, getSelectedItem()))
      return;

    super.setSelectedItem(anObject);
  }

  private void _loadContexts(@NonNull Supplier<Set<String>> pGetContexts)
  {
    fireConnectionStateChanged(IConnectionLoaderStateListener.ELoadingState.LOADING);
    RequestProcessor.getDefault().execute(() -> {
      Set<String> contextNames = pGetContexts.get();
      if (contextsLoaded.getValue().map(pContextsLoaded -> !contextNames.equals(pContextsLoaded)).orElse(true))
        contextsLoaded.onNext(Optional.of(contextNames));

      // fire only if connection is preselected
      if (getSelectedItem() != null)
        fireConnectionStateChanged(IConnectionLoaderStateListener.ELoadingState.FINISHED);
    });
  }

  public BehaviorSubject<Optional<Set<String>>> observeContexts()
  {
    return contextsLoaded;
  }

  public void addConnectionStateListener(IConnectionLoaderStateListener pListener)
  {
    connectionStateListeners.add(pListener);
  }

  public void removeConnectionStateListener(IConnectionLoaderStateListener pListener)
  {
    connectionStateListeners.remove(pListener);
  }

  private void fireConnectionStateChanged(IConnectionLoaderStateListener.ELoadingState pLoadingState)
  {
    for (IConnectionLoaderStateListener connectionStateListener : connectionStateListeners)
    {
      connectionStateListener.stateChanged(pLoadingState);
    }
  }

  /**
   * Tries to preselect the source with the given name
   *
   * @param pSourceName Name of the source to preselect
   */
  private void _tryPreselect(@NonNull String pSourceName)
  {
    _Item itemToSelect = _findItemToSelect(pSourceName);
    setSelectedItem(itemToSelect);
  }

  /**
   * Tries to find the source with the given name in the shownData list
   *
   * @param pSourceName Name of the source to find
   * @return the item that matched the sourceName
   */
  @Nullable
  private _Item _findItemToSelect(@NonNull String pSourceName)
  {
    // Something 100% equal?
    for (_Item data : shownData)
    {
      if (pSourceName.equals(data.getOwner()))
      {
        return data;
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
          return data;
        }
      }
    }
    return null;
  }

  /**
   * A single database connection entry
   */
  private static class _Item
  {
    private final IPossibleConnectionProvider.IPossibleDBConnection connection;

    _Item(@NonNull IPossibleConnectionProvider.IPossibleDBConnection pConnection)
    {
      connection = pConnection;
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
