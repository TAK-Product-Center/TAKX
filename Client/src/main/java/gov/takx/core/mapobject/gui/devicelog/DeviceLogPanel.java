package gov.takx.core.mapobject.gui.devicelog;

import com.formdev.flatlaf.FlatClientProperties;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.mapobject.ICoreMapObjectManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.ITrackSelectListener;
import gov.takx.api.mapobject.TrackSelectEvent;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IDataType;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.NoSuchStructureException;
import gov.takx.api.messages.RaptorDataMessageComparator;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.plugin.IMetadata;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.query.ComparisonType;
import gov.takx.api.query.ConflictingConstraintException;
import gov.takx.api.query.CoreField;
import gov.takx.api.query.IQueryConstraints;
import gov.takx.api.query.IQueryService;
import gov.takx.api.query.NoSuchFieldException;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.CustomColumnControlButton;
import gov.takx.commons.swing.ErrorReportingSwingWorker;
import gov.takx.commons.utils.StringUtils;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.devicelog.DeviceLogTablePanel;
import gov.takx.platform.ui.devicelog.FieldFilter;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.apache.commons.lang3.ArrayUtils;
import org.jdesktop.swingx.JXTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PrinterException;
import java.beans.PropertyChangeEvent;
import java.lang.invoke.MethodHandles;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Panel to show a collection of RDMs.  These messages might come from a single Map Object (when this panel is
 * being used to show the device log for a MO), or it might be filled with an arbitrary collection of RDMs that all
 * share the same data structure.
 *
 * @since 1.49 (reworked to be usable via API in 1.49)
 */
@SuppressWarnings({"CdiManagedBeanInconsistencyInspection", "CdiInjectionPointsInspection", "SpringAutowiredFieldsWarningInspection"})
@PseudoBean
public class DeviceLogPanel extends JPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int CUSTOM_ROW_COUNT = -99;
    private static final int ALL_ROWS = -1;

    private String rdsDetails = null;
    private String lastStructureName;

    private final Object filterLock = new Object();

    private final IQueryService queryService;
    private ExportWorker exportWorker = null;

    @Inject
    private LocationHighlighter locationHighlighter;

    @Inject
    private IPersistServiceProvider persistServiceProvider;

    @Inject
    private ICoreMapObjectManager mapObjectManager;

    @Inject
    private IExtendedGlobeManager globeManager;

    private FieldFilter fieldFilter = null;

    private final String mapObjectId;
    private final DeviceLogTablePanel rdmTablePanel;
    private final JXTable rdmTable;
    private final DefaultComboBoxModel<IRaptorDataStructure> structComboBoxModel = new DefaultComboBoxModel<>();
    private final DefaultComboBoxModel<String> fieldComboBoxModel = new DefaultComboBoxModel<>();
    private final List<String> duplicateStructureList = new ArrayList<>();
    private final Long minReceiveTime;
    private final IPreferenceMonitor<CoordinateSystem> coordinateSystemMonitor =
            UserPreferenceEntry.COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, c -> updateLocationLabels(), false);
    private IRaptorDataStructure selectedStructure = null;

    private int sortedColumnIndex = -1;
    private SortOrder sortOrder = SortOrder.ASCENDING;
    private volatile RefreshWorker refreshWorker;
    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);

    private final ILocation minLocation; // Used if constrained to a bounding box
    private final ILocation maxLocation;

    private int startMessage = 0; // For paging results
    private int rowCount = 0;
    private int totalCount = 0;
    private volatile boolean displayLiveRdms = false;

    public DeviceLogPanel(String mapObjectName, String mapObjectId, String description, Long minReceiveTime,
                          Collection<IRaptorDataStructure> structures, IRaptorDataStructure defaultFilter,
                          IQueryService queryService, String vfsDir, ILocation minLocation, ILocation maxLocation)
    {
        initComponents();

        rdmTablePanel = new DeviceLogTablePanel();
        rdmTablePanel.setTemplateDir(vfsDir);

        rdmTable = rdmTablePanel.getRdmTable();
        rdmTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        rdmTable.setColumnControl(new CustomColumnControlButton(rdmTable).create());

        add(rdmTablePanel, new CellConstraints().xy(1, 2));

        rdmTable.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                // Right-click will select the row, if it is not currently selected...
                if (SwingUtilities.isRightMouseButton(e))
                {
                    Point p = e.getPoint();
                    int rowNumber = rdmTable.rowAtPoint(p);
                    int[] selections = rdmTable.getSelectedRows();
                    List<Integer> selectionList = Arrays.asList(ArrayUtils.toObject(selections));
                    if (!selectionList.contains(rowNumber))
                    {
                        int modelRow = rdmTable.convertRowIndexToModel(rowNumber);
                        ListSelectionModel model = rdmTable.getSelectionModel();
                        model.setSelectionInterval(modelRow, modelRow);
                    }
                }
            }

            @Override
            public void mouseClicked(MouseEvent e)
            {
                rdmTableMouseClicked(e);
            }
        });

        this.queryService = queryService;
        this.mapObjectId = mapObjectId;
        this.minReceiveTime = minReceiveTime;
        this.minLocation = minLocation;
        this.maxLocation = maxLocation;

        nameLabel.setText(mapObjectName);
        unitIDLabel.setText(description);
        goToButton.setEnabled(false);
        comboBoxComparison2.setVisible(false);
        textFieldFilter2.setVisible(false);
        labelOptionalAnd.setVisible(false);
        newDataLabel.setVisible(false);

        btnNext.putClientProperty(FlatClientProperties.MINIMUM_WIDTH, 0);
        btnPrevious.putClientProperty(FlatClientProperties.MINIMUM_WIDTH, 0);

        if (null != structures)
        {
            final String defaultFilterMessageType = defaultFilter != null ? defaultFilter.getMessageType() : null;
            List<String> structureNameList = new ArrayList<>();

            // Create our sorted combo box structure model
            for (IRaptorDataStructure curStruct : new TreeSet<>(structures))
            {
                // We really don't need the manual location structure in our log.  Really.
                final String messageType = curStruct.getMessageType();

                if (!DataStructureBuilder.LOCATION_MESSAGE_TYPE.equalsIgnoreCase(messageType)) // nor this one
                {
                    structComboBoxModel.addElement(curStruct);
                    if (structureNameList.contains(messageType))
                    {
                        duplicateStructureList.add(messageType);
                    } else
                    {
                        structureNameList.add(messageType);
                    }

                    if (messageType.equals(defaultFilterMessageType))
                    {
                        selectedStructure = curStruct;
                    }
                }
            }
        }

        structureComboBox.setModel(structComboBoxModel);
        structureComboBox.setRenderer(new StructureComboBoxRenderer());
        structureComboBox.updateUI();

        final DefaultComboBoxModel<ComparisonType> comparisonComboBoxModel = new DefaultComboBoxModel<>();
        comparisonComboBoxModel.addElement(ComparisonType.EQ);
        comparisonComboBoxModel.addElement(ComparisonType.GT);
        comparisonComboBoxModel.addElement(ComparisonType.GTE);
        comparisonComboBoxModel.addElement(ComparisonType.LT);
        comparisonComboBoxModel.addElement(ComparisonType.LTE);
        comparisonComboBoxModel.addElement(ComparisonType.NEQ);
        comparisonComboBoxModel.addElement(ComparisonType.ISNULL);
        comparisonComboBoxModel.addElement(ComparisonType.ISNOTNULL);
        comparisonComboBoxModel.addElement(ComparisonType.LIKE);
        comparisonComboBoxModel.addElement(ComparisonType.NOTLIKE);

        comboBoxComparison1.setModel(comparisonComboBoxModel);
        comboBoxComparison1.setSelectedIndex(0);

        comboBoxField.setModel(fieldComboBoxModel);

        final DefaultComboBoxModel<RowCounter> rowComboBoxModel = new DefaultComboBoxModel<>();
        rowComboBoxModel.addElement(new RowCounter("Top 100 Rows", 100));
        rowComboBoxModel.addElement(new RowCounter("Top 1000 Rows", 1000));
        rowComboBoxModel.addElement(new RowCounter("All Rows", ALL_ROWS));
        rowComboBoxModel.addElement(new RowCounter("Custom", CUSTOM_ROW_COUNT));

        rowCountComboBox.setModel(rowComboBoxModel);
        rowCountComboBox.setSelectedIndex(0);
        // need to add this after setting the index so that we don't prematurely refresh the data.
        rowCountComboBox.addActionListener(e -> refreshData());
    }

    @PostConstruct
    private void initPostConstruct()
    {
        // This block requires access to the injected beans, must be done post construct.

        if (minLocation != null)
        {
            // Accesses systemPropertyManager
            updateLocationLabels();
        } else
        {
            labelLocation.setVisible(false);
            panelLocation.setVisible(false);
            revalidate();
        }

        // Select the appropriate structure in the combobox, this will result everything getting updated
        // Setting the selected item will cause the action listener to fire, which
        // will render the log messages, and ultimately references the persistServiceProvider bean.
        if (null != selectedStructure)
        {
            structureComboBox.setSelectedItem(selectedStructure);
        } else
        {
            if (structComboBoxModel.getSize() > 0)
            {
                structureComboBox.setSelectedIndex(0);
            }
        }
    }

    public void receiveMessage(final IRaptorDataMessage rdm)
    {
        IRaptorDataStructure structure = getSelectedStructure();

        // We can short-circuit the creation of the query constraints if the incoming RDM has a
        // different structure than the currently selected one.
        if (!structure.getStructure().equals(rdm.getStructure())) return;

        IQueryConstraints constraints = createRefreshQueryConstraints(structure);
        boolean isMatch = queryService.matches(rdm, constraints);
        if (isMatch)
        {
            // If we are in live mode, immediately display the RDM, otherwise inform the
            // user that new data is available on refresh
            if (displayLiveRdms)
            {
                SwingUtilities.invokeLater(() -> {
                    try
                    {
                        int maxRowCount = getMaxRowCount();
                        boolean showAll = (maxRowCount == ALL_ROWS);
                        boolean spaceAvailable = (rowCount < getMaxRowCount());
                        if (showAll || spaceAvailable)
                        {
                            rdmTablePanel.addMessages(Collections.singletonList(rdm));
                            rowCount++;
                        }
                        totalCount++;
                        refreshRecordCountLabel();
                        btnPrevious.setEnabled(startMessage > 0);
                        btnNext.setEnabled(startMessage + rowCount < totalCount);
                    } catch (IllegalArgumentException e)
                    {
                        logger.error("Failed to load RDM", e);
                    }
                });
            } else if (!newDataLabel.isVisible())
            {
                SwingUtilities.invokeLater(() -> newDataLabel.setVisible(!displayLiveRdms));
            }
        }
    }

    /**
     * Install the currently selected structure.  Update all UI elements accordingly.
     */
    private void installSelectedStructure()
    {
        if (refreshWorker != null)
        {
            refreshWorker.cancel();
        }

        final String structureName = getSelectedStructure().getMessageType();

        refreshFieldModel();
        refreshTableModel();

        clearFilter();

        refreshData();

        lastStructureName = structureName;
    }

    /**
     * Update the min/max location labels according to the current coordinate system
     */
    private void updateLocationLabels()
    {
        final var coordinateSystem = coordinateSystemMonitor.getCurrentValue();

        labelMin.setText(CoordinateSystem.getLocationString(minLocation, coordinateSystem, false));
        labelMax.setText(CoordinateSystem.getLocationString(maxLocation, coordinateSystem, false));
    }

    /**
     * Exports the current set of {@link IRaptorDataMessage}s displayed inside of the table or all messages.
     *
     * @param all Pass {@code true} to export all records for the current structure instead of just current table contents
     */
    public void export(final boolean all)
    {
        if (exportWorker == null)
        {
            final IRaptorDataStructure structure = getSelectedStructure();

            if (structure == null) return;

            // We are either exporting all, or just what is in the model (table).
            exportWorker = all
                    ? new ExportWorker(queryService, structure)
                    : new ExportWorker(queryService, structure, rdmTablePanel.getMessages());

            if (exportWorker.queryUserForExportFile(getParent()))
            {
                exportPanel.setVisible(true);
                cancelExportButton.setVisible(true);
                refreshButton.setEnabled(false);
                exportButton.setEnabled(false);
                exportAllButton.setEnabled(false);
                progressBar.setValue(0);

                // Watch for progress updates and state changes (so we know when it's done)
                exportWorker.addPropertyChangeListener(
                        evt -> {
                            //noinspection SwitchStatementWithoutDefaultBranch
                            switch (evt.getPropertyName())
                            {
                                case "progress" -> progressBarPropertyChange(evt);
                                case "state" ->
                                {
                                    if (evt.getNewValue() == SwingWorker.StateValue.DONE) exportComplete();
                                }
                            }
                        });
                exportWorker.execute();
            } else
            {
                exportWorker = null; // user canceled
            }
        } else
        {
            AlwaysOnTopJOptionPane.showErrorDialogLater(getParent(),
                    "Please wait for the current export operation to complete.", "Export Request Error");
        }
    }

    /**
     * @return The currently selected structure, {@code null} if none (or not in our map)
     */
    private IRaptorDataStructure getSelectedStructure()
    {
        return (IRaptorDataStructure) structureComboBox.getSelectedItem();
    }

    /**
     * The export processing is complete, cleanup the UI.
     */
    private void exportComplete()
    {
        exportPanel.setVisible(false);
        cancelExportButton.setVisible(false);
        refreshButton.setEnabled(true);
        exportButton.setEnabled(true);
        exportAllButton.setEnabled(true);
        exportWorker = null;
    }

    /**
     * Prints the current set of {@link IRaptorDataMessage}s displayed inside of the panel.
     */
    public void print()
    {
        try
        {
            rdmTable.print();
        } catch (PrinterException e)
        {
            logger.warn("Unable to print the RDM table: {}", e.getMessage(), e);
            AlwaysOnTopJOptionPane.showErrorDialogLater(getParent(), "Unable to print table, error was: " + e.getMessage(), "Print Failure");
        }
    }

    public void open()
    {
        coordinateSystemMonitor.start();
    }

    /**
     * Ensures that the panel is in a suitable state to be shutdown, e.g. auto-refresh has been disabled.
     *
     * @param shutdown Pass true if we are shutting down
     */
    public void close(boolean shutdown)
    {
        if (!shutdown && lastStructureName != null && !lastStructureName.isEmpty()) rdmTablePanel.saveTableLayout();

        if (shutdown)
        {
            coordinateSystemMonitor.dispose();
        } else
        {
            coordinateSystemMonitor.stop();
        }
    }

    /**
     * Manually trigger a refresh of the device log.  Must be called on the EDT.
     */
    public void refreshData()
    {
        if (logger.isDebugEnabled()) logger.debug("refreshData");

        if (isRefreshing.getAndSet(true))
        {
            // We are already doing a refresh, don't do another
            return;
        }

        labelExport.setText("Retrieving Data");
        exportPanel.setVisible(true);
        buttonApplyFilter.setEnabled(false);
        refreshButton.setText("Cancel");
        progressBar.setValue(0);
        progressBar.setIndeterminate(true);
        displayLiveRdms = false;

        final IRaptorDataStructure structure = getSelectedStructure();

        if (null == structure)
        {
            logger.warn("Unable to locate the selected structure in the device log structures cache: {}", structureComboBox.getSelectedItem());
            return;
        }

        // Disable the goto button prior to CLEARING the tableModel
        labelExport.setText("Displaying data...");
        goToButton.setEnabled(false);

        sortedColumnIndex = rdmTable.getSortedColumnIndex();
        sortOrder = rdmTable.getSortOrder(sortedColumnIndex);
        rdmTable.setSortable(false); // Turn off sorting while we update the table
        rdmTablePanel.clearTableData(); // Empty out the existing model
        rdmTablePanel.setStructure(structure.getStructure());

        refreshWorker = new RefreshWorker(createRefreshQueryConstraints(structure));

        refreshWorker.addPropertyChangeListener(
                evt -> {
                    //noinspection SwitchStatementWithoutDefaultBranch
                    switch (evt.getPropertyName())
                    {
                        case "progress" -> progressBarPropertyChange(evt);
                        case "state" ->
                        {
                            if (evt.getNewValue() == SwingWorker.StateValue.DONE) refreshComplete();
                        }
                    }
                });

        refreshWorker.execute();
    }

    /**
     * The refresh operation is complete, update the UI accordingly.
     */
    private void refreshComplete()
    {
        exportPanel.setVisible(false);
        labelExport.setText("Exporting Data");
        refreshButton.setText("Refresh");
        buttonApplyFilter.setEnabled(true);

        displayLiveRdms = chkLive.isSelected();
        newDataLabel.setVisible(false);

        rowCount = rdmTablePanel.getTableModel().getRowCount();
        totalCount = refreshWorker.getTotalCount();

        refreshRecordCountLabel();

        // Make the appropriate prev/next buttons visible
        btnPrevious.setEnabled(startMessage > 0);
        btnNext.setEnabled(startMessage + rowCount < totalCount);

        rdmTable.setSortable(true); // Re-enable now that we're done
        if (sortedColumnIndex >= 0 && sortedColumnIndex < rdmTable.getColumnCount())
        {
            rdmTable.setSortOrder(sortedColumnIndex, sortOrder);
        }

        refreshWorker = null;
        isRefreshing.getAndSet(false);
    }

    /**
     * Update the label showing the number of RDMs being displayed
     */
    private void refreshRecordCountLabel()
    {
        if (rowCount == totalCount)
        {
            labelRecordCount.setText("Showing ALL " + totalCount + " records");
        } else
        {
            labelRecordCount.setText(String.format("Showing records [%d - %d] of %d",
                    startMessage + 1, startMessage + rowCount, totalCount));
        }
    }

    /**
     * Construct the query constraints for use in refreshing the table contents.
     *
     * @param structure The messages structure to select for the data
     * @return Query constraints or {@code null} on any error
     */
    private IQueryConstraints createRefreshQueryConstraints(IRaptorDataStructure structure)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Query MO ID {} structure {} - {}", mapObjectId, structure.getMessageType(), structure.getVersion());
        }

        IQueryConstraints uiConstraints = queryService.createEmptyConstraints();
        uiConstraints.setSortingOptions(IQueryConstraints.SortOn.RECEIVE_DATE, IQueryConstraints.SortOrder.DESCENDING);

        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Constraining on message type {} - {}", structure.getMessageType(), structure.getVersion());
            }

            uiConstraints.addMessageStructureConstraint(structure);

            if (null != minReceiveTime)
            {
                if (logger.isDebugEnabled()) logger.debug("Constraining on min receive time of {}", minReceiveTime);

                uiConstraints.addReceiveDateRange(minReceiveTime, Long.MAX_VALUE);
            }

            if (minLocation != null)
            {
                uiConstraints.addLocationRange(minLocation, maxLocation);
            }

            // Query for the maximum number of rows specified
            int rowCount = getMaxRowCount();
            uiConstraints.setMaxMessageCount(rowCount);

            if (logger.isDebugEnabled()) logger.debug("Constraining to max message count {}", rowCount);

            if (rowCount == ALL_ROWS) startMessage = 0; // We don't page if we're retrieving all messages

            // Install start record, if configured
            if (startMessage > 0)
            {
                if (logger.isDebugEnabled()) logger.debug("Constraining start message to: {}", startMessage);
                uiConstraints.setStartMessageCount(startMessage);
            }

            synchronized (filterLock)
            {
                if (null != fieldFilter)
                {
                    // We do custom processing of special dates in refreshData()
                    if (!fieldFilter.isSpecialDateTypeMatched())
                    {
                        fieldFilter.applyFilter(uiConstraints); // Apply the filter to the constraints
                    }
                } else
                {
                    if (logger.isDebugEnabled()) logger.debug("No constraint field-level filter set");
                }
            }
        } catch (NoSuchStructureException | NoSuchFieldException | ConflictingConstraintException e)
        {
            logger.error("Failed to construct query for filter", e);
            uiConstraints = null;
        }

        return uiConstraints;
    }

    /**
     * @return The configured max message count
     */
    private int getMaxRowCount()
    {
        final RowCounter counter = (RowCounter) rowCountComboBox.getSelectedItem();
        final Integer rows = counter.rows;

        return rows == CUSTOM_ROW_COUNT ? (Integer) spinnerRowCount.getValue() : rows;
    }

    private void refreshFieldModel()
    {
        fieldComboBoxModel.removeAllElements();

        fieldComboBoxModel.addElement(CoreField.receiveTime.name());
        fieldComboBoxModel.addElement(CoreField.time.name());
        fieldComboBoxModel.addElement(CoreField.latitude.name());
        fieldComboBoxModel.addElement(CoreField.longitude.name());
        fieldComboBoxModel.addElement(CoreField.elevation.name());
        fieldComboBoxModel.addElement(CoreField.speed.name());
        fieldComboBoxModel.addElement(CoreField.heading.name());
        fieldComboBoxModel.addElement(CoreField.pitch.name());
        fieldComboBoxModel.addElement(CoreField.roll.name());
        fieldComboBoxModel.addElement(CoreField.hasMotion.name());

        if (structComboBoxModel.getSize() > 0)
        {
            final IRaptorDataStructure structure = getSelectedStructure();

            if (null != structure)
            {
                // Use a sorted set
                new TreeSet<>(structure.getFields()).stream()
                        .filter(field -> field.getFieldType() < IDataType.BYTE_ARRAY) // Skip all array data types
                        .forEach(field -> fieldComboBoxModel.addElement(field.getFieldName()));
            }
        }
    }

    private void refreshTableModel()
    {
        startMessage = 0;

        final IRaptorDataStructure structure = getSelectedStructure();

        if (structure != null)
        {
            final IMetadata metadata = structure.getStructure().getMetadata();

            rdsDetails = metadata.getMetadataValue(IRaptorDataStructure.METADATA_STRUCTURE_DETAILS);
            buttonDetails.setEnabled(null != rdsDetails);

            final String rdsDescription = metadata.getMetadataValue(IRaptorDataStructure.METADATA_STRUCTURE_DESCRIPTION);
            final String description = null != rdsDescription ? rdsDescription : "- No Description Provided -";

            textPaneDescription.setText(description);

            rdmTablePanel.setStructure(structure.getStructure());
        }
    }

    private void structureComboBoxActionPerformed()
    {
        installSelectedStructure();
    }

    private void chkAutoRefreshItemStateChanged(ItemEvent e)
    {
        final boolean itemSelected = e.getStateChange() == ItemEvent.SELECTED;

        if (itemSelected)
        {
            refreshData();
        }
        displayLiveRdms = itemSelected;
        refreshButton.setEnabled(!itemSelected);
    }

    private void refreshButtonActionPerformed()
    {
        if (refreshButton.getText().equals("Cancel"))
        {
            refreshWorker.cancel(true);
        } else
        {
            refreshData();
        }
    }

    private void exportButtonActionPerformed()
    {
        export(false);
    }

    private void exportAllButtonActionPerformed()
    {
        export(true);
    }

    private void printButtonActionPerformed()
    {
        print();
    }

    private void buttonClearFilterActionPerformed()
    {
        clearFilter();
        refreshData();
    }

    private void clearFilter()
    {
        startMessage = 0;

        fieldFilter = null;
        textFieldFilter1.setText("");
        textFieldFilter2.setText("");

        if (comboBoxField.getItemCount() > 0)
        {
            comboBoxField.setSelectedIndex(0);
        }
        comboBoxComparison1.setSelectedIndex(0);

        if (comboBoxComparison2.getModel().getSize() > 0)
        {
            comboBoxComparison2.setSelectedIndex(0);
        }
        labelCurrentFilter.setText("None");
        buttonClearFilter.setEnabled(false);
    }

    private void comboBoxComparisonItemStateChanged(ItemEvent e)
    {
        ComparisonType ct = (ComparisonType) e.getItem();

        if (!ct.usesValue())
        {
            textFieldFilter1.setText("");
            textFieldFilter1.setVisible(false);

            textFieldFilter2.setText("");
            comboBoxComparison2.setVisible(false);
            textFieldFilter2.setVisible(false);
        } else
        {
            textFieldFilter1.setVisible(true);

            boolean showComparison2 = ct != ComparisonType.EQ && ct != ComparisonType.NEQ
                    && ct != ComparisonType.LIKE && ct != ComparisonType.NOTLIKE;

            comboBoxComparison2.setVisible(showComparison2);
            textFieldFilter2.setVisible(showComparison2);
            labelOptionalAnd.setVisible(showComparison2);

            if (showComparison2)
            {
                ct = (ComparisonType) comboBoxComparison1.getSelectedItem();

                DefaultComboBoxModel<ComparisonType> comparisonComboBoxModel2 = new DefaultComboBoxModel<>();
                comparisonComboBoxModel2.removeAllElements();

                if (ct == ComparisonType.GT || ct == ComparisonType.GTE)
                {
                    comparisonComboBoxModel2.addElement(ComparisonType.LT);
                    comparisonComboBoxModel2.addElement(ComparisonType.LTE);
                } else if (ct == ComparisonType.LT || ct == ComparisonType.LTE)
                {
                    comparisonComboBoxModel2.addElement(ComparisonType.GT);
                    comparisonComboBoxModel2.addElement(ComparisonType.GTE);
                }

                comboBoxComparison2.setModel(comparisonComboBoxModel2);
            }
        }
    }

    private void buttonApplyFilterActionPerformed()
    {
        String pendingFilterValue1 = textFieldFilter1.getText();

        if (logger.isDebugEnabled()) logger.debug("Applying filter");

        final ComparisonType comparisonType = (ComparisonType) comboBoxComparison1.getSelectedItem();
        boolean usesValue = comparisonType.usesValue();

        if (!usesValue || !StringUtils.isEmpty(pendingFilterValue1))
        {
            // synchronize due to auto-refresh thread.  Don't want to refresh while in the middle of updating the user's filter request
            synchronized (filterLock)
            {
                try
                {
                    final boolean useFilter2 = textFieldFilter2.isVisible();

                    fieldFilter = new FieldFilter(getSelectedStructure(),
                            comboBoxField.getSelectedItem().toString(),
                            (ComparisonType) comboBoxComparison1.getSelectedItem(),
                            textFieldFilter1.getText(),
                            useFilter2 ? (ComparisonType) comboBoxComparison2.getSelectedItem() : null,
                            useFilter2 ? textFieldFilter2.getText() : null);

                    if (fieldFilter.isValid())
                    {
                        labelCurrentFilter.setText(fieldFilter.getDescription());
                        buttonClearFilter.setEnabled(true);

                        if (logger.isDebugEnabled()) logger.debug("Refreshing device log table based on new filter");

                        refreshData();
                    } else
                    {
                        clearFilter();
                        AlwaysOnTopJOptionPane.showMessageDialog(null, "Unable to construct filter.", "Filter Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (ParseException e)
                {
                    clearFilter();
                    AlwaysOnTopJOptionPane.showMessageDialog(null, "The value entered could not be converted to the datatype of the selected field.", "Filter Validation Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else
        {
            AlwaysOnTopJOptionPane.showErrorDialogLater("Please provide a filter value first.", "Filter Validation Error");
        }
    }

    private void goToButtonActionPerformed()
    {
        // Should be double values for lat and lon, but going straight to String in case bad data got through.
        final ILocation location = getSelectedLocation();

        if (location != null)
        {
            globeManager.getLastFocusedGlobe().goTo(location);
            locationHighlighter.delayedHighlightLocation(location);
        }
    }

    /**
     * Find the location associated with the RDM currently highlighted in the device log, if any.
     *
     * @return The {@link ILocation} of the currently selected row.
     * @since 1.19
     */
    private ILocation getSelectedLocation()
    {
        int selectedRow = rdmTable.getSelectedRow();

        return selectedRow != -1
                ? rdmTablePanel.getTableModel().getLocation(rdmTable.convertRowIndexToModel(selectedRow))
                : null;
    }

    private void rdmTableMouseClicked(MouseEvent e)
    {
        final int row = rdmTable.getSelectedRow();
        final ILocation location = getSelectedLocation();

        goToButton.setEnabled(row >= 0 && location != null);

        if (location != null)
        {
            if (e.getClickCount() == 1)
            {
                locationHighlighter.highlightLocation(location);
            } else if (e.getClickCount() == 2)
            {
                goToButtonActionPerformed();
            }
        }

        // TODO:  Do we need to limit the MAX number of RDMs to notify of?
        // Get the list of selected rows and notify the Track Select Listeners (if both exist)
        int[] selection = rdmTable.getSelectedRows();
        List<ITrackSelectListener> appSelectListeners = mapObjectManager.getTrackSelectListeners();
        List<ITrackSelectListener> controllerSelectListeners = new ArrayList<>();
        IMapObjectController controller = mapObjectManager.getMapObjectController(mapObjectId);
        if (controller != null)
        {
            controllerSelectListeners = controller.getSelectListeners();
        }
        // Make sure there is a valid selection and some listeners before calculating the list of RDMs, etc
        if (selection.length > 0 && (!appSelectListeners.isEmpty() || !controllerSelectListeners.isEmpty()))
        {
            List<IRaptorDataMessage> rdmList = new ArrayList<>();
            for (int tableRow : selection)
            {
                int modelRow = rdmTable.convertRowIndexToModel(tableRow);
                if (modelRow != -1)
                {
                    IRaptorDataMessage rdm = rdmTablePanel.getTableModel().getRaptorDataMessage(modelRow);
                    if (rdm != null)
                    {
                        rdmList.add(rdm);
                    }
                }
            }
            if (!rdmList.isEmpty())
            {
                TrackSelectEvent selectEvent = null;
                IMapObject mapObject = rdmList.get(0).getMapObject();
                if (e.getClickCount() == 1)
                {
                    if (SwingUtilities.isLeftMouseButton(e))
                    {
                        selectEvent = new TrackSelectEvent(mapObject, TrackSelectEvent.TrackSelectAction.CLICK);
                    } else if (SwingUtilities.isRightMouseButton(e))
                    {
                        selectEvent = new TrackSelectEvent(mapObject, TrackSelectEvent.TrackSelectAction.RIGHT_CLICK);
                    }
                } else if (e.getClickCount() == 2)
                {
                    selectEvent = new TrackSelectEvent(mapObject, TrackSelectEvent.TrackSelectAction.DOUBLE_CLICK);
                }

                if (selectEvent != null)
                {
                    // We guarantee a time sort in the API.
                    rdmList.sort(new RaptorDataMessageComparator());
                    for (ITrackSelectListener listener : appSelectListeners)
                    {
                        listener.onSelect(selectEvent, rdmList);
                    }
                    for (ITrackSelectListener listener : controllerSelectListeners)
                    {
                        listener.onSelect(selectEvent, rdmList);
                    }
                }
            }
        }
    }

    private void rowCountComboBoxActionPerformed()
    {
        spinnerRowCount.setVisible(((RowCounter) rowCountComboBox.getSelectedItem()).rows == CUSTOM_ROW_COUNT);
    }

    private void progressBarPropertyChange(PropertyChangeEvent e)
    {
        if (progressBar.isIndeterminate()) progressBar.setIndeterminate(false);

        progressBar.setValue((Integer) e.getNewValue());
    }

    private void buttonDetailsActionPerformed()
    {
        AlwaysOnTopJOptionPane.showMessageDialog(rdsDetails, "Detailed Description", JOptionPane.INFORMATION_MESSAGE);
    }

    private void cancelExportButtonActionPerformed()
    {
        if (exportWorker != null) exportWorker.cancelExport();
    }

    /**
     * Page backwards - refresh table with previous batch of RDMs.
     */
    private void btnPreviousActionPerformed()
    {
        final int maxRowCount = getMaxRowCount();
        if (maxRowCount == ALL_ROWS)
        {
            startMessage = 0;
        } else
        {
            startMessage = Math.max(0, startMessage - getMaxRowCount());
        }

        refreshData();
    }

    /**
     * Page forwards - refresh table with next batch of RDMs.
     */
    private void btnNextActionPerformed()
    {
        final int maxRowCount = getMaxRowCount();
        if (maxRowCount == ALL_ROWS)
        {
            startMessage = 0;
        } else
        {
            startMessage += rdmTable.getModel().getRowCount();
        }
        refreshData();
    }

    /**
     * If the user presses the enter key in either filter value field, then apply the filter.
     *
     * @param e key event
     */
    private void textFieldFilterKeyPressed(KeyEvent e)
    {
        if (e.getKeyCode() == KeyEvent.VK_ENTER)
        {
            buttonApplyFilter.doClick();
        }
    }

    private class StructureComboBoxRenderer extends DefaultListCellRenderer
    {
        @SuppressWarnings("ReturnOfInnerClass")
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            if (value == null) return this;

            IRaptorDataStructure struct = (IRaptorDataStructure) value;
            if (duplicateStructureList.contains(struct.getMessageType()))
            {
                setText(struct.getMessageType() + " v." + struct.getVersion());
            } else
            {
                setText(struct.getMessageType());
            }

            if (isSelected)
            {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else
            {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            return this;
        }
    }

    /**
     * Simple class to hold a "row count" specification in a combo box.
     */
    private static final class RowCounter
    {
        private final String name;
        private final Integer rows;

        RowCounter(String name, Integer rows)
        {
            this.name = name;
            this.rows = rows;
        }

        public String toString()
        {
            return name;
        }
    }

    /**
     * Worker to load the data (using the current query constraints) from the database and install it into the table.
     */
    private class RefreshWorker extends ErrorReportingSwingWorker<Long, IRaptorDataMessage>
    {
        private int workerTotalCount = 0;
        private Iterator<IRaptorDataMessage> queryIterator;
        private final IQueryConstraints constraints;

        /**
         * An atomic boolean to keep track of attempts to cancel. It was discovered that using the parent's
         * {@link #isCancelled()} always returned false because the task had already completed normally before any calls to
         * {@link #cancel(boolean)} were made. For our use case, we want to know if any attempts to cancel have been
         * made at all so we use this variable to indicate that a cancel has been initiated.
         */
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        RefreshWorker(IQueryConstraints constraints)
        {
            super("Query Failure", "Failed to query device log messages");

            this.constraints = constraints;
        }

        int getTotalCount()
        {
            return workerTotalCount;
        }

        /**
         * Cancels the progress of this refresh worker and sets {@link #cancelled} to true. This method should be called
         * if the caller wants to know that a cancel has been attempted (this result can be checked by using
         * {@link #cancelled()}), instead of a cancel having been successful. Using the parent's cancel method will
         * result in the latter, and can be checked with the normal {@link #isCancelled()} call.
         *
         * @since 2.36
         */
        public void cancel()
        {
            super.cancel(true);
            cancelled.set(true);
        }

        /**
         * Returns whether a cancel has been attempted or not. This method should be used in conjunction with this class's
         * {@link #cancel()} if the caller wants to know that a cancel has been attempted as opposed to a cancel
         * having been successful (the default behavior). For a bit of awareness, the super class's {@link #isCancelled()}
         * method is final and cannot be overridden, hence this method was named differently.
         *
         * @return True, if the task has been cancelled. False, otherwise.
         * @since 2.36
         */
        public boolean cancelled()
        {
            return isCancelled() || cancelled.get();
        }

        @Override
        public Long doInBackground()
        {
            int recordsProcessed = 0;
            final List<IRaptorDataMessage> messages;

            // Look - if we are trying to retrieve over 2.1 BILLION records using the device log,
            // we have bigger problems than the fact that I'm casting this into a negative int value!
            workerTotalCount = (int) queryService.count(constraints);

            // Special case:
            // Textual date search (e.g. "January" or "Wednesday" or "11:00")
            // We must iterate over all records, one at a time, and compare the
            // date chunk until we've met the max count criteria.

            if (fieldFilter != null && fieldFilter.isSpecialDateTypeMatched())
            {
                int maxCount = constraints.getMaxMessageCount();
                if (maxCount <= 0) maxCount = workerTotalCount;

                // Unconstrain the message count since we're doing the filtering manually
                constraints.setMaxMessageCount(IQueryConstraints.DEFAULT_MAX_MESSAGES);

                int inspectedCount = 0;
                queryIterator = queryService.iterator(constraints);
                messages = new ArrayList<>();

                while (!cancelled() && queryIterator.hasNext() && recordsProcessed < maxCount)
                {
                    final IRaptorDataMessage rdm = queryIterator.next();

                    if (fieldFilter.matchSpecialDate(rdm))
                    {
                        recordsProcessed++;
                        messages.add(rdm);
                        publish(rdm);
                    }

                    // We don't want the progress bar to hit 100% or it will disappear
                    int progress = Math.min(99, Math.max(100 * inspectedCount++ / workerTotalCount, 100 * recordsProcessed / maxCount));

                    setProgress(progress);
                }
            } else
            {
                // Otherwise, just a normal pre-fetch query using the constraints (which include the field filter, if any)
                messages = queryService.query(constraints, true);

                // Populate the rows into the model
                for (IRaptorDataMessage rdm : messages)
                {
                    if (cancelled()) break;

                    // Annoying, but required.  We have to ensure the structure is unproxied before handing
                    // it off to the EDT to process or bad things will happen inside hibernate (like NPEs).
                    rdm.getStructure();

                    recordsProcessed++;
                    publish(rdm);
                }
            }

            if (logger.isDebugEnabled()) logger.debug("Total RDMs retrieved: {}", messages.size());

            setProgress(100);

            return (long) recordsProcessed;
        }

        /**
         * Process one or more rows of data published from the background thread.  Do this on the
         * EDT since we are mutating the table model.
         *
         * @param rowList List of row data to add to the table model
         */
        @Override
        protected void process(List<IRaptorDataMessage> rowList)
        {
            if (cancelled())
            {
                return;
            }

            try // Don't let failures here affect the EDT
            {
                rdmTablePanel.addMessages(rowList);
            } catch (IllegalArgumentException e)
            {
                logger.error("Failed to load RDMs", e);
                AlwaysOnTopJOptionPane.showErrorDialog("Failed to load messages into view.\nCause: " + e.getMessage(), "Load Failure");
            }
        }

        /**
         * Cleanup after our execution, regardless of failure or success.
         */
        @Override
        protected void cleanup()
        {
            setProgress(0);

            // A long running query iteration may have been interrupted. If it was, clean up.
            if (queryIterator != null)
            {
                queryService.closeIterator(queryIterator);
                queryIterator = null;
            }

            persistServiceProvider.cleanupPersistService(); // All done - be tidy
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var panel1 = new JPanel();
        var label5 = new JLabel();
        nameLabel = new JLabel();
        var label2 = new JLabel();
        unitIDLabel = new JLabel();
        labelLocation = new JLabel();
        panelLocation = new JPanel();
        var label3 = new JLabel();
        labelMin = new JLabel();
        var label4 = new JLabel();
        labelMax = new JLabel();
        var labelDataStruct = new JLabel();
        structureComboBox = new JComboBox<>();
        exportAllButton = new JButton();
        var label1 = new JLabel();
        var scrollPane2 = new JScrollPane();
        textPaneDescription = new JTextPane();
        buttonDetails = new JButton();
        var labelMaxRows = new JLabel();
        rowCountComboBox = new JComboBox<>();
        spinnerRowCount = new JSpinner();
        var labelFilterSetup = new JLabel();
        comboBoxField = new JComboBox<>();
        comboBoxComparison1 = new JComboBox<>();
        textFieldFilter1 = new JTextField();
        buttonApplyFilter = new JButton();
        labelOptionalAnd = new JLabel();
        comboBoxComparison2 = new JComboBox<>();
        textFieldFilter2 = new JTextField();
        var labelFilterDescription = new JLabel();
        labelCurrentFilter = new JLabel();
        buttonClearFilter = new JButton();
        var panel5 = new JPanel();
        var panel4 = new JPanel();
        labelRecordCount = new JLabel();
        btnPrevious = new JButton();
        btnNext = new JButton();
        exportPanel = new JPanel();
        labelExport = new JLabel();
        progressBar = new JProgressBar();
        cancelExportButton = new JButton();
        var panel3 = new JPanel();
        newDataLabel = new JLabel();
        exportButton = new JButton();
        var printButton = new JButton();
        goToButton = new JButton();
        chkLive = new JCheckBox();
        refreshButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "fill:default, fill:default:grow, 2*($lgap, default)"));

        //======== panel1 ========
        {
            panel1.setName("panel1");
            panel1.setLayout(new FormLayout(
                    "default, $lcgap, default:grow, $lcgap, default, $lcgap, default:grow, $lcgap, default",
                    "9*(default, $lgap), default"));

            //---- label5 ----
            label5.setText("Name:");
            label5.setName("label5");
            panel1.add(label5, cc.xy(1, 1));

            //---- nameLabel ----
            nameLabel.setName("nameLabel");
            panel1.add(nameLabel, cc.xywh(3, 1, 7, 1));

            //---- label2 ----
            label2.setText("Unit ID:");
            label2.setName("label2");
            panel1.add(label2, cc.xy(1, 3));

            //---- unitIDLabel ----
            unitIDLabel.setName("unitIDLabel");
            panel1.add(unitIDLabel, cc.xywh(3, 3, 7, 1));

            //---- labelLocation ----
            labelLocation.setText("Search Location:");
            labelLocation.setName("labelLocation");
            panel1.add(labelLocation, cc.xy(1, 5));

            //======== panelLocation ========
            {
                panelLocation.setName("panelLocation");
                panelLocation.setLayout(new FormLayout(
                        "default, right:default, $lcgap, default, $ugap, right:default, $lcgap, default",
                        "default"));

                //---- label3 ----
                label3.setText("Min:");
                label3.setName("label3");
                panelLocation.add(label3, cc.xy(2, 1));

                //---- labelMin ----
                labelMin.setName("labelMin");
                panelLocation.add(labelMin, cc.xy(4, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

                //---- label4 ----
                label4.setText("Max:");
                label4.setName("label4");
                panelLocation.add(label4, cc.xy(6, 1));

                //---- labelMax ----
                labelMax.setName("labelMax");
                panelLocation.add(labelMax, cc.xy(8, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
            }
            panel1.add(panelLocation, cc.xywh(3, 5, 7, 1));

            //---- labelDataStruct ----
            labelDataStruct.setText("Message Type:");
            labelDataStruct.setName("labelDataStruct");
            panel1.add(labelDataStruct, cc.xy(1, 7));

            //---- structureComboBox ----
            structureComboBox.setName("structureComboBox");
            structureComboBox.addActionListener(e -> structureComboBoxActionPerformed());
            panel1.add(structureComboBox, cc.xywh(3, 7, 5, 1));

            //---- exportAllButton ----
            exportAllButton.setText("Export All");
            exportAllButton.setToolTipText("Export all messages for the currently selected Message Type.");
            exportAllButton.setName("exportAllButton");
            exportAllButton.addActionListener(e -> exportAllButtonActionPerformed());
            panel1.add(exportAllButton, cc.xy(9, 7));

            //---- label1 ----
            label1.setText("Description:");
            label1.setName("label1");
            panel1.add(label1, cc.xy(1, 9));

            //======== scrollPane2 ========
            {
                scrollPane2.setName("scrollPane2");

                //---- textPaneDescription ----
                textPaneDescription.setEditable(false);
                textPaneDescription.setName("textPaneDescription");
                scrollPane2.setViewportView(textPaneDescription);
            }
            panel1.add(scrollPane2, cc.xywh(3, 9, 5, 3));

            //---- buttonDetails ----
            buttonDetails.setText("Details");
            buttonDetails.setMnemonic('D');
            buttonDetails.setEnabled(false);
            buttonDetails.setToolTipText("View details of the current Message Type.");
            buttonDetails.setName("buttonDetails");
            buttonDetails.addActionListener(e -> buttonDetailsActionPerformed());
            panel1.add(buttonDetails, cc.xy(9, 11));

            //---- labelMaxRows ----
            labelMaxRows.setText("Max Rows:");
            labelMaxRows.setName("labelMaxRows");
            panel1.add(labelMaxRows, cc.xy(1, 13));

            //---- rowCountComboBox ----
            rowCountComboBox.setName("rowCountComboBox");
            rowCountComboBox.addActionListener(e -> rowCountComboBoxActionPerformed());
            panel1.add(rowCountComboBox, cc.xy(3, 13));

            //---- spinnerRowCount ----
            spinnerRowCount.setVisible(false);
            spinnerRowCount.setModel(new SpinnerNumberModel(1, 1, null, 1));
            spinnerRowCount.setName("spinnerRowCount");
            panel1.add(spinnerRowCount, cc.xy(5, 13));

            //---- labelFilterSetup ----
            labelFilterSetup.setText("Filter:");
            labelFilterSetup.setName("labelFilterSetup");
            panel1.add(labelFilterSetup, cc.xy(1, 15));

            //---- comboBoxField ----
            comboBoxField.setName("comboBoxField");
            panel1.add(comboBoxField, cc.xy(3, 15));

            //---- comboBoxComparison1 ----
            comboBoxComparison1.setName("comboBoxComparison1");
            comboBoxComparison1.addItemListener(e -> comboBoxComparisonItemStateChanged(e));
            panel1.add(comboBoxComparison1, cc.xy(5, 15));

            //---- textFieldFilter1 ----
            textFieldFilter1.setName("textFieldFilter1");
            textFieldFilter1.addKeyListener(new KeyAdapter()
            {
                @Override
                public void keyPressed(KeyEvent e)
                {
                    textFieldFilterKeyPressed(e);
                }
            });
            panel1.add(textFieldFilter1, cc.xy(7, 15));

            //---- buttonApplyFilter ----
            buttonApplyFilter.setText("Apply");
            buttonApplyFilter.setToolTipText("Apply the filter parameters to the message list.");
            buttonApplyFilter.setName("buttonApplyFilter");
            buttonApplyFilter.addActionListener(e -> buttonApplyFilterActionPerformed());
            panel1.add(buttonApplyFilter, cc.xy(9, 15));

            //---- labelOptionalAnd ----
            labelOptionalAnd.setText("<html><i>(optional)</i> <b>AND</b>");
            labelOptionalAnd.setHorizontalAlignment(SwingConstants.TRAILING);
            labelOptionalAnd.setName("labelOptionalAnd");
            panel1.add(labelOptionalAnd, cc.xy(3, 17));

            //---- comboBoxComparison2 ----
            comboBoxComparison2.setName("comboBoxComparison2");
            panel1.add(comboBoxComparison2, cc.xy(5, 17));

            //---- textFieldFilter2 ----
            textFieldFilter2.setName("textFieldFilter2");
            textFieldFilter2.addKeyListener(new KeyAdapter()
            {
                @Override
                public void keyPressed(KeyEvent e)
                {
                    textFieldFilterKeyPressed(e);
                }
            });
            panel1.add(textFieldFilter2, cc.xy(7, 17));

            //---- labelFilterDescription ----
            labelFilterDescription.setText("Current Filter:");
            labelFilterDescription.setName("labelFilterDescription");
            panel1.add(labelFilterDescription, cc.xy(1, 19));

            //---- labelCurrentFilter ----
            labelCurrentFilter.setText("None");
            labelCurrentFilter.setName("labelCurrentFilter");
            panel1.add(labelCurrentFilter, cc.xywh(3, 19, 5, 1));

            //---- buttonClearFilter ----
            buttonClearFilter.setText("Clear");
            buttonClearFilter.setEnabled(false);
            buttonClearFilter.setToolTipText("Clear the filter parameters.");
            buttonClearFilter.setName("buttonClearFilter");
            buttonClearFilter.addActionListener(e -> buttonClearFilterActionPerformed());
            panel1.add(buttonClearFilter, cc.xy(9, 19));
        }
        add(panel1, cc.xy(1, 1));

        //======== panel5 ========
        {
            panel5.setName("panel5");
            panel5.setLayout(new FormLayout(
                    "default, $lcgap, default:grow, $lcgap, default",
                    "default"));

            //======== panel4 ========
            {
                panel4.setName("panel4");
                panel4.setLayout(new FormLayout(
                        "pref, $lcgap, min, $lcgap, left:min",
                        "default"));

                //---- labelRecordCount ----
                labelRecordCount.setText("0 records");
                labelRecordCount.setName("labelRecordCount");
                panel4.add(labelRecordCount, cc.xy(1, 1));

                //---- btnPrevious ----
                btnPrevious.setText("<");
                btnPrevious.setToolTipText("Fetch previous batch of records");
                btnPrevious.setFocusable(false);
                btnPrevious.setFocusPainted(false);
                btnPrevious.setEnabled(false);
                btnPrevious.setName("btnPrevious");
                btnPrevious.addActionListener(e -> btnPreviousActionPerformed());
                panel4.add(btnPrevious, cc.xy(3, 1));

                //---- btnNext ----
                btnNext.setText(">");
                btnNext.setToolTipText("Fetch next batch of records");
                btnNext.setFocusable(false);
                btnNext.setFocusPainted(false);
                btnNext.setEnabled(false);
                btnNext.setName("btnNext");
                btnNext.addActionListener(e -> btnNextActionPerformed());
                panel4.add(btnNext, cc.xy(5, 1));
            }
            panel5.add(panel4, cc.xy(1, 1));

            //======== exportPanel ========
            {
                exportPanel.setVisible(false);
                exportPanel.setName("exportPanel");
                exportPanel.setLayout(new FormLayout(
                        "default, $lcgap, default:grow, $lcgap, default",
                        "default"));

                //---- labelExport ----
                labelExport.setText("Exporting Data");
                labelExport.setForeground(Color.red);
                labelExport.setName("labelExport");
                exportPanel.add(labelExport, cc.xy(1, 1));

                //---- progressBar ----
                progressBar.setName("progressBar");
                exportPanel.add(progressBar, cc.xywh(3, 1, 3, 1));
            }
            panel5.add(exportPanel, cc.xy(3, 1));

            //---- cancelExportButton ----
            cancelExportButton.setText("Cancel");
            cancelExportButton.setVisible(false);
            cancelExportButton.setName("cancelExportButton");
            cancelExportButton.addActionListener(e -> cancelExportButtonActionPerformed());
            panel5.add(cancelExportButton, cc.xy(5, 1));
        }
        add(panel5, cc.xy(1, 4));

        //======== panel3 ========
        {
            panel3.setName("panel3");
            panel3.setLayout(new FormLayout(
                    "4*(default, $lcgap), default:grow, 2*($lcgap, default)",
                    "default, $lgap, default"));

            //---- newDataLabel ----
            newDataLabel.setText("New Data Available");
            newDataLabel.setToolTipText("Display whether new data is available on refresh.");
            newDataLabel.setName("newDataLabel");
            panel3.add(newDataLabel, cc.xywh(11, 1, 3, 1, CellConstraints.FILL, CellConstraints.FILL));

            //---- exportButton ----
            exportButton.setText("Export");
            exportButton.setToolTipText("Export the current list of messages.");
            exportButton.setName("exportButton");
            exportButton.addActionListener(e -> exportButtonActionPerformed());
            panel3.add(exportButton, cc.xy(1, 3));

            //---- printButton ----
            printButton.setText("Print");
            printButton.setToolTipText("Print the current list of messages.");
            printButton.setName("printButton");
            printButton.addActionListener(e -> printButtonActionPerformed());
            panel3.add(printButton, cc.xy(3, 3));

            //---- goToButton ----
            goToButton.setText("Go To");
            goToButton.setToolTipText("Go to globe location of the selected message.");
            goToButton.setName("goToButton");
            goToButton.addActionListener(e -> goToButtonActionPerformed());
            panel3.add(goToButton, cc.xy(5, 3));

            //---- chkLive ----
            chkLive.setText("Live");
            chkLive.setHorizontalTextPosition(SwingConstants.LEADING);
            chkLive.setToolTipText("Incoming RDM messages will be added to the message table.");
            chkLive.setName("chkLive");
            chkLive.addItemListener(e -> chkAutoRefreshItemStateChanged(e));
            panel3.add(chkLive, cc.xy(11, 3, CellConstraints.RIGHT, CellConstraints.DEFAULT));

            //---- refreshButton ----
            refreshButton.setText("Refresh");
            refreshButton.setToolTipText("Refresh the list of messages above.");
            refreshButton.setName("refreshButton");
            refreshButton.addActionListener(e -> refreshButtonActionPerformed());
            panel3.add(refreshButton, cc.xy(13, 3));
        }
        add(panel3, cc.xy(1, 6));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel nameLabel;
    private JLabel unitIDLabel;
    private JLabel labelLocation;
    private JPanel panelLocation;
    private JLabel labelMin;
    private JLabel labelMax;
    private JComboBox<IRaptorDataStructure> structureComboBox;
    private JButton exportAllButton;
    private JTextPane textPaneDescription;
    private JButton buttonDetails;
    private JComboBox<RowCounter> rowCountComboBox;
    private JSpinner spinnerRowCount;
    private JComboBox<String> comboBoxField;
    private JComboBox<ComparisonType> comboBoxComparison1;
    private JTextField textFieldFilter1;
    private JButton buttonApplyFilter;
    private JLabel labelOptionalAnd;
    private JComboBox<ComparisonType> comboBoxComparison2;
    private JTextField textFieldFilter2;
    private JLabel labelCurrentFilter;
    private JButton buttonClearFilter;
    private JLabel labelRecordCount;
    private JButton btnPrevious;
    private JButton btnNext;
    private JPanel exportPanel;
    private JLabel labelExport;
    private JProgressBar progressBar;
    private JButton cancelExportButton;
    private JLabel newDataLabel;
    private JButton exportButton;
    private JButton goToButton;
    private JCheckBox chkLive;
    private JButton refreshButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
