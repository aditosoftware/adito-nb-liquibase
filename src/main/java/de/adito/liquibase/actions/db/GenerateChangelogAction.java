package de.adito.liquibase.actions.db;

import de.adito.liquibase.actions.AbstractLiquibaseAction;
import de.adito.liquibase.internal.executors.ILiquibaseExecutorFacade;
import liquibase.exception.LiquibaseException;
import org.jetbrains.annotations.NotNull;
import org.netbeans.modules.db.explorer.DatabaseConnection;
import org.netbeans.modules.db.explorer.node.TableNode;
import org.openide.awt.*;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

import java.io.IOException;
import java.sql.Connection;
import java.util.concurrent.CancellationException;

/**
 * Performs the "Generate Changelog" Action in Liquibase
 *
 * @author s.seemann, 22.12.2020
 */
@NbBundle.Messages("CTL_GenerateChangelog=Generate Changelog...")
@ActionID(id = "de.adito.liquibase.actions.db.GenerateChangelogAction", category = "Database")
@ActionRegistration(displayName = "#CTL_GenerateChangelog", lazy = false)
@ActionReferences(value = {
    @ActionReference(path = "Databases/Explorer/Table/Actions", position = 127, separatorBefore = 126)
})
public class GenerateChangelogAction extends AbstractLiquibaseAction
{

  @Override
  protected boolean enable0(@NotNull Node[] pNodes)
  {
    return pNodes.length == 1;
  }

  @Override
  public String getName()
  {
    return NbBundle.getMessage(GenerateChangelogAction.class, "CTL_GenerateChangelog");
  }

  @Override
  protected void performAction0(@NotNull Node[] pActivatedNodes) throws CancellationException, LiquibaseException, IOException
  {
    Node node = pActivatedNodes[0];
    TableNode tabNode = node.getLookup().lookup(TableNode.class);
    Connection con = node.getLookup().lookup(DatabaseConnection.class).getJDBCConnection();
    if (con != null && tabNode != null)
      ILiquibaseExecutorFacade.INSTANCE.executeGenerateChangelog(con, tabNode.getName(), _getRootNodeName(node));
  }

  private String _getRootNodeName(@NotNull Node pNode)
  {
    Node parent = pNode.getParentNode();
    if (parent == null)
      return pNode.getDisplayName();
    return _getRootNodeName(parent);
  }

  @Override
  protected boolean asynchronous()
  {
    return false;
  }
}
