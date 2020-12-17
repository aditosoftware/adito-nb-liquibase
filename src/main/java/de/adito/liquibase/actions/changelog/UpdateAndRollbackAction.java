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
 * Performs the "Update SQL" and the "Future Rollback SQL" Action in Liquibase
 *
 * @author s.seemann, 10.12.2020
 */
@NbBundle.Messages("CTL_UpdateAndRollbackAction=Generate Update And Future Rollback SQL ...")
@ActionID(category = "Liquibase", id = "de.adito.liquibase.actions.changelog.UpdateAndRollbackAction")
@ActionRegistration(displayName = "#CTL_UpdateAndRollbackAction", lazy = false)
@ActionReference(path = "Plugins/Liquibase/Changelog/Actions", position = 151)
public class UpdateAndRollbackAction extends AbstractLiquibaseAction
{
  @Override
  protected void performAction0(@NotNull Node[] pNodes) throws CancellationException, LiquibaseException, IOException
  {
    ILiquibaseExecutorFacade.INSTANCE.executeUpdateAndRollbackSQL(getConnectionProvider(), getChangelogProvider());
  }

  @Override
  public String getName()
  {
    return Bundle.CTL_UpdateAndRollbackAction();
  }

  @Override
  protected boolean enable0(@NotNull Node[] pNodes)
  {
    return true;
  }
}
