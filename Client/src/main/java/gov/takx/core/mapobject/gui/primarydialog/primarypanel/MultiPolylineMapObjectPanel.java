package gov.takx.core.mapobject.gui.primarydialog.primarypanel;

import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IShapePathMapObject;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.dataservices.persist.MultiPolylineMapObject;
import gov.takx.platform.preferences.UserPreferenceEntry;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

/**
 * A panel that displays the shapes and locations from MultiPolyline map objects.
 *
 * @since 3.0.1
 */
public class MultiPolylineMapObjectPanel extends AMapObjectPanel
{
    private final MultiPolylineMapObject multiPolylineMapObject;
    private final DefaultTableCellRenderer centerAlignedCellRenderer;
    private final String[] columnHeaders = {"Name", "Location"};
    private final IPreferenceMonitor<CoordinateSystem> coordinateSystemMonitor =
            UserPreferenceEntry.COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, this::coordinateSystemChanged);

    public MultiPolylineMapObjectPanel(IMapObjectController mapObjectController)
    {
        super(mapObjectController);

        initComponents();

        centerAlignedCellRenderer = new DefaultTableCellRenderer();
        centerAlignedCellRenderer.setHorizontalAlignment(JLabel.CENTER);

        multiPolylineMapObject = (MultiPolylineMapObject) mapObjectController.getMapObject();

        updateTableData();
    }

    @Override
    public void releaseResources()
    {
        coordinateSystemMonitor.dispose();
        super.releaseResources();
    }

    private void coordinateSystemChanged(CoordinateSystem coordinateSystem)
    {
        updateTableData();
    }

    @InvokeOnEDT
    private void updateTableData()
    {
        List<IShapePathMapObject<?>> shapePathMapObjects = multiPolylineMapObject.getPolylines();
        Object[][] tableData = new Object[shapePathMapObjects.size()][columnHeaders.length];

        for (int row = 0; row < shapePathMapObjects.size(); row++)
        {
            IShapePathMapObject<?> shapePathMapObject = shapePathMapObjects.get(row);
            GeoPoint geoPoint = shapePathMapObject.getLocation();

            tableData[row][0] = shapePathMapObject.getName();
            tableData[row][1] = CoordinateSystem.getLocationString(geoPoint, coordinateSystemMonitor.getCurrentValue());
        }

        ((DefaultTableModel) multiPolylineTable.getModel()).setDataVector(tableData, columnHeaders);

        multiPolylineTable.getColumnModel().getColumn(0).setCellRenderer(centerAlignedCellRenderer);
        multiPolylineTable.getColumnModel().getColumn(1).setCellRenderer(centerAlignedCellRenderer);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var scrollPane1 = new JScrollPane();
        multiPolylineTable = new JTable();

        //======== this ========
        setName("this");
        setLayout(new GridBagLayout());
        ((GridBagLayout) getLayout()).columnWidths = new int[]{0, 0};
        ((GridBagLayout) getLayout()).rowHeights = new int[]{0, 0};
        ((GridBagLayout) getLayout()).columnWeights = new double[]{0.0, 1.0E-4};
        ((GridBagLayout) getLayout()).rowWeights = new double[]{0.0, 1.0E-4};

        //======== scrollPane1 ========
        {
            scrollPane1.setName("scrollPane1");

            //---- multiPolylineTable ----
            multiPolylineTable.setModel(new DefaultTableModel());
            multiPolylineTable.setPreferredScrollableViewportSize(new Dimension(200, 100));
            multiPolylineTable.setName("multiPolylineTable");
            scrollPane1.setViewportView(multiPolylineTable);
        }
        add(scrollPane1, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTable multiPolylineTable;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
