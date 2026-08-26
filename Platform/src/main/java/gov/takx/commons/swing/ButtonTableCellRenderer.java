package gov.takx.commons.swing;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import java.awt.Component;

/**
 * A table cell renderer that uses a {@link JButton} as the renderer component.  It is meant to be used in conjunction
 * with {@link ButtonTableCellEditor} to support rendering and editing (action invocation) a given table column.
 * <p>
 * By default, the button is not updated with any information from the cell being rendered, so this is intended for
 * embedding a "standard" action button, like edit, or delete, within a table.  Subclasses can override
 * {@link #updateButtonForCell} if they need to customize the button per-cell.
 * <p>
 * Note: You must use a different button instance with this renderer than used in the paired editor, or rendering
 * errors will occur.
 * TODO DCW TAKX-3396 usages of this class should eventually be replaced with {@link gov.takx.commons.swing.actionlist.ActionListTableCellRendererEditor}
 *
 * @param <T> value type
 * @see ButtonTableCellEditor
 * @since 3.2
 */
public class ButtonTableCellRenderer<T> extends DefaultTableCellRenderer
{
    protected final JButton button;

    public ButtonTableCellRenderer(JButton button)
    {
        this.button = button;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column)
    {
        //noinspection unchecked
        updateButtonForCell(button, table, (T) value, isSelected, row, column);
        return button;
    }

    /**
     * Update the button with any cell-specific data. This is called from {@link #getTableCellRendererComponent} before
     * returning the component.
     * <p>
     * Default implementation does nothing.
     */
    @SuppressWarnings("unused")
    protected void updateButtonForCell(JButton button, JTable table, T value, boolean isSelected, int row, int column)
    {
        // Noop
    }
}
