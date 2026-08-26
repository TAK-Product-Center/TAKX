package gov.takx.core.view.gui.maps.exporters.dataset;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import gov.takx.api.annotation.InvokeOnEDT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;

/**
 * Model used to back a list of layers (imagery/elevation).
 *
 * @since 1.43 (renamed in 5.7)
 */
public class LayerTableModel extends AbstractTableModel implements PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final List<MapDataWrapper> rows = Lists.newArrayList();

    @Override
    public int getRowCount()
    {
        return rows.size();
    }

    @Override
    public int getColumnCount()
    {
        return 1;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        return rows.get(rowIndex);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return (columnIndex == 0);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
        // Deliberately not setting the value in the list, as the editor panel
        // is capable of mutating the state of the MapDataWrapper.
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    @InvokeOnEDT
    public void addRow(MapDataWrapper row)
    {
        synchronized (rows)
        {
            int i = findPlaceForRow(row);
            if (i >= 0)
            {
                rows.add(i, row);
                row.addPropertyChangeListener(this);
                fireTableRowsInserted(i, i);
            }
        }
    }

    @InvokeOnEDT
    public void clear()
    {
        synchronized (rows)
        {
            final int size = rows.size();
            for (MapDataWrapper row : rows)
            {
                row.removePropertyChangeListener(this);
            }

            rows.clear();
            fireTableRowsDeleted(0, size - 1);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        updateTable((MapDataWrapper) evt.getSource());
    }

    @InvokeOnEDT
    private void updateTable(MapDataWrapper wrapper)
    {
        int i = rows.indexOf(wrapper);
        if (i >= 0)
        {
            fireTableCellUpdated(i, 0);
        } else
        {
            wrapper.removePropertyChangeListener(this);
        }
    }

    // Find the location at which a row should be inserted to maintain the sort order.
    // Assumes caller has already synchronized access to the collection of rows.
    private int findPlaceForRow(MapDataWrapper row)
    {
        int lo = 0;
        int hi = rows.size();

        // invariants:  (a) if 0  <= i < lo    then rows.get(i) <= row
        //              (b) if hi <= j < nRows then row < rows.get(j)
        // Both hold initially because initially the respective ranges are empty.
        // Terminates with lo == hi because at every step, either lo increases
        // or hi decreases, and neither ever moves past the other.
        while (lo < hi)
        {
            // lo <= mid < hi, because integer division truncates and the sum is non-negative.
            // (there is a potential integer overflow, but we should never have enough layers
            //  to need to worry about that).
            int mid = (lo + hi) / 2;
            MapDataWrapper midRow = rows.get(mid);

            int cmp = compareRows(midRow, row);
            if (cmp < 0)
            {
                // preserves (a) because rows.get(mid) < row and the list is sorted.
                // makes progress because lo <= mid < mid + 1.
                lo = mid + 1;
            } else if (cmp == 0)
            {
                // item is already present; return an invalid index to indicate that it
                // should not be added at all.
                return -1;
            } else
            {
                // preserves (b) because row < rows.get(mid) and the list is sorted.
                // makes progress because mid < hi.
                hi = mid;
            }
        }

        if (lo != hi) logger.error("Failed to properly find insertion point: {} != {}", lo, hi);

        return lo;
    }

    private static int compareRows(MapDataWrapper left, MapDataWrapper right)
    {
        return left.getCacheName().compareTo(right.getCacheName());
    }

    public Collection<MapDataWrapper> getSelectedRows()
    {
        synchronized (rows)
        {
            return rows.stream().filter(MapDataWrapper::isSelected).toList();
        }
    }

    public Collection<MapDataWrapper> getRows()
    {
        synchronized (rows)
        {
            return ImmutableList.copyOf(rows);
        }
    }

    public MapDataWrapper getRow(int row)
    {
        return rows.get(row);
    }
}
