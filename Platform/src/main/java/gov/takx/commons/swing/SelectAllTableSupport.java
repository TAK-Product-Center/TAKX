package gov.takx.commons.swing;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Provides static methods for configuring a table with a select/deselect all column, rendered as a checkbox, that can 
 * be used to select/deselect all rows in the table.
 *
 * @since 3.3
 */
public class SelectAllTableSupport
{
    public static final Function<Boolean, String> DEFAULT_TOOLTIP_CREATOR = selected -> selected ? "Deselect all" : "Select all";

    /**
     * Configure the provided table with a select/deselect all column, including a customized header tooltip and adding 
     * an optional callback on change in state.
     *
     * @param table The table
     * @param tableModel The table model
     * @param selectAllColumnIndex The index of the column which should have the select all checkbox
     * @param allCheckedInitially True if all items in the column start as checked; false if the starting state is unchecked
     * @param mouseListenerCallback Optional callback to be notified when a change is made to the column header selection
     * @param tooltipCreator Function to create a tooltip based on the current state of the renderer checkbox
     */
    public static void configureSelectAllColumn(@Nonnull JTable table, @Nonnull AbstractTableModel tableModel,
                                                int selectAllColumnIndex, boolean allCheckedInitially,
                                                @Nullable Consumer<Boolean> mouseListenerCallback,
                                                @Nonnull Function<Boolean, String> tooltipCreator)
    {
        AtomicBoolean allChecked = new AtomicBoolean(allCheckedInitially);
        AtomicBoolean updatingModel = new AtomicBoolean(false);
        table.getColumnModel().getColumn(selectAllColumnIndex).setHeaderRenderer(new SelectAllTableCellRenderer(allChecked, tooltipCreator));
        final JTableHeader tableHeader = table.getTableHeader();
        tableHeader.addMouseListener(new HeaderMouseListener(allChecked, mouseListenerCallback, table, tableModel, selectAllColumnIndex, updatingModel));

        // Need to listen for changes to the table model that were not initiated by this class to keep the header in
        // sync with the table in case rows are added/removed or the selection state is changed programmatically.
        tableModel.addTableModelListener(e -> {
            if (updatingModel.get()) return;

            // Check if change to the table model is inconsistent with the allChecked state
            boolean allRowsChecked = true;
            int rowCount = tableModel.getRowCount();
            for (int rowIndex = 0; allRowsChecked && rowIndex < rowCount; rowIndex++)
            {
                allRowsChecked = (boolean) tableModel.getValueAt(rowIndex, selectAllColumnIndex);
            }

            if (allChecked.getAndSet(allRowsChecked) != allRowsChecked) tableHeader.repaint();
        });
    }

    /**
     * Configure the provided table with a select/deselect all column.
     *
     * @param table The table
     * @param tableModel The table model
     * @param selectAllColumnIndex The index of the column which should have the select all checkbox
     * @param allChecked True if all items in the column start as checked; false if the starting state is unchecked
     */
    public static void configureSelectAllColumn(@Nonnull JTable table, @Nonnull AbstractTableModel tableModel,
                                                int selectAllColumnIndex, boolean allChecked)
    {
        configureSelectAllColumn(table, tableModel, selectAllColumnIndex, allChecked, null, DEFAULT_TOOLTIP_CREATOR);
    }

    /**
     * Configure the provided table with a select/deselect all column, including a customized header tooltip and adding
     * an optional callback on change in state. It also sets a fixed width for the select all column.
     *
     * @param table The table
     * @param tableModel The table model
     * @param selectAllColumnIndex The index of the column which should have the select all checkbox
     * @param allChecked True if all items in the column start as checked; false if the starting state is unchecked
     * @param mouseListenerCallback Optional callback to be notified when a change is made to the column header selection
     * @param tooltipCreator Function to create a tooltip based on the current state of the renderer checkbox
     * @param columnWidth The width to set for the select all column, in pixels
     */
    public static void configureSelectAllColumn(@Nonnull JTable table, @Nonnull AbstractTableModel tableModel,
                                                int selectAllColumnIndex, boolean allChecked,
                                                @Nullable Consumer<Boolean> mouseListenerCallback,
                                                @Nonnull Function<Boolean, String> tooltipCreator, int columnWidth)
    {
        configureSelectAllColumn(table, tableModel, selectAllColumnIndex, allChecked, mouseListenerCallback, tooltipCreator);

        // Force the checkbox column to a fixed width
        TableColumn column = table.getColumnModel().getColumn(selectAllColumnIndex);
        column.setMinWidth(columnWidth);
        column.setMaxWidth(columnWidth);
        column.setPreferredWidth(columnWidth);
    }

    /**
     * Constructor that sets a fixed width for the select all column.
     *
     * @param table The table
     * @param tableModel The table model
     * @param selectAllColumnIndex The index of the column which should have the select all checkbox
     * @param allChecked True if all items in the column start as checked; false if the starting state is unchecked
     * @param columnWidth The width to set for the select all column, in pixels
     */
    public static void configureSelectAllColumn(@Nonnull JTable table, @Nonnull AbstractTableModel tableModel,
                                                int selectAllColumnIndex, boolean allChecked, int columnWidth)
    {
        configureSelectAllColumn(table, tableModel, selectAllColumnIndex, allChecked, null, DEFAULT_TOOLTIP_CREATOR, columnWidth);
    }

    /**
     * Renderer for the table header of a boolean column that is meant to have a select/deselect all action. Largely
     * copied from JTable.BooleanRenderer as a starting point (can't extend that class since it's private).
     */
    private static class SelectAllTableCellRenderer extends JCheckBox implements TableCellRenderer
    {
        private static final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);
        private final AtomicBoolean allChecked;
        private final Function<Boolean, String> tooltipCreator;

        /**
         * Constructor.
         *
         * @param allChecked Atomic boolean to manage the state of the select all header
         * @param tooltipCreator Function to create a tooltip based on the current state of the renderer checkbox
         */
        public SelectAllTableCellRenderer(@Nonnull AtomicBoolean allChecked, @Nonnull Function<Boolean, String> tooltipCreator)
        {
            this.allChecked = allChecked;
            this.tooltipCreator = tooltipCreator;
            setHorizontalAlignment(JLabel.CENTER);
            setBorderPainted(true);

        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            if (isSelected)
            {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else
            {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            }

            boolean checked = allChecked.get();
            setSelected(checked);
            setToolTipText(tooltipCreator.apply(checked));

            if (hasFocus)
            {
                setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
            } else
            {
                setBorder(noFocusBorder);
            }

            return this;
        }
    }

    /**
     * Mouse listener for handing the select/deselect all action in the table header.
     */
    private static class HeaderMouseListener extends MouseAdapter
    {
        private final AtomicBoolean allChecked;
        private final Consumer<Boolean> callback;
        private final JTable table;
        private final AbstractTableModel tableModel;
        private final int selectAllColumnIndex;
        private final AtomicBoolean updatingModel;

        /**
         * Constructor.
         *
         * @param allChecked Atomic boolean to manage the state of the select all header
         * @param callback Optional callback to be notified when a change is made to the column header selection
         * @param table The table
         * @param tableModel The table model
         * @param selectAllColumnIndex The index of the column which should have the select all checkbox
         * @param updatingModel Atomic boolean to indicate that this class is updating the model to avoid recursive updates
         * @since 3.4.1 (updatingModel param added)
         */
        public HeaderMouseListener(@Nonnull AtomicBoolean allChecked, @Nullable Consumer<Boolean> callback,
                                   @Nonnull JTable table, @Nonnull AbstractTableModel tableModel,
                                   int selectAllColumnIndex, AtomicBoolean updatingModel)
        {
            this.allChecked = allChecked;
            this.callback = callback;
            this.table = table;
            this.tableModel = tableModel;
            this.selectAllColumnIndex = selectAllColumnIndex;
            this.updatingModel = updatingModel;
        }

        @Override
        public void mousePressed(MouseEvent event)
        {
            // The index of the column whose header was clicked
            int clickedColumnIndex = table.getColumnModel().getColumnIndexAtX(event.getX());

            // Return if not clicked on the monitored column
            if (clickedColumnIndex != selectAllColumnIndex) return;

            // Toggle the boolean state. Since this method will only be called on the EDT, we don't need to synchronize
            boolean checked = allChecked.get();
            checked = !checked;
            allChecked.set(checked);

            setAllRowsSelected(checked);
            table.getTableHeader().repaint();

            if (callback != null) callback.accept(checked);
        }

        /**
         * Set all cells in a column as selected/deselected.
         *
         * @param selected True to select all rows; false to deselect
         */
        protected void setAllRowsSelected(boolean selected)
        {
            updatingModel.set(true);
            int rowCount = tableModel.getRowCount();
            for (int rowIndex = 0; rowIndex < rowCount; rowIndex++)
            {
                tableModel.setValueAt(selected, rowIndex, selectAllColumnIndex);
            }

            tableModel.fireTableDataChanged();
            updatingModel.set(false);
        }
    }
}
