package com.github.wglanzer.nbm.util;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import org.jetbrains.annotations.NotNull;
import org.openide.nodes.Node;
import org.openide.windows.TopComponent;

import java.beans.PropertyChangeListener;
import java.util.function.Consumer;

/**
 * Contains a set of NetBeans-Observables
 *
 * @author w.glanzer, 25.10.2018
 */
public class NetBeansObservables
{

  @NotNull
  public static Observable<Node[]> activatedNodesObservable()
  {
    return _ActivatedNodesObservable.create()
        .subscribeWith(BehaviorSubject.create());
  }

  /**
   * Observable for TopComponent.getActivatedNodes()
   */
  private static class _ActivatedNodesObservable extends AbstractListenerObservable<PropertyChangeListener, TopComponent.Registry, Node[]>
  {
    private static Observable<Node[]> create()
    {
      return Observable.create(new _ActivatedNodesObservable(TopComponent.getRegistry()))
          .startWith(TopComponent.getRegistry().getActivatedNodes());
    }

    private _ActivatedNodesObservable(@NotNull TopComponent.Registry pListenableValue)
    {
      super(pListenableValue);
    }

    @NotNull
    @Override
    protected PropertyChangeListener registerListener(@NotNull TopComponent.Registry pListenableValue, @NotNull Consumer<Node[]> pOnNext)
    {
      PropertyChangeListener pcl = evt -> {
        if(evt.getPropertyName().equals(TopComponent.Registry.PROP_ACTIVATED_NODES))
          pOnNext.accept(pListenableValue.getActivatedNodes());
      };
      pListenableValue.addPropertyChangeListener(pcl);
      return pcl;
    }

    @Override
    protected void removeListener(@NotNull TopComponent.Registry pListenableValue, @NotNull PropertyChangeListener pPropertyChangeListener)
    {
      pListenableValue.removePropertyChangeListener(pPropertyChangeListener);
    }
  }

}
