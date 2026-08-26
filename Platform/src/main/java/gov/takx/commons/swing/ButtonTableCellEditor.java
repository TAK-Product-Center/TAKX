package gov.takx.commons.swing;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import java.awt.Component;
import java.util.EventObject;
import java.util.function.Consumer;

/**
 * A table cell editor that uses a {@link JButton} as the editor component.  When the button is clicked, a
 * configured handler is invoked with the value of the cell to process the event. This editor is meant to be used in
 * conjunction with {@link ButtonTableCellRenderer} to support rendering and editing a given table column.
 * <p>
 * By default, the button is not updated with any information from the cell being rendered, so this is intended for
 * embedding a "standard" action button, like edit, or delete, within a table.  Subclasses can override
 * {@link #updateButtonForCell} if they need to customize the button per-cell.
 * <p>
 * Note: You must use a different button instance with this editor than used in the paired renderer, or rendering
 * errors will occur.
 * TODO DCW TAKX-3396 usages of this class should eventually be replaced with {@link gov.takx.commons.swing.actionlist.ActionListTableCellRendererEditor}
 *
 * @param <T> value type
 * @see ButtonTableCellRenderer
 * @since 3.2
 */
public class ButtonTableCellEditor<T> extends AbstractCellEditor implements TableCellEditor
{
    protected final JButton button;
    private final boolean shouldSelectCell;
    private T value;

    /**
     * Create a cell editor using the given button.  This constructor form does not select the cell when the
     * button is pressed.
     *
     * @param button Pre-configured button to use as the editor component of the cell
     * @param handler Handler invoked with cell value when the button is clicked
     */
    public ButtonTableCellEditor(JButton button, Consumer<T> handler)
    {
        this(button, handler, false);
    }

    /**
     * Create a cell editor using the given button.
     *
     * @param button Pre-configured button to use as the editor component of the cell
     * @param handler Handler invoked with cell value when the button is clicked
     * @param shouldSelectCell Pass {@code true} if the cell should be selected when the button is pressed
     */
    public ButtonTableCellEditor(JButton button, Consumer<T> handler, boolean shouldSelectCell)
    {
        this.button = button;
        this.shouldSelectCell = shouldSelectCell;

        button.addActionListener(e -> {
            handler.accept(value);
            stopCellEditing();
        });
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
    {
        //noinspection unchecked
        this.value = (T) value;

        updateButtonForCell(button, table, this.value, isSelected, row, column);
        return button;
    }

    /**
     * Update the button with any cell-specific data. This is called from {@link #getTableCellEditorComponent} before
     * returning the component.
     * <p>
     * Default implementation does nothing.
     */
    @SuppressWarnings("unused")
    protected void updateButtonForCell(JButton button, JTable table, T value, boolean isSelected, int row, int column)
    {
        // Noop
    }

    @Override
    public T getCellEditorValue()
    {
        return value;
    }

    @Override
    public boolean shouldSelectCell(EventObject ignored)
    {
        return shouldSelectCell;
    }
}
