package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.swingx.treetable.TreeTableCellEditor;

import javax.swing.table.TableCellEditor;
import javax.swing.tree.TreePath;

import java.awt.Point;

/**
 * Extension of {@link JXTreeTable} that adds convenience methods and extra features.
 * <ol>
 *     <li>Setting the editor for the hierarchical column</li>
 * </ol>
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class JXTreeTableExt extends JXTreeTable
{
    private TreeTableCellEditor hierarchicalEditor;

    /**
     * @since 5.8
     */
    @Override
    public TableCellEditor getCellEditor(int row, int column)
    {
        return isHierarchical(column) && hierarchicalEditor != null ? hierarchicalEditor : super.getCellEditor(row, column);
    }

    /**
     * @since 5.8
     */
    @Override
    public boolean isCellEditable(int row, int column)
    {
        // The hierarchical column must be editable for the editor to work
        return isHierarchical(column) && hierarchicalEditor != null || super.isCellEditable(row, column);
    }

    /**
     * @return the editor for the hierarchical column
     * @since 5.8
     */
    @Nullable
    public TreeTableCellEditor getHierarchicalEditor()
    {
        return hierarchicalEditor;
    }

    /**
     * @param hierarchicalEditor the editor for the hierarchical column
     * @since 5.8
     */
    public void setHierarchicalEditor(@Nullable TreeTableCellEditor hierarchicalEditor)
    {
        this.hierarchicalEditor = hierarchicalEditor;
    }

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

    /**
     * @param point an event point
     * @return the TreePath at the given point
     * @see JXTreeTable#getPathForLocation(int, int)
     * @since 5.8
     */
    @Nullable
    public TreePath getPathForLocation(@Nonnull Point point)
    {
        return getPathForLocation(point.x, point.y);
    }
}
