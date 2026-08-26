package gov.takx.platform.io.panels;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.plugin.io.AWizardPanel;
import gov.takx.commons.swing.SelectAllTableSupport;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Wizard panel for choosing the map objects to export. This wizard
 * stores the selected map objects into the wizard properties map as a
 * {@code Set<IMapObject>} under the key
 * {@link CommonWizardSettings#SELECTED_MAP_OBJECTS}.
 *
 */
@SuppressWarnings("unused") // Used in plugins
@PseudoBean
public class ChooseDevicesPanel extends AWizardPanel
{
    /**
     * Identifier for this panel.
     */
    public static final String IDENTIFIER = "platform.io.panels.ChooseDevicesPanel";

    // For sorting the model
    private static final Comparator<IMapObject> MAP_OBJECT_COMPARATOR =
            Comparator.comparing(IMapObject::getFamily)
                    .thenComparing(IMapObject::getType)
                    .thenComparing(IMapObject::getUnitID);

    /**
     * The index of the checkbox column for selecting/de-selecting all devices
     */
    private static final int CHECKBOX_COLUMN = 0;

    /**
     * Possible selection mode.
     */
    public enum SelectionMode
    {
        /**
         * Allow the user to select only a single device.
         */
        SINGLE_DEVICE,

        /**
         * Allow the user to select more than one device, but all must
         * be of the same type (for example "Acme 1" and "Acme 2" may
         * be selected, but a non-Acme device may not added to the
         * selection.
         */
        SINGLE_DEVICE_TYPE,

        /**
         * Allow the user to select multiple devices of different types.
         */
        MULTIPLE_DEVICE_TYPE
    }

    private static final String SINGLE_DEVICE_HELP = "Select a map object to export.";
    private static final String SINGLE_DEVICE_TYPE_HELP = "Select map objects to export. All objects must be of the same type.";
    private static final String MULTIPLE_DEVICE_TYPE_HELP = "Select map objects to export.";

    private final SelectionMode selectionMode;
    private final Set<IMapObject> selections;

    @Inject
    private IMapObjectManager mapObjectManager;

    /**
     * Create a panel and allow the user to select multiple devices of
     * different types.
     *
     * @since 3.0 (delegate parameter removed)
     */
    public ChooseDevicesPanel()
    {
        this(SelectionMode.MULTIPLE_DEVICE_TYPE);
    }

    /**
     * Create a panel with the given selection mode.
     *
     * @param selectionMode The selection mode to use in this panel
     * @since 3.0 (delegate parameter removed)
     */
    public ChooseDevicesPanel(SelectionMode selectionMode)
    {
        this.selectionMode = selectionMode;
        selections = new HashSet<>();

        initComponents();

        if (selectionMode == SelectionMode.MULTIPLE_DEVICE_TYPE)
        {
            mapObjectTable.setDragEnabled(false);
            mapObjectTable.getTableHeader().setReorderingAllowed(false);

            SelectAllTableSupport.configureSelectAllColumn(mapObjectTable, (DefaultTableModel) mapObjectTable.getModel(), CHECKBOX_COLUMN, false, 28);
        }
    }

    @PostConstruct
    private void postConstruct()
    {
        initTable();
    }

    /**
     * Initialize the table of map objects.
     */
    private void initTable()
    {
        final List<IMapObject> mapObjects = mapObjectManager.getMapObjectControllers().stream()
                .map(IMapObjectController::getMapObject)
                .sorted(MAP_OBJECT_COMPARATOR)
                .collect(Collectors.toList());

        DefaultTableModel model = (DefaultTableModel) mapObjectTable.getModel();
        mapObjectTable.setDefaultRenderer(Object.class, new MapObjectTableCellRenderer());

        for (IMapObject mo : mapObjects)
        {
            model.addRow(new Object[]{Boolean.FALSE, mo.getName(), mo.getType(), mo});
        }
    }

    @Override
    public String getIdentifier()
    {
        return IDENTIFIER;
    }

    @Override
    public void aboutToDisplayPanel()
    {
        validateContents();
    }

    @Override
    public void displayingPanel()
    {
    }

    @Override
    public void aboutToHidePanel()
    {
        wizard.getModel().setProperty(CommonWizardSettings.SELECTED_MAP_OBJECTS, selections);
        wizard.getModel().setProperty(CommonWizardSettings.EXCLUDE_ZERO_ZERO_POINTS, checkBoxExcludeZeroZero.isSelected());
    }

    // TODO:  We need to add some correct logic to support SelectionMode to protect the operators from themselves.
    private void buildSelectionList()
    {
        selections.clear();

        DefaultTableModel model = (DefaultTableModel) mapObjectTable.getModel();

        for (int i = 0; i < model.getRowCount(); i++)
        {
            if ((Boolean) model.getValueAt(i, CHECKBOX_COLUMN))
            {
                selections.add((IMapObject) model.getValueAt(i, 3));
            }
        }
    }

    private void validateContents()
    {
        if (wizard != null)
        {
            wizard.getModel().setNextButtonEnabled(!selections.isEmpty());
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var helpLabel = new JLabel();
        var scrollPane1 = new JScrollPane();
        mapObjectTable = new JTable();
        checkBoxExcludeZeroZero = new JCheckBox();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "top:min, $rgap, fill:default:grow, $lgap, default"));

        //---- helpLabel ----
        helpLabel.setText("Please select the map objects you would like to export:");
        helpLabel.setName("helpLabel");
        add(helpLabel, cc.xy(1, 1));

        //======== scrollPane1 ========
        {
            scrollPane1.setName("scrollPane1");

            //---- mapObjectTable ----
            mapObjectTable.setPreferredScrollableViewportSize(new Dimension(100, 100));
            mapObjectTable.setModel(new DefaultTableModel(
                    new Object[][]{
                    },
                    new String[]{
                            null, "Name", "Type", "Id"
                    })
            {
                Class<?>[] columnTypes = new Class<?>[]{
                        Boolean.class, Object.class, Object.class, Object.class
                };
                boolean[] columnEditable = new boolean[]{
                        true, false, false, false
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
            mapObjectTable.setName("mapObjectTable");
            scrollPane1.setViewportView(mapObjectTable);
        }
        add(scrollPane1, cc.xy(1, 3));

        //---- checkBoxExcludeZeroZero ----
        checkBoxExcludeZeroZero.setText("Exclude Points with a 0,0 Latitude and Longitude");
        checkBoxExcludeZeroZero.setSelected(true);
        checkBoxExcludeZeroZero.setToolTipText("Checking this box will prevent any records with a 0,0 latitude and longitude from being exported to the file");
        checkBoxExcludeZeroZero.setRequestFocusEnabled(false);
        checkBoxExcludeZeroZero.setName("checkBoxExcludeZeroZero");
        add(checkBoxExcludeZeroZero, cc.xy(1, 5));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents

        switch (selectionMode)
        {
            case SINGLE_DEVICE -> helpLabel.setText(SINGLE_DEVICE_HELP);
            case SINGLE_DEVICE_TYPE -> helpLabel.setText(SINGLE_DEVICE_TYPE_HELP);
            case MULTIPLE_DEVICE_TYPE -> helpLabel.setText(MULTIPLE_DEVICE_TYPE_HELP);
        }

        mapObjectTable.getModel().addTableModelListener(e -> {
            buildSelectionList();
            validateContents();
        });
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTable mapObjectTable;
    private JCheckBox checkBoxExcludeZeroZero;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    /**
     * Renderer to ensure MOs are rendered in the table with a user-friendly value. Since they are stored in the ID
     * column, this should be the unitId.
     *
     * @since 3.0
     */
    private static class MapObjectTableCellRenderer extends DefaultTableCellRenderer
    {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value instanceof IMapObject) setText(((IMapObject) value).getUnitID());

            return this;
        }
    }
}
