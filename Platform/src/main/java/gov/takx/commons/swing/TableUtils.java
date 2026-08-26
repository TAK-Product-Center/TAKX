package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.annotation.InvokeOnEDT;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;

import java.lang.invoke.MethodHandles;

/**
 * Utilities for Swing Tables.
 *
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class TableUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private TableUtils()
    {
    }

    /**
     * Set table column min and max widths to a specified value.
     *
     * @param width the width
     * @param columns the columns
     */
    public static void setColumnWidths(int width, @Nonnull TableColumn... columns)
    {
        for (var column : columns)
        {
            column.setMaxWidth(width);
            column.setMinWidth(width);
        }
    }

    /**
     * Update table column widths to match their contents.
     *
     * @param table the table
     * @param min {@code true} to set min, max, and preferred widths, {@code false} to set only max and preferred
     * @param columns the columns to update
     */
    @InvokeOnEDT
    public static void setColumnWidthsFromContents(@Nonnull JTable table, boolean min, @Nonnull TableColumn... columns)
    {
        for (var column : columns)
        {
            var width = 0;
            var col = table.convertColumnIndexToView(column.getModelIndex());
            if (col > -1)
            {
                for (var row = 0; row < table.getRowCount(); row++)
                {
                    try
                    {
                        var renderer = table.getCellRenderer(row, col);
                        var comp = table.prepareRenderer(renderer, row, col);
                        width = Math.max(width, comp.getPreferredSize().width);
                    } catch (IndexOutOfBoundsException e)
                    {
                        // The primary reason this exception happens is from calling this method off the EDT at the same time
                        //  that model updates are occurring. We are not going to exacerbate the problem by propagating the
                        //  exception, but the offending code still needs to be addressed because it can result in badly sized and
                        //  potentially unusable table rows.
                        if (!SwingUtilities.isEventDispatchThread())
                        {
                            logger.error("Caught IndexOutOfBoundsException, likely because you are adjusting the UI outside the EDT.", e);
                        }
                        break;
                    }
                }
                if (min) column.setMinWidth(width);
                column.setMaxWidth(width);
                column.setPreferredWidth(width);
            }
        }
    }

    /**
     * Update table column min, max, and preferred widths to match their contents.
     *
     * @param table the table
     * @param columns the columns to update
     */
    @SuppressWarnings("unused")
    public static void setColumnWidthsFromContents(@Nonnull JTable table, @Nonnull TableColumn... columns)
    {
        setColumnWidthsFromContents(table, true, columns);
    }

    /**
     * Set the given table's row height to the maximum row height it contains.
     *
     * @param table The table to update
     * @param padding Additional padding to add to the calculated row height
     * @param maxSamples The maximum number of rows to sample or {@code -1} to sample all rows
     */
    @InvokeOnEDT
    public static void setRowHeightFromContents(JTable table, int padding, int maxSamples)
    {
        var sampleCount = maxSamples != -1 ? Math.min(table.getRowCount(), maxSamples) : table.getRowCount();
        if (sampleCount == 0) return;
        var columnCount = table.getColumnCount();
        if (columnCount == 0) return;
        var rowHeight = 0;
        for (var row = 0; row < sampleCount; row++)
        {
            for (var column = 0; column < columnCount; column++)
            {
                var renderer = table.getCellRenderer(row, column);
                var cellHeight = table.prepareRenderer(renderer, row, column).getPreferredSize().height;
                rowHeight = Math.max(rowHeight, cellHeight);
            }
        }
        table.setRowHeight(rowHeight + padding);
    }
}
