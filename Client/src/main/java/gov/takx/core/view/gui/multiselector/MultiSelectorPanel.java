package gov.takx.core.view.gui.multiselector;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.core.comms.cot.CotExporterManager;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.globe.MultiSelectorConfiguration;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.shapes.ShapeType;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.swing.ButtonAction;
import gov.takx.commons.swing.SplitButton;
import gov.takx.core.globe.MultiSelectorService;
import gov.takx.core.mapobject.gui.SendAsCotDialog;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.core.view.gui.takserver.DataPackageAddCreateDialog;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A panel to display the selected map elements
 *
 * @since 3.5
 */
@PseudoBean(singletonObserver = true)
public class MultiSelectorPanel extends ATitledPanel implements IResourceCleaner
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int ICON_COLUMN_SIZE = 32;
    private static final int SELECTED_COLUMN_SIZE = 64;
    public static final String TITLE = ButtonType.MULTI_SELECTOR.requireDisplayText();

    private final MultiSelectorTableModel selectedObjectsTableModel = new MultiSelectorTableModel();
    private final Map<String, Consumer<List<IMapObject>>> exportTypesAndFunctions = new LinkedHashMap<>();

    @Inject
    private MultiSelectorService multiSelectorService;

    @Inject
    private CotExporterManager cotExporterManager;

    @Inject
    private IMapObjectManager mapObjectManager;

    public MultiSelectorPanel()
    {
        super(TITLE);
        initComponents();

        selectedObjectsTable.setModel(selectedObjectsTableModel);

        // Make table sortable by all columns except the Icon column
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(selectedObjectsTableModel);
        sorter.setSortable(MultiSelectorTableModel.Column.Icon.ordinal(), false);
        selectedObjectsTable.setRowSorter(sorter);

        final TableColumnModel columnModel = selectedObjectsTable.getColumnModel();
        columnModel.getColumn(MultiSelectorTableModel.Column.Icon.ordinal()).setMaxWidth(ICON_COLUMN_SIZE);
        columnModel.getColumn(MultiSelectorTableModel.Column.Selected.ordinal()).setMaxWidth(SELECTED_COLUMN_SIZE);

        selectedObjectsTable.setDefaultRenderer(String.class, new StringCellRenderer());

        exportTypesAndFunctions.put("Send as CoT", this::exportAsCoT);
        exportTypesAndFunctions.put("Data Package", this::exportAsDataPackage);
        createExportMenu();
    }

    private void createExportMenu()
    {
        JPopupMenu exportPopupMenu = new JPopupMenu();
        exportTypesAndFunctions.forEach((name, function) -> {
            exportPopupMenu.add(new ButtonAction(name, e -> function.accept(getSelectedObjects())));
        });
        exportButton.setPopupMenu(exportPopupMenu);
    }

    /**
     * Add an object to the selected objects table.
     * @param mapObject The map object to add to the table
     */
    @InvokeOnEDT
    public void addSelectedObject(@Nonnull IMapObject mapObject)
    {
        selectedObjectsTableModel.addRow(mapObject);
    }

    /**
     * Add a list of objects to the selected objects table.
     * @param mapObjectList The list of map objects to add to the table
     */
    @InvokeOnEDT
    public void addSelectedObjects(@Nonnull Set<IMapObject> mapObjectList)
    {
        mapObjectList.forEach(this::addSelectedObject);
    }

    /**
     * The panel tracks which objects have the "Selected" property enabled, and this returns all those with it set as true.
     *
     * @return A list of actively selected objects
     */
    @Nonnull
    public List<IMapObject> getSelectedObjects()
    {
        return selectedObjectsTableModel.getSelectedObjects();
    }

    /**
     * Listen for MO removal and remove the MO from the selected objects table if present.
     */
    @InvokeOnEDT
    private void onMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        selectedObjectsTableModel.removeRow(event.getMapObject());
    }

    /**
     * Clear the table model and dispose of the resources in it
     */
    @InvokeOnEDT
    public void clearTable()
    {
        selectedObjectsTableModel.clearTable();
    }

    @InvokeOnEDT
    private void deleteButtonClicked()
    {
        selectedObjectsTableModel.getSelectedObjects().forEach(mapObjectManager::deleteMapObject);
        selectedObjectsTableModel.fireTableDataChanged();
    }

    private List<CotEvent> getMapObjectsAsCotEvents(List<IMapObject> mapObjects)
    {
        // filter out NEVER_COT, convert to CoT, and filter out the nulls
        return mapObjects.stream()
                .filter(x -> !x.getMetaBoolean(StandardAttributeNames.NEVER_COT, false))
                .map(x -> cotExporterManager.export(x))
                .filter(Objects::nonNull).toList();
    }

    private void exportAsCoT(List<IMapObject> mapObjects)
    {
        new SendAsCotDialog("Multiselector Export",
                () -> getMapObjectsAsCotEvents(mapObjects),
                () -> logger.warn("Error sending MultiSelector elements as CoT, no items that can be converted to CoT selected."))
                .setVisible(true);
    }

    private void exportAsDataPackage(List<IMapObject> mapObjects)
    {
        new DataPackageAddCreateDialog(mapObjects).setVisible(true);
    }

    @Override
    public void releaseResources()
    {
        selectedObjectsTableModel.releaseResources();
    }

    private void clearButtonClicked()
    {
        multiSelectorService.disarmAndClear();
    }

    private void appendSelectionButtonPressed()
    {
        multiSelectorService.arm(MultiSelectorConfiguration.builder(ShapeType.FREEHAND_POLYGON).append(true).build());
    }

    /**
     * Renderer to set the tooltip to the cell value in case the value is clipped in the display.
     */
    private static final class StringCellRenderer extends DefaultTableCellRenderer
    {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            final JComponent c = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setToolTipText(value.toString());
            return c;
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var buttonPanel = new JPanel();
        exportButton = new SplitButton();
        var deleteButton = new JButton();
        var appendSelectionButton = new JButton();
        var selectedObjectsScrollPane = new JScrollPane();
        selectedObjectsTable = new JTable();
        var btnClear = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "default, fill:default:grow, default"));

        //======== buttonPanel ========
        {
            buttonPanel.setName("buttonPanel");
            buttonPanel.setLayout(new FormLayout(
                    "2*(pref:grow), default",
                    "fill:pref"));

            //---- exportButton ----
            exportButton.setText("Export");
            exportButton.setName("exportButton");
            buttonPanel.add(exportButton, cc.xy(1, 1));

            //---- deleteButton ----
            deleteButton.setText("Delete Selected");
            deleteButton.setName("deleteButton");
            deleteButton.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    deleteButtonClicked();
                }
            });
            buttonPanel.add(deleteButton, cc.xy(2, 1));

            //---- appendSelectionButton ----
            appendSelectionButton.setText("Append");
            appendSelectionButton.setToolTipText("Activate selector in append mode to add to the current selection");
            appendSelectionButton.setName("appendSelectionButton");
            appendSelectionButton.addActionListener(e -> appendSelectionButtonPressed());
            buttonPanel.add(appendSelectionButton, cc.xy(3, 1));
        }
        add(buttonPanel, cc.xy(1, 1));

        //======== selectedObjectsScrollPane ========
        {
            selectedObjectsScrollPane.setName("selectedObjectsScrollPane");

            //---- selectedObjectsTable ----
            selectedObjectsTable.setPreferredScrollableViewportSize(new Dimension(250, 100));
            selectedObjectsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            selectedObjectsTable.setName("selectedObjectsTable");
            selectedObjectsScrollPane.setViewportView(selectedObjectsTable);
        }
        add(selectedObjectsScrollPane, cc.xy(1, 2, CellConstraints.FILL, CellConstraints.FILL));

        //---- btnClear ----
        btnClear.setText("Clear");
        btnClear.setName("btnClear");
        btnClear.addActionListener(e -> clearButtonClicked());
        add(btnClear, cc.xy(1, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private SplitButton exportButton;
    private JTable selectedObjectsTable;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
