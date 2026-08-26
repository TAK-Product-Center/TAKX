package gov.takx.core.view.gui.cachecleaner;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.nasa.worldwindx.examples.util.FileStoreDataSet;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.commons.swing.AlwaysOnTopFileChooser;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.StatusDialog;
import gov.takx.core.view.gui.DialogHeaderPanel;
import gov.takx.platform.ui.AStandardDialog;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 */
public class CacheCleanerDialog extends AStandardDialog implements CacheContentListener, CacheCleanerTaskListener
{
    private final CacheContents cacheContents;
    private final DefaultListModel<File> locationListModel;
    private final Set<File> locationsUpdatingSet = Collections.synchronizedSet(new HashSet<>());
    private final AlwaysOnTopFileChooser locationChooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
    private final AddDrivePopupMenu addDrivePopup = new AddDrivePopupMenu(this);
    private StatusDialog cleanTaskStatusDialog = null;

    public CacheCleanerDialog()
    {
        initComponents();

        // Set up the location chooser to look for directories
        initLocationChooser();

        setDefaultCloseOperation(HIDE_ON_CLOSE);

        dialogHeaderPanel.setDescription("<html>Select a Map Set location in the folder list and its contents will be " +
                "displayed in the lower panel.  Select the Map Set(s) you wish to clean from the lower panel and press " +
                "the clean button to remove all image or elevation data associated with that Map Set.  If you wish to " +
                "keep the most recently used data in a Map Set you are cleaning, click on the check box at the bottom " +
                "of the panel with the title \"Only delete Map Set data unused in X months (or longer)\" and choose a " +
                "month value to remove old data which has not been used in a long time.</html>");
        dialogHeaderPanel.setIcon(ButtonType.CACHE_CLEANER.getImageUrl());

        locationListModel = new DefaultListModel<>();
        locationList.setModel(locationListModel);
        locationList.addListSelectionListener(e -> {
            // Whenever a different location is selected, the clean button should be disabled
            cleanButton.setEnabled(false);

            // Attempt to load the cache table with the contents of the selected cache location
            populateCacheTable();
        });

        // Set up a cell renderer for the location JList. When locations are updating, they will display with
        // italics, grayed-out, and an 'updating' suffix.
        locationList.setCellRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
            {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                File f = (File) value;
                String text = f.toString();
                if (locationsUpdatingSet.contains(f))
                {
                    text += "   (Updating...)";
                    final Font font = getFont();
                    setFont(font.deriveFont(font.getStyle() | Font.ITALIC));
                    setForeground(Color.GRAY); // TODO KMB: Fix for FlatLaf (TAKX-739)
                }
                setText(text);
                return this;
            }
        });

        // Instantiate the cache contents object. This class keeps track of the cache locations and generates
        // collections of cache information data sets.
        cacheContents = new CacheContents(this);

        // Fill the location JList with the default cache locations
        populateLocationList(true);
    }

    /**
     * CacheContentListener method which is notified when a particular cache location has started updating.
     *
     * @param location File cache location
     */
    @Override
    public void cacheUpdateStarted(File location)
    {
        // Add this File location to the set of currently updating locations (concurrent set)
        locationsUpdatingSet.add(location);
        repaint();
    }

    /**
     * CacheContentListener method which is notified when a particular cache location has completed updating.
     *
     * @param location File cache location
     */
    @Override
    @InvokeOnEDT
    public void cacheUpdateCompleted(final File location)
    {
        // Remove this File location from the set of currently updating locations
        locationsUpdatingSet.remove(location);
        populateCacheTable();
        repaint();
    }

    /**
     * Enable or disable the "Clean" cache button.
     *
     * @param enable if {@code true} enable the clean button, otherwise disable it.
     */
    public void enableCleanButton(boolean enable)
    {
        cleanButton.setEnabled(enable);
    }

    /**
     * CacheCleanerTaskListener method: cache cleaning has begun.  Show a status dialog indicating cache cleaning.
     */
    @Override
    @InvokeOnEDT
    public void cacheCleanerTaskStarted()
    {
        cleanTaskStatusDialog = new StatusDialog(true);
        cleanTaskStatusDialog.setStatusText("Cleaning Selected Map Set Data...");
        cleanTaskStatusDialog.setVisible(true);
    }

    /**
     * CacheCleanerTaskListener method: the cache cleaner task has completed.  Hide the status dialog and refresh
     * the selected cache location.
     *
     * @param success if {@code true} the task was successful.  Otherwise, the task failed.
     */
    @Override
    @InvokeOnEDT
    public void cacheCleanerTaskCompleted(final boolean success)
    {
        if (cleanTaskStatusDialog != null)
        {
            cleanTaskStatusDialog.setVisible(false);
            cleanTaskStatusDialog.dispose();
            cleanTaskStatusDialog = null;
        }

        populateCacheTable();
    }

    /**
     * Add a root drive to the location JList. Shows a confirmation dialog that warns the user that searching an
     * entire drive for cache data may take a long time.
     *
     * @param rootPath path to a drive
     */
    public void addRootDrive(String rootPath)
    {
        File root = new File(rootPath);
        if (root.exists() && root.canRead())
        {
            int ret = AlwaysOnTopJOptionPane.showConfirmDialog(this,
                    "<html>Searching an entire drive for Map Set data may take a VERY long time.  " +
                            "Are you sure you want to add it?</html>");
            if (ret == JOptionPane.OK_OPTION)
            {
                cacheContents.addFileStoreLocation(root);
                cacheContents.updateDataSet(root);
                populateLocationList(false);
            }
        }
    }

    /**
     * Fill the locations JList with currently available cache locations.
     *
     * @param update if {@code true} force an update of all cache locations to refresh their data sets.
     */
    private void populateLocationList(boolean update)
    {
        List<File> locations = cacheContents.getFileStoreLocations();
        if (update)
        {
            cacheContents.updateAllDataSets();
        }
        if (locations == null) return;

        locationListModel.clear();
        for (File location : locations)
        {
            locationListModel.addElement(location);
        }
    }

    /**
     * Fill the cache table with the data set contents of the selected cache location.
     */
    private void populateCacheTable()
    {
        File value = locationList.getSelectedValue();
        if (value == null) return;

        cacheTable.setDataSets(cacheContents.getDataSetsForLocation(value));
    }

    /**
     * Initialize the cache location chooser to look for directories.
     */
    private void initLocationChooser()
    {
        locationChooser.setDialogTitle("Select Map Set Location Directory");
        locationChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    }

    /**
     * Sets up a list of selected cache data sets, along with a value for declaring whether to delete the entire sets
     * or only delete data which has not been viewed in a number of months.
     */
    private void cleanButtonActionPerformed()
    {
        // Disable the clean button - the user should only be able to clean one cache data set collection at a time.
        cleanButton.setEnabled(false);

        try
        {
            selectiveDeleteSpinner.commitEdit();
        } catch (Exception e)
        {
            selectiveDeleteSpinner.setValue(selectiveDeleteSpinner.getPreviousValue());
        }

        // If the selective delete checkbox is selected, use the value in the spinner. Otherwise,
        // set months to -1, which means that the entire data set will be deleted
        int months = selectiveDeleteCheckbox.isSelected() ? (Integer) selectiveDeleteSpinner.getValue() : -1;

        // Iterate through the list of selected data sets and wrap them with the months value
        ArrayList<CacheCleanerTask.DataSetCleanInfo> cleanInfo = new ArrayList<>();
        for (FileStoreDataSet selectedDataSet : cacheTable.getSelectedDataSets())
        {
            cleanInfo.add(new CacheCleanerTask.DataSetCleanInfo(selectedDataSet, months));
        }

        if (!cleanInfo.isEmpty())
        {
            // Start cleaning the selected data sets (on a background thread)
            new CacheCleanerTask(cleanInfo, this).beginClean();
        }
    }

    /**
     * Attempt to shutdown any updating going on right now and dispose of the dialog.
     */
    private void closeButtonActionPerformed()
    {
        cacheContents.shutdownUpdater();
        dispose();
    }

    /**
     * Opens the location directory chooser and adds the selected folder (if it is valid and not a duplicate) to
     * the list of locations.
     */
    private void buttonAddLocationActionPerformed()
    {
        if (locationChooser.showDialog(this, "Select Location") == JFileChooser.APPROVE_OPTION)
        {
            File selected = locationChooser.getSelectedFile();
            if (selected != null && selected.exists() && selected.isDirectory())
            {
                if (!isLocationInList(selected))
                {
                    cacheContents.addFileStoreLocation(selected);
                    cacheContents.updateDataSet(selected);
                    populateLocationList(false);
                } else
                {
                    AlwaysOnTopJOptionPane.showErrorDialog("The selected location is already in the " +
                            "Map Set location list", "Can't Add Selected Location");
                }
            } else
            {
                AlwaysOnTopJOptionPane.showErrorDialog("Could not select invalid directory: " + selected,
                        "Invalid Directory");
            }
        }
    }

    /**
     * Iterate through the list of locations to find out whether the given location already exists in the list.
     *
     * @param location a list to check for existence in the locations JList
     * @return {@code true} if the location already exists in the list or {@code false} otherwise.
     */
    private boolean isLocationInList(File location)
    {
        Enumeration<File> walker = locationListModel.elements();
        while (walker.hasMoreElements())
        {
            File listFile = walker.nextElement();
            if (location.equals(listFile))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Shows a popup menu of available drives to add to the location JList.
     */
    private void buttonAddEntireDriveActionPerformed()
    {
        addDrivePopup.showMenu(buttonAddEntireDrive);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        var label1 = new JLabel();
        var panel2 = new JPanel();
        var scrollPane2 = new JScrollPane();
        locationList = new JList<>();
        var panel3 = new JPanel();
        var buttonAddLocation = new JButton();
        buttonAddEntireDrive = new JButton();
        var separator2 = new JSeparator();
        var label2 = new JLabel();
        var scrollPane1 = new JScrollPane();
        cacheTable = new CacheTable(this);
        var buttonBar = new JPanel();
        var panel4 = new JPanel();
        selectiveDeleteCheckbox = new JCheckBox();
        selectiveDeleteSpinner = new JSpinner();
        var selectiveDeleteLabel = new JLabel();
        cleanButton = new JButton();
        var closeButton = new JButton();
        dialogHeaderPanel = new DialogHeaderPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Cache Cleaner");
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
                        "4*(default, $lgap), default:grow"));

                //---- label1 ----
                label1.setText("Select a Map Set location");
                label1.setName("label1");
                contentPanel.add(label1, cc.xy(1, 1));

                //======== panel2 ========
                {
                    panel2.setName("panel2");
                    panel2.setLayout(new FormLayout(
                            "default:grow",
                            "default, $lgap, default"));

                    //======== scrollPane2 ========
                    {
                        scrollPane2.setName("scrollPane2");

                        //---- locationList ----
                        locationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                        locationList.setName("locationList");
                        scrollPane2.setViewportView(locationList);
                    }
                    panel2.add(scrollPane2, cc.xy(1, 1));

                    //======== panel3 ========
                    {
                        panel3.setName("panel3");
                        panel3.setLayout(new FormLayout(
                                "default, $lcgap, default",
                                "default"));

                        //---- buttonAddLocation ----
                        buttonAddLocation.setText("Add Location");
                        buttonAddLocation.setName("buttonAddLocation");
                        buttonAddLocation.addActionListener(e -> buttonAddLocationActionPerformed());
                        panel3.add(buttonAddLocation, cc.xy(1, 1));

                        //---- buttonAddEntireDrive ----
                        buttonAddEntireDrive.setText("Add Entire Drive");
                        buttonAddEntireDrive.setName("buttonAddEntireDrive");
                        buttonAddEntireDrive.addActionListener(e -> buttonAddEntireDriveActionPerformed());
                        panel3.add(buttonAddEntireDrive, cc.xy(3, 1));
                    }
                    panel2.add(panel3, cc.xy(1, 3));
                }
                contentPanel.add(panel2, cc.xy(1, 3));

                //---- separator2 ----
                separator2.setName("separator2");
                contentPanel.add(separator2, cc.xy(1, 5));

                //---- label2 ----
                label2.setText("Select Map Set data to clean");
                label2.setName("label2");
                contentPanel.add(label2, cc.xy(1, 7));

                //======== scrollPane1 ========
                {
                    scrollPane1.setName("scrollPane1");

                    //---- cacheTable ----
                    cacheTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    cacheTable.setName("cacheTable");
                    scrollPane1.setViewportView(cacheTable);
                }
                contentPanel.add(scrollPane1, cc.xy(1, 9, CellConstraints.FILL, CellConstraints.FILL));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$lcgap, default, $glue, $button, $rgap, $button",
                        "pref"));

                //======== panel4 ========
                {
                    panel4.setName("panel4");
                    panel4.setLayout(new FormLayout(
                            "2*(default, $lcgap), default",
                            "default"));

                    //---- selectiveDeleteCheckbox ----
                    selectiveDeleteCheckbox.setText("Only delete Map Set data unused in");
                    selectiveDeleteCheckbox.setName("selectiveDeleteCheckbox");
                    panel4.add(selectiveDeleteCheckbox, cc.xy(1, 1));

                    //---- selectiveDeleteSpinner ----
                    selectiveDeleteSpinner.setModel(new SpinnerNumberModel(1, 0, 999, 1));
                    selectiveDeleteSpinner.setName("selectiveDeleteSpinner");
                    panel4.add(selectiveDeleteSpinner, cc.xy(3, 1));

                    //---- selectiveDeleteLabel ----
                    selectiveDeleteLabel.setText("months (or longer)");
                    selectiveDeleteLabel.setName("selectiveDeleteLabel");
                    panel4.add(selectiveDeleteLabel, cc.xy(5, 1));
                }
                buttonBar.add(panel4, cc.xy(2, 1));

                //---- cleanButton ----
                cleanButton.setText("Clean");
                cleanButton.setEnabled(false);
                cleanButton.setName("cleanButton");
                cleanButton.addActionListener(e -> cleanButtonActionPerformed());
                buttonBar.add(cleanButton, cc.xy(4, 1));

                //---- closeButton ----
                closeButton.setText("Close");
                closeButton.setName("closeButton");
                closeButton.addActionListener(e -> closeButtonActionPerformed());
                buttonBar.add(closeButton, cc.xy(6, 1));
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
    private JList<File> locationList;
    private JButton buttonAddEntireDrive;
    private CacheTable cacheTable;
    private JCheckBox selectiveDeleteCheckbox;
    private JSpinner selectiveDeleteSpinner;
    private JButton cleanButton;
    private DialogHeaderPanel dialogHeaderPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
