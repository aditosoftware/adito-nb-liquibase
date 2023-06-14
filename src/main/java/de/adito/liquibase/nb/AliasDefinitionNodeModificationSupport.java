package de.adito.liquibase.nb;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.nodes.INodeModificationSupport;
import de.adito.liquibase.actions.*;
import de.adito.nbm.blueprints.api.IBlueprintActionsProvider;
import de.adito.nbm.project.ProjectTabUtil;
import de.adito.notification.INotificationFacade;
import de.adito.observables.netbeans.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.functions.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.*;
import org.netbeans.api.project.*;
import org.openide.actions.OpenAction;
import org.openide.filesystems.FileObject;
import org.openide.loaders.*;
import org.openide.nodes.*;
import org.openide.util.*;
import org.openide.util.actions.SystemAction;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.lookup.*;

import javax.swing.*;
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
  public boolean canModify(@NonNull Node pNode)
  {
    return true;
  }

  @Nullable
  @Override
  public Node modify(@NonNull Node pNode, @NonNull Map<Object, Object> pAttributes)
  {
    return new _AliasDefinitionNode(pNode);
  }

  /**
   * Node for aliasDefinitions
   */
  private static class _AliasDefinitionNode extends FilterNode implements Disposable
  {
    private final CompositeDisposable disposable;
    private List<String[]> expanded = null;

    private _AliasDefinitionNode(@NonNull Node pAliasDefNode)
    {
      this(pAliasDefNode, new InstanceContent());
    }

    private _AliasDefinitionNode(@NonNull Node pAliasDefNode, @NonNull InstanceContent pInstanceContent)
    {
      super(new AbstractNode(org.openide.nodes.Children.LEAF), null,
            new ProxyLookup(new AbstractLookup(pInstanceContent), Lookups.exclude(pAliasDefNode.getLookup(), Node.class)));

      disposable = new CompositeDisposable();
      disposable.add(_watchLiquibaseFolder(pAliasDefNode).subscribe(pFileObject -> {
        if (pFileObject.isPresent())
        {
          Node foNode = _getNode(pFileObject.get());
          _FolderNode node = new _FolderNode(foNode != null ? foNode : new AbstractNode(Children.LEAF));
          changeOriginal(node, true);
          changeOriginal(pAliasDefNode, false);

          if (foNode != null)
            node.changeOriginal(foNode);

          if (expanded != null)
          {
            ProjectTabUtil.setExpandedNodes(expanded);
            expanded = null;
          }
        }
        else
          changeOriginal(pAliasDefNode, true);
      }));

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

    @Override
    public Action getPreferredAction()
    {
      return SystemAction.get(OpenAction.class);
    }

    /**
     * Creates a new observable to watch the corresponding liquibase folder instance
     *
     * @param pAliasDefNode Node of the aliasDefinition
     * @return the liquibase folder observable
     */
    @NonNull
    private Observable<Optional<FileObject>> _watchLiquibaseFolder(@NonNull Node pAliasDefNode)
    {
      return _watchAliasAODFile(pAliasDefNode)

          // Watch AliasFolder in corresponding liquibase folder
          .switchMap(pAliasOpt -> pAliasOpt
              .map(pAlias -> {
                Project project = FileOwnerQuery.getOwner(pAlias);
                if (project != null)
                  return LiquibaseFolderService.observe(project)
                      .switchMap(pServiceOpt -> pServiceOpt
                          .map(pService -> pService.observeLiquibaseFolderForAlias(pAlias.getName()))
                          .orElseGet(() -> Observable.just(Optional.empty())));

                return null;
              })
              .orElseGet(() -> Observable.just(Optional.empty())))

          // only changes
          .distinctUntilChanged();
    }

    /**
     * Creates a new observable that observes the aliasDefinition aod file
     *
     * @param pAliasDefNode Node
     * @return Name-Observable
     */
    @NonNull
    private Observable<Optional<FileObject>> _watchAliasAODFile(@NonNull Node pAliasDefNode)
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
    private Node _getNode(@NonNull FileObject pFileObject)
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
    private class _LiquibaseFolderMover implements Consumer<Optional<FileObject>>
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

          expanded = ProjectTabUtil.getExpandedNodes();
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

  /**
   * Represents a single wrapped node in the corresponding .liquibase folder
   */
  private static class _FolderNode extends FilterNode
  {
    public _FolderNode(@NonNull Node pOriginal)
    {
      super(pOriginal, new _Children(pOriginal), new ProxyLookup(Lookups.fixed((AbstractFolderBasedAction.IFolderProvider)
                                                                                   () -> pOriginal.getLookup().lookup(FileObject.class)),
                                                                 pOriginal.getLookup()));
    }

    @Override
    public Action[] getActions(boolean pContext)
    {
      Action[] actionArr = super.getActions(pContext);
      List<Action> actions = actionArr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(actionArr));

      // Replace "New"-Action with our own folder, if available
      boolean wasRemoved = actions.removeIf(pAction -> pAction != null &&
          pAction.getClass().getName().startsWith("org.netbeans.modules.project.ui.actions.NewFile"));
      if (wasRemoved)
      {
        actions.add(0, new NewActionsContainer());
        actions.add(1, Lookup.getDefault().lookup(IBlueprintActionsProvider.class).createModelActionGroup("liquibase"));
      }

      return actions.toArray(new Action[0]);
    }

    public void changeOriginal(@NonNull Node pNode)
    {
      if (!pNode.equals(getOriginal()))
        changeOriginal(pNode, true);
    }

    @Override
    public Action getPreferredAction()
    {
      return null;
    }

    /**
     * Children-Impl
     */
    private static class _Children extends FilterNode.Children
    {
      public _Children(@NonNull Node pOriginal)
      {
        super(pOriginal);
      }

      @Override
      protected Node copyNode(@NonNull Node pNode)
      {
        FileObject fo = pNode.getLookup().lookup(FileObject.class);
        if (fo != null)
        {
          if (fo.isFolder())
            return new _FolderNode(pNode);
          else
            return new _XmlNode(pNode);
        }
        return super.copyNode(pNode);
      }
    }
  }

  /**
   * Represents a single wrapped node for the corresponding changelog-file
   */
  private static class _XmlNode extends FilterNode
  {

    public _XmlNode(Node original)
    {
      super(original);
    }

    @Override
    public Action[] getActions(boolean pContext)
    {
      Action[] actionArr = super.getActions(pContext);
      List<Action> actions = actionArr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(actionArr));

      for (int i = 0; i < actions.size(); i++)
      {
        Action action = actions.get(i);
        if (action != null && action.getClass().getName().equals("org.netbeans.modules.xml.actions.CollectXMLAction"))
        {
          actions.add(i + 1, null);
          // load custom actions
          List<? extends Action> foundActions = Utilities.actionsForPath("Plugins/Liquibase/Changelog/Container");
          int finalI = i + 2;
          foundActions.forEach(pAction -> actions.add(finalI, pAction));
          break;
        }
      }

      return actions.toArray(new Action[0]);
    }
  }
}
