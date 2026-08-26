package gov.takx.core.view.gui.properties;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.globe.IProjectLabel;
import gov.takx.api.globe.IProjectLabelService;
import gov.takx.api.globe.ProjectLabelLocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.project.IProject;
import gov.takx.api.project.IProjectManager;
import gov.takx.api.ui.AProgramPreferencesPanel;
import gov.takx.commons.file.FileUtils;
import gov.takx.core.view.gui.screenimageutils.ProjectSnapshotService;
import gov.takx.platform.preferences.ProjectPreferenceEntry;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import net.java.dev.colorchooser.ColorChooser;
import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXEditorPane;
import org.jdesktop.swingx.JXImageView;
import org.jdesktop.swingx.JXTable;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static gov.takx.api.globe.ProjectLabelLocation.LOWER_CENTER;
import static gov.takx.api.globe.ProjectLabelLocation.LOWER_LEFT;
import static gov.takx.api.globe.ProjectLabelLocation.LOWER_RIGHT;
import static gov.takx.api.globe.ProjectLabelLocation.UPPER_CENTER;
import static gov.takx.api.globe.ProjectLabelLocation.UPPER_LEFT;
import static gov.takx.api.globe.ProjectLabelLocation.UPPER_RIGHT;

/**
 * ProjectSettingsPanel class. This is a panel provided through the app frame so that the user may edit and
 * view various project properties as well as the project snapshot.
 *
 */
@PseudoBean
public class ProjectSettingsPanel extends AProgramPreferencesPanel
{
    private final static int IMAGE_PANEL_WIDTH_PX = 340;
    private final static int IMAGE_PANEL_HEIGHT_PX = 240;
    private String origTitle;
    private String title;
    private String origDesc;
    private String desc;
    private String origLocation;
    private String location;
    private String origNotes;
    private String notes;
    private Long origStartDate;
    private Long startDate;
    private Long origEndDate;
    private Long endDate;
    private BufferedImage noSnapshotImage;
    private final List<ProjectLabelConfig> projectLabelConfigs = new ArrayList<>();
    private boolean origProjectLabelsVisibility;

    @Inject
    private IProjectLabelService labelService;

    @Inject
    private IProjectManager projectManager;

    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    private ProjectSnapshotService snapshotService;

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager projectVFSManager;

    public ProjectSettingsPanel()
    {
        initComponents();
    }

    @PostConstruct
    private void postConstruct()
    {
        initializeFields();

        noSnapshotImage = snapshotService.makeCaptionImage(false, IMAGE_PANEL_WIDTH_PX, IMAGE_PANEL_HEIGHT_PX);

        projectLabelConfigs.add(new ProjectLabelConfig(UPPER_CENTER, upperCenterTextField, upperCenterCheckBox, upperCenterColorChooser, upperCenterSizeSpinner));
        projectLabelConfigs.add(new ProjectLabelConfig(LOWER_CENTER, lowerCenterTextField, lowerCenterCheckBox, lowerCenterColorChooser, lowerCenterSizeSpinner));
        projectLabelConfigs.add(new ProjectLabelConfig(UPPER_RIGHT, upperRightTextField, upperRightCheckBox, upperRightColorChooser, upperRightSizeSpinner));
        projectLabelConfigs.add(new ProjectLabelConfig(UPPER_LEFT, upperLeftTextField, upperLeftCheckBox, upperLeftColorChooser, upperLeftSizeSpinner));
        projectLabelConfigs.add(new ProjectLabelConfig(LOWER_RIGHT, lowerRightTextField, lowerRightCheckBox, lowerRightColorChooser, lowerRightSizeSpinner));
        projectLabelConfigs.add(new ProjectLabelConfig(LOWER_LEFT, lowerLeftTextField, lowerLeftCheckBox, lowerLeftColorChooser, lowerLeftSizeSpinner));

        projectLayerCheckBox.setSelected(labelService.isProjectLabelLayerVisible());
        origProjectLabelsVisibility = projectLayerCheckBox.isSelected();
        enableProjectLabels(origProjectLabelsVisibility);
    }

    private void resetProjectLabelValues()
    {
        projectLabelConfigs.forEach(ProjectLabelConfig::resetComponentsToOrigProjectValues);
    }

    /**
     * Updates to start or end date, mark panel as dirty.
     */
    private void dateFieldActionPerformed()
    {
        setPanelDirty();
    }

    /**
     * When any of the editable text fields change, mark the panel as dirty.
     */
    private void textFieldKeyReleased()
    {
        setPanelDirty();
    }

    /**
     * Updates any changed values in the project property file and saves the file if anything was updated.
     */
    @Override
    public boolean applyAction()
    {
        boolean prefsUpdated = false;

        // Extract all the form data

        startDate = startDateField.getDate().getTime();
        endDate = endDateField.getDate().getTime();
        title = titleTextField.getText();
        desc = descriptionTextField.getText();
        location = locationTextField.getText();
        notes = notesTextField.getText();

        if (!origTitle.equals(title))
        {
            ProjectPreferenceEntry.TITLE.put(title);
            prefsUpdated = true;
        }
        if (!Objects.equals(origStartDate, startDate))
        {
            ProjectPreferenceEntry.START_DATE.put(startDate);
            prefsUpdated = true;
        }
        if (!Objects.equals(origEndDate, endDate))
        {
            ProjectPreferenceEntry.END_DATE.put(endDate);
            prefsUpdated = true;
        }
        if (!origDesc.equals(desc))
        {
            ProjectPreferenceEntry.DESCRIPTION.put(desc);
            prefsUpdated = true;
        }
        if (!origLocation.equals(location))
        {
            ProjectPreferenceEntry.LOCATION.put(location);
            prefsUpdated = true;
        }
        if (!origNotes.equals(notes))
        {
            ProjectPreferenceEntry.NOTES.put(notes);
            prefsUpdated = true;
        }

        // project label settings
        if (projectLabelsHaveChanged())
        {
            applyProjectLabelChanges();
            prefsUpdated = true;
        }

        if (origProjectLabelsVisibility != projectLayerCheckBox.isSelected())
        {
            origProjectLabelsVisibility = projectLayerCheckBox.isSelected();
            labelService.setProjectLabelLayerVisible(origProjectLabelsVisibility);
            prefsUpdated = true;
        }

        if (prefsUpdated) projectManager.getCurrentOpenProject().saveProjectPreferences();

        return true;
    }

    private boolean projectLabelsHaveChanged()
    {
        return projectLabelConfigs.stream().anyMatch(ProjectLabelConfig::hasChanged);
    }

    private void applyProjectLabelChanges()
    {
        projectLabelConfigs.forEach(ProjectLabelConfig::applyChanges);
    }

    /**
     * Restores the defaults of the panel by reinitializing all the fields to the values contained in the project
     * properties file.
     */
    @Override
    public void restoreDefaults()
    {
        initializeFields();

        //project Label initialization
        origProjectLabelsVisibility = labelService.isProjectLabelLayerVisible();
        resetProjectLabelValues();
        enableProjectLabels(projectLayerCheckBox.isSelected());
    }

    @Override
    public void releaseResources()
    {
    }

    /**
     * Initializes all the project property fields of the current project and trys to set the snapshot if there is one.
     */
    private void initializeFields()
    {
        final IProject project = projectManager.getCurrentOpenProject();
        final File projectDirectory = new File(project.getProjectDirectory());

        projectIdTextField.setText(project.getProjectName());

        origTitle = title = ProjectPreferenceEntry.TITLE.getStringValue();
        if (origTitle.isEmpty()) origTitle = title = "None";
        titleTextField.setText(title);

        origStartDate = startDate = ProjectPreferenceEntry.START_DATE.getLongValue();
        if (origStartDate == null) origStartDate = startDate = System.currentTimeMillis();
        startDateField.setDate(new Date(startDate));

        origEndDate = endDate = ProjectPreferenceEntry.END_DATE.getLongValue();
        if (origEndDate == null) origEndDate = endDate = System.currentTimeMillis();
        endDateField.setDate(new Date(endDate));

        origDesc = desc = ProjectPreferenceEntry.DESCRIPTION.getStringValue();
        if (origDesc.isEmpty()) origDesc = desc = "No description has been set.";
        descriptionTextField.setText(desc);

        origLocation = location = ProjectPreferenceEntry.LOCATION.getStringValue();
        if (origLocation.isEmpty()) origLocation = location = "No location has been set.";
        locationTextField.setText(location);

        origNotes = notes = ProjectPreferenceEntry.NOTES.getStringValue();
        if (origNotes.isEmpty()) origNotes = notes = "No notes have been set.";
        notesTextField.setText(notes);

        FileUtils.DirInfo dirInfo = FileUtils.getDirectoryInfo(projectDirectory);
        sizeLabel.setText(FileUtils.formatFileSize(dirInfo.size));

        dirInfo = FileUtils.getDirectoryInfo(projectVFSManager.getRootDirectory());
        VFSLabel.setText(dirInfo.fileCount + " (" + FileUtils.formatFileSize(dirInfo.size) + ")");

        for (IMapObjectController controller : mapObjectManager.getMapObjectControllers())
        {
            addRowOfData(controller.getMapObject());
        }

        // Get a scaled snapshot image that fits our panel, if it exists.
        // Otherwise, fill the snapshot view with the default no image.
        final var image = snapshotService.getSnapshotForDirectory(projectDirectory, IMAGE_PANEL_WIDTH_PX, IMAGE_PANEL_HEIGHT_PX);
        imagePanel.setImage(image != null ? image : noSnapshotImage);
    }

    /**
     * Adds a row of data to the map object panel.
     */
    @InvokeOnEDT
    private void addRowOfData(IMapObject mapObject)
    {
        final Object[] rowData = {mapObject.getUnitID(), mapObject.getType(), mapObject.getFamily(), mapObject.getName()};
        ((DefaultTableModel) moTable.getModel()).addRow(rowData);
    }

    private void projectLayerCheckBoxActionPerformed()
    {
        setPanelDirty();
        enableProjectLabels(projectLayerCheckBox.isSelected());
    }

    private void enableProjectLabels(boolean enabled)
    {
        projectLabelConfigs.forEach(projectLabelConfig -> projectLabelConfig.setEnabled(enabled));
    }

    private void visibilityCheckBoxStateChanged()
    {
        projectLabelConfigs.forEach(ProjectLabelConfig::setEditableFieldsEnabled);

        if (projectLabelsHaveChanged()) setPanelDirty();
    }

    private void projectLabelTextFieldActionPerformed()
    {
        if (projectLabelsHaveChanged()) setPanelDirty();
    }

    private void projectLabelTextFieldCaretUpdate()
    {
        if (projectLabelsHaveChanged()) setPanelDirty();
    }

    private void colorChooserActionPerformed()
    {
        if (projectLabelsHaveChanged()) setPanelDirty();
    }

    private void sizeSpinnerStateChanged()
    {
        if (projectLabelsHaveChanged()) setPanelDirty();
    }

    private class ProjectLabelConfig
    {
        private final ProjectLabelLocation projectLabelName;
        private final IProjectLabel currentLabel;
        private boolean origVisible;
        private String origText;
        private Color origColor;
        private int origSize;
        private final JTextField textField;
        private final JCheckBox visibleCheckBox;
        private final ColorChooser colorChooser;
        private final JSpinner sizeSpinner;

        public ProjectLabelConfig(ProjectLabelLocation location, JTextField textField, JCheckBox visibleCheckBox, ColorChooser colorChooser, JSpinner sizeSpinner)
        {
            projectLabelName = location;
            this.textField = textField;
            this.visibleCheckBox = visibleCheckBox;
            this.colorChooser = colorChooser;
            this.sizeSpinner = sizeSpinner;

            currentLabel = labelService.getLabel(location);

            origVisible = currentLabel.isVisible();
            origText = currentLabel.getLabel();
            origColor = currentLabel.getColor();
            origSize = currentLabel.getSize();

            resetComponentsToOrigProjectValues();
        }

        public void resetComponentsToOrigProjectValues()
        {
            visibleCheckBox.setSelected(origVisible);
            textField.setText(origText);
            colorChooser.setColor(origColor);
            sizeSpinner.setValue(origSize);
        }

        public boolean applyChanges()
        {
            if (hasChanged())
            {
                origText = textField.getText();
                origVisible = visibleCheckBox.isSelected();
                origColor = colorChooser.getColor();
                origSize = (Integer) sizeSpinner.getValue();

                currentLabel.setLabel(origText);
                currentLabel.setVisible(origVisible);
                currentLabel.setColor(origColor);
                currentLabel.setSize(origSize);

                labelService.setLabel(projectLabelName, currentLabel);

                return true;
            }
            return false;
        }

        public boolean hasChanged()
        {
            return !origText.equals(textField.getText()) ||
                    origVisible != visibleCheckBox.isSelected() ||
                    !origColor.equals(colorChooser.getColor()) ||
                    origSize != (Integer) sizeSpinner.getValue();
        }

        public void setEnabled(boolean enabled)
        {
            visibleCheckBox.setEnabled(enabled);
            setEditableFieldsEnabled();
        }

        public void setEditableFieldsEnabled()
        {
            textField.setEnabled(visibleCheckBox.isSelected());
            colorChooser.setEnabled(visibleCheckBox.isSelected());
            sizeSpinner.setEnabled(visibleCheckBox.isSelected());
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var tabbedPane2 = new JTabbedPane();
        var panel3 = new JPanel();
        var coordPanel = new JPanel();
        var label15 = new JLabel();
        projectIdTextField = new JTextField();
        var label16 = new JLabel();
        titleTextField = new JTextField();
        var label19 = new JLabel();
        startDateField = new JXDatePicker();
        var label20 = new JLabel();
        endDateField = new JXDatePicker();
        var label18 = new JLabel();
        sizeLabel = new JLabel();
        var label21 = new JLabel();
        VFSLabel = new JLabel();
        var separator4 = new JSeparator();
        var tabbedPane1 = new JTabbedPane();
        var descriptionTabPanel = new JPanel();
        var scrollPane11 = new JScrollPane();
        descriptionTextField = new JXEditorPane();
        var locationTabPanel = new JPanel();
        var scrollPane12 = new JScrollPane();
        locationTextField = new JXEditorPane();
        var moTabPanel = new JPanel();
        var scrollPane1 = new JScrollPane();
        moTable = new JXTable();
        var notesTabPanel = new JPanel();
        var scrollPane13 = new JScrollPane();
        notesTextField = new JXEditorPane();
        var separator2 = new JSeparator();
        var imageScrollPane = new JScrollPane();
        imagePanel = new JXImageView();
        var separator5 = new JSeparator();
        var panel4 = new JPanel();
        var projectLabelPanel = new JPanel();
        projectLayerCheckBox = new JCheckBox();
        var panel1 = new JPanel();
        var label4 = new JLabel();
        var label1 = new JLabel();
        var label2 = new JLabel();
        var label3 = new JLabel();
        upperCenterCheckBox = new JCheckBox();
        upperCenterTextField = new JTextField();
        upperCenterColorChooser = new ColorChooser();
        upperCenterSizeSpinner = new JSpinner();
        lowerCenterCheckBox = new JCheckBox();
        lowerCenterTextField = new JTextField();
        lowerCenterColorChooser = new ColorChooser();
        lowerCenterSizeSpinner = new JSpinner();
        upperRightCheckBox = new JCheckBox();
        upperRightTextField = new JTextField();
        upperRightColorChooser = new ColorChooser();
        upperRightSizeSpinner = new JSpinner();
        upperLeftCheckBox = new JCheckBox();
        upperLeftTextField = new JTextField();
        upperLeftColorChooser = new ColorChooser();
        upperLeftSizeSpinner = new JSpinner();
        lowerRightCheckBox = new JCheckBox();
        lowerRightTextField = new JTextField();
        lowerRightColorChooser = new ColorChooser();
        lowerRightSizeSpinner = new JSpinner();
        lowerLeftCheckBox = new JCheckBox();
        lowerLeftTextField = new JTextField();
        lowerLeftColorChooser = new ColorChooser();
        lowerLeftSizeSpinner = new JSpinner();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow"));

        //======== tabbedPane2 ========
        {
            tabbedPane2.setName("tabbedPane2");

            //======== panel3 ========
            {
                panel3.setName("panel3");
                panel3.setLayout(new FormLayout(
                        "default:grow",
                        "2*(default), default:grow, default, fill:[240px,pref], default"));

                //======== coordPanel ========
                {
                    coordPanel.setName("coordPanel");
                    coordPanel.setLayout(new FormLayout(
                            "default, $lcgap, default:grow, 2*($lcgap, default), $lcgap, default:grow",
                            "3*(default, $lgap), default"));

                    //---- label15 ----
                    label15.setText("Project ID");
                    label15.setName("label15");
                    coordPanel.add(label15, cc.xy(1, 1));

                    //---- projectIdTextField ----
                    projectIdTextField.setEnabled(false);
                    projectIdTextField.setName("projectIdTextField");
                    coordPanel.add(projectIdTextField, cc.xywh(3, 1, 7, 1));

                    //---- label16 ----
                    label16.setText("Title");
                    label16.setName("label16");
                    coordPanel.add(label16, cc.xy(1, 3));

                    //---- titleTextField ----
                    titleTextField.setName("titleTextField");
                    titleTextField.addKeyListener(new KeyAdapter()
                    {
                        @Override
                        public void keyReleased(KeyEvent e)
                        {
                            textFieldKeyReleased();
                        }
                    });
                    coordPanel.add(titleTextField, cc.xywh(3, 3, 7, 1));

                    //---- label19 ----
                    label19.setText("Start");
                    label19.setName("label19");
                    coordPanel.add(label19, cc.xy(1, 5));

                    //---- startDateField ----
                    startDateField.setName("startDateField");
                    startDateField.addActionListener(e -> dateFieldActionPerformed());
                    coordPanel.add(startDateField, cc.xy(3, 5));

                    //---- label20 ----
                    label20.setText("Stop");
                    label20.setName("label20");
                    coordPanel.add(label20, cc.xy(7, 5));

                    //---- endDateField ----
                    endDateField.setName("endDateField");
                    endDateField.addActionListener(e -> dateFieldActionPerformed());
                    coordPanel.add(endDateField, cc.xy(9, 5));

                    //---- label18 ----
                    label18.setText("Size");
                    label18.setName("label18");
                    coordPanel.add(label18, cc.xy(1, 7));

                    //---- sizeLabel ----
                    sizeLabel.setText("2.2 mb");
                    sizeLabel.setName("sizeLabel");
                    coordPanel.add(sizeLabel, cc.xy(3, 7));

                    //---- label21 ----
                    label21.setText("VFS");
                    label21.setName("label21");
                    coordPanel.add(label21, cc.xy(7, 7));

                    //---- VFSLabel ----
                    VFSLabel.setText("12 (1.4 mb)");
                    VFSLabel.setName("VFSLabel");
                    coordPanel.add(VFSLabel, cc.xy(9, 7));
                }
                panel3.add(coordPanel, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.BOTTOM));

                //---- separator4 ----
                separator4.setName("separator4");
                panel3.add(separator4, cc.xy(1, 2));

                //======== tabbedPane1 ========
                {
                    tabbedPane1.setName("tabbedPane1");

                    //======== descriptionTabPanel ========
                    {
                        descriptionTabPanel.setName("descriptionTabPanel");
                        descriptionTabPanel.setLayout(new FormLayout(
                                "default:grow",
                                "fill:default:grow"));

                        //======== scrollPane11 ========
                        {
                            scrollPane11.setName("scrollPane11");

                            //---- descriptionTextField ----
                            descriptionTextField.setName("descriptionTextField");
                            descriptionTextField.addKeyListener(new KeyAdapter()
                            {
                                @Override
                                public void keyReleased(KeyEvent e)
                                {
                                    textFieldKeyReleased();
                                }
                            });
                            scrollPane11.setViewportView(descriptionTextField);
                        }
                        descriptionTabPanel.add(scrollPane11, cc.xy(1, 1));
                    }
                    tabbedPane1.addTab("Description", descriptionTabPanel);

                    //======== locationTabPanel ========
                    {
                        locationTabPanel.setName("locationTabPanel");
                        locationTabPanel.setLayout(new FormLayout(
                                "default:grow",
                                "fill:default:grow"));

                        //======== scrollPane12 ========
                        {
                            scrollPane12.setName("scrollPane12");

                            //---- locationTextField ----
                            locationTextField.setName("locationTextField");
                            locationTextField.addKeyListener(new KeyAdapter()
                            {
                                @Override
                                public void keyReleased(KeyEvent e)
                                {
                                    textFieldKeyReleased();
                                }
                            });
                            scrollPane12.setViewportView(locationTextField);
                        }
                        locationTabPanel.add(scrollPane12, cc.xy(1, 1));
                    }
                    tabbedPane1.addTab("Location", locationTabPanel);

                    //======== moTabPanel ========
                    {
                        moTabPanel.setName("moTabPanel");
                        moTabPanel.setLayout(new FormLayout(
                                "default:grow",
                                "fill:default:grow"));

                        //======== scrollPane1 ========
                        {
                            scrollPane1.setName("scrollPane1");

                            //---- moTable ----
                            moTable.setModel(new DefaultTableModel(
                                    new Object[][]{
                                    },
                                    new String[]{
                                            "Unit ID", "Type", "Family", "Name"
                                    }));
                            {
                                TableColumnModel cm = moTable.getColumnModel();
                                cm.getColumn(0).setPreferredWidth(45);
                                cm.getColumn(1).setPreferredWidth(70);
                                cm.getColumn(2).setPreferredWidth(140);
                                cm.getColumn(3).setPreferredWidth(160);
                            }
                            moTable.setEditable(false);
                            moTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                            moTable.setTerminateEditOnFocusLost(false);
                            moTable.setPreferredScrollableViewportSize(new Dimension(200, 100));
                            moTable.setName("moTable");
                            scrollPane1.setViewportView(moTable);
                        }
                        moTabPanel.add(scrollPane1, cc.xy(1, 1));
                    }
                    tabbedPane1.addTab("Map Objects", moTabPanel);

                    //======== notesTabPanel ========
                    {
                        notesTabPanel.setName("notesTabPanel");
                        notesTabPanel.setLayout(new FormLayout(
                                "default:grow",
                                "fill:default:grow"));

                        //======== scrollPane13 ========
                        {
                            scrollPane13.setName("scrollPane13");

                            //---- notesTextField ----
                            notesTextField.setName("notesTextField");
                            notesTextField.addKeyListener(new KeyAdapter()
                            {
                                @Override
                                public void keyReleased(KeyEvent e)
                                {
                                    textFieldKeyReleased();
                                }
                            });
                            scrollPane13.setViewportView(notesTextField);
                        }
                        notesTabPanel.add(scrollPane13, cc.xy(1, 1));
                    }
                    tabbedPane1.addTab("Notes", notesTabPanel);
                }
                panel3.add(tabbedPane1, cc.xy(1, 3));

                //---- separator2 ----
                separator2.setName("separator2");
                panel3.add(separator2, cc.xy(1, 4));

                //======== imageScrollPane ========
                {
                    imageScrollPane.setName("imageScrollPane");

                    //---- imagePanel ----
                    imagePanel.setEditable(false);
                    imagePanel.setScale(0.0);
                    imagePanel.setBackgroundPainter(null);
                    imagePanel.setName("imagePanel");
                    imageScrollPane.setViewportView(imagePanel);
                }
                panel3.add(imageScrollPane, cc.xy(1, 5));

                //---- separator5 ----
                separator5.setName("separator5");
                panel3.add(separator5, cc.xy(1, 6));
            }
            tabbedPane2.addTab("Info", panel3);

            //======== panel4 ========
            {
                panel4.setName("panel4");
                panel4.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default, $lgap, fill:default:grow"));

                //======== projectLabelPanel ========
                {
                    projectLabelPanel.setName("projectLabelPanel");
                    projectLabelPanel.setLayout(new FormLayout(
                            "default, $lcgap, default:grow",
                            "default"));

                    //---- projectLayerCheckBox ----
                    projectLayerCheckBox.setText("Show Project Labels");
                    projectLayerCheckBox.setName("projectLayerCheckBox");
                    projectLayerCheckBox.addActionListener(e -> projectLayerCheckBoxActionPerformed());
                    projectLabelPanel.add(projectLayerCheckBox, cc.xy(1, 1));
                }
                panel4.add(projectLabelPanel, cc.xy(1, 1));

                //======== panel1 ========
                {
                    panel1.setName("panel1");
                    panel1.setLayout(new FormLayout(
                            "$ugap, $rgap, default, $lcgap, default:grow, 2*($lcgap, default)",
                            "6*(default, $nlgap), default"));

                    //---- label4 ----
                    label4.setText("Visible");
                    label4.setHorizontalAlignment(SwingConstants.CENTER);
                    label4.setName("label4");
                    panel1.add(label4, cc.xy(3, 1));

                    //---- label1 ----
                    label1.setText("Text");
                    label1.setHorizontalAlignment(SwingConstants.CENTER);
                    label1.setName("label1");
                    panel1.add(label1, cc.xy(5, 1));

                    //---- label2 ----
                    label2.setText("Color");
                    label2.setHorizontalAlignment(SwingConstants.CENTER);
                    label2.setName("label2");
                    panel1.add(label2, cc.xy(7, 1));

                    //---- label3 ----
                    label3.setText("   Size   ");
                    label3.setHorizontalAlignment(SwingConstants.CENTER);
                    label3.setName("label3");
                    panel1.add(label3, cc.xy(9, 1));

                    //---- upperCenterCheckBox ----
                    upperCenterCheckBox.setText("Upper Center:");
                    upperCenterCheckBox.setEnabled(false);
                    upperCenterCheckBox.setName("upperCenterCheckBox");
                    upperCenterCheckBox.addChangeListener(e -> visibilityCheckBoxStateChanged());
                    panel1.add(upperCenterCheckBox, cc.xy(3, 3));

                    //---- upperCenterTextField ----
                    upperCenterTextField.setEnabled(false);
                    upperCenterTextField.setName("upperCenterTextField");
                    upperCenterTextField.addActionListener(e -> projectLabelTextFieldActionPerformed());
                    upperCenterTextField.addCaretListener(e -> projectLabelTextFieldCaretUpdate());
                    panel1.add(upperCenterTextField, cc.xy(5, 3));

                    //---- upperCenterColorChooser ----
                    upperCenterColorChooser.setName("upperCenterColorChooser");
                    upperCenterColorChooser.addActionListener(e -> colorChooserActionPerformed());
                    panel1.add(upperCenterColorChooser, cc.xy(7, 3));

                    //---- upperCenterSizeSpinner ----
                    upperCenterSizeSpinner.setModel(new SpinnerNumberModel(24, 8, 72, 2));
                    upperCenterSizeSpinner.setName("upperCenterSizeSpinner");
                    upperCenterSizeSpinner.addChangeListener(e -> sizeSpinnerStateChanged());
                    panel1.add(upperCenterSizeSpinner, cc.xy(9, 3));

                    //---- lowerCenterCheckBox ----
                    lowerCenterCheckBox.setText("Lower Center");
                    lowerCenterCheckBox.setEnabled(false);
                    lowerCenterCheckBox.setName("lowerCenterCheckBox");
                    lowerCenterCheckBox.addChangeListener(e -> visibilityCheckBoxStateChanged());
                    panel1.add(lowerCenterCheckBox, cc.xy(3, 5));

                    //---- lowerCenterTextField ----
                    lowerCenterTextField.setEnabled(false);
                    lowerCenterTextField.setName("lowerCenterTextField");
                    lowerCenterTextField.addActionListener(e -> projectLabelTextFieldActionPerformed());
                    lowerCenterTextField.addCaretListener(e -> projectLabelTextFieldCaretUpdate());
                    panel1.add(lowerCenterTextField, cc.xy(5, 5));

                    //---- lowerCenterColorChooser ----
                    lowerCenterColorChooser.setName("lowerCenterColorChooser");
                    lowerCenterColorChooser.addActionListener(e -> colorChooserActionPerformed());
                    panel1.add(lowerCenterColorChooser, cc.xy(7, 5));

                    //---- lowerCenterSizeSpinner ----
                    lowerCenterSizeSpinner.setModel(new SpinnerNumberModel(24, 8, 72, 2));
                    lowerCenterSizeSpinner.setName("lowerCenterSizeSpinner");
                    lowerCenterSizeSpinner.addChangeListener(e -> sizeSpinnerStateChanged());
                    panel1.add(lowerCenterSizeSpinner, cc.xy(9, 5));

                    //---- upperRightCheckBox ----
                    upperRightCheckBox.setText("Upper Right");
                    upperRightCheckBox.setEnabled(false);
                    upperRightCheckBox.setName("upperRightCheckBox");
                    upperRightCheckBox.addChangeListener(e -> visibilityCheckBoxStateChanged());
                    panel1.add(upperRightCheckBox, cc.xy(3, 7));

                    //---- upperRightTextField ----
                    upperRightTextField.setEnabled(false);
                    upperRightTextField.setName("upperRightTextField");
                    upperRightTextField.addActionListener(e -> projectLabelTextFieldActionPerformed());
                    upperRightTextField.addCaretListener(e -> projectLabelTextFieldCaretUpdate());
                    panel1.add(upperRightTextField, cc.xy(5, 7));

                    //---- upperRightColorChooser ----
                    upperRightColorChooser.setName("upperRightColorChooser");
                    upperRightColorChooser.addActionListener(e -> colorChooserActionPerformed());
                    panel1.add(upperRightColorChooser, cc.xy(7, 7));

                    //---- upperRightSizeSpinner ----
                    upperRightSizeSpinner.setModel(new SpinnerNumberModel(24, 8, 72, 2));
                    upperRightSizeSpinner.setName("upperRightSizeSpinner");
                    upperRightSizeSpinner.addChangeListener(e -> sizeSpinnerStateChanged());
                    panel1.add(upperRightSizeSpinner, cc.xy(9, 7));

                    //---- upperLeftCheckBox ----
                    upperLeftCheckBox.setText("Upper Left");
                    upperLeftCheckBox.setEnabled(false);
                    upperLeftCheckBox.setName("upperLeftCheckBox");
                    upperLeftCheckBox.addChangeListener(e -> visibilityCheckBoxStateChanged());
                    panel1.add(upperLeftCheckBox, cc.xy(3, 9));

                    //---- upperLeftTextField ----
                    upperLeftTextField.setEnabled(false);
                    upperLeftTextField.setName("upperLeftTextField");
                    upperLeftTextField.addActionListener(e -> projectLabelTextFieldActionPerformed());
                    upperLeftTextField.addCaretListener(e -> projectLabelTextFieldCaretUpdate());
                    panel1.add(upperLeftTextField, cc.xy(5, 9));

                    //---- upperLeftColorChooser ----
                    upperLeftColorChooser.setName("upperLeftColorChooser");
                    upperLeftColorChooser.addActionListener(e -> colorChooserActionPerformed());
                    panel1.add(upperLeftColorChooser, cc.xy(7, 9));

                    //---- upperLeftSizeSpinner ----
                    upperLeftSizeSpinner.setModel(new SpinnerNumberModel(24, 8, 72, 2));
                    upperLeftSizeSpinner.setName("upperLeftSizeSpinner");
                    upperLeftSizeSpinner.addChangeListener(e -> sizeSpinnerStateChanged());
                    panel1.add(upperLeftSizeSpinner, cc.xy(9, 9));

                    //---- lowerRightCheckBox ----
                    lowerRightCheckBox.setText("Lower Right");
                    lowerRightCheckBox.setEnabled(false);
                    lowerRightCheckBox.setName("lowerRightCheckBox");
                    lowerRightCheckBox.addChangeListener(e -> visibilityCheckBoxStateChanged());
                    panel1.add(lowerRightCheckBox, cc.xy(3, 11));

                    //---- lowerRightTextField ----
                    lowerRightTextField.setEnabled(false);
                    lowerRightTextField.setName("lowerRightTextField");
                    lowerRightTextField.addActionListener(e -> projectLabelTextFieldActionPerformed());
                    lowerRightTextField.addCaretListener(e -> projectLabelTextFieldCaretUpdate());
                    panel1.add(lowerRightTextField, cc.xy(5, 11));

                    //---- lowerRightColorChooser ----
                    lowerRightColorChooser.setName("lowerRightColorChooser");
                    lowerRightColorChooser.addActionListener(e -> colorChooserActionPerformed());
                    panel1.add(lowerRightColorChooser, cc.xy(7, 11));

                    //---- lowerRightSizeSpinner ----
                    lowerRightSizeSpinner.setModel(new SpinnerNumberModel(24, 8, 72, 2));
                    lowerRightSizeSpinner.setName("lowerRightSizeSpinner");
                    lowerRightSizeSpinner.addChangeListener(e -> sizeSpinnerStateChanged());
                    panel1.add(lowerRightSizeSpinner, cc.xy(9, 11));

                    //---- lowerLeftCheckBox ----
                    lowerLeftCheckBox.setText("Lower Left");
                    lowerLeftCheckBox.setEnabled(false);
                    lowerLeftCheckBox.setName("lowerLeftCheckBox");
                    lowerLeftCheckBox.addChangeListener(e -> visibilityCheckBoxStateChanged());
                    panel1.add(lowerLeftCheckBox, cc.xy(3, 13));

                    //---- lowerLeftTextField ----
                    lowerLeftTextField.setEnabled(false);
                    lowerLeftTextField.setName("lowerLeftTextField");
                    lowerLeftTextField.addActionListener(e -> projectLabelTextFieldActionPerformed());
                    lowerLeftTextField.addCaretListener(e -> projectLabelTextFieldCaretUpdate());
                    panel1.add(lowerLeftTextField, cc.xy(5, 13));

                    //---- lowerLeftColorChooser ----
                    lowerLeftColorChooser.setName("lowerLeftColorChooser");
                    lowerLeftColorChooser.addActionListener(e -> colorChooserActionPerformed());
                    panel1.add(lowerLeftColorChooser, cc.xy(7, 13));

                    //---- lowerLeftSizeSpinner ----
                    lowerLeftSizeSpinner.setModel(new SpinnerNumberModel(24, 8, 72, 2));
                    lowerLeftSizeSpinner.setName("lowerLeftSizeSpinner");
                    lowerLeftSizeSpinner.addChangeListener(e -> sizeSpinnerStateChanged());
                    panel1.add(lowerLeftSizeSpinner, cc.xy(9, 13));
                }
                panel4.add(panel1, cc.xy(1, 3));
            }
            tabbedPane2.addTab("Display", panel4);
        }
        add(tabbedPane2, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTextField projectIdTextField;
    private JTextField titleTextField;
    private JXDatePicker startDateField;
    private JXDatePicker endDateField;
    private JLabel sizeLabel;
    private JLabel VFSLabel;
    private JXEditorPane descriptionTextField;
    private JXEditorPane locationTextField;
    private JXTable moTable;
    private JXEditorPane notesTextField;
    private JXImageView imagePanel;
    private JCheckBox projectLayerCheckBox;
    private JCheckBox upperCenterCheckBox;
    private JTextField upperCenterTextField;
    private ColorChooser upperCenterColorChooser;
    private JSpinner upperCenterSizeSpinner;
    private JCheckBox lowerCenterCheckBox;
    private JTextField lowerCenterTextField;
    private ColorChooser lowerCenterColorChooser;
    private JSpinner lowerCenterSizeSpinner;
    private JCheckBox upperRightCheckBox;
    private JTextField upperRightTextField;
    private ColorChooser upperRightColorChooser;
    private JSpinner upperRightSizeSpinner;
    private JCheckBox upperLeftCheckBox;
    private JTextField upperLeftTextField;
    private ColorChooser upperLeftColorChooser;
    private JSpinner upperLeftSizeSpinner;
    private JCheckBox lowerRightCheckBox;
    private JTextField lowerRightTextField;
    private ColorChooser lowerRightColorChooser;
    private JSpinner lowerRightSizeSpinner;
    private JCheckBox lowerLeftCheckBox;
    private JTextField lowerLeftTextField;
    private ColorChooser lowerLeftColorChooser;
    private JSpinner lowerLeftSizeSpinner;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
