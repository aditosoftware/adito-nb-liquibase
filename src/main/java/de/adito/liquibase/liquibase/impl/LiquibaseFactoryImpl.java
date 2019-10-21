package de.adito.liquibase.liquibase.impl;

import com.google.inject.Singleton;
import de.adito.liquibase.liquibase.ILiquibaseProvider;
import de.adito.liquibase.liquibase.internal.ILiquibaseFactory;
import liquibase.Liquibase;
import liquibase.database.*;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.*;
import liquibase.resource.FileSystemResourceAccessor;
import org.jetbrains.annotations.NotNull;
import org.netbeans.api.project.*;
import org.openide.*;
import org.openide.util.NbBundle;

import java.io.File;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.function.Supplier;

/**
 * @author w.glanzer, 24.10.2018
 */
@Singleton
@NbBundle.Messages({
    "LBL_ContinueValidation=Clear CheckSums before Validation?"
})
public class LiquibaseFactoryImpl implements ILiquibaseFactory
{

  @NotNull
  @Override
  public ILiquibaseProvider create(@NotNull Supplier<Connection> pConnectionSupplier, @NotNull String pChangeLogFile)
  {
    return new _Provider(pConnectionSupplier, pChangeLogFile);
  }

  private static class _Provider implements ILiquibaseProvider
  {
    private final Supplier<Connection> connectionSupplier;
    private final FileSystemResourceAccessor fileSystemResourceAccessor;
    private final String relativeChangeLogFile;

    _Provider(Supplier<Connection> pConnectionSupplier, String pChangeLogFile)
    {
      Project project = FileOwnerQuery.getOwner(new File(pChangeLogFile).toURI());
      String basePath = project != null ? project.getProjectDirectory().getPath() : new File(pChangeLogFile).getParentFile().getAbsolutePath();

      connectionSupplier = pConnectionSupplier;
      fileSystemResourceAccessor = new FileSystemResourceAccessor(basePath);
      relativeChangeLogFile = Paths.get(basePath).relativize(Paths.get(pChangeLogFile)).toFile().getPath();
    }

    @Override
    public <Ex extends Exception> void executeWith(@NotNull ILiquibaseProvider.ILiquibaseConsumer<Ex> pExecutor) throws Ex, LiquibaseException
    {
      Connection jdbcCon = connectionSupplier.get();
      if (jdbcCon != null)
      {
        JdbcConnection con = new JdbcConnection(jdbcCon);
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(con);
        Liquibase base = new Liquibase(relativeChangeLogFile, fileSystemResourceAccessor, database);

        try
        {
          base.validate();
        }
        catch (ValidationFailedException vfe)
        {
          NotifyDescriptor.Confirmation descr = new DialogDescriptor.Confirmation(vfe.getLocalizedMessage() + "\n" + Bundle.LBL_ContinueValidation(), NotifyDescriptor.YES_NO_OPTION);
          Object result = DialogDisplayer.getDefault().notify(descr);
          if (result == NotifyDescriptor.YES_OPTION)
          {
            // Clear
            base.clearCheckSums();

            // Validate and Continue
            base.validate();
          }
          else
            throw vfe; //rethrow
        }

        pExecutor.accept(base);
      }
    }
  }

}
