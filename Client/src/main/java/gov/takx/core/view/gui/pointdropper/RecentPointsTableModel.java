package gov.takx.core.view.gui.pointdropper;

import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.annotation.Nonnull;

import javax.swing.ImageIcon;
import javax.swing.table.AbstractTableModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Table model holding Map Objects recently created by the point dropper.
 *
 * @since 3.1
 */
public class RecentPointsTableModel extends AbstractTableModel
{
    private static final int ICON_SIZE = 16;

    private final IPreferenceMonitor<CoordinateSystem> coordinateSystemMonitor =
            UserPreferenceEntry.COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, cs -> fireTableDataChanged());

    private final List<IMapObject> mapObjects = new ArrayList<>();

    /**
     * Add the given Map Object to the model.  It is added to the beginning of the list for parity with WinTAK.
     */
    void addRow(IMapObject mapObject)
    {
        mapObjects.add(0, mapObject);
        fireTableRowsInserted(0, 0);
    }

    /**
     * Remove the given Map Object from the model.
     */
    void removeRow(IMapObject mapObject)
    {
        if (mapObjects.remove(mapObject)) fireTableDataChanged();
    }

    /**
     * @return The Map Object at the given row
     * @since 5.2
     */
    @Nonnull
    IMapObject getRowObject(int row)
    {
        return mapObjects.get(row);
    }

    /**
     * Dispose resources.
     */
    void dispose()
    {
        coordinateSystemMonitor.dispose();
    }

    @Override
    public int getRowCount()
    {
        return mapObjects.size();
    }

    @Override
    public int getColumnCount()
    {
        return Column.values.length;
    }

    @Override
    public String getColumnName(int column)
    {
        return Column.values[column].toString();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        return switch (Column.values[columnIndex])
        {
            case Icon -> ImageIcon.class;
            case Name, Location -> String.class;
        };
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        if (rowIndex < 0 || rowIndex >= mapObjects.size()) return null;

        final IMapObject mapObject = mapObjects.get(rowIndex);

        return switch (Column.values[columnIndex])
        {
            case Icon -> mapObject.getSizedIcon(ICON_SIZE);
            case Name -> mapObject.getName();
            case Location -> CoordinateSystem.getLocationString(mapObject.getLocation(), coordinateSystemMonitor.getCurrentValue(), false);
        };
    }

    enum Column
    {
        Icon,
        Name,
        Location;

        static final Column[] values = values();
    }
}
