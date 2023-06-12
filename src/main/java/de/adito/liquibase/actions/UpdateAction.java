package de.adito.liquibase.actions;

import de.adito.liquibase.internal.executors.ILiquibaseExecutorFacade;
import liquibase.exception.LiquibaseException;
import lombok.NonNull;
import org.openide.awt.*;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

import java.io.IOException;
import java.util.concurrent.CancellationException;

/**
 * Performs the "Update" Action in Liquibase
 *
 * @author w.glanzer, 10.08.2020
 */
@NbBundle.Messages("CTL_UpdateAction=Update...")
@ActionID(category = "Liquibase", id = "de.adito.liquibase.actions.UpdateAction")
@ActionRegistration(displayName = "#CTL_UpdateAction", lazy = false)
@ActionReference(path = "Plugins/Liquibase/Actions", position = 100, separatorAfter = 150)
public class UpdateAction extends AbstractLiquibaseAction
{

  @Override
  protected void performAction0(@NonNull Node[] pNodes) throws CancellationException, LiquibaseException, IOException
  {
    ILiquibaseExecutorFacade.INSTANCE.executeUpdate(getConnectionProvider(), getChangelogProvider());
  }

  @Override
  public String getName()
  {
    return Bundle.CTL_UpdateAction();
  }

}

