package gov.takx.core.view.gui.gazetteer;

import gov.takx.api.gazetteer.GazetteerEntry;

import javax.swing.table.AbstractTableModel;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Table model for holding gazetteer search results.
 *
 * @since 3.0
 */
class SearchResultsTableModel extends AbstractTableModel
{
    enum Columns
    {
        Selected,
        Name;

        static final Columns[] VALUES = values();
    }

    private static final int SELECTED_COL_INDEX = Columns.Selected.ordinal();

    private final BitSet selected = new BitSet();
    private final List<GazetteerEntry> searchResults = new ArrayList<>();

    /**
     * Install a new set of results.
     *
     * @param searchResults New model contents
     */
    public void setSearchResults(Collection<GazetteerEntry> searchResults)
    {
        selected.clear();

        this.searchResults.clear();
        this.searchResults.addAll(searchResults);

        fireTableDataChanged();
    }

    /**
     * Get the entry at the given model row index.
     *
     * @param row Row to retrieve
     */
    public GazetteerEntry getSearchResultAtRow(int row)
    {
        return searchResults.get(row);
    }

    /**
     * @param row Model index of row to access
     * @return {@code true} If the given row is selected
     */
    public boolean isSelected(int row)
    {
        return selected.get(row);
    }

    /**
     * Set the selected value for all rows in the model.
     *
     * @param selected New selected state
     */
    public void setAllSelected(boolean selected)
    {
        for (int i = 0, rowCount = getRowCount(); i < rowCount; i++)
        {
            setValueAt(selected, i, SELECTED_COL_INDEX);
        }
    }

    /**
     * Clear the model.
     */
    public void clear()
    {
        setSearchResults(Collections.emptyList());
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        return columnIndex == SELECTED_COL_INDEX ? Boolean.class : String.class;
    }

    @Override
    public String getColumnName(int columnIndex)
    {
        return (Columns.VALUES[columnIndex]).name();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        if (rowIndex < getRowCount())
        {
            return switch (Columns.VALUES[columnIndex])
            {
                case Selected -> selected.get(rowIndex);
                case Name -> searchResults.get(rowIndex).getName();
            };
        }

        return null;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return columnIndex == SELECTED_COL_INDEX; // Only the "selected" column is editable
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
        if (columnIndex == SELECTED_COL_INDEX)
        {
            final var selected = (Boolean) aValue;

            // If the value didn't change, avoid firing cell updated
            if (!Objects.equals(getValueAt(rowIndex, columnIndex), selected))
            {
                this.selected.set(rowIndex, selected);
                fireTableCellUpdated(rowIndex, SELECTED_COL_INDEX);
            }
        }
    }

    @Override
    public int getColumnCount()
    {
        return Columns.VALUES.length;
    }

    @Override
    public int getRowCount()
    {
        return searchResults.size();
    }
}
