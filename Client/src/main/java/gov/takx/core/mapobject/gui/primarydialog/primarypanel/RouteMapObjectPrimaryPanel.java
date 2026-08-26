package gov.takx.core.mapobject.gui.primarydialog.primarypanel;

import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.shapes.IRoute;
import gov.takx.dataservices.persist.RouteMapObject;
import gov.takx.platform.preferences.UserPreferenceEntry;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * A panel that displays the waypoints along route map objects.
 *
 * @since 3.0.1
 */
public class RouteMapObjectPrimaryPanel extends AMapObjectPanel
{
    private final RouteMapObject routeMapObject;
    private final DefaultTableCellRenderer centerAlignmentCellRenderer;
    private final String[] columnHeaders = {"Waypoint Name", "Location"};
    private final IPreferenceMonitor<CoordinateSystem> coordinateSystemMonitor =
            UserPreferenceEntry.COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, this::coordinateSystemChanged);

    public RouteMapObjectPrimaryPanel(IMapObjectController mapObjectController)
    {
        super(mapObjectController);

        initComponents();

        centerAlignmentCellRenderer = new DefaultTableCellRenderer();
        centerAlignmentCellRenderer.setHorizontalAlignment(JLabel.CENTER);

        routeMapObject = (RouteMapObject) mapObjectController.getMapObject();

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
        final var waypoints = routeMapObject.getWaypoints();
        final var tableData = new Object[waypoints.size()][columnHeaders.length];

        for (int row = 0; row < waypoints.size(); row++)
        {
            final var waypoint = waypoints.get(row);
            final var metaData = waypoint.getMetaData();
            final var geoPoint = waypoint.get();
            final String name = metaData.getStringAttribute(IRoute.DISPLAY_NAME, "");

            tableData[row][0] = !name.isBlank() ? name : metaData.getStringAttribute(StandardAttributeNames.UID);
            tableData[row][1] = CoordinateSystem.getLocationString(geoPoint, coordinateSystemMonitor.getCurrentValue());
        }

        routeWaypointsTable.setModel(new NonEditableTableModel(tableData, columnHeaders));
        routeWaypointsTable.getColumnModel().getColumn(0).setCellRenderer(centerAlignmentCellRenderer);
        routeWaypointsTable.getColumnModel().getColumn(1).setCellRenderer(centerAlignmentCellRenderer);
    }

    /**
     * @since 5.1
     */
    private static class NonEditableTableModel extends DefaultTableModel
    {
        NonEditableTableModel(Object[][] data, Object[] columnNames)
        {
            super(data, columnNames);
        }

        @Override
        public boolean isCellEditable(int row, int column)
        {
            return false;
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var scrollPane1 = new JScrollPane();
        routeWaypointsTable = new JTable();

        //======== this ========
        setName("this");
        setLayout(new BorderLayout());

        //======== scrollPane1 ========
        {
            scrollPane1.setName("scrollPane1");

            //---- routeWaypointsTable ----
            routeWaypointsTable.setModel(new DefaultTableModel());
            routeWaypointsTable.setPreferredScrollableViewportSize(new Dimension(200, 100));
            routeWaypointsTable.setName("routeWaypointsTable");
            scrollPane1.setViewportView(routeWaypointsTable);
        }
        add(scrollPane1, BorderLayout.CENTER);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTable routeWaypointsTable;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
