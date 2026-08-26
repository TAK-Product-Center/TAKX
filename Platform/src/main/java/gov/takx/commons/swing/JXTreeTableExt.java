package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.table.TableColumnExt;

/**
 * Extension of {@link JXTreeTable} that adds convenience methods.
 *
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class JXTreeTableExt extends JXTreeTable
{
    /**
     * Moves the given column to the new index in the underlying column model. See
     * {@link javax.swing.table.TableColumnModel#moveColumn(int, int)}.
     *
     * @param column the table column
     * @param newIndex the new index
     */
    @SuppressWarnings("unused")
    public void moveColumn(@Nonnull TableColumnExt column, int newIndex)
    {
        getColumnModel().moveColumn(getColumnModel().getColumnIndex(column.getIdentifier()), newIndex);
    }

    /**
     * Toggles the expand/collapse state of a row.
     *
     * @param row the row index
     */
    public void toggleExpandRow(int row)
    {
        if (isExpanded(row))
        {
            collapseRow(row);
        } else
        {
            expandRow(row);
        }
    }
}
