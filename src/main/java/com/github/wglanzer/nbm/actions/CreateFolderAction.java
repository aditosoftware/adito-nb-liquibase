package com.github.wglanzer.nbm.actions;

import com.github.wglanzer.nbm.liquibase.ILiquibaseProvider;
import com.github.wglanzer.nbm.util.Util;
import com.google.common.base.Strings;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.liquibase.LiquiConstants;
import info.clearthought.layout.TableLayout;
import org.jetbrains.annotations.NotNull;
import org.openide.*;
import org.openide.awt.*;
import org.openide.loaders.DataFolder;
import org.openide.nodes.Node;
import org.openide.util.*;
import org.openide.windows.TopComponent;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;


@ActionID(category = "Liquibase", id = "com.github.wglanzer.nbm.actions.CreateFolderAction")
@ActionRegistration(displayName = "--Create Folder", lazy = false)
@ActionReferences({
    @ActionReference(path = LiquiConstants.ACTION_REFERENCE, position = 1250),
})
public class CreateFolderAction extends AbstractLiquibaseAction
{
  @Override
  protected void performAction(Node[] activatedNodes)
  {
    perform();
  }

  @Override
  protected void execute(@NotNull ILiquibaseProvider pLiquibase) throws Exception
  {
    Node[] nodes = TopComponent.getRegistry().getActivatedNodes();
    if (nodes.length == 1)
    {
      DataFolder folder = nodes[0].getLookup().lookup(DataFolder.class);
      if (folder != null)
      {
        try
        {
          String liquibaseHome = Util.getFolderPath(nodes[0]);
          String input = _startDialog();
          input = Strings.nullToEmpty(input).trim();
          if (input.length() > 0)
            Files.createDirectory(new File(liquibaseHome, input).toPath());
        }
        catch (Exception pE)
        {
          getNotificationFacade().error(pE);
        }
      }
    }
  }

  private String _startDialog()
  {
    InputPanel panel = new InputPanel();
    DialogDescriptor desc = new DialogDescriptor(panel, getName());

    Dialog dialog = DialogDisplayer.getDefault().createDialog(desc);
    dialog.setVisible(true);
    dialog.dispose();

    if (desc.getValue() == DialogDescriptor.OK_OPTION)
      return panel.getInput();

    return null;
  }

  @Override
  protected boolean enable(Node[] activatedNodes)
  {
    Node[] nodes = TopComponent.getRegistry().getActivatedNodes();
    if (nodes.length == 1)
      return Util.getFolderPath(nodes[0]) != null;

    return false;
  }

  @Override
  public String getName()
  {
    return NbBundle.getMessage(CreateFolderAction.class, "Create_folder" )+ "...";
  }

  @Override
  public HelpCtx getHelpCtx()
  {
    return HelpCtx.DEFAULT_HELP;
  }

  private class InputPanel extends JPanel
  {

    private JTextField textField;

    InputPanel()
    {
      double fill = TableLayout.FILL;
      double pref = TableLayout.PREFERRED;
      double gap = 8;

      double[] cols = {gap, fill, gap};
      double[] rows = {gap,
                       pref,
                       4,
                       pref,
                       gap,
                       };


      setLayout(new TableLayout(cols, rows));
      add(new JLabel("--Enter new folder name"), "1,1");
      add(_createCheckBox(), "1,3");

      setPreferredSize(new Dimension(320, 80));
    }


    private JComponent _createCheckBox()
    {
      textField = new JTextField();
      return textField;
    }

    String getInput()
    {
      return textField.getText();
    }


  }

}
