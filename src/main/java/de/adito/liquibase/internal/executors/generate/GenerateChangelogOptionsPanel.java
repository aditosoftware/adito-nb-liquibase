package de.adito.liquibase.internal.executors.generate;

import de.adito.liquibase.nb.LiquibaseFolderService;
import info.clearthought.layout.TableLayout;
import org.jetbrains.annotations.*;
import org.netbeans.api.project.Project;
import org.openide.*;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Panel with user interaction for the GENERATE CHANGELOG command
 *
 * @author s.seemann, 22.12.2020
 */
public class GenerateChangelogOptionsPanel extends JPanel
{
  private JTextField pathTxt;
  private JTextField changelogNameTxt;
  private JTextField authorTxt;
  private final JCheckBox catalogChbx;
  private final JCheckBox schemaChbx;
  private DialogDescriptor desc;

  private final String subfolderName;
  private final List<JCheckBox> chbxGenerateTypes = new ArrayList<>();

  @NbBundle.Messages({
      "LBL_ChbxCatalog=Include Catalog",
      "LBL_ChbxSchema=Include Schema",
      "LBL_PathChangelog=Path",
      "LBL_NameChangelog=ChangeLog Name",
      "LBL_AuthorChangelog=Author",
      "LBL_Types=Types"
  })
  public GenerateChangelogOptionsPanel(@Nullable String pSubfolderName)
  {
    subfolderName = pSubfolderName;

    double fill = TableLayout.FILL;
    double pref = TableLayout.PREFERRED;
    double gap = 8;

    double[] cols = {gap, pref, gap, fill, gap, pref, gap};
    double[] rows = {gap, pref, gap, pref, gap, pref, gap, pref, gap, pref, gap, pref, fill};

    catalogChbx = new JCheckBox(Bundle.LBL_ChbxCatalog());
    schemaChbx = new JCheckBox(Bundle.LBL_ChbxSchema());

    setLayout(new TableLayout(cols, rows));
    add(new JLabel(Bundle.LBL_PathChangelog()), "1,1");
    add(_createPathTextfield(), "3,1");
    add(_createFileChooser(), "5,1");

    add(new JLabel(Bundle.LBL_NameChangelog()), "1,3");
    add(_createChangeLogNameTextfield(), "3,3");

    add(new JLabel(Bundle.LBL_AuthorChangelog()), "1,5");
    add(_createAuthorTextfield(), "3,5");

    add(catalogChbx, "3,7");
    add(schemaChbx, "3,9");

    add(new JLabel(Bundle.LBL_Types()), "1,11");
    add(_createTypeChbxs(), "3,11");

    setPreferredSize(new Dimension(800, (int) getPreferredSize().getHeight()));
  }

  private JComponent _createTypeChbxs()
  {
    JPanel container = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

    Arrays.stream(EGenerateType.values()).forEach(pType -> {
      JCheckBox chbx = new JCheckBox(pType.getDisplayName());
      chbx.setSelected(pType.isDefaultType());
      chbx.setName(pType.name());
      chbx.addActionListener(evt -> _validate());

      container.add(chbx);
      chbxGenerateTypes.add(chbx);
    });

    return container;
  }

  private JComponent _createPathTextfield()
  {
    pathTxt = new JTextField();
    pathTxt.setEditable(false);
    _addValidationListener(pathTxt);

    Project project = TopComponent.getRegistry().getActivated().getLookup().lookup(Project.class);
    if (project != null)
      LiquibaseFolderService.getInstance(project).observeLiquibaseFolder()
          .blockingFirst()
          .ifPresent(pFileObject -> {
            String path = FileUtil.toFile(pFileObject).getPath();
            if (subfolderName != null)
              path = path + File.separator + subfolderName;
            pathTxt.setText(path);
          });

    return pathTxt;
  }

  private JComponent _createChangeLogNameTextfield()
  {
    changelogNameTxt = new JTextField();
    _addValidationListener(changelogNameTxt);
    return changelogNameTxt;
  }

  private JComponent _createAuthorTextfield()
  {
    authorTxt = new JTextField();
    authorTxt.setText(System.getProperty("user.name"));
    return authorTxt;
  }

  @NbBundle.Messages({
      "LBL_ButtonBrowse=Browse",
      "LBL_TitleFileChooser=Select Path for Changelog"
  })
  private JComponent _createFileChooser()
  {
    JButton browse = new JButton(Bundle.LBL_ButtonBrowse());

    browse.addActionListener(e -> {
      JFileChooser chooser = new JFileChooser(pathTxt.getText());
      chooser.setDialogTitle(Bundle.LBL_TitleFileChooser());
      chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      int option = chooser.showOpenDialog(this);
      if (option == JFileChooser.APPROVE_OPTION)
      {
        File selectedFile = chooser.getSelectedFile();
        pathTxt.setText(selectedFile.getPath());
      }
    });

    return browse;
  }

  private void _addValidationListener(@NotNull JTextField pTxtField)
  {
    pTxtField.getDocument().addDocumentListener(new DocumentListener()
    {
      @Override
      public void insertUpdate(DocumentEvent e)
      {
        _validate();
      }

      @Override
      public void removeUpdate(DocumentEvent e)
      {
        _validate();
      }

      @Override
      public void changedUpdate(DocumentEvent e)
      {
        _validate();
      }
    });
  }

  private void _validate()
  {
    if (desc == null)
      return;

    String path = pathTxt.getText();
    String name = changelogNameTxt.getText();
    if ("".equals(path) || "".equals(name) || getGenerateTypes().isEmpty())
    {
      desc.setValid(false);
      return;
    }

    name = _modifyChangelogName(name);

    File f = new File(path + File.separatorChar + name);
    try
    {
      //noinspection ResultOfMethodCallIgnored
      f.getCanonicalPath();

      desc.setValid(!f.exists());
    }
    catch (IOException e)
    {
      desc.setValid(false);
    }
  }

  private String _modifyChangelogName(@NotNull String pName)
  {
    if (!pName.endsWith(".xml"))
      pName = pName + ".xml";

    return pName;
  }

  @NbBundle.Messages({
      "LBL_TitleDialog=Generate ChangeLog for "
  })
  public boolean showDialog(@NotNull String pTableName)
  {
    desc = new DialogDescriptor(this, Bundle.LBL_TitleDialog() + "\"" + pTableName + "\"");

    _validate();
    Dialog dialog = DialogDisplayer.getDefault().createDialog(desc);
    dialog.pack();
    dialog.setMinimumSize(dialog.getSize());
    dialog.setVisible(true);
    dialog.dispose();

    return desc.getValue() == DialogDescriptor.OK_OPTION;
  }

  /**
   * Returns the full path to the changelog-file. The filename ends with ".xml"
   *
   * @return the full path
   */
  public String getPath()
  {
    String name = changelogNameTxt.getText();
    name = _modifyChangelogName(name);

    String path = pathTxt.getText();
    return path + File.separatorChar + name;
  }

  public boolean isCatalogIncluded()
  {
    return catalogChbx.isSelected();
  }

  public boolean isSchemaIncluded()
  {
    return schemaChbx.isSelected();
  }

  public String getAuthor()
  {
    return authorTxt.getText();
  }

  public List<EGenerateType> getGenerateTypes()
  {
    return chbxGenerateTypes.stream()
        .filter(AbstractButton::isSelected)
        .map(Component::getName)
        .map(EGenerateType::valueOf)
        .collect(Collectors.toList());
  }
}
