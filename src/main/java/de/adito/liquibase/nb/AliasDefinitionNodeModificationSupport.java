package de.adito.liquibase.nb;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.nodes.INodeModificationSupport;
import de.adito.liquibase.notification.INotificationFacade;
import de.adito.observables.netbeans.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.functions.Consumer;
import org.jetbrains.annotations.*;
import org.netbeans.api.project.*;
import org.openide.filesystems.FileObject;
import org.openide.loaders.*;
import org.openide.nodes.*;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.lookup.*;

import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Modifies the aliasDefinition-Nodes to display the contents
 * of the .liquibase-Folder directly under its alias
 *
 * @author w.glanzer, 04.08.2020
 */
@ServiceProvider(service = INodeModificationSupport.class, path = "Projects/de-adito-project/Nodes/aliasDefinition", position = 100)
public class AliasDefinitionNodeModificationSupport implements INodeModificationSupport
{

  @Override
  public boolean canModify(@NotNull Node pNode)
  {
    return true;
  }

  @Nullable
  @Override
  public Node modify(@NotNull Node pNode, @NotNull Map<Object, Object> pAttributes)
  {
    return new _AliasDefinitionNode(pNode);
  }

  /**
   * Node for aliasDefinitions
   */
  private static class _AliasDefinitionNode extends FilterNode implements Disposable
  {
    private final CompositeDisposable disposable;

    private _AliasDefinitionNode(@NotNull Node pAliasDefNode)
    {
      this(pAliasDefNode, new InstanceContent());
    }

    private _AliasDefinitionNode(@NotNull Node pAliasDefNode, @NotNull InstanceContent pInstanceContent)
    {
      super(new AbstractNode(Children.LEAF), null,
            new ProxyLookup(new AbstractLookup(pInstanceContent), Lookups.exclude(pAliasDefNode.getLookup(), Node.class)));

      disposable = new CompositeDisposable();

      // Original Changing Disposable
      disposable.add(_watchLiquibaseFolderNode(pAliasDefNode).subscribe(pFolderNodeOpt -> FilterNode.Children.MUTEX.postWriteRequest(() -> {
        changeOriginal(pFolderNodeOpt.orElseGet(() -> new AbstractNode(Children.LEAF)), true);
        changeOriginal(pAliasDefNode, false);
      })));

      // Liquibase-Folder-Mover
      disposable.add(_watchAliasAODFile(pAliasDefNode)
                         .debounce(500, TimeUnit.MILLISECONDS)
                         .subscribe(new _LiquibaseFolderMover()));
    }

    @Override
    public PasteType[] getPasteTypes(Transferable t)
    {
      return new PasteType[0];
    }

    @Override
    public void dispose()
    {
      if (disposable != null && !disposable.isDisposed())
        disposable.dispose();
    }

    @Override
    public boolean isDisposed()
    {
      return disposable != null && disposable.isDisposed();
    }

    /**
     * Creates a new observable to watch the corresponding liquibase folder instance
     *
     * @param pAliasDefNode Node of the aliasDefinition
     * @return the liquibase folder observable
     */
    @NotNull
    private Observable<Optional<Node>> _watchLiquibaseFolderNode(@NotNull Node pAliasDefNode)
    {
      return _watchAliasAODFile(pAliasDefNode)

          // Watch AliasFolder in corresponding liquibase folder
          .switchMap(pAliasOpt -> pAliasOpt
              .map(pAlias -> {
                Project project = FileOwnerQuery.getOwner(pAlias);
                if (project != null)
                  return LiquibaseFolderService.getInstance(project).observeLiquibaseFolderForAlias(pAlias.getName());
                return null;
              })
              .orElseGet(() -> Observable.just(Optional.empty())))

          // only changes
          .distinctUntilChanged()

          // FileObject to Node
          .switchMap(pFileOpt -> pFileOpt
              .map(this::_getNode)
              .map(pNode -> NodeObservable.create(pNode)
                  .map(Optional::of))
              .orElseGet(() -> Observable.just(Optional.empty())));
    }

    /**
     * Creates a new observable that observes the aliasDefinition aod file
     *
     * @param pAliasDefNode Node
     * @return Name-Observable
     */
    @NotNull
    private Observable<Optional<FileObject>> _watchAliasAODFile(@NotNull Node pAliasDefNode)
    {
      return LookupResultObservable.create(pAliasDefNode.getLookup(), DataObject.class)

          // Find FileObject to our DataObject
          .map(pFileObjects -> pFileObjects.stream()
              .map(DataObject::getPrimaryFile)
              .filter(Objects::nonNull)
              .findFirst())

          // Watch FileObject (rename, delete, etc) for AOD File
          .flatMap(pFileObjOpt -> pFileObjOpt
              .map(pFileObj -> FileObjectObservable.create(pFileObj)
                  .map(Optional::of))
              .orElseGet(() -> Observable.just(Optional.empty())));
    }

    /**
     * Generates the node from a fileobject
     *
     * @param pFileObject FileObject to get the node from
     * @return the node, or null if it cannot be read
     */
    @Nullable
    private Node _getNode(@NotNull FileObject pFileObject)
    {
      try
      {
        DataObject dataObject = DataObject.find(pFileObject);
        if (dataObject != null)
          return dataObject.getNodeDelegate();
      }
      catch (DataObjectNotFoundException e)
      {
        // nothing
      }

      return null;
    }

    /**
     * Consumer that accepts a new name for our liquibase folder
     */
    private static class _LiquibaseFolderMover implements Consumer<Optional<FileObject>>
    {
      private AtomicReference<Project> projectDirRef;
      private AtomicReference<String> oldNameRef;

      @Override
      public void accept(Optional<FileObject> pNewData)
      {
        synchronized (_LiquibaseFolderMover.class)
        {
          if (oldNameRef == null || projectDirRef == null) // initial auf alle werte warten
          {
            projectDirRef = pNewData
                .map(FileOwnerQuery::getOwner)
                .map(AtomicReference::new)
                .orElse(null);
            oldNameRef = pNewData
                .map(FileObject::getName)
                .map(AtomicReference::new)
                .orElse(null);
            return;
          }

          Project project = projectDirRef.get();
          String newName = pNewData.map(FileObject::getName).orElse(null);
          String oldName = oldNameRef.getAndSet(newName);

          // Nothing to move
          if (Objects.equals(oldName, newName))
            return;

          try
          {
            LiquibaseFolderService.getInstance(project).moveAlias(oldName, newName);
          }
          catch (IOException e)
          {
            INotificationFacade.INSTANCE.error(e);
          }
        }
      }
    }

  }

}