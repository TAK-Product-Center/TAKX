package gov.takx.core.view.gui.preferences;

import com.google.common.base.Strings;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.preferences.IUserPreferenceManager;
import gov.takx.api.ui.AProgramPreferencesPanel;
import gov.takx.commons.swing.AlwaysOnTopFileChooser;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.ErrorReportingSwingWorker;
import gov.takx.core.toolbar.ToolbarConfiguration;
import gov.takx.core.toolbar.ToolbarConfigurationManager;
import gov.takx.core.toolbar.ToolbarFileManager;
import gov.takx.core.view.gui.preferences.toolbar.IToolbarConfigurationDialogListener;
import gov.takx.core.view.gui.preferences.toolbar.ToolbarConfigurationDialog;
import gov.takx.core.view.gui.preferences.toolbar.ToolbarConfigurationListCellRenderer;
import gov.takx.core.view.gui.preferences.toolbar.ToolbarConfigurationListModel;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.toolbar.ToolbarButtonLabelLocation;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

/**
 * This panel allows the ability to customize the toolbar.  Toolbar configurations can be created, modified, and
 * selected for activation.
 *
 * @since 2.0
 */
@PseudoBean
public class UIConfigurationPreferencesPanel extends AProgramPreferencesPanel implements IToolbarConfigurationDialogListener
{
    @Inject
    private ToolbarFileManager fileManager;

    @Inject
    private ToolbarConfigurationManager toolbarConfigManager;

    @Inject
    private IUserPreferenceManager userPreferenceManager;

    private final IPreferenceMonitor<ToolbarButtonLabelLocation> toolbarLabelLocationMonitor =
            UserPreferenceEntry.TOOLBAR_BUTTON_LABEL_LOCATION.newMonitor(ToolbarButtonLabelLocation.class,
                    this::onToolbarLabelLocationChanged);

//    private final HashMap<String, String> skinMap = new HashMap<>();

    /**
     * Set of skins available through the core Substance library.
     */
//    private static final Class<?>[] SKINS = new Class[]
//            {
//                    DefaultRaptorSkin.class,
//                    AutumnSkin.class,
//                    BusinessBlackSteelSkin.class,
//                    BusinessBlueSteelSkin.class,
//                    ChallengerDeepSkin.class,
//                    CremeCoffeeSkin.class,
//                    CremeSkin.class,
//                    DustCoffeeSkin.class,
//                    DustSkin.class,
//                    EmeraldDuskSkin.class,
//                    MistAquaSkin.class,
//                    ModerateSkin.class,
//                    NebulaSkin.class,
//                    OfficeBlue2007Skin.class,
//                    OfficeSilver2007Skin.class,
//                    RavenSkin.class,
//                    SaharaSkin.class,
//                    TwilightSkin.class,
//                    GeminiSkin.class,
//                    GraphiteAquaSkin.class,
//                    GraphiteGlassSkin.class,
//                    GraphiteSkin.class,
//                    MagellanSkin.class,
//                    MarinerSkin.class,
//                    MistSilverSkin.class,
//                    OfficeBlack2007Skin.class
//            };

    /**
     * Tough names for the wimpy Substance skins.
     *
     * @since 1.13
     */
//    private static final Map<Class<?>, String> FRIENDLY_NAMES = new ImmutableMap.Builder<Class<?>, String>()
//            .put(DefaultRaptorSkin.class, "Default Raptor Skin")
//            .put(BusinessBlackSteelSkin.class, "Black Steel")
//            .put(BusinessBlueSteelSkin.class, "Blue Steel")
//            .put(ChallengerDeepSkin.class, "Dark Purple")
//            .put(CremeCoffeeSkin.class, "Tan on White")
//            .put(CremeSkin.class, "Light Blue on White")
//            .put(DustCoffeeSkin.class, "Brown on Tan")
//            .put(DustSkin.class, "Brown on White")
//            .put(EmeraldDuskSkin.class, "Dark Green")
//            .put(MistAquaSkin.class, "Dark Gray on Light Gray")
//            .put(ModerateSkin.class, "Blue on Gray")
//            .put(NebulaSkin.class, "Light Blue on Gray")
//            .put(OfficeBlue2007Skin.class, "Light Blue")
//            .put(OfficeSilver2007Skin.class, "All Gray")
//            .put(RavenSkin.class, "Brown on Black")
//            .put(SaharaSkin.class, "Green on White")
//            .put(TwilightSkin.class, "Black on Brown")
//            .put(AutumnSkin.class, "Orange")
//            .put(GeminiSkin.class, "Black on Blue-Gray")
//            .put(GraphiteAquaSkin.class, "Graphite with Blue Highlights")
//            .put(GraphiteGlassSkin.class, "Graphite Glass")
//            .put(GraphiteSkin.class, "Graphite")
//            .put(MagellanSkin.class, "All Blue")
//            .put(MarinerSkin.class, "Brown on Textured White")
//            .put(MistSilverSkin.class, "Light Gray")
//            .put(OfficeBlack2007Skin.class, "Black on Light Gray")
//            .build();

    private ToolbarConfiguration currentConfiguration;
//    private String currentSkin;

    private ToolbarConfigurationListModel configurationListModel;

    private AlwaysOnTopFileChooser fileChooser;

    public UIConfigurationPreferencesPanel()
    {
        initComponents();
    }

    @PostConstruct
    private void postConstruct()
    {
        // TODO KMB: For TAKX MVP1, we hid the skin changing functionality since none of the other skins look good with
        //  the new white icons. We either need to update the skins or get rid of the logic entirely. TAKX-739
//        initSkins();
//
//        String skin = UserPreferenceEntry.SKIN.getStringValue();
//        if (skin.isEmpty()) skin = DefaultRaptorSkin.class.getName();
//
//        skinComboBox.setSelectedItem(getSkinDisplayNameFromClass(skin));
        skinsPanel.setVisible(false);

        toolbarLabelLocationComboBox.setModel(new DefaultComboBoxModel<>(ToolbarButtonLabelLocation.values()));
        toolbarLabelLocationComboBox.setSelectedItem(toolbarLabelLocationMonitor.getCurrentValue());

        // Setup the file chooser which will be used for import/export
        fileChooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
        FileFilter xmlFileFilter = new FileNameExtensionFilter("XML File (.xml)", "xml");
        fileChooser.resetChoosableFileFilters();
        fileChooser.addChoosableFileFilter(xmlFileFilter);

        initConfigurations();

        createConfigButton.setText(null);
        createConfigButton.setIcon(UIResources.ADD_ICON);

        deleteConfigButton.setText(null);
        deleteConfigButton.setIcon(UIResources.DELETE_ICON);

        editConfigButton.setText(null);
        editConfigButton.setIcon(UIResources.EDIT_ICON);

        importConfigButton.setText(null);
        importConfigButton.setIcon(UIResources.IMPORT_ICON);

        exportConfigButton.setText(null);
        exportConfigButton.setIcon(UIResources.EXPORT_ICON);
    }

    @Override
    public boolean applyAction()
    {
//        String className = skinMap.get(skinComboBox.getSelectedItem());
//        String currentSkin = UserPreferenceEntry.SKIN.getStringValue();
//        if (className != null && !className.equals(currentSkin))
//        {
//            SubstanceLookAndFeel.setSkin(ClassLoaderUtil.createNewInstance(SubstanceSkin.class, className));
//            UserPreferenceEntry.SKIN.put(className);
//            SwingUtilities.updateComponentTreeUI(JFrame.getFrames()[0]);
//        }

//        final String selectedSkin = (String) skinComboBox.getSelectedItem();
//        if (!currentSkin.equals(selectedSkin))
//        {
//            String lafClassName = "Dark".equals(selectedSkin) ? FlatDarkLaf.class.getName() : FlatLightLaf.class.getName();
//
//            if (!lafClassName.equals(UIManager.getLookAndFeel().getClass().getName()))
//            {
//                SwingUtilities.invokeLater(() -> {
//                    try
//                    {
//                        FlatAnimatedLafChange.showSnapshot();
//
//                        // change look and feel
//                        UIManager.setLookAndFeel(lafClassName);
//
//                        // update all components
//                        FlatLaf.updateUI();
//                        FlatAnimatedLafChange.hideSnapshotWithAnimation();
//                        currentSkin = selectedSkin;
//                    } catch (Exception ex)
//                    {
//                        logger.error("Error changing look and feel to {}", lafClassName, ex);
//                    }
//                });
//            }
//        }

        ToolbarConfiguration selectedConfiguration = toolbarConfigList.getSelectedValue();
        if (currentConfiguration != selectedConfiguration)
        {
            toolbarConfigManager.activateConfiguration(selectedConfiguration);
            currentConfiguration = selectedConfiguration;
        }

        var selectedLabelLocation = (ToolbarButtonLabelLocation) toolbarLabelLocationComboBox.getSelectedItem();
        //noinspection ConstantConditions - selection won't ever be null
        UserPreferenceEntry.TOOLBAR_BUTTON_LABEL_LOCATION.put(selectedLabelLocation.name());

        return userPreferenceManager.savePreferences();
    }

    @Override
    public void restoreDefaults()
    {
//        skinComboBox.setSelectedItem(getSkinDisplayNameFromClass(DefaultRaptorSkin.class.getName()));
        toolbarConfigList.setSelectedIndex(0);
    }

    @Override
    public void releaseResources()
    {
    }

    /**
     * Adds the new configuration to the list model and selects it
     *
     * @param configuration The configuration that was added
     */
    @Override
    public void configurationAdded(@Nonnull ToolbarConfiguration configuration)
    {
        configurationListModel.addElement(configuration);
        toolbarConfigList.setSelectedValue(configuration, true);
    }

    /**
     * Removes the old configuration from the list model, adds the new one, and selects it
     *
     * @param oldConfigurationName The old configuration name (in case it changed)
     * @param newConfiguration The configuration that was modified
     */
    @Override
    public void configurationChanged(@Nonnull String oldConfigurationName, @Nonnull ToolbarConfiguration newConfiguration)
    {
        for (int i = 0; i < configurationListModel.getSize(); i++)
        {
            if (configurationListModel.get(i).getName().equals(oldConfigurationName))
            {
                configurationListModel.remove(i);
                break;
            }
        }

        configurationListModel.addElement(newConfiguration);
        toolbarConfigList.setSelectedValue(newConfiguration, true);
    }

//    private String getSkinDisplayNameFromClass(String skinClassName)
//    {
//        for (Map.Entry<String, String> skinEntry : skinMap.entrySet())
//        {
//            if (skinEntry.getValue().equals(skinClassName))
//            {
//                return skinEntry.getKey();
//            }
//        }
//        return null;
//    }

    private void initConfigurations()
    {
        ToolbarConfiguration defaultConfiguration = toolbarConfigManager.getDefaultConfiguration();
        boolean showDefaultConfiguration = fileManager.shouldDefaultConfigurationBeDisplayed();
        configurationListModel = new ToolbarConfigurationListModel(defaultConfiguration, showDefaultConfiguration);
        toolbarConfigManager.getNonDefaultConfigurations()
                .forEach(configurationListModel::addElement);

        toolbarConfigList.setModel(configurationListModel);

        // Set selection to the currently active configuration
        toolbarConfigList.setSelectedValue(toolbarConfigManager.getActiveConfiguration(), true);
        currentConfiguration = toolbarConfigList.getSelectedValue();
    }

//    private void initSkins()
//    {
//        // Check to see if the application has specified a subset of skins to make available.
//        final List<String> restrictedSkins = PreferenceEntry.SKINS_NODE.node().keys();
//        final boolean allowAll = restrictedSkins.isEmpty(); // If there are no skin restrictions, then allow all
//
//        for (Class<?> c : SKINS)
//        {
//            final String displayName = FRIENDLY_NAMES.get(c);
//
//            if (allowAll || restrictedSkins.contains(displayName))
//            {
//                // Map the display name to the class name.
//                skinMap.put(displayName, c.getName());
//                skinComboBox.addItem(displayName);
//            }
//        }
//    }

    private void createConfigButtonActionPerformed()
    {
        // New configurations use the default configuration as a starting point, but are deletable
        final var newConfiguration = new ToolbarConfiguration(toolbarConfigManager.getDefaultConfiguration());
        newConfiguration.setDeletable(true);

        new ToolbarConfigurationDialog((JDialog) getRootPane().getParent(), newConfiguration, true, this)
                .setVisible(true);
    }

    private void editConfigButtonActionPerformed()
    {
        ToolbarConfiguration selectedConfiguration = toolbarConfigList.getSelectedValue();

        // This should never be null since the edit button is disabled when nothing is selected, but it doesn't hurt to
        // double check.
        if (selectedConfiguration != null)
        {
            openEditDialog(selectedConfiguration);
        }
    }

    /**
     * Opens the {@link ToolbarConfigurationDialog} for editing the provided configuration.
     *
     * @param configuration The configuration to edit
     */
    private void openEditDialog(ToolbarConfiguration configuration)
    {
        // Create a copy of the selected configuration (so changes can easily be aborted)
        ToolbarConfigurationDialog configDialog = new ToolbarConfigurationDialog((JDialog) getRootPane().getParent(),
                new ToolbarConfiguration(configuration), false, this);
        configDialog.setVisible(true);
    }

    private void skinComboBoxActionPerformed()
    {
        setPanelDirty();
    }

    private void toolbarConfigListSelectionChanged()
    {
        ToolbarConfiguration selectedConfiguration = toolbarConfigList.getSelectedValue();

        // Edit button should only be enabled if an editable configuration is selected
        editConfigButton.setEnabled(selectedConfiguration != null && isEditable(selectedConfiguration));

        // Prevent deletion of configurations marked as not-deletable, including the default configuration
        deleteConfigButton.setEnabled(selectedConfiguration != null && selectedConfiguration.isDeletable());

        exportConfigButton.setEnabled(selectedConfiguration != null);

        setPanelDirty();
    }

    /**
     * Determines if the provided configuration is editable
     *
     * @param configuration The configuration to check
     * @return True if the configuration is editable
     */
    private boolean isEditable(ToolbarConfiguration configuration)
    {
        return !configuration.isDefault() && configuration.isEditable();
    }

    private void deleteConfigButtonActionPerformed()
    {
        deleteSelectedConfiguration();
    }

    /**
     * Deletes the selected configuration from TAKX (as long as deletion of the selected configuration is allowed).
     */
    private void deleteSelectedConfiguration()
    {
        ToolbarConfiguration selectedConfiguration = toolbarConfigList.getSelectedValue();

        if (!selectedConfiguration.isDefault())
        {
            configurationListModel.removeElement(selectedConfiguration);

            toolbarConfigManager.removeConfiguration(selectedConfiguration);

            ToolbarConfiguration defaultConfiguration = toolbarConfigManager.getDefaultConfiguration();
            toolbarConfigList.setSelectedValue(defaultConfiguration, true);
        }
    }

    private void exportConfigButtonActionPerformed()
    {
        ToolbarConfiguration selectedConfiguration = toolbarConfigList.getSelectedValue();
        fileChooser.setDialogTitle("Export toolbar configuration");
        String extension = ".xml";
        int returnValue = fileChooser.showSaveDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION)
        {
            File chooserFile = fileChooser.getSelectedFile();
            boolean addSuffix = !chooserFile.getAbsolutePath().toLowerCase().endsWith(extension);
            final File xmlFile = (addSuffix)
                    ? new File(chooserFile.getAbsolutePath() + extension)
                    : chooserFile;

            String filePath = xmlFile.getAbsolutePath();

            // AlwaysOnTopJFileChooser already overrides approveSelection to ask for overwrite.  Only need to ask
            // about overwriting if a suffix was added.
            if (addSuffix && xmlFile.exists())
            {
                Object[] options = {"Yes", "No"};
                int overwriteFile = AlwaysOnTopJOptionPane.showOptionDialog(fileChooser,
                        "A file already exists at " + filePath + ".\nAre you sure you want to overwrite it?",
                        "Confirm Overwrite", JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION, null, options, JOptionPane.NO_OPTION);

                if (overwriteFile != JOptionPane.YES_OPTION)
                {
                    return;
                }
            }

            // Use a swing worker to perform the export
            ErrorReportingSwingWorker<Boolean, Long> worker = new ErrorReportingSwingWorker<>("Toolbar Configuration Export",
                    "Failed to properly export a toolbar configuration")
            {
                @Override
                protected Boolean doInBackground()
                {
                    return toolbarConfigManager.exportConfiguration(filePath, selectedConfiguration);
                }

                @Override
                protected void onSuccess(Boolean result)
                {
                    if (result)
                    {
                        AlwaysOnTopJOptionPane.showMessageDialog(JFrame.getFrames()[0],
                                "Exported configuration to " + filePath,
                                "Toolbar Configuration Export", JOptionPane.INFORMATION_MESSAGE);
                    } else
                    {
                        AlwaysOnTopJOptionPane.showErrorDialog("Error exporting configuration to file",
                                "Toolbar Configuration Export");
                    }
                }
            };
            worker.execute();
        }
    }

    private void importConfigButtonActionPerformed()
    {
        fileChooser.setDialogTitle("Select toolbar configuration to import");
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION)
        {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();

            // Use a swing worker to perform the import
            ErrorReportingSwingWorker<String, Long> worker = new ErrorReportingSwingWorker<>("Toolbar Configuration Import",
                    "Failed to properly import a toolbar configuration")
            {
                @Override
                protected String doInBackground()
                {
                    try
                    {
                        ToolbarConfiguration configuration = toolbarConfigManager.importConfiguration(filePath);

                        if (configuration != null)
                        {
                            // Check to make sure there isn't already a configuration with that name
                            if (toolbarConfigManager.getConfiguration(configuration.getName()) != null)
                            {
                                return "A configuration already exists with the same name as the configuration in the file.";
                            }

                            toolbarConfigManager.addConfiguration(configuration);
                            configurationListModel.addElement(configuration);
                            toolbarConfigList.setSelectedValue(configuration, true);
                            return null;
                        }
                    } catch (IllegalArgumentException e)
                    {
                        return e.getMessage();
                    }

                    return "Read from file failed";
                }

                /**
                 * Notifies the user of the result of the import operation. If successful, the error message will be
                 * {@code null}.
                 *
                 * @param errorMessage The error message to display if a problem occurred
                 */
                @Override
                protected void onSuccess(String errorMessage)
                {
                    if (!Strings.isNullOrEmpty(errorMessage))
                    {
                        AlwaysOnTopJOptionPane.showErrorDialog("Error importing configuration from file: " + errorMessage,
                                "Toolbar Configuration Import");
                    }
                }
            };
            worker.execute();
        }
    }

    private void toolbarConfigListMouseClicked(MouseEvent mouseEvent)
    {
        // When an editable configuration is double-clicked, open the edit dialog
        if (mouseEvent.getClickCount() == 2)
        {
            ToolbarConfiguration selectedConfiguration = toolbarConfigList.getSelectedValue();

            if (selectedConfiguration != null && isEditable(selectedConfiguration))
            {
                openEditDialog(selectedConfiguration);
            }
        }
    }

    private void toolbarConfigListKeyReleased(KeyEvent e)
    {
        if (e.getKeyCode() == KeyEvent.VK_DELETE)
        {
            deleteSelectedConfiguration();
        }
    }

    private void toolbarLabelLocationComboBoxChanged()
    {
        setPanelDirty();
    }

    /**
     * Handles an update to the button label location preference by changing the selection in the combo box.
     *
     * @param labelLocation The new button label location
     * @since 3.4
     */
    @InvokeOnEDT
    private void onToolbarLabelLocationChanged(ToolbarButtonLabelLocation labelLocation)
    {
        toolbarLabelLocationComboBox.setSelectedItem(labelLocation);
    }

    /**
     * @since 5.2
     */
    private void notificationDurationSpinnerStateChanged()
    {
        setPanelDirty();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var toolbarConfigPanel = new JPanel();
        createConfigButton = new JButton();
        editConfigButton = new JButton();
        deleteConfigButton = new JButton();
        importConfigButton = new JButton();
        exportConfigButton = new JButton();
        var toolbarConfigListScrollPane = new JScrollPane();
        toolbarConfigList = new JList<>();
        toolbarConfigList.setCellRenderer(new ToolbarConfigurationListCellRenderer());
        var labelLocationPanel = new JPanel();
        var toolbarLabelLocationLabel = new JLabel();
        toolbarLabelLocationComboBox = new JComboBox<>();
        skinsPanel = new JPanel();
        var skinLabel = new JLabel();
        skinComboBox = new JComboBox<>();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "2*(default, $lgap)"));

        //======== toolbarConfigPanel ========
        {
            toolbarConfigPanel.setBorder(new CompoundBorder(
                    new TitledBorder(new TitledBorder("text"), "Toolbar Configuration"),
                    new EmptyBorder(3, 3, 3, 3)));
            toolbarConfigPanel.setName("toolbarConfigPanel");
            toolbarConfigPanel.setLayout(new FormLayout(
                    "5*(default, $lcgap), default:grow",
                    "default, $lgap, default:grow, $lgap, default"));

            //---- createConfigButton ----
            createConfigButton.setToolTipText("Create a new toolbar configuration");
            createConfigButton.setText("Add");
            createConfigButton.setName("createConfigButton");
            createConfigButton.addActionListener(e -> createConfigButtonActionPerformed());
            toolbarConfigPanel.add(createConfigButton, cc.xy(1, 1));

            //---- editConfigButton ----
            editConfigButton.setToolTipText("Edit selected toolbar configuration");
            editConfigButton.setEnabled(false);
            editConfigButton.setText("Edit");
            editConfigButton.setName("editConfigButton");
            editConfigButton.addActionListener(e -> editConfigButtonActionPerformed());
            toolbarConfigPanel.add(editConfigButton, cc.xy(3, 1));

            //---- deleteConfigButton ----
            deleteConfigButton.setText("Delete");
            deleteConfigButton.setToolTipText("Delete a toolbar configuration");
            deleteConfigButton.setName("deleteConfigButton");
            deleteConfigButton.addActionListener(e -> deleteConfigButtonActionPerformed());
            toolbarConfigPanel.add(deleteConfigButton, cc.xy(5, 1));

            //---- importConfigButton ----
            importConfigButton.setText("Import");
            importConfigButton.setToolTipText("Import a toolbar configuration from a file");
            importConfigButton.setName("importConfigButton");
            importConfigButton.addActionListener(e -> importConfigButtonActionPerformed());
            toolbarConfigPanel.add(importConfigButton, cc.xy(7, 1));

            //---- exportConfigButton ----
            exportConfigButton.setText("Export");
            exportConfigButton.setToolTipText("Export the selected toolbar configuration to a file");
            exportConfigButton.setEnabled(false);
            exportConfigButton.setName("exportConfigButton");
            exportConfigButton.addActionListener(e -> exportConfigButtonActionPerformed());
            toolbarConfigPanel.add(exportConfigButton, cc.xy(9, 1));

            //======== toolbarConfigListScrollPane ========
            {
                toolbarConfigListScrollPane.setName("toolbarConfigListScrollPane");

                //---- toolbarConfigList ----
                toolbarConfigList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                toolbarConfigList.setName("toolbarConfigList");
                toolbarConfigList.addListSelectionListener(e -> toolbarConfigListSelectionChanged());
                toolbarConfigList.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mouseClicked(MouseEvent e)
                    {
                        toolbarConfigListMouseClicked(e);
                    }
                });
                toolbarConfigList.addKeyListener(new KeyAdapter()
                {
                    @Override
                    public void keyReleased(KeyEvent e)
                    {
                        toolbarConfigListKeyReleased(e);
                    }
                });
                toolbarConfigListScrollPane.setViewportView(toolbarConfigList);
            }
            toolbarConfigPanel.add(toolbarConfigListScrollPane, cc.xywh(1, 3, 11, 1));

            //======== labelLocationPanel ========
            {
                labelLocationPanel.setName("labelLocationPanel");
                labelLocationPanel.setLayout(new FormLayout(
                        "100dlu, $lcgap, left:default:grow",
                        "default"));

                //---- toolbarLabelLocationLabel ----
                toolbarLabelLocationLabel.setText("Toolbar button label location");
                toolbarLabelLocationLabel.setName("toolbarLabelLocationLabel");
                labelLocationPanel.add(toolbarLabelLocationLabel, cc.xy(1, 1));

                //---- toolbarLabelLocationComboBox ----
                toolbarLabelLocationComboBox.setName("toolbarLabelLocationComboBox");
                toolbarLabelLocationComboBox.addActionListener(e -> toolbarLabelLocationComboBoxChanged());
                labelLocationPanel.add(toolbarLabelLocationComboBox, cc.xy(3, 1));
            }
            toolbarConfigPanel.add(labelLocationPanel, cc.xywh(1, 5, 11, 1));
        }
        add(toolbarConfigPanel, cc.xy(1, 1));

        //======== skinsPanel ========
        {
            skinsPanel.setBorder(new TitledBorder("Skins"));
            skinsPanel.setName("skinsPanel");
            skinsPanel.setLayout(new FormLayout(
                    "100dlu, $lcgap, default:grow",
                    "default"));

            //---- skinLabel ----
            skinLabel.setText("Skin");
            skinLabel.setName("skinLabel");
            skinsPanel.add(skinLabel, cc.xy(1, 1));

            //---- skinComboBox ----
            skinComboBox.setModel(new DefaultComboBoxModel<>(new String[]{
                    "Dark",
                    "Light"
            }));
            skinComboBox.setName("skinComboBox");
            skinComboBox.addActionListener(e -> skinComboBoxActionPerformed());
            skinsPanel.add(skinComboBox, cc.xy(3, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));
        }
        add(skinsPanel, cc.xy(1, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JButton createConfigButton;
    private JButton editConfigButton;
    private JButton deleteConfigButton;
    private JButton importConfigButton;
    private JButton exportConfigButton;
    private JList<ToolbarConfiguration> toolbarConfigList;
    protected JComboBox<ToolbarButtonLabelLocation> toolbarLabelLocationComboBox;
    private JPanel skinsPanel;
    protected JComboBox<String> skinComboBox;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
