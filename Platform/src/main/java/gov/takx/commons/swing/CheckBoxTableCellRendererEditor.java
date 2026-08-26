package gov.takx.commons.swing;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.Sizes;
import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;

import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import java.awt.Component;
import java.awt.event.ActionEvent;

/**
 * A combined {@link TableCellRenderer} and {@link TableCellEditor} that renders a clickable checkbox in a table column.
 * Uses a {@link TriStateJCheckBox}.
 *
 * @since 5.3
 */
@IncubatingApi(since = "5.3")
public class CheckBoxTableCellRendererEditor
{
    private final JTable table;
    private final CheckBoxTableCellRenderer renderer = new CheckBoxTableCellRenderer();
    private final CheckBoxTableCellEditor editor;

    private CheckBoxTableCellRendererEditor(@Nonnull JTable table, @Nonnull Action action)
    {
        this.table = table;
        editor = new CheckBoxTableCellEditor(action);
    }

    /**
     * Creates a new instance and sets it as the cell renderer and editor for the given column. Also adds required listeners
     * to the table.
     * 
     * @param table the table
     * @param column the column
     * @param action the action to be performed when the checkbox is clicked
     * @return the newly created instance
     */
    @Nonnull
    public static CheckBoxTableCellRendererEditor setRendererAndEditor(@Nonnull JTable table, @Nonnull TableColumn column, @Nonnull Action action)
    {
        var rendererEditor = new CheckBoxTableCellRendererEditor(table, action);
        column.setCellRenderer(rendererEditor.renderer);
        column.setCellEditor(rendererEditor.editor);
        TableEditCellRolloverListener.addForColumns(table, column);
        return rendererEditor;
    }

    /**
     * Creates a new instance and sets it as the cell renderer and editor for the given column. Also adds required listeners
     * to the table.
     *
     * @param table the table
     * @param column the column
     * @param actionPerformedListener the listener to be called when the checkbox is clicked
     * @return the newly created instance
     * @since 5.7
     */
    @SuppressWarnings("UnusedReturnValue")
    @Nonnull
    public static CheckBoxTableCellRendererEditor setRendererAndEditor(@Nonnull JTable table, @Nonnull TableColumn column, @Nonnull SimpleAction.ActionPerformedListener actionPerformedListener)
    {
        return setRendererAndEditor(table, column, new SimpleAction(null, actionPerformedListener));
    }

    private static class CheckBoxTableCellRenderer extends DefaultJPanelTableCellRenderer
    {
        private final TriStateJCheckBox checkBox = new TriStateJCheckBox();

        public CheckBoxTableCellRenderer()
        {
            var layout = new FormLayout();
            layout.appendRow(new RowSpec(RowSpec.DEFAULT, Sizes.DEFAULT, RowSpec.DEFAULT_GROW));
            layout.appendColumn(new ColumnSpec(ColumnSpec.DEFAULT, Sizes.DEFAULT, ColumnSpec.DEFAULT_GROW));
            setLayout(layout);
            checkBox.setHorizontalAlignment(SwingConstants.CENTER);
            var cellConstraints = new CellConstraints();
            add(checkBox, cellConstraints.xy(1, 1));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            checkBox.setState((TriStateJCheckBox.State) value);
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    private class CheckBoxTableCellEditor extends AbstractCellEditor implements TableCellEditor
    {
        private final CheckBoxTableCellRenderer renderer = new CheckBoxTableCellRenderer();
        private final Action action;
        private TriStateJCheckBox.State value;

        public CheckBoxTableCellEditor(@Nonnull Action action)
        {
            this.action = action;
            renderer.checkBox.addActionListener(this::actionPerformed);
        }

        private void actionPerformed(ActionEvent e)
        {
            var row = table.convertRowIndexToModel(table.getEditingRow());
            var event = new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, "" + row);
            action.actionPerformed(event);
        }

        @Override
        public Object getCellEditorValue()
        {
            return value;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
        {
            this.value = (TriStateJCheckBox.State) value;
            return renderer.getTableCellRendererComponent(table, value, isSelected, false, row, column);
        }
    }
}