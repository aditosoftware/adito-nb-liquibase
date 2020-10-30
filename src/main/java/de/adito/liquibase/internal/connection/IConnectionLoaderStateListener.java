package de.adito.liquibase.internal.connection;

/**
 * Listener for the loading state of connections. Can fire if the list of connections is still loading, finished or finished with a special condition
 *
 * @author m.kaspera, 29.10.2020
 */
public interface IConnectionLoaderStateListener
{

  enum ELoadingState{
    /**
     * Connections are still loading
     */
    LOADING,
    /**
     * Loading of the connections finished
     */
    FINISHED,
    /**
     * Loading of the connections finished and a suggested connection was found in the loaded list
     */
    FINISHED_FOUND_SELECTED
  }

  void stateChanged(ELoadingState pNewLoadingState);

}
