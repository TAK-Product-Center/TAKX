package gov.takx.core.view.gui.maps;

import com.atakmap.coremap.locale.LocaleUtil;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.nasa.worldwind.avlist.AVKey;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.PlaceholderTextField;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.globe.imagerywizard.FileSet;
import gov.takx.core.globe.imagerywizard.FileSetFilter;
import gov.takx.core.globe.imagerywizard.FileSetTable;
import gov.takx.core.globe.imagerywizard.InstallConstants;
import gov.takx.core.view.gui.DialogHeaderPanel;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dialog for loading local imagery or elevation files so they can be added as a layer on the globe.
 *
 * @since 3.4.1 (renamed from ConvertMapsDialog)
 */
public abstract class ALoadMapFilesDialog extends AStandardDialog
{
    private final MapFilesSelection mapFilesSelection;
    private final boolean supportSelection;
    private String currentLayerName;

    /**
     * Constructor for displaying the dialog for map files that were dragged and dropped onto the globe (or for
     * selecting them manually via the dialog).
     *
     * @param files The map files being loaded
     */
    protected ALoadMapFilesDialog(boolean supportSelection, @Nonnull String typeString, @Nonnull String iconName, @Nonnull String description,
                                  FileSetFilter fileSetFilter, FileFilter extensionFilter, File... files)
    {
        this.supportSelection = supportSelection;
        String filesString = supportSelection ? "file(s) or " : "";

        initComponents();
        setTitle("Load " + typeString + " Files");
        dialogHeaderPanel.setDescription(description);
        dialogHeaderPanel.setIcon(iconName);
        selectInstructionsLabel.setText("Select " + filesString + "directory to search for " + typeString.toLowerCase(LocaleUtil.getCurrent()) + " files.");
        selectedFilesTextField.setPlaceholderText("Click browse to select " + filesString + "a directory...");

        // Add the FileSetTable to this dialog
        FileSetTable mapsSelectionTable = new FileSetTable(supportSelection);
        tablePanel.add(new JScrollPane(mapsSelectionTable), new CellConstraints(1, 1));

        mapFilesSelection = new MapFilesSelection(fileSetFilter, this, mapsSelectionTable, extensionFilter);
        mapsSelectionTable.getModel().addTableModelListener(e -> {
            updateLayerNameForSelection();
            connectButton.setEnabled(!mapFilesSelection.getSelectedFileSets().isEmpty());
        });

        if (files != null)
        {
            List<File> filesToConnect = new ArrayList<>();

            for (File f : files)
            {
                if (f.isDirectory() && files.length > 1) // No need to split out a single directory.
                {
                    File[] subFiles = f.listFiles();

                    if (subFiles != null)
                    {
                        Collections.addAll(filesToConnect, subFiles);
                    }
                } else
                {
                    filesToConnect.add(f);
                }
            }

            mergeCheck.setVisible(files.length > 1);

            String path = mapFilesSelection.doFileCheck(filesToConnect.toArray(new File[0]));
            selectedFilesTextField.setText(path);
            updateLayerNameForSelection();
            connectButton.setEnabled(true);
        }

        setModal(false);
    }

    /**
     * Sets the name for the layer created from the selected files.
     */
    private void updateLayerNameForSelection()
    {
        // Only update the layer name if the user hasn't modified it so we don't accidentally override the user entry.
        final String layerName = layerNameTextField.getText();
        if (layerName.isEmpty() || layerName.equals(currentLayerName))
        {
            currentLayerName = getDefaultLayerName();

            layerNameTextField.setText(currentLayerName);
        }
    }

    /**
     * @return The default name for the layer created when importing the selected files. For a single file, it will be
     * the file name. For multiple files, it will be the parent directory.
     */
    @Nonnull
    private String getDefaultLayerName()
    {
        final List<FileSet> selectedFileSets = mapFilesSelection.getSelectedFileSets();

        if (selectedFileSets.size() == 1)
        {
            return (String) selectedFileSets.get(0).getValue(AVKey.TITLE);
        } else
        {
            return FileUtils.extractLastWordFromPath(mapFilesSelection.getSelectionPath());
        }
    }

    /**
     * Perform the import for the selected map files.
     * @param selectedFileSets Selected file sets to import.
     * @param wizardDataMap Data map containing additional import properties.
     * @since 5.7 (signature change)
     */
    protected abstract void doImport(@Nonnull List<FileSet> selectedFileSets, @Nonnull Map<String, Object> wizardDataMap);

    private void cancelButtonActionPerformed()
    {
        mapFilesSelection.cancelFileSetUpdate();
        setVisible(false);
        dispose();
    }

    private void connectButtonActionPerformed()
    {
        String layerName = layerNameTextField.getText();
        if (StringUtils.isBlank(layerName)) layerName = getDefaultLayerName();

        List<FileSet> selectedFileSets = mapFilesSelection.getSelectedFileSets();
        Map<String, Object> wizardDataMap = mapFilesSelection.getWizardDataMap();

        if (selectedFileSets.size() == 1)
        {
            importSingleFileSet(selectedFileSets.get(0), layerName, wizardDataMap);
        } else if (mergeCheck.isSelected())
        {
            importMergedFileSets(layerName, wizardDataMap);
        } else
        {
            importFileSetsSeparately(selectedFileSets, wizardDataMap);
        }

        setVisible(false);
        dispose();
    }

    /**
     * Import a single fileset with the given layer name and properties.
     * @since 5.7
     */
    private void importSingleFileSet(@Nonnull FileSet fileSet, @Nonnull String layerName, @Nonnull Map<String, Object> wizardDataMap)
    {
        // Assign display name with given layer name
        fileSet.setValue(AVKey.DISPLAY_NAME, layerName);
        doImport(List.of(fileSet), wizardDataMap);
    }

    /**
     * Import multiple filesets from {@link #mapFilesSelection} with the given layer name and properties as a single merged layer.
     * @since 5.7
     */
    private void importMergedFileSets(@Nonnull String layerName, @Nonnull Map<String, Object> wizardDataMap)
    {
        // Merge file set information
        mapFilesSelection.mergeSelectedFileSets(layerName);

        List<FileSet> mergedFileSets = mapFilesSelection.getSelectedFileSets();
        if (!mergedFileSets.isEmpty())
        {
            FileSet fileSet = mergedFileSets.get(0);
            importSingleFileSet(fileSet, layerName, wizardDataMap);
        }
    }

    /**
     * Import multiple filesets with the given properties using default layer naming conventions.
     * @since 5.7
     */
    private void importFileSetsSeparately(@Nonnull List<FileSet> fileSets, @Nonnull Map<String, Object> wizardDataMap)
    {
        for (FileSet fileSet : fileSets)
        {
            List<FileSet> singleSetList = List.of(fileSet);

            Map<String, Object> singleDataMap = new HashMap<>(wizardDataMap);
            singleDataMap.put(InstallConstants.FILE_SETS, singleSetList);

            doImport(singleSetList, singleDataMap);
        }
    }

    private void browseButtonActionPerformed()
    {
        String path = mapFilesSelection.browseLocalPath(!supportSelection);
        if (path != null)
        {
            selectedFilesTextField.setText(path);
            connectButton.setEnabled(true);
            updateLayerNameForSelection();
        }
    }

    /**
     * Called when {@link #mergeCheck} is toggled.
     * @since 5.7
     */
    private void mergeCheckStateChanged()
    {
        // When merging is disabled, each layer source will create a layer with default naming conventions.
        boolean doMerge = mergeCheck.isSelected();
        layerNameLabel.setEnabled(doMerge);
        layerNameTextField.setEnabled(doMerge);
    }

    @SuppressWarnings("DuplicatedCode")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        selectInstructionsLabel = new JLabel();
        var panel2 = new JPanel();
        selectedFilesTextField = new PlaceholderTextField();
        var browseButton = new JButton();
        var separator2 = new JSeparator();
        tablePanel = new JPanel();
        var mergePanel = new JPanel();
        layerNameLabel = new JLabel();
        layerNameTextField = new JTextField();
        mergeCheck = new JCheckBox();
        var separator3 = new JSeparator();
        var buttonBar = new JPanel();
        connectButton = new JButton();
        var cancelButton = new JButton();
        dialogHeaderPanel = new DialogHeaderPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default:grow",
                        "2*(default, $lgap), $rgap, $lgap, default:grow, $lgap, default, $lgap, $rgap"));

                //---- selectInstructionsLabel ----
                selectInstructionsLabel.setText("Select file(s) or directory to search for imagery files.");
                selectInstructionsLabel.setName("selectInstructionsLabel");
                contentPanel.add(selectInstructionsLabel, cc.xy(1, 1));

                //======== panel2 ========
                {
                    panel2.setName("panel2");
                    panel2.setLayout(new FormLayout(
                            "default:grow, $lcgap, default",
                            "default"));

                    //---- selectedFilesTextField ----
                    selectedFilesTextField.setEditable(false);
                    selectedFilesTextField.setPlaceholderText("Click browse to select file(s) or a directory...");
                    selectedFilesTextField.setName("selectedFilesTextField");
                    panel2.add(selectedFilesTextField, cc.xy(1, 1));

                    //---- browseButton ----
                    browseButton.setText("Browse");
                    browseButton.setName("browseButton");
                    browseButton.addActionListener(e -> browseButtonActionPerformed());
                    panel2.add(browseButton, cc.xy(3, 1));
                }
                contentPanel.add(panel2, cc.xy(1, 3));

                //---- separator2 ----
                separator2.setName("separator2");
                contentPanel.add(separator2, cc.xy(1, 5));

                //======== tablePanel ========
                {
                    tablePanel.setName("tablePanel");
                    tablePanel.setLayout(new FormLayout(
                            "default:grow",
                            "default:grow"));
                }
                contentPanel.add(tablePanel, cc.xy(1, 7, CellConstraints.DEFAULT, CellConstraints.FILL));

                //======== mergePanel ========
                {
                    mergePanel.setName("mergePanel");
                    mergePanel.setLayout(new FormLayout(
                            "default, $lcgap, default:grow, default",
                            "default"));

                    //---- layerNameLabel ----
                    layerNameLabel.setText("Name for new layer:");
                    layerNameLabel.setName("layerNameLabel");
                    mergePanel.add(layerNameLabel, cc.xy(1, 1));

                    //---- layerNameTextField ----
                    layerNameTextField.setName("layerNameTextField");
                    mergePanel.add(layerNameTextField, cc.xy(3, 1));

                    //---- mergeCheck ----
                    mergeCheck.setText("Merge");
                    mergeCheck.setSelected(true);
                    mergeCheck.setName("mergeCheck");
                    mergeCheck.addChangeListener(e -> mergeCheckStateChanged());
                    mergePanel.add(mergeCheck, cc.xy(4, 1));
                }
                contentPanel.add(mergePanel, cc.xy(1, 9));

                //---- separator3 ----
                separator3.setName("separator3");
                contentPanel.add(separator3, cc.xy(1, 11));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button",
                        "pref"));

                //---- connectButton ----
                connectButton.setText("Connect");
                connectButton.setEnabled(false);
                connectButton.setName("connectButton");
                connectButton.addActionListener(e -> connectButtonActionPerformed());
                buttonBar.add(connectButton, cc.xy(2, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonBar.add(cancelButton, cc.xy(4, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);

            //---- dialogHeaderPanel ----
            dialogHeaderPanel.setName("dialogHeaderPanel");
            dialogPane.add(dialogHeaderPanel, BorderLayout.NORTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel selectInstructionsLabel;
    private PlaceholderTextField selectedFilesTextField;
    private JPanel tablePanel;
    private JLabel layerNameLabel;
    private JTextField layerNameTextField;
    private JCheckBox mergeCheck;
    private JButton connectButton;
    private DialogHeaderPanel dialogHeaderPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
