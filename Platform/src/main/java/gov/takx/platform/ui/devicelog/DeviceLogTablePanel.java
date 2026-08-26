package gov.takx.platform.ui.devicelog;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IStructure;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.query.CoreField;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.units.AltitudeUnits;
import gov.takx.platform.utils.units.SpeedUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.FormatStringValue;
import org.jdesktop.swingx.renderer.StringValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import java.awt.event.HierarchyEvent;
import java.lang.invoke.MethodHandles;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * This panel offers a simple table to display RDMs.  All the RDMs must share a common defining structure.
 * The table model is derived from the structure definition.
 * <p/>
 * The layout of the table (column ordering, widths, and visibility) will be saved and restored (according to the
 * structure used by the RDMs).  This feature is enabled by setting a template directory (used to store the layout
 * descriptors), via {@link #setTemplateDir(String)}.
 *
 * @since 1.49
 */
@SuppressWarnings({"unused"})
@PseudoBean
public class DeviceLogTablePanel extends JPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private StructureTableModel tableModel;
    private IStructure structure = null;
    private TableLayoutManager tableLayoutManager = null;

    private final DefaultTableRenderer rightAlignRenderer = new DefaultTableRenderer(StringValues.TO_STRING, JLabel.RIGHT);
    private final DefaultTableRenderer dateRenderer = new DefaultTableRenderer(new DateStringValue(), JLabel.RIGHT);
    private final DefaultTableRenderer booleanRenderer = new DefaultTableRenderer(StringValues.TO_STRING, JLabel.RIGHT);
    private final DefaultTableRenderer numberRenderer = new DefaultTableRenderer(StringValues.TO_STRING, JLabel.RIGHT);
    private final DefaultTableRenderer motionRenderer = new DefaultTableRenderer(new MotionStringValue(), JLabel.RIGHT);

    private final DefaultTableRenderer decimalLocationRenderer =
            new DefaultTableRenderer(new FormatStringValue(new DecimalFormat("##0.######")), JLabel.RIGHT);

    private final IPreferenceMonitor<AltitudeUnits> altitudeUnitsMonitor =
            UserPreferenceEntry.DISPLAY_ALTITUDE_UNITS.newMonitor(AltitudeUnits.class, this::updateAltitudeUnits);

    private final IPreferenceMonitor<SpeedUnits> speedUnitsMonitor =
            UserPreferenceEntry.DISPLAY_SPEED_UNITS.newMonitor(SpeedUnits.class, this::updateSpeedUnits);

    private SpeedStringValue speedStringValue;
    private DefaultTableRenderer speedRenderer;
    private ElevationStringValue elevationStringValue;
    private DefaultTableRenderer elevationRenderer;

    private boolean useColumnSpecificFiltering = false; // Filter against all columns, or add control to pick one

    /**
     * Constructor.
     */
    public DeviceLogTablePanel()
    {
        initComponents();

        rdmTable.putClientProperty(JXTable.USE_DTCR_COLORMEMORY_HACK, Boolean.FALSE);
        tableFilter.setTable(getRdmTable());
    }

    @PostConstruct
    private void postConstruct()
    {
        speedStringValue = new SpeedStringValue(speedUnitsMonitor.getCurrentValue());
        speedRenderer = new DefaultTableRenderer(speedStringValue, JLabel.RIGHT);
        elevationStringValue = new ElevationStringValue(altitudeUnitsMonitor.getCurrentValue());
        elevationRenderer = new DefaultTableRenderer(elevationStringValue, JLabel.RIGHT);
    }

    /**
     * Create an RDM table and initialize it with the given RDMs.
     *
     * @param rdms Messages to install into table
     * @throws IllegalArgumentException If rdms is {@code null} or the rdms reference more than one structure
     */
    @SuppressWarnings("WeakerAccess") // Used by plugins
    public DeviceLogTablePanel(Collection<IRaptorDataMessage> rdms) throws IllegalArgumentException
    {
        this();

        setMessages(rdms);
    }

    /**
     * Set the directory in which table layout templates are to be stored.  When a template directory is set, this
     * table will store/restore the table's layout.  Generally, this directory is in the VFS of the consumer.
     *
     * @param templateDir The directory in which to store layouts
     */
    public void setTemplateDir(String templateDir)
    {
        tableLayoutManager = new TableLayoutManager(templateDir);
    }

    /**
     * Save the current table layout to the templates.
     */
    public void saveTableLayout()
    {
        // Don't save the layout if the table is not showing since it won't have correct column widths
        if (tableLayoutManager != null && structure != null && rdmTable.isShowing())
        {
            tableLayoutManager.saveColumnStructure(rdmTable, structure);
        }
    }

    /**
     * @return The underlying table displaying the RDM data, always non-null
     */
    public JXTable getRdmTable()
    {
        return rdmTable;
    }

    /**
     * Remove all the data from the model.
     */
    public void clear()
    {
        saveTableLayout();

        if (tableModel != null) tableModel.clear();
        structure = null; // Will reinitialize our model when new data is added

        tableFilter.clear();
    }

    /**
     * Clear the table model data, but leave the model column definition intact.
     *
     * @since 2.3
     */
    public void clearTableData()
    {
        if (tableModel != null) tableModel.clear();
    }

    /**
     * Force a refresh of the search text field and associated actions.
     */
    public void searchFilterRefresh()
    {
        tableFilter.clear();
    }

    /**
     * Set the structure to be used for the column definitions.  If we already have data and this structure is
     * different, then the current data is cleared.
     *
     * @param structure new structure to install on table
     */
    public synchronized void setStructure(@Nonnull IStructure structure)
    {
        if (!structure.equals(this.structure))
        {
            clear(); // Clear does a saveTableLayout

            this.structure = structure;
            initTableModel();
        }
    }

    /**
     * Set the collection of messages to be displayed in the table.  The structure of the table will be determined
     * from the first RDM in the collection.  All the RDMs must be defined by that structure.
     *
     * @param rdms RDMs to display
     * @throws IllegalArgumentException If more than one structure is encountered in the list
     */
    public void setMessages(@Nonnull Collection<IRaptorDataMessage> rdms) throws IllegalArgumentException
    {
        structure = null; // Determine structure from first RDM

        testAndInitFromMessages(rdms);

        tableModel.setModelData(rdms);
    }

    /**
     * Get the current set of messages being displayed in the table.
     *
     * @return a copy of the table data model as a list of {@link IRaptorDataMessage} instances
     * @since 2.8
     */
    public List<IRaptorDataMessage> getMessages()
    {
        return tableModel.getDataModel();
    }

    /**
     * Add a collection of RDMs to the end of the model.  If the model already contains data and the new
     * RDMs reference a different structure, an exception will be thrown.
     *
     * @param rdms RDMs to add
     * @throws IllegalArgumentException If more than one structure is encountered in the list, or the
     *                                  structure in the list is different than the previously initialized structure
     */
    public void addMessages(@Nonnull Collection<IRaptorDataMessage> rdms) throws IllegalArgumentException
    {
        if (rdms.isEmpty()) return; // nothing to add

        testAndInitFromMessages(rdms); // Initialize/test structure

        // If unsorted, insert at the beginning.  Otherwise, just add to the end of the list.
        int sortedColumnIndex = rdmTable.getSortedColumnIndex();
        if (sortedColumnIndex == -1)
        {
            tableModel.addRows(0, rdms);
        } else
        {
            tableModel.addRows(rdms);
        }
    }

    /**
     * @return The current table model for the RDM table, may be {@code null} if not initialized with data yet.
     */
    public StructureTableModel getTableModel()
    {
        return tableModel;
    }

    /**
     * @return {@code true} if column-specific filtering has been enabled
     * @since 2.16
     */
    public boolean isUseColumnSpecificFiltering()
    {
        return useColumnSpecificFiltering;
    }

    /**
     * Set whether filtering is performed against all visible columns or a single column.  If
     * {@code useColumnSpecificFiltering} is {@code true} then a control will be displayed allowing the user to
     * select a specific column against which the filter text will be applied.  If it is {@code false} then
     * the column selection control will be hidden, and the filter will be matched against every visible column.
     * <p>
     * Be aware that on very large models (100K+ rows), filtering against all columns is incredibly slow and should
     * be avoided.
     *
     * @param useColumnSpecificFiltering Pass {@code true} to enable column-specific filtering
     * @since 2.16
     */
    public void setUseColumnSpecificFiltering(boolean useColumnSpecificFiltering)
    {
        this.useColumnSpecificFiltering = useColumnSpecificFiltering;
        tableFilter.setUseColumnSpecificFiltering(useColumnSpecificFiltering);
    }

    /**
     * Scan the given RDMs. If our structure is {@code null} to start, then initialize our table model from the first
     * RDMs structure. If any RDM has a different structure than the initial structure, then throw an error.
     *
     * @param rdms RDMs to process
     * @throws IllegalArgumentException If more than one structure is encountered in the list
     */
    private synchronized void testAndInitFromMessages(Collection<IRaptorDataMessage> rdms) throws IllegalArgumentException
    {
        for (IRaptorDataMessage rdm : rdms)
        {
            final IStructure s = rdm.getStructure();

            if (structure == null)
            {
                setStructure(s);
            } else
            {
                if (!structure.equals(s))
                {
                    final String structMsg = String.format("%s(%s) and %s(%s)",
                            structure.getMessageType(), structure.getHashValue(),
                            s.getMessageType(), s.getHashValue());

                    throw new IllegalArgumentException("RDMs reference more than one structure: " + structMsg);
                }
            }
        }
    }

    /**
     * Construct the table model from the current structure and configure the table columns to their default settings.
     */
    private void initTableModel()
    {
        tableModel = new StructureTableModel(structure);
        rdmTable.setModel(tableModel);

        rdmTable.setDefaultRenderer(Object.class, rightAlignRenderer); // Generally make everything right aligned
        rdmTable.setDefaultRenderer(Date.class, dateRenderer); // All dates consistently formatted
        rdmTable.setDefaultRenderer(Boolean.class, booleanRenderer); // True/false/unknown instead of checkbox
        rdmTable.setDefaultRenderer(Number.class, numberRenderer); // Uses an empty String for null Number values

        // Install our custom renderers for location, speed, elevation, and motion duration
        rdmTable.getColumnExt(CoreField.latitude.name()).setCellRenderer(decimalLocationRenderer);
        rdmTable.getColumnExt(CoreField.longitude.name()).setCellRenderer(decimalLocationRenderer);
        rdmTable.getColumnExt(CoreField.elevation.name()).setCellRenderer(elevationRenderer);
        rdmTable.getColumnExt(CoreField.speed.name()).setCellRenderer(speedRenderer);
        rdmTable.getColumnExt(CoreField.hasMotion.name()).setCellRenderer(motionRenderer);

        // Now hide the columns we don't want to see by default
        hideColumns(CoreField.family.name(), CoreField.type.name(), CoreField.unitId.name(),
                CoreField.hasLocation.name(), CoreField.uuid.name());

        // Restore the table layout if we have a configured template handler
        if (tableLayoutManager != null)
        {
            tableLayoutManager.updateTableFromTemplate(rdmTable, structure);
        }
    }

    /**
     * Hide the indicated columns in the RDM able.
     */
    private void hideColumns(String... columnNames)
    {
        for (String columnName : columnNames)
        {
            rdmTable.getColumnExt(columnName).setVisible(false);
        }
    }

    /**
     * @param units The altitude units to use when displaying speed values
     * @since 5.5
     */
    private void updateAltitudeUnits(AltitudeUnits units)
    {
        elevationStringValue.updateUnitsOfMeasure(units);
    }

    /**
     * @param units The speed units when displaying speed values
     * @since 5.5
     */
    private void updateSpeedUnits(SpeedUnits units)
    {
        speedStringValue.updateUnitsOfMeasure(units);
    }

    /**
     * Handle a possible change in our visibility.  If we are showing, then we need to install the current
     * {@link AltitudeUnits} and {@link SpeedUnits} and monitor them for changes.  If not, we can stop monitoring for
     * changes.
     *
     * @param e hierarchy event
     */
    private void thisHierarchyChanged(HierarchyEvent e)
    {
        if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0)
        {
            if (isShowing())
            {
                altitudeUnitsMonitor.start();
                speedUnitsMonitor.start();

                // Since this panel can be used in dialogs that are long-lived, we need to update the UoM as well
                updateAltitudeUnits(altitudeUnitsMonitor.getCurrentValue());
                updateSpeedUnits(speedUnitsMonitor.getCurrentValue());

                repaint();
            } else
            {
                altitudeUnitsMonitor.stop();
                speedUnitsMonitor.stop();
            }
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        JScrollPane scrollPane1 = new JScrollPane();
        rdmTable = new DeviceLogTable();
        JPanel panel2 = new JPanel();
        tableFilter = new TableFilterComponent();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        addHierarchyListener(e -> thisHierarchyChanged(e));
        setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow, 2*($lgap, default)"));

        //======== scrollPane1 ========
        {

            //---- rdmTable ----
            rdmTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            rdmTable.setEditable(false);
            rdmTable.setColumnControlVisible(true);
            rdmTable.setHorizontalScrollEnabled(true);
            rdmTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            rdmTable.setName("rdmTable");
            scrollPane1.setViewportView(rdmTable);
        }
        add(scrollPane1, cc.xy(1, 1));

        //======== panel2 ========
        {
            panel2.setLayout(new FormLayout(
                    "default:grow",
                    "default"));
            panel2.add(tableFilter, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
        }
        add(panel2, cc.xy(1, 3, CellConstraints.FILL, CellConstraints.DEFAULT));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private DeviceLogTable rdmTable;
    private TableFilterComponent tableFilter;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
