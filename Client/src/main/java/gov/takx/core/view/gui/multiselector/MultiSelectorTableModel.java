package gov.takx.core.view.gui.multiselector;

import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.annotation.Nonnull;

import javax.swing.ImageIcon;
import javax.swing.table.AbstractTableModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Table model to hold the list of selected Map Object during a multi-select operation.
 *
 * @since 3.5
 */
class MultiSelectorTableModel extends AbstractTableModel implements IResourceCleaner
{
    private static final int ICON_SIZE = 16;

    private final IPreferenceMonitor<CoordinateSystem> coordinateSystemMonitor =
            UserPreferenceEntry.COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, cs -> fireTableDataChanged());

    private final List<IMapObject> mapObjects = new ArrayList<>();
    private final List<Boolean> mapObjectsSelected = new ArrayList<>();

    void addRow(@Nonnull IMapObject mapObject)
    {
        mapObjects.add(mapObject);
        mapObjectsSelected.add(true);
        fireTableDataChanged();
    }

    /**
     * Remove the given Map Object from the model.
     */
    void removeRow(@Nonnull IMapObject mapObject)
    {
        try
        {
            mapObjectsSelected.remove(mapObjects.indexOf(mapObject));
            mapObjects.remove(mapObject);

        } catch (IndexOutOfBoundsException ignored)
        {
            return;
        }
        fireTableDataChanged();
    }

    @Override
    public void releaseResources()
    {
        coordinateSystemMonitor.dispose();
    }

    void clearTable()
    {
        mapObjects.clear();
        mapObjectsSelected.clear();
        fireTableDataChanged();
    }

    @Nonnull
    List<IMapObject> getSelectedObjects()
    {
        List<IMapObject> returnObjects = new ArrayList<>();
        for (int i = 0; i < mapObjectsSelected.size(); i++)
        {
            if (mapObjectsSelected.get(i)) returnObjects.add(mapObjects.get(i));
        }
        return returnObjects;

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
            case Selected -> Boolean.class;
            case Icon -> ImageIcon.class;
            case Name, Location -> String.class;
        };
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        if (rowIndex < 0 || rowIndex >= mapObjects.size())
        {
            return null;
        }

        final IMapObject mapObject = mapObjects.get(rowIndex);

        return switch (Column.values[columnIndex])
        {
            case Selected -> mapObjectsSelected.get(rowIndex);
            case Icon -> mapObject.getSizedIcon(ICON_SIZE);
            case Name -> mapObject.getName();
            case Location ->
            {
                final CoordinateSystem coordinateSystem = coordinateSystemMonitor.getCurrentValue();
                yield CoordinateSystem.getLocationString(mapObject.getLocation(), coordinateSystem, false);
            }
        };
    }

    @Override
    public void setValueAt(Object value, int row, int col)
    {
        mapObjectsSelected.set(row, (Boolean) value);
        fireTableCellUpdated(row, col);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return Column.values[columnIndex] == Column.Selected;
    }

    enum Column
    {
        Selected,
        Icon,
        Name,
        Location;

        static final Column[] values = values();
    }

}
