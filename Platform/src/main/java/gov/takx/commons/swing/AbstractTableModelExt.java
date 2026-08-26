package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;

import javax.swing.table.AbstractTableModel;

/**
 * Extension of {@link AbstractTableModel} to add some convenient functions for firing update events.
 *
 * @since 5.6
 */
@IncubatingApi(since = "5.6")
public abstract class AbstractTableModelExt extends AbstractTableModel
{
    /**
     * Fire table cell updated event for all cells in the given column.
     *
     * @param column the column index
     */
    public void fireTableColumnUpdated(int column)
    {
        fireTableColumnsUpdated(column, column);
    }

    /**
     * Fire table cell updated event for all cells in the given columns.
     *
     * @param firstColumn the first column, inclusive
     * @param lastColumn the last column, inclusive
     */
    public void fireTableColumnsUpdated(int firstColumn, int lastColumn)
    {
        fireTableCellsUpdated(0, getRowCount() - 1, firstColumn, lastColumn);
    }

    /**
     * Fire table cell updated event for all cells in the given rows and columns.
     *
     * @param rows the row indexes
     * @param columns the column indexes
     */
    public void fireTableCellsUpdated(int[] rows, int[] columns)
    {
        for (int row : rows)
        {
            for (int column : columns)
            {
                fireTableCellUpdated(row, column);
            }
        }
    }

    /**
     * Fire table cell updated event for all cells in the given row and column and range.
     *
     * @param firstRow the first row, inclusive
     * @param lastRow the last row, inclusive
     * @param firstColumn the first column, inclusive
     * @param lastColumn the last column, inclusive
     */
    public void fireTableCellsUpdated(int firstRow, int lastRow, int firstColumn, int lastColumn)
    {
        for (int i = firstRow; i <= lastRow; i++)
        {
            for (int j = firstColumn; j <= lastColumn; j++)
            {
                fireTableCellUpdated(i, j);
            }
        }
    }
}
