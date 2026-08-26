package gov.takx.core.view.gui.projectadmin;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.project.IProjectManager;
import gov.takx.api.project.IProjectMetadata;
import gov.takx.api.project.ProjectConstants;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.ErrorReportingSwingWorker;
import gov.takx.core.project.ProjectAssistant;
import gov.takx.core.view.gui.screenimageutils.ProjectSnapshotService;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.jdesktop.swingx.JXTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@PseudoBean
public class ProjectCleaner extends AStandardDialog
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final ChronoUnit[] TIMEFRAME_UNITS = {ChronoUnit.DAYS, ChronoUnit.WEEKS, ChronoUnit.MONTHS};
    private static final ChronoUnit DEFAULT_TIMEFRAME_UNIT = ChronoUnit.MONTHS;

    @Inject
    private IProjectManager projectManager;

    @Inject
    private ProjectSnapshotService snapshotService;

    @Inject
    private KnownProjectDirectoriesManager knownProjectDirectoriesManager;

    // Folders that may hold project folders
    private final Set<Path> projectDataDirectories = new CopyOnWriteArraySet<>();

    // Used to map from proj/pkg file (abs path) to parsed metadata
    private final Map<Path, IProjectMetadata> metadataMap = new HashMap<>();

    private final TableRowSorter<TableModel> sorter;

    private ImageIcon noSnapshotImage;
    private boolean projectsChanged;

    public ProjectCleaner()
    {
        initComponents();

        setModal(true);

        projectDetailsPanel.enableEditing(false);

        sorter = new TableRowSorter<>(projectTable.getModel());
        projectTable.setRowSorter(sorter);
        projectTable.getTableHeader().setReorderingAllowed(false);
        projectTable.setDefaultRenderer(Object.class, new ProjectCleanerTableRenderer());
        projectTable.getSelectionModel().addListSelectionListener(e -> tableRowSelected());

        DefaultComboBoxModel<ChronoUnit> comboBoxModel = new DefaultComboBoxModel<>(TIMEFRAME_UNITS);
        chronoUnitComboBox.setModel(comboBoxModel);
        chronoUnitComboBox.setSelectedItem(DEFAULT_TIMEFRAME_UNIT);
    }

    @PostConstruct
    private void postConstruct()
    {
        // Add the projects directory
        projectDataDirectories.add(projectManager.getProjectsDirectory().toPath());

        // Add any other directories where projects have been saved or opened
        projectDataDirectories.addAll(knownProjectDirectoriesManager.getKnownProjectDirectories());

        // Find all the projects in the directories and add them to the UI
        refreshProjects();

        // Create the default "No Snapshot" preview image
        noSnapshotImage = new ImageIcon(snapshotService.makeCaptionImage(false, imagePanel.getWidth(), imagePanel.getHeight()));

        pack();
    }

    @InvokeOnEDT
    private void refreshProjects()
    {
        // Clear the detail panel
        clearProjectDetailsPanel();

        // Clear the table
        DefaultTableModel model = (DefaultTableModel) projectTable.getModel();
        model.setRowCount(0);

        // Read the project metadata in the background and then add the entries to the table
        ErrorReportingSwingWorker<Void, ExtendedProjectMetadata> worker = new ErrorReportingSwingWorker<>("Project Cleaner", "Failed to find project files to clean")
        {
            @Override
            protected Void doInBackground()
            {
                List<Path> projectFiles = findProjectFiles();
                for (Path projectFile : projectFiles)
                {
                    try
                    {
                        IProjectMetadata metadata = getMetaData(projectFile);
                        ExtendedProjectMetadata extendedMetadata = new ExtendedProjectMetadata(metadata, projectFile);
                        publish(extendedMetadata);
                    } catch (IOException e)
                    {
                        logger.error("IOException parsing project file {}", projectFile);
                    }
                }
                return null;
            }

            @Override
            protected void process(List<ExtendedProjectMetadata> metadataList)
            {
                metadataList.forEach(m -> addToTable(m));
            }
        };
        worker.execute();
    }

    private IProjectMetadata getMetaData(Path projectFile) throws IOException
    {
        if (!metadataMap.containsKey(projectFile))
        {
            // Not found, go get it and put it into the map for later
            IProjectMetadata pmd = new ProjectAssistant().parseProjectFile(projectFile.toFile());
            metadataMap.put(projectFile, pmd);
        }

        return metadataMap.get(projectFile);
    }

    private void addToTable(ExtendedProjectMetadata metadata)
    {
        Object[] row = new Object[TableColumn.values().length];
        row[TableColumn.DELETE.index()] = Boolean.FALSE;
        row[TableColumn.PROJECT_ID.index()] = metadata.getId();
        row[TableColumn.TITLE.index()] = metadata.getTitle();
        row[TableColumn.CREATED.index()] = metadata.getCreated();
        row[TableColumn.LAST_MODIFIED.index()] = metadata.getLastModified();
        row[TableColumn.PATH.index()] = metadata.getProjectFile();

        DefaultTableModel model = (DefaultTableModel) projectTable.getModel();
        model.addRow(row);
    }

    private List<Path> findProjectFiles()
    {
        // Given a directory
        // C:/Path/to/Projects/
        // Find all the valid .projx files that are nested like this
        // C:/Path/to/Projects/2016_07_26_11_45_02/2016_07_26_11_45_02.projx
        // C:/Path/to/Projects/2016_07_26_12_15_02/2016_07_26_12_15_02.projx
        // C:/Path/to/Projects/2016_07_26_12_31_23/2016_07_26_12_31_23.projx
        return projectDataDirectories.stream()
                .filter(Files::isDirectory)
                .flatMap(ProjectCleaner::listFiles)
                .filter(Files::isDirectory)
                .flatMap(ProjectCleaner::listFiles)
                .filter(this::isValidProjectFile)
                .collect(Collectors.toList());
    }

    private boolean isValidProjectFile(Path file)
    {
        return Files.isRegularFile(file)
                && isProjectFile(file)
                && hasData(file)
                && !isOpenProject(file);
    }

    private static boolean isProjectFile(Path file)
    {
        return ProjectConstants.PROJECT_EXTENSION.equals(FileUtils.getExtension(file.toFile()));
    }

    private static boolean hasData(Path file)
    {
        try
        {
            return Files.size(file) > 0;
        } catch (IOException e)
        {
            logger.debug("Couldn't get size of {}", file, e);
            return false;
        }
    }

    private boolean isOpenProject(Path file)
    {
        final String projectDirectory = projectManager.getProjectDirectory();

        if (projectDirectory == null)
        {
            return false;
        }

        try
        {
            Path openProject = Paths.get(projectDirectory);
            return file.getParent().equals(openProject);
        } catch (InvalidPathException e)
        {
            logger.debug("InvalidPathException for {}: {}", projectDirectory, e);
            return false;
        }
    }

    private static Stream<Path> listFiles(Path path)
    {
        try
        {
            return Files.list(path);
        } catch (IOException e)
        {
            logger.debug("Couldn't list files in {}", path, e);
            return Stream.empty();
        }
    }

    public boolean projectsChanged()
    {
        return projectsChanged;
    }

    private void selectAllButtonActionPerformed()
    {
        for (int row = 0; row < projectTable.getModel().getRowCount(); ++row)
        {
            projectTable.getModel().setValueAt(Boolean.TRUE, row, TableColumn.DELETE.index());
        }
    }

    private void selectNoneButtonActionPerformed()
    {
        for (int row = 0; row < projectTable.getModel().getRowCount(); ++row)
        {
            projectTable.getModel().setValueAt(Boolean.FALSE, row, TableColumn.DELETE.index());
        }
    }

    private void filterMonthsCheckBoxActionPerformed()
    {
        updateTableFilter();
    }

    private void monthSpinnerStateChanged()
    {
        updateTableFilter();
    }

    private void chronoUnitComboBoxActionPerformed()
    {
        updateTableFilter();
    }

    private void updateTableFilter()
    {
        if (filterMonthsCheckBox.isSelected())
        {
            final int timeframe = (int) timeframeSpinner.getValue();
            final ChronoUnit chronoUnit = chronoUnitComboBox.getItemAt(chronoUnitComboBox.getSelectedIndex());
            sorter.setRowFilter(new CreatedTimeRowFilter(timeframe, chronoUnit));
        } else
        {
            sorter.setRowFilter(null);
        }
    }

    private void cleanButtonActionPerformed()
    {
        // Get all the selected projects in the table
        List<Path> toDelete = new ArrayList<>();
        TableModel tableModel = projectTable.getModel();
        for (int i = 0; i < projectTable.getRowCount(); ++i)
        {
            int modelRow = projectTable.convertRowIndexToModel(i);
            Boolean selected = (Boolean) tableModel.getValueAt(modelRow, TableColumn.DELETE.index());
            if (selected != null && selected)
            {
                Path path = (Path) tableModel.getValueAt(modelRow, TableColumn.PATH.index());
                toDelete.add(path.getParent());
            }
        }

        if (!toDelete.isEmpty())
        {
            // Show the confirm dialog with the selected projects, afterwards update the table if necessary
            ProjectCleanerConfirmDialog confirmDialog = new ProjectCleanerConfirmDialog(this, toDelete);
            confirmDialog.setVisible(true);
            if (confirmDialog.projectsChanged())
            {
                projectsChanged = true;
                refreshProjects();
            }
        } else
        {
            AlwaysOnTopJOptionPane.showErrorDialog("Please select at least one project to delete", "No Projects Selected");
        }
    }

    private void doneButtonActionPerformed()
    {
        setVisible(false);
        dispose();
    }

    private void tableRowSelected()
    {
        int row = projectTable.getSelectedRow();

        if (row < 0)
        {
            projectIdTextField.setText("");
            titleTextField.setText("");
            projectLocationTextField.setText("");

            clearProjectDetailsPanel();
        } else
        {
            String projectId = (String) projectTable.getValueAt(row, TableColumn.PROJECT_ID.index());
            String title = (String) projectTable.getValueAt(row, TableColumn.TITLE.index());
            Path path = (Path) projectTable.getValueAt(row, TableColumn.PATH.index());

            projectIdTextField.setText(projectId);
            titleTextField.setText(title);
            projectLocationTextField.setText(path.toString());

            projectDetailsPanel.setProjectDetails(metadataMap.get(path));

            final var image = snapshotService.getSnapshotForDirectory(path.getParent().toFile(), 360, 300);
            imagePanel.setIcon(image != null ? new ImageIcon(image) : noSnapshotImage);
        }
    }

    @InvokeOnEDT
    private void clearProjectDetailsPanel()
    {
        projectIdTextField.setText("");
        titleTextField.setText("");
        projectDetailsPanel.clear();
        imagePanel.setIcon(noSnapshotImage);
    }

    private void manageLocationsButtonActionPerformed()
    {
        ProjectCleanerDirectoryDialog dialog = new ProjectCleanerDirectoryDialog(this, projectDataDirectories);
        dialog.setVisible(true);

        if (dialog.getDirectoriesChanged())
        {
            // Update the table showing the current projects
            final Set<Path> directories = new HashSet<>(dialog.getDirectories());
            projectDataDirectories.clear();
            projectDataDirectories.addAll(directories);
            refreshProjects();

            knownProjectDirectoriesManager.setKnownDirectories(directories);// Update the preference
        }
    }

    enum TableColumn
    {
        DELETE(0),
        PROJECT_ID(1),
        TITLE(2),
        LAST_MODIFIED(3),
        CREATED(4),
        PATH(5);

        private final int index;

        TableColumn(int index)
        {
            this.index = index;
        }

        public int index()
        {
            return index;
        }
    }

    private static class ProjectCleanerTableRenderer extends DefaultTableCellRenderer
    {
        @Override
        public void setValue(Object value)
        {
            if (value instanceof LocalDateTime localDateTime)
            {
                setText(FORMATTER.format(localDateTime));
            } else
            {
                super.setValue(value);
            }
        }
    }

    private static class ExtendedProjectMetadata
    {
        boolean selected;
        Path projectFile;
        String id;
        String title;
        String description;
        LocalDateTime created;
        LocalDateTime lastModified;

        public ExtendedProjectMetadata(IProjectMetadata pmd, Path projectFile)
        {
            this.projectFile = projectFile;
            selected = false;
            id = FileUtils.getNameWithoutExtension(projectFile.toFile());
            title = pmd.getTitle() == null ? "None" : pmd.getTitle();
            description = pmd.getDescription() == null ? "No description" : pmd.getDescription();
            created = LocalDateTime.ofEpochSecond(pmd.getCreationDate() / 1000, 0, ZoneOffset.UTC);
            BasicFileAttributes attributes = null;
            try
            {
                attributes = Files.readAttributes(projectFile, BasicFileAttributes.class);
            } catch (IOException e)
            {
                logger.debug("Failed reading file attributes for {}", projectFile);
            }
            if (attributes != null)
            {
                if (pmd.getCreationDate() == 0)
                {
                    created = LocalDateTime.ofInstant(attributes.creationTime().toInstant(), ZoneId.systemDefault());
                }
                lastModified = LocalDateTime.ofInstant(attributes.lastModifiedTime().toInstant(), ZoneId.systemDefault());
            } else
            {
                lastModified = LocalDateTime.ofInstant(new Date(pmd.getEndDate()).toInstant(), ZoneId.systemDefault());
            }
        }

        public void setSelected(boolean selected)
        {
            this.selected = selected;
        }

        public boolean isSelected()
        {
            return selected;
        }

        public String getId()
        {
            return id;
        }

        public String getTitle()
        {
            return title;
        }

        public LocalDateTime getCreated()
        {
            return created;
        }

        public LocalDateTime getLastModified()
        {
            return lastModified;
        }

        public Path getProjectFile()
        {
            return projectFile;
        }
    }

    /**
     * Row filter based on the creation time of the project.
     */
    private static class CreatedTimeRowFilter extends RowFilter<TableModel, Integer>
    {
        private final int timeframe;
        private final ChronoUnit chronoUnit;

        public CreatedTimeRowFilter(int timeframe, ChronoUnit chronoUnit)
        {
            this.timeframe = timeframe;
            this.chronoUnit = chronoUnit;
        }

        @Override
        public boolean include(Entry<? extends TableModel, ? extends Integer> entry)
        {
            LocalDateTime created = (LocalDateTime) entry.getValue(TableColumn.CREATED.index());
            if (created == null)
            {
                logger.debug("Couldn't get created value for {}", entry);
                return false;
            }
            return created.isBefore(LocalDateTime.now().minus(timeframe, chronoUnit));
        }
    }

    @SuppressWarnings("FieldMayBeFinal")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        var label2 = new JLabel();
        var manageLocationsButton = new JButton();
        var scrollPane1 = new JScrollPane();
        projectTable = new JXTable();
        var panel1 = new JPanel();
        var selectAllButton = new JButton();
        var selectNoneButton = new JButton();
        var panel4 = new JPanel();
        var panel5 = new JPanel();
        var label3 = new JLabel();
        projectIdTextField = new JTextField();
        var label8 = new JLabel();
        titleTextField = new JTextField();
        var label4 = new JLabel();
        projectLocationTextField = new JTextField();
        projectDetailsPanel = new ProjectDetailsPanel();
        imagePanel = new JLabel();
        var panel2 = new JPanel();
        filterMonthsCheckBox = new JCheckBox();
        timeframeSpinner = new JSpinner();
        chronoUnitComboBox = new JComboBox<>();
        var label1 = new JLabel();
        var panel3 = new JPanel();
        var cleanButton = new JButton();
        var doneButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Project Cleaner");
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
                        "default:grow, $lcgap, default",
                        "4*(default, $lgap), default"));

                //---- label2 ----
                label2.setText("Select projects to delete from the file system.");
                label2.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
                label2.setName("label2");
                contentPanel.add(label2, cc.xy(1, 1));

                //---- manageLocationsButton ----
                manageLocationsButton.setText("Manage Locations");
                manageLocationsButton.setName("manageLocationsButton");
                manageLocationsButton.addActionListener(e -> manageLocationsButtonActionPerformed());
                contentPanel.add(manageLocationsButton, cc.xy(3, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));

                //======== scrollPane1 ========
                {
                    scrollPane1.setName("scrollPane1");

                    //---- projectTable ----
                    projectTable.setModel(new DefaultTableModel(
                            new Object[][]{
                            },
                            new String[]{
                                    "Delete?", "Project ID", "Title", "Last Used", "Created", "Project Location"
                            })
                    {
                        Class<?>[] columnTypes = new Class<?>[]{
                                Boolean.class, String.class, String.class, Object.class, Object.class, Object.class
                        };
                        boolean[] columnEditable = new boolean[]{
                                true, false, false, false, false, false
                        };

                        @Override
                        public Class<?> getColumnClass(int columnIndex)
                        {
                            return columnTypes[columnIndex];
                        }

                        @Override
                        public boolean isCellEditable(int rowIndex, int columnIndex)
                        {
                            return columnEditable[columnIndex];
                        }
                    });
                    {
                        TableColumnModel cm = projectTable.getColumnModel();
                        cm.getColumn(0).setPreferredWidth(50);
                        cm.getColumn(1).setPreferredWidth(120);
                        cm.getColumn(5).setPreferredWidth(350);
                    }
                    projectTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
                    projectTable.setVisibleRowCount(8);
                    projectTable.setPreferredScrollableViewportSize(new Dimension(300, 100));
                    projectTable.setName("projectTable");
                    scrollPane1.setViewportView(projectTable);
                }
                contentPanel.add(scrollPane1, cc.xywh(1, 3, 3, 1));

                //======== panel1 ========
                {
                    panel1.setName("panel1");
                    panel1.setLayout(new FormLayout(
                            "$button, $lcgap, $button",
                            "default"));

                    //---- selectAllButton ----
                    selectAllButton.setText("Select All");
                    selectAllButton.setName("selectAllButton");
                    selectAllButton.addActionListener(e -> selectAllButtonActionPerformed());
                    panel1.add(selectAllButton, cc.xy(1, 1));

                    //---- selectNoneButton ----
                    selectNoneButton.setText("Select None");
                    selectNoneButton.setName("selectNoneButton");
                    selectNoneButton.addActionListener(e -> selectNoneButtonActionPerformed());
                    panel1.add(selectNoneButton, cc.xy(3, 1));
                }
                contentPanel.add(panel1, cc.xy(1, 5));

                //======== panel4 ========
                {
                    panel4.setBorder(new TitledBorder("Project Details"));
                    panel4.setName("panel4");
                    panel4.setLayout(new FormLayout(
                            "default:grow, $lcgap, [360px,default]:grow",
                            "2*(default, $lgap), default"));

                    //======== panel5 ========
                    {
                        panel5.setName("panel5");
                        panel5.setLayout(new FormLayout(
                                "default, $lcgap, default:grow",
                                "2*(default, $lgap), default"));

                        //---- label3 ----
                        label3.setText("Project ID");
                        label3.setName("label3");
                        panel5.add(label3, cc.xy(1, 1));

                        //---- projectIdTextField ----
                        projectIdTextField.setEditable(false);
                        projectIdTextField.setName("projectIdTextField");
                        panel5.add(projectIdTextField, cc.xy(3, 1));

                        //---- label8 ----
                        label8.setText("Title");
                        label8.setName("label8");
                        panel5.add(label8, cc.xy(1, 3));

                        //---- titleTextField ----
                        titleTextField.setEditable(false);
                        titleTextField.setName("titleTextField");
                        panel5.add(titleTextField, cc.xy(3, 3));

                        //---- label4 ----
                        label4.setText("Location");
                        label4.setName("label4");
                        panel5.add(label4, cc.xy(1, 5));

                        //---- projectLocationTextField ----
                        projectLocationTextField.setEditable(false);
                        projectLocationTextField.setName("projectLocationTextField");
                        panel5.add(projectLocationTextField, cc.xy(3, 5));
                    }
                    panel4.add(panel5, cc.xy(1, 1));

                    //---- projectDetailsPanel ----
                    projectDetailsPanel.setName("projectDetailsPanel");
                    panel4.add(projectDetailsPanel, cc.xy(1, 3));

                    //---- imagePanel ----
                    imagePanel.setName("imagePanel");
                    panel4.add(imagePanel, cc.xywh(3, 1, 1, 5));
                }
                contentPanel.add(panel4, cc.xywh(1, 7, 3, 1));

                //======== panel2 ========
                {
                    panel2.setName("panel2");
                    panel2.setLayout(new FormLayout(
                            "3*(default, $lcgap), default",
                            "default"));

                    //---- filterMonthsCheckBox ----
                    filterMonthsCheckBox.setText("Only show projects older than");
                    filterMonthsCheckBox.setName("filterMonthsCheckBox");
                    filterMonthsCheckBox.addActionListener(e -> filterMonthsCheckBoxActionPerformed());
                    panel2.add(filterMonthsCheckBox, cc.xy(1, 1));

                    //---- timeframeSpinner ----
                    timeframeSpinner.setModel(new SpinnerNumberModel(0, 0, 12, 1));
                    timeframeSpinner.setName("timeframeSpinner");
                    timeframeSpinner.addChangeListener(e -> monthSpinnerStateChanged());
                    panel2.add(timeframeSpinner, cc.xy(3, 1));

                    //---- chronoUnitComboBox ----
                    chronoUnitComboBox.setName("chronoUnitComboBox");
                    chronoUnitComboBox.addActionListener(e -> chronoUnitComboBoxActionPerformed());
                    panel2.add(chronoUnitComboBox, cc.xy(5, 1));

                    //---- label1 ----
                    label1.setText("(or longer)");
                    label1.setName("label1");
                    panel2.add(label1, cc.xy(7, 1));
                }
                contentPanel.add(panel2, cc.xy(1, 9));

                //======== panel3 ========
                {
                    panel3.setName("panel3");
                    panel3.setLayout(new FormLayout(
                            "$button, $lcgap, $button",
                            "default"));

                    //---- cleanButton ----
                    cleanButton.setText("Delete Selected");
                    cleanButton.setName("cleanButton");
                    cleanButton.addActionListener(e -> cleanButtonActionPerformed());
                    panel3.add(cleanButton, cc.xy(1, 1));

                    //---- doneButton ----
                    doneButton.setText("Done");
                    doneButton.setName("doneButton");
                    doneButton.addActionListener(e -> doneButtonActionPerformed());
                    panel3.add(doneButton, cc.xy(3, 1));
                }
                contentPanel.add(panel3, cc.xy(3, 9));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JXTable projectTable;
    private JTextField projectIdTextField;
    private JTextField titleTextField;
    private JTextField projectLocationTextField;
    private ProjectDetailsPanel projectDetailsPanel;
    private JLabel imagePanel;
    private JCheckBox filterMonthsCheckBox;
    private JSpinner timeframeSpinner;
    private JComboBox<ChronoUnit> chronoUnitComboBox;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
