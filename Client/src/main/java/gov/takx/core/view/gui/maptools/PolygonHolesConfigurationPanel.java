package gov.takx.core.view.gui.maptools;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.mapobject.IShapePolygonMapObject;
import gov.takx.api.plugin.device.APluginConfigurationPanel;
import gov.takx.api.shapes.IPolygon;
import gov.takx.commons.lang.AlphaNumericComparator;
import gov.takx.core.mapobject.PolygonPluginController;
import gov.takx.platform.ui.UIResources;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import java.awt.Dimension;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * This panel is used to manage holes for the polygon that this panel belongs too.
 *
 * @since 1.48 (renamed from PolygonHolesPreferencesPanel in 5.0)
 */
public class PolygonHolesConfigurationPanel extends APluginConfigurationPanel
{
    public static final int IS_HOLE_COLUMN = 0;
    public static final int POLYGON_COLUMN = 1;

    private static final AlphaNumericComparator<IShapePolygonMapObject> POLYGON_COMPARATOR = new AlphaNumericComparator<>(IShapePolygonMapObject::getName);

    private final PolygonPluginController polygonPluginController;
    private final DefaultTableModel polygonsTableModel;
    private final IShapePolygonMapObject polygonMapObject;

    public PolygonHolesConfigurationPanel(final PolygonPluginController polygonPluginController, IShapePolygonMapObject polygonMapObject)
    {
        super("Polygon Holes");
        initComponents();

        this.polygonPluginController = polygonPluginController;
        this.polygonMapObject = polygonMapObject;
        polygonsTableModel = (DefaultTableModel) holesTable.getModel();

        polygonsTableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) checkModelDirty();
        });

        TableRowSorter<TableModel> sorter = new TableRowSorter<>(polygonsTableModel);

        sorter.setComparator(1, POLYGON_COMPARATOR);
        sorter.setSortKeys(List.of(new RowSorter.SortKey(POLYGON_COLUMN, SortOrder.ASCENDING)));
        holesTable.setRowSorter(sorter);
    }

    private void checkModelDirty()
    {
        final Set<IPolygon> currentHoles = polygonMapObject.getHoles();

        //noinspection UseOfObsoleteCollectionType
        for (Vector<?> row : polygonsTableModel.getDataVector())
        {
            final boolean modelHole = (boolean) row.get(IS_HOLE_COLUMN);
            final IShapePolygonMapObject currentPolygon = (IShapePolygonMapObject) row.get(POLYGON_COLUMN);
            final boolean currentHole = currentHoles.contains(currentPolygon.getAssociatedShape());
            if (modelHole != currentHole)
            {
                setPanelDirty();
                break;
            }
        }
    }

    @Override
    public void applyAction()
    {
        polygonsTableModel.getDataVector().forEach(row -> {
            boolean hole = (boolean) row.get(IS_HOLE_COLUMN);
            IShapePolygonMapObject polygonMO = (IShapePolygonMapObject) row.get(POLYGON_COLUMN);
            polygonPluginController.updateHoleFromView(polygonMO, hole);
        });
    }

    public void addOrUpdateContainedPolygon(boolean isHole, IShapePolygonMapObject containedPolygon)
    {
        // Make sure the table doesn't already have the polygon to prevent duplicates
        final int polygonRow = getPolygonRowFromTable(containedPolygon);
        if (polygonRow >= 0)
        {
            polygonsTableModel.setValueAt(isHole, polygonRow, IS_HOLE_COLUMN);
        } else
        {
            polygonsTableModel.addRow(new Object[]{isHole, containedPolygon});
        }
    }

    private int getPolygonRowFromTable(IShapePolygonMapObject polygon)
    {
        for (int i = 0; i < polygonsTableModel.getRowCount(); i++)
        {
            if (polygonsTableModel.getValueAt(i, POLYGON_COLUMN).equals(polygon))
            {
                return i;
            }
        }
        return -1;
    }

    public void removeEligiblePolygon(IShapePolygonMapObject polygonToTest)
    {
        polygonsTableModel.getDataVector().removeIf(row -> row.get(POLYGON_COLUMN).equals(polygonToTest));
        polygonsTableModel.fireTableDataChanged();
    }

    @Override
    public URL getIcon()
    {
        return UIResources.getUrl(PLATFORM_ICONS, "polygon_hole.png");
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var scrollPane1 = new JScrollPane();
        holesTable = new JTable();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(new TitledBorder("Polygon Holes"));
        setName("panelPolygonHolesPreferences");
        setLayout(new FormLayout(
                "default:grow",
                "fill:default"));

        //======== scrollPane1 ========
        {
            scrollPane1.setName("scrollPane1");

            //---- holesTable ----
            holesTable.setModel(new DefaultTableModel(
                    new Object[][]{
                    },
                    new String[]{
                            "Hole", "Eligible Polygons"
                    })
            {
                Class<?>[] columnTypes = new Class<?>[]{
                        Boolean.class, Object.class
                };
                boolean[] columnEditable = new boolean[]{
                        true, false
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
                TableColumnModel cm = holesTable.getColumnModel();
                cm.getColumn(0).setMaxWidth(100);
                cm.getColumn(0).setPreferredWidth(35);
            }
            holesTable.setAutoCreateRowSorter(true);
            holesTable.setPreferredScrollableViewportSize(new Dimension(100, 200));
            holesTable.setName("holesTable");
            scrollPane1.setViewportView(holesTable);
        }
        add(scrollPane1, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTable holesTable;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
