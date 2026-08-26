package gov.takx.core.view.gui.projectadmin;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.project.IProjectManager;
import gov.takx.api.project.IProjectMetadata;
import gov.takx.api.project.ProjectConstants;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.AlwaysOnTopFileChooser;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.ErrorReportingSwingWorker;
import gov.takx.commons.swing.documents.LimitedDocument;
import gov.takx.commons.utils.PlatformVersion;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.configuration.ProgramEnvironment;
import gov.takx.core.project.ProjectAssistant;
import gov.takx.core.project.ProjectMetadata;
import gov.takx.core.project.ProjectSelection;
import gov.takx.core.view.gui.screenimageutils.ProjectSnapshotService;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.ui.AStandardDialog;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.jdesktop.swingx.JXImageView;
import org.jdesktop.swingx.JXTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class ProjectExplorer extends AStandardDialog
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String NO_RECENT_PROJECT = "No recent project from file->open";
    private static final String HIDE_INFO_ICON = "double_arrow_up.png";
    private static final String SHOW_INFO_ICON = "double_arrow_down.png";

    private static final String TOOL_TIP_FORMAT = "<html><font color=#ffff><b>%s</b></font></html>";

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MMM-dd").withZone(ZoneId.systemDefault());

    public enum StartupMode
    {
        STARTUP, NEW, EXISTING
    }

    // Constants for accessing project table columns
    private static final int PROJ_ROW_ID = 0;
    private static final int PROJ_ROW_TITLE = 1;
    private static final int PROJ_ROW_DATE = 2;
    private static final int PROJ_ROW_PATH = 3;
    private static final int PROJ_ROW_DESCRIPTION = 4;
    private static final int PROJ_ID_FOR_DUPLICATES = 5;

    private static final String DESCRIPTION_RIGHT_ADDITIONAL_SHOWING = "Hide additional project fields by clicking the up arrow.";
    private static final String NEW_PROJECT_DESCRIPTION_RIGHT_ADDITIONAL_HIDDEN = "Edit additional project fields by clicking the down arrow.";
    private static final String EXISTING_PROJECT_DESCRIPTION_RIGHT_ADDITIONAL_HIDDEN = "View additional project fields by clicking the down arrow.";

    private static final String SHOW_TOOLTIP = "Click to show additional project information";
    private static final String HIDE_TOOLTIP = "Click to hide additional project information";

    private static final String NEW_PROJECT_DESCRIPTION_LEFT = "Enter project info to the right";
    private static final String EXISTING_PROJECT_DESCRIPTION_LEFT = "Select a project, or \"Browse\" to locate a .projx or .pkgx file";

    private final static String EXIT = "Exit";
    private final static String CANCEL = "Cancel";
    private final static String OPEN = "Open";
    private final static String CREATE = "Create";

    private static final CellConstraints cc = new CellConstraints();

    private static final int PANEL_WIDTH = 355;
    private static final int PANEL_HEIGHT = 330;

    private final String mostRecentProjectName;
    private final Icon hideInfoIcon = UIResources.getStandardSizedIcon(PLATFORM_ICONS, HIDE_INFO_ICON);
    private final Icon showInfoIcon = UIResources.getStandardSizedIcon(PLATFORM_ICONS, SHOW_INFO_ICON);

    private BufferedImage noSnapshotImage;
    private BufferedImage packageImage;
    private ProjectSelection projectSelection;
    private ProjectMetadata newProjectMetadata;

    // Used to map from proj/pkg file (abs path) to parsed metadata
    private final Map<String, IProjectMetadata> metadataMap = new HashMap<>();

    private boolean ignoreEvent;
    private StartupMode mode;
    private boolean mousePressedInTable;
    private boolean additionalFieldsShowing;
    private final ProjectDetailsPanel projectDetailsPanel;

    @Inject
    @ProgramPreference(PreferenceEntry.DEFAULT_WMS_OFFLINE_MODE)
    private boolean defaultWMSOfflineMode;

    @Inject
    @ProgramPreference(PreferenceEntry.DEFAULT_TO_NEW_PROJECT_ON_STARTUP)
    private boolean defaultToNewProjectOnStartup;

    @Inject
    private ProgramEnvironment programEnvironment;

    @Inject
    private IProjectManager projectManager;

    @Inject
    private ProjectSnapshotService snapshotService;

    /**
     * This is the project dialog constructor.
     *
     * @param recentProjectName the most recent project (may be {@code null} or an empty String for no recent project)
     * @param mode              the mode to set this project dialog to
     */
    public ProjectExplorer(String recentProjectName, StartupMode mode)
    {
        // The startup mode will define the layout of this dialog
        // STARTUP, NEW and EXISTING modes will look significantly different (see customize() method)

        this.mode = mode;

        mostRecentProjectName = StringUtils.isEmpty(recentProjectName)
                ? NO_RECENT_PROJECT
                : recentProjectName;

        initComponents();

        projectDetailsPanel = new ProjectDetailsPanel();
        additionalButton.setIcon(UIResources.getStandardSizedIcon(PLATFORM_ICONS, SHOW_INFO_ICON));
    }

    @PostConstruct
    private void postConstruct()
    {
        customize();

        File path = projectManager.getProjectsDirectory();
        if (!path.exists())
        {
            path = new File(programEnvironment.getProjectDataDirectory(), "Projects");
        }

        programEnvironment.setProjectRoot(path.getAbsolutePath());
        projectDirectoryTextField.setText(path.getAbsolutePath());

        // No reason to scan existing projects when creating a NEW project
        if (mode != StartupMode.NEW) scanProjectsDirectory(path);
    }

    /**
     * @return The project selection made by the user
     */
    public ProjectSelection getProjectSelection()
    {
        return projectSelection;
    }

    private boolean isWMSOfflineMode()
    {
        return !checkBoxOnline.isSelected();
    }

    /**
     * This method simply customizes the look and feel a bit from the default TAKX settings and sets the project mode.
     */
    private void customize()
    {
        projectIdTextField.setDocument(new LimitedDocument(255, ProjectConstants.PROJECT_NAME_CHAR_VALIDATOR));

        ToolTipManager.sharedInstance().unregisterComponent(projectTable);
        ToolTipManager.sharedInstance().unregisterComponent(projectTable.getTableHeader());
        projectTable.getSelectionModel().addListSelectionListener(new SelectionListener(projectTable));

        projectTable.getColumnExt(PROJ_ROW_ID).setCellRenderer(new IdAndPathCellRenderer());

        // hide the path column which has the alternate id for projects with duplicate names
        projectTable.getColumnExt(PROJ_ID_FOR_DUPLICATES).setVisible(false);

        // hide the description column used for tooltips
        projectTable.getColumnExt(PROJ_ROW_DESCRIPTION).setVisible(false);

        // hide the path column which has the absolute path to the project
        projectTable.getColumnExt(PROJ_ROW_PATH).setVisible(false);

        // Custom comparator for the date column in the project table
        projectTable.getColumnExt(PROJ_ROW_DATE).setComparator(Comparator.comparingLong(key -> (Long) key));
        projectTable.getColumnExt(PROJ_ROW_DATE).setCellRenderer(new DateCellRenderer());

        getRootPane().setDefaultButton(openButton);

        // Customize the look depending on whether the StartupMode is STARTUP, NEW, or EXISTING

        cancelButton.setText(mode == StartupMode.STARTUP ? EXIT : CANCEL);

        switch (mode)
        {
            case STARTUP ->
            {
                panel12.add(panelNewOrExistingSelector, cc.xy(1, 1));
                if (mostRecentProjectName.equals(NO_RECENT_PROJECT) || defaultToNewProjectOnStartup)
                {
                    setNewProjectMode();
                } else
                {
                    setExistingProjectMode(true);
                }
            }
            case NEW ->
            {
                setTitle("Create New");
                setNewProjectMode();
                panel12.setVisible(false);
                imageScrollPane.setVisible(false);
                manageButton.setVisible(false);
            }
            case EXISTING ->
            {
                setExistingProjectMode(true);
                manageButton.setVisible(false);
                panel12.add(panelProjectBrowse, cc.xy(1, 1));
            }
        }

        setLocationRelativeTo(JFrame.getFrames()[0]);
        setResizable(false);

        // Create the default "No Snapshot" preview images
        noSnapshotImage = snapshotService.makeCaptionImage(false, PANEL_WIDTH, PANEL_HEIGHT);
        packageImage = snapshotService.makeCaptionImage(true, PANEL_WIDTH, PANEL_HEIGHT);

        imagePanel.setImage(noSnapshotImage);

        //project description tool tip on mouse over in project
        projectTable.addMouseMotionListener(new MouseMotionAdapter()
        {
            @Override
            public void mouseMoved(MouseEvent e)
            {
                Point p = e.getPoint();
                int row = projectTable.rowAtPoint(p);
                if (row != -1)
                {
                    row = projectTable.convertRowIndexToModel(row);
                    final Object description = projectTable.getModel().getValueAt(row, PROJ_ROW_DESCRIPTION);
                    projectTable.setToolTipText(String.format(TOOL_TIP_FORMAT, description));
                }
            }
        });

        //Launch a project with double click
        projectTable.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                Point p = e.getPoint();
                if (e.getClickCount() == 2 && projectTable.rowAtPoint(p) != -1 && SwingUtilities.isLeftMouseButton(e))
                {
                    openButtonActionPerformed();
                }
            }
        });

        pack();
    }

    /**
     * This enables or disables all of the fields on the right hand project properties panel. Enabled for existing,
     * disabled for new projects or no project.
     *
     * @param enabled whether or not the property panel should be enabled.
     */
    private void setRightPanelFieldsEnabled(boolean enabled)
    {
        projectIdTextField.setEditable(enabled);
        titleTextField.setEditable(enabled);
        projectDetailsPanel.setEnabled(enabled);
    }

    /**
     * This clears all of the values set on the right hand project information panel.
     *
     * @param clearSnapshot {@code true} if the snapshot image should also be reset
     */
    @InvokeOnEDT
    private void clearRightPanel(final boolean clearSnapshot)
    {
        projectIdTextField.setText("");
        titleTextField.setText("");
        projectDetailsPanel.clear();

        if (clearSnapshot)
        {
            imagePanel.setImage(noSnapshotImage);
        }
    }

    /**
     * Pull the project metadata and install it into the UI components.  If the project file hasn't been parsed
     * yet, then do so and store it for later.
     *
     * @param propertyFile the property file of a project
     * @throws IOException thrown if the file doesn't exist, but this shouldn't happen.
     */
    private void loadProjectMetadata(File propertyFile) throws IOException
    {
        IProjectMetadata pmd = getProjectMetadata(propertyFile);

        projectDetailsPanel.setProjectDetails(pmd);
        checkBoxOnline.setSelected(!pmd.getWmsOfflineMode());
    }

    /**
     * Get the project metadata for the given project or package file.  If not already cached, then parse it and save it.
     *
     * @param projectFile Project or package file to process
     * @return project metadata
     * @throws IOException on any error to open/read the file
     */
    private IProjectMetadata getProjectMetadata(File projectFile) throws IOException
    {
        // If we already have the metadata for this file, then just use it
        final String absolutePath = projectFile.getAbsolutePath();
        IProjectMetadata pmd = metadataMap.get(absolutePath);

        if (pmd == null)
        {
            // Not found, go get it and put it into the map for later
            pmd = new ProjectAssistant().parseProjectFile(projectFile);
            metadataMap.put(absolutePath, pmd);
        }

        return pmd;
    }

    /**
     * Add a row to the project table with the data from the given project/package file.  If given a package file,
     * then the project file will be extracted from it.
     *
     * @param propertyFile the property file of a project or a package file
     * @throws IOException io exception
     */
    private void addRowForProject(File propertyFile) throws IOException
    {
        final String extension = FileUtils.getExtension(propertyFile);

        if (null != extension)
        {
            final String absolutePath = propertyFile.getAbsolutePath();
            final IProjectMetadata pmd = getProjectMetadata(propertyFile);

            // Only show projects that are compatible with this major version
            final PlatformVersion projectVersion = pmd.getPlatformVersion();

            if (projectVersion.getMajor() == PlatformVersion.CURRENT_PLATFORM_VERSION.getMajor())
            {
                final boolean isPackage = extension.equalsIgnoreCase(ProjectConstants.PACKAGE_EXTENSION);

                long creationTime = pmd.getCreationDate();
                if (creationTime == 0) creationTime = FileUtils.getFileCreationTime(propertyFile);

                String title = pmd.getTitle();
                if (StringUtils.isEmpty(title)) title = isPackage ? "Package" : "None";

                String description;

                if (isPackage)
                {
                    description = absolutePath;
                } else
                {
                    description = pmd.getDescription();
                    if (StringUtils.isEmpty(description)) description = "No Description";
                }

                final String id = FileUtils.getNameWithoutExtension(propertyFile);

                addRowOfData(id, title, creationTime, absolutePath, description);
            } else
            {
                logger.debug("Ignoring old project, version = {} @ {}", projectVersion, propertyFile);
            }
        }
    }

    /**
     * This method lets the app frame know if it is opening the most recent project, opening another project,
     * or creating a new project.
     */
    private void openButtonActionPerformed()
    {
        final String name = projectIdTextField.getText();
        boolean valid = false;

        switch (mode)
        {
            case NEW:
                if (!projectManager.isValidNewProjectName(name))
                {
                    AlwaysOnTopJOptionPane.showErrorDialog(this, "Project name already exists, or no name was given. Please enter another name.", "Project Name - " + name);
                } else
                {
                    constructProjectMetadata();
                    projectSelection = ProjectSelection.makeNew(projectManager.getProjectFilePath(name), newProjectMetadata, isWMSOfflineMode());
                    valid = true;
                }
                break;

            case EXISTING:
                int row = projectTable.convertRowIndexToModel(projectTable.getSelectedRow());
                String path = (String) projectTable.getModel().getValueAt(row, PROJ_ROW_PATH);
                if (path.isEmpty())
                {
                    path = projectManager.getProjectFilePath(name);
                }
                projectSelection = ProjectSelection.makeExisting(path, isWMSOfflineMode());
                valid = true;
                break;

            default:
                logger.error("Unsupported startupOption: {}", mode);
        }

        // Only close if things are valid
        if (valid)
        {
            setVisible(false);
        }
    }

    /**
     * The new project button has been selected so clear any project selections and enabled the right hand fields
     */
    private void newButtonActionPerformed()
    {
        setNewProjectMode();
    }

    /**
     * The existing project button has been selected so clear any project selection, and set it to the most
     * recent project.
     */
    private void existingButtonActionPerformed()
    {
        setExistingProjectMode(true);
    }

    /**
     * Opens a file browser for selecting a project to open.
     */
    private void browseButtonActionPerformed()
    {
        // Change UI to "Open" mode.
        setExistingProjectMode(true);

        // Let the user choose the package or project to open.
        String path = new ProjectSelector().selectProjectToOpen(this);

        // Check to see if the user selected a package or project. If so, open it.
        if (path != null)
        {
            projectSelection = ProjectSelection.makeExisting(path, isWMSOfflineMode());
            setVisible(false);
        }
    }

    /**
     * Construct the project metadata from user entered fields which will be used for creating the new project.
     */
    private void constructProjectMetadata()
    {
        newProjectMetadata = new ProjectMetadata();

        newProjectMetadata.setTitle(titleTextField.getText());
        newProjectMetadata.setStartDate(projectDetailsPanel.getStartTime());
        newProjectMetadata.setEndDate(projectDetailsPanel.getEndTime());
        newProjectMetadata.setDescription(projectDetailsPanel.getDescriptionText());
        newProjectMetadata.setLocation(projectDetailsPanel.getLocationText());
        newProjectMetadata.setNotes(projectDetailsPanel.getNotesText());
        newProjectMetadata.setSize(projectDetailsPanel.getSizeText());
        newProjectMetadata.setVfsSize(projectDetailsPanel.getVfsText());
    }

    /**
     * Scan the given projects directory and add all the located packages and projects to the table.
     * This method must be called on the EDT.
     *
     * @param projectsDir Directory to scan for projects
     */
    private void scanProjectsDirectory(File projectsDir)
    {
        setOpenEnabled(mode == StartupMode.NEW);
        setBrowseEnabled(false);

        ((DefaultTableModel) projectTable.getModel()).setRowCount(0);

        new ProjectLoader(projectsDir.listFiles(), progressBar).execute();
    }

    /**
     * This sets the open/create button to enabled or disabled.
     *
     * @param isEnabled whether or not the open/create should be enabled.
     */
    @InvokeOnEDT
    private void setOpenEnabled(final boolean isEnabled)
    {
        openButton.setEnabled(isEnabled);
    }

    /**
     * Adds a row of project info to the project table.
     *
     * @param id          the project id
     * @param title       the project title
     * @param date        the file creation date in millis
     * @param path        path to project
     * @param description Description of project
     */
    @InvokeOnEDT
    private void addRowOfData(final String id, final String title, final long date, final String path, final String description)
    {
        DefaultTableModel model = (DefaultTableModel) projectTable.getModel();

        Object[] row = new Object[6];
        row[PROJ_ROW_ID] = id;
        row[PROJ_ROW_TITLE] = title;
        row[PROJ_ROW_DATE] = date;
        row[PROJ_ROW_PATH] = path;
        row[PROJ_ROW_DESCRIPTION] = description;
        row[PROJ_ID_FOR_DUPLICATES] = checkForDuplicates(id, path);

        model.addRow(row);

        // If we added the first row and we're note in NEW mode, then select that row
        if (model.getRowCount() == 1 && mode != StartupMode.NEW)
        {
            projectTable.setRowSelectionInterval(0, 0); // See the SelectionListener
        }
    }

    /**
     * Check to see if there are any other projects in the list with the same id. If so, augment the id with additional
     * info. Also update the other duplicate(s) as necessary.
     *
     * @param id   The project id.
     * @param path The path to the project or package file.
     * @return The id augmented with additional info if it's a duplicate, {@code null} otherwise.
     */
    private String checkForDuplicates(String id, String path)
    {
        boolean foundDuplicate = false;
        TableModel model = projectTable.getModel();

        for (int i = 0; i < model.getRowCount(); i++)
        {
            String nextId = (String) model.getValueAt(i, PROJ_ROW_ID);

            if (nextId.trim().equalsIgnoreCase(id.trim())
                    && model.getValueAt(i, PROJ_ID_FOR_DUPLICATES) == null)
            {
                String value = addParentToId(nextId, (String) model.getValueAt(i, PROJ_ROW_PATH));
                model.setValueAt(value, i, PROJ_ID_FOR_DUPLICATES);
                foundDuplicate = true;
            }
        }

        return foundDuplicate ? addParentToId(id, path) : null;
    }

    /**
     * Generate an alternate id that helps the user distinguish between different projects with the same name.
     *
     * @param id   The project id.
     * @param path The path to the project or package file.
     * @return The original id augmented with additional info.
     */
    private String addParentToId(String id, String path)
    {
        File file = new File(path);
        String parentPath = path.endsWith(ProjectConstants.PACKAGE_EXTENSION)
                ? "in package " + file.getName()
                : "in folder " + file.getParentFile().getName();
        return id + "   (" + parentPath + ")";
    }

    /**
     * Enables and disables the browse button. This is used to keep the user from trying to import projects while any
     * projects are in the process of being imported.
     *
     * @param shouldEnable enable import or not
     */
    private void setBrowseEnabled(boolean shouldEnable)
    {
        browseButton.setEnabled(shouldEnable);
    }

    /**
     * This method sets the project details when a row is selected in the project table.
     *
     * @param table       the project table
     * @param selectedRow the selected row in the project table.
     */
    private void setDetails(final JXTable table, final int selectedRow)
    {
        if (mousePressedInTable)
        {
            mousePressedInTable = false;
            ignoreEvent = true;
            table.clearSelection();
            int viewRow = table.convertRowIndexToView(selectedRow);
            table.setRowSelectionInterval(viewRow, viewRow);
            ignoreEvent = false;
        }

        clearRightPanel(false); // Start clean so missing old data won't hang around if we don't have new data

        DefaultTableModel dtm = (DefaultTableModel) table.getModel();
        String projectName = (String) dtm.getValueAt(selectedRow, PROJ_ROW_ID);
        String projectPath = (String) dtm.getValueAt(selectedRow, PROJ_ROW_PATH);
        String projectTitleValue = (String) dtm.getValueAt(selectedRow, PROJ_ROW_TITLE);

        projectIdTextField.setText(projectName);
        titleTextField.setText(projectTitleValue);

        File projectFile = new File(projectPath);

        // Parse the project file and update the right hand panel details
        try
        {
            loadProjectMetadata(projectFile);

            if (projectPath.endsWith(ProjectConstants.PROJECT_EXTENSION))
            {
                final var snapshotImage = snapshotService.getSnapshotForDirectory(projectFile.getParentFile(), PANEL_WIDTH, PANEL_HEIGHT);
                imagePanel.setImage(snapshotImage != null ? snapshotImage : noSnapshotImage);
            } else
            {
                imagePanel.setImage(packageImage); // No snapshot available for packages
            }
        } catch (IOException e)
        {
            logger.error("IOException parsing {} project.", projectIdTextField.getText(), e);
        }
    }

    /**
     * Cancels out of the new project/open project dialog or exits when first starting up the application.
     */
    private void cancelButtonActionPerformed()
    {
        projectSelection = ProjectSelection.makeCanceled(); // The user canceled

        clearRightPanel(true);
        setVisible(false);
    }

    /**
     * This sets the dialog in new project mode which lets you create new projects or open existing ones.
     * The only difference is that the openButton displays "Create" and will now create a new project when pressed.
     */
    private void setNewProjectMode()
    {
        createNewLabel.setText(NEW_PROJECT_DESCRIPTION_LEFT);
        existingLabel.setText("");

        if (additionalFieldsShowing)
        {
            rightDescriptionLabel.setText(DESCRIPTION_RIGHT_ADDITIONAL_SHOWING);
        } else
        {
            rightDescriptionLabel.setText(NEW_PROJECT_DESCRIPTION_RIGHT_ADDITIONAL_HIDDEN);
        }

        clearRightPanel(true);
        setRightPanelFieldsEnabled(true);
        projectTable.clearSelection();

        newButton.setSelected(true);
        projectIdTextField.setText(projectManager.getDefaultProjectName());
        openButton.setText(CREATE);
        openButton.setEnabled(true);
        newButton.requestFocusInWindow();

        // Set the WMS offline mode from the program properties default
        checkBoxOnline.setSelected(!defaultWMSOfflineMode);

        mode = StartupMode.NEW;
    }

    /**
     * This sets the dialog in existing project mode which lets you create new projects or open existing ones.
     * The only difference is that the openButton displays "Open" and will now open an existing project when pressed.
     *
     * @param selectFirstRow Pass {@code true} if the first row in the projects table should be selected
     */
    private void setExistingProjectMode(boolean selectFirstRow)
    {
        createNewLabel.setText("");
        existingLabel.setText(EXISTING_PROJECT_DESCRIPTION_LEFT);

        if (additionalFieldsShowing)
        {
            rightDescriptionLabel.setText(DESCRIPTION_RIGHT_ADDITIONAL_SHOWING);
        } else
        {
            rightDescriptionLabel.setText(EXISTING_PROJECT_DESCRIPTION_RIGHT_ADDITIONAL_HIDDEN);
        }

        existingButton.setSelected(true);
        projectTable.setEnabled(true);
        openButton.setText(OPEN);
        existingButton.requestFocusInWindow();
        setRightPanelFieldsEnabled(false);
        if (selectFirstRow)
        {
            clearRightPanel(true);

            if (projectTable.getRowCount() > 0)
            {
                projectTable.setRowSelectionInterval(0, 0);
            }
        }

        if (projectTable.getSelectedRow() == -1) openButton.setEnabled(false); // Disable if nothing selected

        mode = StartupMode.EXISTING;
    }

    /**
     * This method updates the title of the project dialog when the user is typing in a project name.
     */
    private void projectIdTextFieldKeyReleased()
    {
        openButton.setEnabled(!projectIdTextField.getText().isEmpty());
    }

    /**
     * This method trims the project id of any whitespace at the beginning and end of the project id string to avoid file
     * creation errors.
     */
    private void projectIdTextFieldFocusLost()
    {
        projectIdTextField.setText(projectIdTextField.getText().trim());
        projectIdTextFieldKeyReleased();
    }

    /**
     * If new project is selected and the user clicks the project table it automatically re enables the project table
     * and switches it to existing project mode.
     */
    private void projectTableMouseReleased()
    {
        if (newButton.isSelected())
        {
            setExistingProjectMode(false);
        }
    }

    /**
     * This is a work around for when you switch between new and existing project mode. For some reason, even if you
     * clear table selection before disabling a table, when you re-enable the table the column header of the cell that
     * was selected before disabling stays greyed out - even though functionally it's still working as enabled. It only
     * stops being greyed out once you click in that column again. This doesn't affect anything, but it's annoying
     * and looks weird. To get around this, whenever a row is selected manually by the user we clear the selection
     * and then programmatically set the row selection. This lets us know it was a user click, not a programmatic one.
     */
    private void projectTableMousePressed()
    {
        mousePressedInTable = true;
    }

    /**
     * A private class to handle row selections in the project table
     */
    private class SelectionListener implements ListSelectionListener
    {
        private final JXTable table;

        SelectionListener(JXTable table)
        {
            this.table = table;
        }

        /**
         * This updates the panel with the currently selected project info whenever a new row in the project
         * table is selected.
         *
         * @param e the selection event
         */
        @Override
        public void valueChanged(ListSelectionEvent e)
        {
            if (!e.getValueIsAdjusting() && !ignoreEvent)
            {
                int selectedRow = table.getSelectedRow();
                if (selectedRow != -1)
                {
                    selectedRow = table.convertRowIndexToModel(selectedRow);

                    setExistingProjectMode(false);
                    setOpenEnabled(true);
                    setDetails(table, selectedRow);
                } else
                {
                    setOpenEnabled(false);
                }
            }
        }
    }

    /**
     * Handles cleanup for when the user closed the dialog using the X button.
     *
     * @since 3.3
     */
    @Override
    protected void onClosedByXButton()
    {
        cancelButtonActionPerformed();
    }

    /**
     * Handles cleanup for when the user closed the dialog using the ESC key.
     *
     * @since 3.3
     */
    @Override
    protected void onEscape()
    {
        cancelButtonActionPerformed();
    }

    private void additionalButtonActionPerformed()
    {
        if (!additionalFieldsShowing)
        {
            rightPanel.add(projectDetailsPanel, cc.xy(1, 3));
            additionalButton.setIcon(hideInfoIcon);
            additionalButton.setToolTipText(HIDE_TOOLTIP);

            rightDescriptionLabel.setText(DESCRIPTION_RIGHT_ADDITIONAL_SHOWING);
        } else
        {
            rightPanel.remove(projectDetailsPanel);
            additionalButton.setIcon(showInfoIcon);
            additionalButton.setToolTipText(SHOW_TOOLTIP);

            rightDescriptionLabel.setText(mode == StartupMode.EXISTING
                    ? EXISTING_PROJECT_DESCRIPTION_RIGHT_ADDITIONAL_HIDDEN
                    : NEW_PROJECT_DESCRIPTION_RIGHT_ADDITIONAL_HIDDEN);
        }

        additionalFieldsShowing = !additionalFieldsShowing;
        pack();
    }

    private void projectDirectoryBrowseButtonActionPerformed()
    {
        JFileChooser fc = new AlwaysOnTopFileChooser(projectDirectoryTextField.getText());
        fc.setDialogTitle("Browse to a Project folder");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setMultiSelectionEnabled(false);

        int retVal = fc.showOpenDialog(this);
        if (retVal == JFileChooser.APPROVE_OPTION)
        {
            File projDir = fc.getSelectedFile();
            progressBar.setValue(0);
            projectDirectoryTextField.setText(projDir.getPath());
            programEnvironment.setProjectRoot(projDir.getPath());

            scanProjectsDirectory(projDir);
        }
    }

    private void manageButtonActionPerformed()
    {
        ProjectCleaner cleaner = new ProjectCleaner();
        cleaner.setVisible(true);

        if (cleaner.projectsChanged())
        {
            File file = new File(programEnvironment.getProjectRoot());
            scanProjectsDirectory(file);
        }
    }

    /**
     * Swing worker responsible for finding the most recent project first, if there is one, and then immediately
     * adding it to the project table and enabling the controls.
     *
     * @since 2.37
     */
    private class ProjectLoader extends ErrorReportingSwingWorker<Object, Void>
    {
        // Simple file filter to accept only project (projx) files
        private final FileFilter projectFileFilter = pathname -> pathname.getName().endsWith(ProjectConstants.PROJECT_EXTENSION);

        private int projectCount;
        private final JProgressBar progressBar;
        private final List<File> skippedProjects = new ArrayList<>();
        private final File[] projectFolders;

        /**
         * @param projectFolders the directories to search for files in.
         * @param progressBar    Progress bar to update as the scan proceeds
         */
        ProjectLoader(File[] projectFolders, JProgressBar progressBar)
        {
            super("Project Explorer", "Failed to Scan project in selected project directory");
            this.projectFolders = projectFolders;
            this.progressBar = progressBar;
            progressBar.setValue(0);
        }

        @Override
        protected Object doInBackground()
        {
            locateProjectAndPackageFiles(projectFolders);
            setupProgressBar();
            addAnySkippedFiles();
            return null;
        }

        @Override
        protected void done()
        {
            super.done();
            setBrowseEnabled(true);
        }

        /**
         * Sets up the progress bar using projectCount.
         */
        @InvokeOnEDT
        private void setupProgressBar()
        {
            progressBar.setMaximum(projectCount);
            progressBar.setString(progressBar.getValue() + "/" + projectCount);
            progressBar.setVisible(true);
        }

        /**
         * Updates the progress bar.
         */
        @InvokeOnEDT
        private void updateProgressBar()
        {
            int value = progressBar.getValue() + 1;
            progressBar.setValue(value);
            progressBar.setString(value + "/" + projectCount);
        }

        private void locateProjectAndPackageFiles(File[] directories)
        {
            for (File directory : directories)
            {
                verifyDirectoryContents(directory);
            }
        }

        /**
         * Reads all files in a given directory searching for the most recent project so that it is added to the table first.
         * Note that we only process one level down, meaning we are looking for package files in the current directory
         * and project files in the directories directly beneath the starting directory.
         *
         * @param directory the directory to search.
         */
        private void verifyDirectoryContents(File directory)
        {
            if (directory.isDirectory())
            {
                File[] projectsOrPackages = directory.listFiles(projectFileFilter);

                if (projectsOrPackages != null && projectsOrPackages.length > 0)
                {
                    for (File projectOrPackage : projectsOrPackages)
                    {
                        processProjectOrPackage(projectOrPackage);
                    }
                }
            } else
            {
                // It is a file, perhaps a package file?
                final String fileName = directory.getName();

                if (fileName.endsWith(ProjectConstants.PACKAGE_EXTENSION))
                {
                    processProjectOrPackage(directory);
                } else
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Detected an unrecognized file type in the ProjectData/Projects folder: {}", fileName);
                    }
                }
            }
        }

        /**
         * Add a project or package file to our project list.  If the given project is the most recent project, then
         * immediately add it to the table (which will cause it to be selected).  Otherwise, add it to our project list.
         *
         * @param file The project/package file
         */
        private void processProjectOrPackage(File file)
        {
            projectCount++;

            // Packages can't be the last selected project (even if they have a matching name)
            final boolean isPackage = file.getName().endsWith(ProjectConstants.PACKAGE_EXTENSION);

            if (!isPackage && FileUtils.getNameWithoutExtension(file).equals(mostRecentProjectName))
            {
                addProject(file);
            } else
            {
                skippedProjects.add(file);
            }
        }

        /**
         * This method adds any files that were skipped when traversing the projects directory when first opening the
         * project dialog and searching for the most recent project.
         */
        private void addAnySkippedFiles()
        {
            if (!skippedProjects.isEmpty())
            {
                // Sort by last modified time to get recent projects to the top
                skippedProjects.sort((f1, f2) -> -(Long.compare(f1.lastModified(), f2.lastModified())));
                skippedProjects.forEach(this::addProject);
            }
        }

        private void addProject(File file)
        {
            try
            {
                addRowForProject(file);
            } catch (IOException e)
            {
                logger.error("Error parsing project properties: {}", file, e);
            }

            updateProgressBar();
        }
    }

    /**
     * Optionally uses the alternate project id to help distinguish between duplicate project ids.
     */
    private static class IdAndPathCellRenderer extends DefaultTableCellRenderer
    {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            String alternateId = (String) table.getModel().getValueAt(table.convertRowIndexToModel(row), PROJ_ID_FOR_DUPLICATES);

            setText(alternateId != null ? alternateId : (String) value);

            return this;
        }
    }

    /**
     * Render a long value as a formatted date.
     *
     * @since 3.0
     */
    private static class DateCellRenderer extends DefaultTableCellRenderer
    {
        @Override
        public Component getTableCellRendererComponent(JTable jtable, Object value, boolean selected, boolean hasFocus, int row, int column)
        {
            value = DATE_FORMATTER.format(Instant.ofEpochMilli((Long) value));

            return super.getTableCellRendererComponent(jtable, value, selected, hasFocus, row, column);
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        panel12 = new JPanel();
        var scrollPane5 = new JScrollPane();
        projectTable = new JXTable();
        progressBar = new JProgressBar();
        rightPanel = new JPanel();
        var idPanel = new JPanel();
        var panel15 = new JPanel();
        var separator2 = new JSeparator();
        var panel1 = new JPanel();
        var panel3 = new JPanel();
        var label1 = new JLabel();
        projectIdTextField = new JTextField();
        var label4 = new JLabel();
        titleTextField = new JTextField();
        var separator1 = new JSeparator();
        var panel8 = new JPanel();
        rightDescriptionLabel = new JLabel();
        additionalButton = new JButton();
        additionalButton.setToolTipText(SHOW_TOOLTIP);
        imageScrollPane = new JScrollPane();
        imagePanel = new JXImageView();
        var buttonBar = new JPanel();
        checkBoxOnline = new JCheckBox();
        manageButton = new JButton();
        openButton = new JButton();
        cancelButton = new JButton();
        createNewLabel = new JLabel();
        panelNewOrExistingSelector = new JPanel();
        var projectPanel = new JPanel();
        newButton = new JRadioButton();
        var existingProjectPanel = new JPanel();
        existingButton = new JRadioButton();
        browseButton = new JButton();
        var panel41 = new JPanel();
        existingLabel = new JLabel();
        var panel10 = new JPanel();
        var directoryLabel = new JLabel();
        projectDirectoryTextField = new JTextField();
        var projectDirectoryBrowseButton = new JButton();
        panelProjectBrowse = new JPanel();
        var buttonBrowsProject = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Project Explorer ");
        setModal(true);
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
                        "default:grow, $ugap, default:grow",
                        "fill:default:grow"));

                //======== panel12 ========
                {
                    panel12.setName("panel12");
                    panel12.setLayout(new FormLayout(
                            "default:grow",
                            "fill:default, $lgap, fill:default:grow, $lgap, fill:default"));

                    //======== scrollPane5 ========
                    {
                        scrollPane5.setName("scrollPane5");

                        //---- projectTable ----
                        projectTable.setModel(new DefaultTableModel(
                                new Object[][]{
                                },
                                new String[]{
                                        "Project ID", "Title", "Creation Date", "Path", "Description", "ID For Duplicate"
                                }));
                        {
                            TableColumnModel cm = projectTable.getColumnModel();
                            cm.getColumn(0).setPreferredWidth(125);
                            cm.getColumn(2).setPreferredWidth(70);
                        }
                        projectTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                        projectTable.setEditable(false);
                        projectTable.setRolloverEnabled(false);
                        projectTable.setAutoStartEditOnKeyStroke(false);
                        projectTable.setTerminateEditOnFocusLost(false);
                        projectTable.setVisibleRowCount(13);
                        projectTable.setShowVerticalLines(false);
                        projectTable.setHorizontalScrollEnabled(true);
                        projectTable.setShowHorizontalLines(false);
                        projectTable.setPreferredScrollableViewportSize(new Dimension(300, 200));
                        projectTable.setName("projectTable");
                        projectTable.addMouseListener(new MouseAdapter()
                        {
                            @Override
                            public void mousePressed(MouseEvent e)
                            {
                                projectTableMousePressed();
                            }

                            @Override
                            public void mouseReleased(MouseEvent e)
                            {
                                projectTableMouseReleased();
                            }
                        });
                        scrollPane5.setViewportView(projectTable);
                    }
                    panel12.add(scrollPane5, cc.xy(1, 3));

                    //---- progressBar ----
                    progressBar.setStringPainted(true);
                    progressBar.setName("progressBar");
                    panel12.add(progressBar, cc.xy(1, 5));
                }
                contentPanel.add(panel12, cc.xy(1, 1, CellConstraints.LEFT, CellConstraints.TOP));

                //======== rightPanel ========
                {
                    rightPanel.setName("rightPanel");
                    rightPanel.setLayout(new FormLayout(
                            "default:grow",
                            "fill:default, $nlgap, fill:default, $lgap, fill:default:grow"));

                    //======== idPanel ========
                    {
                        idPanel.setName("idPanel");
                        idPanel.setLayout(new FormLayout(
                                "default:grow",
                                "fill:default:grow"));

                        //======== panel15 ========
                        {
                            panel15.setName("panel15");
                            panel15.setLayout(new FormLayout(
                                    "default:grow",
                                    "default, $lgap, fill:default:grow, $lgap, default"));

                            //---- separator2 ----
                            separator2.setName("separator2");
                            panel15.add(separator2, cc.xy(1, 1));

                            //======== panel1 ========
                            {
                                panel1.setName("panel1");
                                panel1.setLayout(new FormLayout(
                                        "default:grow",
                                        "fill:default"));

                                //======== panel3 ========
                                {
                                    panel3.setName("panel3");
                                    panel3.setLayout(new FormLayout(
                                            "default, $lcgap, default:grow",
                                            "default, $lgap, default"));

                                    //---- label1 ----
                                    label1.setText("Project ID");
                                    label1.setName("label1");
                                    panel3.add(label1, cc.xy(1, 1));

                                    //---- projectIdTextField ----
                                    projectIdTextField.setName("projectIdTextField");
                                    projectIdTextField.addKeyListener(new KeyAdapter()
                                    {
                                        @Override
                                        public void keyReleased(KeyEvent e)
                                        {
                                            projectIdTextFieldKeyReleased();
                                        }
                                    });
                                    projectIdTextField.addFocusListener(new FocusAdapter()
                                    {
                                        @Override
                                        public void focusLost(FocusEvent e)
                                        {
                                            projectIdTextFieldFocusLost();
                                        }
                                    });
                                    panel3.add(projectIdTextField, cc.xy(3, 1));

                                    //---- label4 ----
                                    label4.setText("Title");
                                    label4.setName("label4");
                                    panel3.add(label4, cc.xy(1, 3));

                                    //---- titleTextField ----
                                    titleTextField.setName("titleTextField");
                                    titleTextField.addKeyListener(new KeyAdapter()
                                    {
                                        @Override
                                        public void keyReleased(KeyEvent e)
                                        {
                                            projectIdTextFieldKeyReleased();
                                        }
                                    });
                                    titleTextField.addFocusListener(new FocusAdapter()
                                    {
                                        @Override
                                        public void focusLost(FocusEvent e)
                                        {
                                            projectIdTextFieldFocusLost();
                                        }
                                    });
                                    panel3.add(titleTextField, cc.xy(3, 3, CellConstraints.DEFAULT, CellConstraints.TOP));
                                }
                                panel1.add(panel3, cc.xy(1, 1));
                            }
                            panel15.add(panel1, cc.xy(1, 3));

                            //---- separator1 ----
                            separator1.setName("separator1");
                            panel15.add(separator1, cc.xy(1, 4, CellConstraints.DEFAULT, CellConstraints.CENTER));

                            //======== panel8 ========
                            {
                                panel8.setName("panel8");
                                panel8.setLayout(new FormLayout(
                                        "default:grow, $lcgap, default",
                                        "fill:default"));

                                //---- rightDescriptionLabel ----
                                rightDescriptionLabel.setName("rightDescriptionLabel");
                                panel8.add(rightDescriptionLabel, cc.xy(1, 1));

                                //---- additionalButton ----
                                additionalButton.setVerticalTextPosition(SwingConstants.BOTTOM);
                                additionalButton.setHorizontalTextPosition(SwingConstants.CENTER);
                                additionalButton.setIconTextGap(1);
                                additionalButton.setName("additionalButton");
                                additionalButton.addActionListener(e -> additionalButtonActionPerformed());
                                panel8.add(additionalButton, cc.xy(3, 1));
                            }
                            panel15.add(panel8, cc.xy(1, 5, CellConstraints.CENTER, CellConstraints.DEFAULT));
                        }
                        idPanel.add(panel15, cc.xy(1, 1));
                    }
                    rightPanel.add(idPanel, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

                    //======== imageScrollPane ========
                    {
                        imageScrollPane.setName("imageScrollPane");

                        //---- imagePanel ----
                        imagePanel.setEditable(false);
                        imagePanel.setScale(0.0);
                        imagePanel.setName("imagePanel");
                        imageScrollPane.setViewportView(imagePanel);
                    }
                    rightPanel.add(imageScrollPane, cc.xy(1, 5));
                }
                contentPanel.add(rightPanel, cc.xy(3, 1));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "default:grow, $lcgap, default, $lcgap, $button, $lcgap, default:grow, $button, $lcgap, $button",
                        "fill:default"));

                //---- checkBoxOnline ----
                checkBoxOnline.setText("Connect to External Imagery and Elevation Servers");
                checkBoxOnline.setSelected(true);
                checkBoxOnline.setToolTipText("If checked, TAKX will automatically connect to WMS servers associated with imagery and elevation layers to obtain data for those layers.");
                checkBoxOnline.setName("checkBoxOnline");
                buttonBar.add(checkBoxOnline, cc.xy(1, 1, CellConstraints.LEFT, CellConstraints.CENTER));

                //---- manageButton ----
                manageButton.setText("Delete Projects");
                manageButton.setName("manageButton");
                manageButton.addActionListener(e -> manageButtonActionPerformed());
                buttonBar.add(manageButton, cc.xy(5, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

                //---- openButton ----
                openButton.setText("Open");
                openButton.setName("openButton");
                openButton.addActionListener(e -> openButtonActionPerformed());
                buttonBar.add(openButton, cc.xy(8, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonBar.add(cancelButton, cc.xy(10, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());

        //---- createNewLabel ----
        createNewLabel.setName("createNewLabel");

        //======== panelNewOrExistingSelector ========
        {
            panelNewOrExistingSelector.setName("panelNewOrExistingSelector");
            panelNewOrExistingSelector.setLayout(new FormLayout(
                    "default:grow",
                    "3*(default, $lgap), default"));

            //======== projectPanel ========
            {
                projectPanel.setName("projectPanel");
                projectPanel.setLayout(new FormLayout(
                        "default:grow",
                        "top:default"));

                //---- newButton ----
                newButton.setText("Create New ");
                newButton.setName("newButton");
                newButton.addActionListener(e -> newButtonActionPerformed());
                projectPanel.add(newButton, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.CENTER));
            }
            panelNewOrExistingSelector.add(projectPanel, cc.xy(1, 1, CellConstraints.LEFT, CellConstraints.TOP));

            //======== existingProjectPanel ========
            {
                existingProjectPanel.setName("existingProjectPanel");
                existingProjectPanel.setLayout(new FormLayout(
                        "default:grow, $lcgap, $button",
                        "pref"));

                //---- existingButton ----
                existingButton.setText("Open Existing");
                existingButton.setName("existingButton");
                existingButton.addActionListener(e -> existingButtonActionPerformed());
                existingProjectPanel.add(existingButton, cc.xy(1, 1, CellConstraints.LEFT, CellConstraints.CENTER));

                //---- browseButton ----
                browseButton.setText("Browse");
                browseButton.setToolTipText("Click to locate and open a .projx or .pkgx file");
                browseButton.setName("browseButton");
                browseButton.addActionListener(e -> browseButtonActionPerformed());
                existingProjectPanel.add(browseButton, cc.xy(3, 1, CellConstraints.DEFAULT, CellConstraints.CENTER));
            }
            panelNewOrExistingSelector.add(existingProjectPanel, cc.xy(1, 3, CellConstraints.LEFT, CellConstraints.CENTER));

            //======== panel41 ========
            {
                panel41.setName("panel41");
                panel41.setLayout(new FormLayout(
                        "center:default",
                        "fill:default"));

                //---- existingLabel ----
                existingLabel.setName("existingLabel");
                panel41.add(existingLabel, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.BOTTOM));
            }
            panelNewOrExistingSelector.add(panel41, cc.xy(1, 5));

            //======== panel10 ========
            {
                panel10.setName("panel10");
                panel10.setLayout(new FormLayout(
                        "default, $lcgap, default:grow, $lcgap, default",
                        "default"));

                //---- directoryLabel ----
                directoryLabel.setText("Directory:");
                directoryLabel.setName("directoryLabel");
                panel10.add(directoryLabel, cc.xy(1, 1));

                //---- projectDirectoryTextField ----
                projectDirectoryTextField.setEditable(false);
                projectDirectoryTextField.setName("projectDirectoryTextField");
                panel10.add(projectDirectoryTextField, cc.xy(3, 1));

                //---- projectDirectoryBrowseButton ----
                projectDirectoryBrowseButton.setText("Browse");
                projectDirectoryBrowseButton.setName("projectDirectoryBrowseButton");
                projectDirectoryBrowseButton.addActionListener(e -> projectDirectoryBrowseButtonActionPerformed());
                panel10.add(projectDirectoryBrowseButton, cc.xy(5, 1));
            }
            panelNewOrExistingSelector.add(panel10, cc.xy(1, 7));
        }

        //======== panelProjectBrowse ========
        {
            panelProjectBrowse.setName("panelProjectBrowse");
            panelProjectBrowse.setLayout(new FormLayout(
                    "default:grow, $lcgap, $button",
                    "default:grow, $lgap, default"));

            //---- buttonBrowsProject ----
            buttonBrowsProject.setText("Browse");
            buttonBrowsProject.setToolTipText("Click to locate and open a .projx or .pkgx file");
            buttonBrowsProject.setName("buttonBrowsProject");
            buttonBrowsProject.addActionListener(e -> browseButtonActionPerformed());
            panelProjectBrowse.add(buttonBrowsProject, cc.xy(3, 3));
        }

        //---- buttonGroupNewOrExisting ----
        var buttonGroupNewOrExisting = new ButtonGroup();
        buttonGroupNewOrExisting.add(newButton);
        buttonGroupNewOrExisting.add(existingButton);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel panel12;
    private JXTable projectTable;
    private JProgressBar progressBar;
    private JPanel rightPanel;
    private JTextField projectIdTextField;
    private JTextField titleTextField;
    private JLabel rightDescriptionLabel;
    private JButton additionalButton;
    private JScrollPane imageScrollPane;
    private JXImageView imagePanel;
    private JCheckBox checkBoxOnline;
    private JButton manageButton;
    private JButton openButton;
    private JButton cancelButton;
    private JLabel createNewLabel;
    private JPanel panelNewOrExistingSelector;
    private JRadioButton newButton;
    private JRadioButton existingButton;
    private JButton browseButton;
    private JLabel existingLabel;
    private JTextField projectDirectoryTextField;
    private JPanel panelProjectBrowse;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
