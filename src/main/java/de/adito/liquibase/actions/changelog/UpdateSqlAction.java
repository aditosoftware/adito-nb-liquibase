package de.adito.liquibase.actions.changelog;

import de.adito.liquibase.actions.AbstractLiquibaseAction;
import de.adito.liquibase.internal.executors.ILiquibaseExecutorFacade;
import liquibase.exception.LiquibaseException;
import org.jetbrains.annotations.NotNull;
import org.openide.awt.*;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

import java.io.IOException;
import java.util.concurrent.CancellationException;

/**
 * Performs the "Update SQL" Action in Liquibase
 *
 * @author s.seemann, 09.12.2020
 */
@NbBundle.Messages("CTL_UpdateSqlAction=Generate Update SQL ...")
@ActionID(category = "Liquibase", id = "de.adito.liquibase.actions.changelog.UpdateSqlAction")
@ActionRegistration(displayName = "#CTL_UpdateSqlAction", lazy = false)
@ActionReference(path = "Plugins/Liquibase/Changelog/Actions", position = 148)
public class UpdateSqlAction extends AbstractLiquibaseAction
{
  @Override
  protected void performAction0(@NotNull Node[] pNodes) throws CancellationException, LiquibaseException, IOException
  {
    ILiquibaseExecutorFacade.INSTANCE.executeUpdateSQL(getConnectionProvider(), getChangelogProvider());
  }

  @Override
  public String getName()
  {
    return Bundle.CTL_UpdateSqlAction();
  }

  @Override
  protected boolean enable0(@NotNull Node[] pNodes)
  {
    return true;
  }
}
