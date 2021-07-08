package de.adito.liquibase.internal.executors;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.database.IAliasDiffService;
import de.adito.liquibase.internal.base.*;
import de.adito.liquibase.internal.changelog.IChangelogProvider;
import de.adito.liquibase.internal.connection.IConnectionProvider;
import de.adito.liquibase.internal.executors.generate.GenerateChangelogOptionsPanel;
import de.adito.liquibase.nb.NbUtil;
import de.adito.notification.INotificationFacade;
import liquibase.*;
import liquibase.configuration.*;
import liquibase.diff.output.*;
import liquibase.diff.output.changelog.DiffToChangeLog;
import liquibase.exception.*;
import liquibase.integration.ant.type.ChangeLogOutputFile;
import liquibase.structure.DatabaseObject;
import org.apache.tools.ant.types.resources.FileResource;
import org.jetbrains.annotations.*;
import org.netbeans.api.project.*;
import org.openide.*;
import org.openide.filesystems.*;
import org.openide.util.*;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.regex.Pattern;

/**
 * @author w.glanzer, 30.07.2020
 */
class LiquibaseExecutorFacadeImpl implements ILiquibaseExecutorFacade
{

  private static final String _PATTERN_AUTHOR = "author=\".*\\(generated\\)\"";
  private static final String _PATTERN_ID = "id=\"\\d*-\\d*\"";
  private static final String _PATTERN_CHANGESET = "<changeSet " + _PATTERN_AUTHOR + " " + _PATTERN_ID + ">";

  @Override
  public void executeDropAll(@NotNull IConnectionProvider pConnectionProvider) throws LiquibaseException, IOException
  {
    ILiquibaseProvider.getInstance(pConnectionProvider).executeOn(null, (pLiquibase, pContexts) -> _dropAll(pLiquibase));
  }

  @Override
  public void executeUpdate(@NotNull IConnectionProvider pConnectionProvider, @NotNull IChangelogProvider pChangeLogProvider) throws LiquibaseException, IOException
  {
    ILiquibaseProvider.getInstance(pConnectionProvider).executeOn(true, pChangeLogProvider,
                                                                  (liquibase, contexts) -> _update(liquibase, contexts, pChangeLogProvider));
  }

  @Override
  public void executeDropAllAndUpdate(@NotNull IConnectionProvider pConnectionProvider, @NotNull IChangelogProvider pChangeLogProvider) throws LiquibaseException, IOException
  {
    ILiquibaseProvider.getInstance(pConnectionProvider).executeOn(true, pChangeLogProvider, (liquibase, contexts) -> {
      _dropAll(liquibase);
      _update(liquibase, contexts, pChangeLogProvider);
    });

  }

  @NbBundle.Messages({
      "LBL_UpdateSuccess=Update Succesfull",
      "LBL_DiffWithDBTables=Diff with DB tables"
  })
  private void _update(@NotNull Liquibase pLiquibase, @NotNull Contexts pContexts, @NotNull IChangelogProvider pChangeLogProvider) throws LiquibaseException
  {
    // prefetch information that will be used in notification
    File changeLog = pChangeLogProvider.hasChangelogsAvailable() ? pChangeLogProvider.findCurrentChangeLog() : null;
    String aliasName = pChangeLogProvider.findAliasName();

    // Execute Update
    pLiquibase.update(pContexts);

    // Finished
    INotificationFacade.INSTANCE.notify(Bundle.LBL_UpdateSuccess(), Bundle.LBL_DiffWithDBTables(), false, e -> {
      // Perform Diff on click
      if (changeLog != null && aliasName != null)
      {
        Project owner = FileOwnerQuery.getOwner(changeLog.toURI());
        if (owner != null)
        {
          IAliasDiffService aliasDiffService = Lookup.getDefault().lookup(IAliasDiffService.class);
          aliasDiffService.executeDiffWithDB(owner, aliasName);
        }
      }
    });
  }

  @NbBundle.Messages({
      "LBL_DropAllConfirmation=Do you really want to drop all tables from the selected database?",
      "LBL_DropAllConfirmation_Title=Drop All Confirmation",
      "BTN_DropAllConfirmation=Drop All",
      "LBL_DropSuccess_Title=Drop Success",
      "LBL_DropSuccess_Message=Dropped all data from database"
  })
  private void _dropAll(@NotNull Liquibase pLiquibase) throws DatabaseException
  {
    // Then Display Warning
    NotifyDescriptor descr = new NotifyDescriptor(Bundle.LBL_DropAllConfirmation(), Bundle.LBL_DropAllConfirmation_Title(),
                                                  NotifyDescriptor.OK_CANCEL_OPTION, NotifyDescriptor.QUESTION_MESSAGE,
                                                  new Object[]{Bundle.BTN_DropAllConfirmation(), NotifyDescriptor.CANCEL_OPTION},
                                                  NotifyDescriptor.CANCEL_OPTION);
    if (DialogDisplayer.getDefault().notify(descr) == Bundle.BTN_DropAllConfirmation())
    {
      // Execute Action
      pLiquibase.dropAll();

      // Finished
      INotificationFacade.INSTANCE.notify(Bundle.LBL_DropSuccess_Title(), Bundle.LBL_DropSuccess_Message(), true, null);
    }
    else
      throw new CancellationException();
  }

  @Override
  public void executeUpdateSQL(@NotNull IConnectionProvider pConnectionProvider, @NotNull IChangelogProvider pChangeLogProvider) throws LiquibaseException, IOException
  {
    ILiquibaseProvider.getInstance(pConnectionProvider).executeOn(true, pChangeLogProvider, (pLiquibase, pContexts) -> {
      Writer writer = new StringWriter();
      _getUpdateSql(pLiquibase, pContexts, writer);

      // Finished
      INotificationFacade.INSTANCE.showSql(writer.toString());
    });
  }

  @NbBundle.Messages({
      "LBL_TitleRollbackImpossible=Cannot create future rollback SQL",
      "LBL_MessageRollbackImpossible=Maybe in the changelog is a insert-tag or sql-tag defined, but no rollback-tag"
  })
  @Override
  public void executeFutureRollbackSQL(@NotNull IConnectionProvider pConnectionProvider, @NotNull IChangelogProvider pChangeLogProvider) throws LiquibaseException, IOException
  {
    ILiquibaseProvider.getInstance(pConnectionProvider).executeOn(true, pChangeLogProvider, (pLiquibase, pContexts) -> {
      Writer writer = new StringWriter();
      if (!_getFutureRollbackSql(pLiquibase, pContexts, writer))
        return;

      // Finished
      INotificationFacade.INSTANCE.showSql(writer.toString());
    });
  }

  @Override
  public void executeUpdateAndRollbackSQL(@NotNull IConnectionProvider pConnectionProvider, @NotNull IChangelogProvider pChangeLogProvider) throws LiquibaseException, IOException
  {
    ILiquibaseProvider.getInstance(pConnectionProvider).executeOn(true, pChangeLogProvider, (pLiquibase, pContexts) -> {
      Writer writer = new StringWriter();
      _getUpdateSql(pLiquibase, pContexts, writer);
      if (!_getFutureRollbackSql(pLiquibase, pContexts, writer))
        return;

      // Finished
      INotificationFacade.INSTANCE.showSql(writer.toString());
    });
  }

  /**
   * Executes the UPDATE SQL command. It does not check, if the changelog is already run or not.
   */
  private void _getUpdateSql(@NotNull AbstractADITOLiquibase pLiquibase, @NotNull Contexts pContexts, @NotNull Writer pOutput) throws LiquibaseException
  {
    try
    {
      pLiquibase.setSkipFilter(true);
      pLiquibase.update(pContexts, pOutput);
    }
    finally
    {
      pLiquibase.setSkipFilter(false);
    }
  }

  /**
   * Executes the FUTURE ROLLBACK SQL command. If an {@link RollbackImpossibleException} occures, it isn't shown as error, but as information.
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean _getFutureRollbackSql(@NotNull AbstractADITOLiquibase pLiquibase, @NotNull Contexts pContexts, @NotNull Writer pOutput) throws LiquibaseException
  {
    try
    {
      pLiquibase.futureRollbackSQL(pContexts, new LabelExpression(), pOutput);
    }
    catch (Throwable pE)
    {
      Throwable temp = pE.getCause();
      if (temp != null)
        temp = temp.getCause();

      // special handling, because the user receives additional information about the cause
      if (temp instanceof RollbackImpossibleException)
      {
        INotificationFacade.INSTANCE.notify(Bundle.LBL_TitleRollbackImpossible(), Bundle.LBL_MessageRollbackImpossible(), false, null);
        return false;
      }
      throw pE;
    }

    return true;
  }

  @Override
  @NbBundle.Messages({
      "LBL_TitleGenerate=Changelog generated",
      "LBL_MessageGenerate=Cangelog successfully generated"
  })
  public void executeGenerateChangelog(@NotNull Connection pConnectionProvider, @NotNull String pTableName, @Nullable String pSubfolderName) throws LiquibaseException, IOException
  {
    ILiquibaseProvider.getInstance(pConnectionProvider).executeOn(false, null, (pLiquibase, pString) -> {
      GenerateChangelogOptionsPanel dialog = new GenerateChangelogOptionsPanel(pSubfolderName);
      boolean success = dialog.showDialog(pTableName);

      if (success)
      {
        List<Class<? extends DatabaseObject>> types = new ArrayList<>();
        dialog.getGenerateTypes().forEach(pType -> types.addAll(pType.getSnapshotTypes()));
        //noinspection unchecked
        _createChangelogFile(pLiquibase, pTableName, dialog.getPath(), dialog.getAuthor(), dialog.isCatalogIncluded(), dialog.isSchemaIncluded(),
                             types.toArray(new Class[0]));
        INotificationFacade.INSTANCE.notify(Bundle.LBL_TitleGenerate(), Bundle.LBL_MessageGenerate(), false, null);
      }
    });
  }

  /**
   * Generates a Changelog-File
   */
  private void _createChangelogFile(@NotNull Liquibase pLiquibase, @NotNull String pTableName, @NotNull String pFullPath, @NotNull String pAuthor,
                                    boolean pIncludeCatalog, boolean pIncludeSchema, Class<? extends DatabaseObject>[] pSnapshotTypes) throws LiquibaseException
  {
    CatalogAndSchema catalogAndSchema = new CatalogAndSchema(pLiquibase.getDatabase().getDefaultCatalogName(), pLiquibase.getDatabase().getDefaultSchemaName());
    DiffOutputControl diffOutputControl = new DiffOutputControl(pIncludeCatalog, pIncludeSchema, true, null);
    diffOutputControl.setObjectChangeFilter(new StandardObjectChangeFilter(StandardObjectChangeFilter.FilterType.INCLUDE, "table:" + pTableName));
    DiffToChangeLog diffToChangeLog = new DiffToChangeLog(diffOutputControl);

    ChangeLogOutputFile outputFile = new ChangeLogOutputFile();
    File f = new File(pFullPath);
    FileResource resource = new FileResource(f);
    outputFile.setOutputFile(resource);

    String encoding = outputFile.getEncoding();
    if (encoding == null)
      encoding = _getDefaultOutputEncoding();

    OutputStream outstream = null;
    try
    {
      outstream = resource.getOutputStream();
      PrintStream printStream = new PrintStream(outstream, true, encoding);
      pLiquibase.generateChangeLog(catalogAndSchema, diffToChangeLog, printStream, pSnapshotTypes);

      outstream.close();
    }
    catch (DatabaseException | ParserConfigurationException | IOException pE)
    {
      // if something fails, delete the file
      if (outstream != null)
      {
        try
        {
          outstream.close();
        }
        catch (IOException pIOException)
        {
          throw new LiquibaseException(pIOException);
        }
      }
      //noinspection ResultOfMethodCallIgnored
      f.delete();
      throw new LiquibaseException(pE);
    }

    _modifyChangeset(f, pAuthor);
  }

  /**
   * @return the default encoding of the global liqubiase configuration
   */
  private String _getDefaultOutputEncoding()
  {
    LiquibaseConfiguration liquibaseConfiguration = LiquibaseConfiguration.getInstance();
    GlobalConfiguration globalConfiguration = liquibaseConfiguration.getConfiguration(GlobalConfiguration.class);
    return globalConfiguration.getOutputEncoding();
  }

  /**
   * Changes the author and sets an unique UUID of each changeset and opens the file.
   *
   * @param pFile   the file, which should be modified
   * @param pAuthor the name of the author
   * @throws LiquibaseException when something goes wrong
   */
  private void _modifyChangeset(@NotNull File pFile, @NotNull String pAuthor) throws LiquibaseException
  {
    FileObject fo = FileUtil.toFileObject(pFile);

    try
    {
      List<String> lines = new ArrayList<>(fo.asLines());
      FileWriter writer = new FileWriter(fo.getPath());
      Object attr = fo.getAttribute(FileObject.DEFAULT_LINE_SEPARATOR_ATTR);
      String lineSeparator = System.lineSeparator();
      if (attr instanceof String)
        lineSeparator = attr.toString();

      Pattern pattern = Pattern.compile(_PATTERN_CHANGESET);
      for (int i = 0; i < lines.size(); i++)
      {
        String s = lines.get(i);
        if (pattern.matcher(s.trim()).matches())
        {
          // set own author and own id
          s = s.replaceFirst(_PATTERN_AUTHOR, "author=\"" + pAuthor + "\"")
              .replaceFirst(_PATTERN_ID, "id=\"" + UUID.randomUUID().toString() + "\"");
          //noinspection SuspiciousListRemoveInLoop
          lines.remove(i);
          lines.add(i, s);
        }
      }

      writer.append(String.join(lineSeparator, lines));
      writer.flush();
    }
    catch (IOException pE)
    {
      throw new LiquibaseException(pE);
    }

    NbUtil.open(pFile);
  }
}