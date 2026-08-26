package gov.takx.core.view.gui.layers.viewmanager;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import jakarta.annotation.Nullable;
import org.jdesktop.swingx.JXTable;

import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import java.awt.Font;
import java.awt.Point;
import java.util.regex.Pattern;

/**
 * An abstract panel definition for all elements of a given type in the Maps Panel;
 * provides display, sorting, filter and interaction functionality
 *
 * @param <T> Our table type
 * @since 5.0
 */
public abstract class AMapGroupPanel<T extends JXTable> extends JPanel
{
    protected final String group;
    protected final LayerViewPanel owner;
    protected final LayerViewManager manager;
    protected final T groupTable;
    protected final AbstractTableModel tableModel;
    protected final TablePatternFilter patternFilter;

    protected boolean filterEnabled = false;

    /**
     * Creates a new {@link AMapGroupPanel} to display elements in the Maps Panel
     * @param group The name of the group to create
     * @param owner The panel in which to place this group
     * @param manager The layer manager to get and manage layer states
     * @param groupTable The table for backing this panel (data, rendering, etc.)
     * @param tableModel The model for interaction-backing the table
     */
    protected AMapGroupPanel(String group, LayerViewPanel owner, LayerViewManager manager, T groupTable, AbstractTableModel tableModel)
    {
        this.group = group;
        this.owner = owner;
        this.manager = manager;
        this.groupTable = groupTable;
        this.tableModel = tableModel;
        this.groupTable.setModel(tableModel);

        configureTable();

        setName("panelGroup");
        setLayout(new FormLayout("default:grow", "top:default"));

        final CellConstraints cc = new CellConstraints();
        add(this.groupTable, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));

        patternFilter = new TablePatternFilter(this.groupTable);
    }

    private void configureTable()
    {
        groupTable.setName("groupTable");
        groupTable.setTableHeader(null);
        groupTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupTable.setShowVerticalLines(false);
        groupTable.setShowHorizontalLines(false);
        groupTable.setRolloverEnabled(false);
        groupTable.setRowSelectionAllowed(true);
        groupTable.setColumnSelectionAllowed(false);
        groupTable.setRowHeight(22);
        groupTable.putClientProperty(JXTable.USE_DTCR_COLORMEMORY_HACK, Boolean.FALSE);

        final Font font = groupTable.getFont();
        groupTable.setFont(font.deriveFont(Font.PLAIN, font.getSize() + 1f));

        // Set column widths
        setColumnWidth(LayerViewUtils.Column.VISIBLE.ordinal(), 22);
        setColumnWidth(LayerViewUtils.Column.STATUS.ordinal(), 20);
        setColumnWidth(LayerViewUtils.Column.BOUNDING_BOX.ordinal(), 20);
        setColumnWidth(LayerViewUtils.Column.FAVORITE.ordinal(), 20);

        // Hide the OBJECT column
        groupTable.getColumnExt(LayerViewUtils.Column.OBJECT.ordinal()).setVisible(false);
    }

    /**
     * Clear selected rows on this panel's table.
     */
    @InvokeOnEDT
    public void clearSelection()
    {
        groupTable.clearSelection();
    }

    /**
     * Refresh the layer table by notifying the model that its data may have changed.
     */
    public void refreshTable()
    {
        tableModel.fireTableDataChanged();
    }

    /**
     * Clear out the search pattern and selection.
     */
    public void clear()
    {
        patternFilter.setPattern(null);
        clearSelection();
    }

    /**
     * Enable/disable dragging of entries in this panel's table.
     */
    public abstract void setDragEnabled(boolean dragEnabled);

    /**
     * Set the search filter String.  Matches the filter text against table entry names.  If an entry name contains the text,
     * ignoring case, display it in the table.  If the name does not contain the filter text, then hide it
     * in the table.  If the filter text is set to {@code null}, this will clear the filter and display all entries.
     */
    public void setFilterString(@Nullable String filterText)
    {
        Pattern pattern = null;
        filterEnabled = false;
        if (filterText != null && !filterText.isEmpty())
        {
            filterEnabled = true;
            try
            {
                pattern = Pattern.compile(".*" + Pattern.quote(filterText) + ".*", Pattern.CASE_INSENSITIVE);
            } catch (Exception e)
            {
                // ignore
            }
        }

        patternFilter.setPattern(pattern);
        groupTable.setRowFilter(patternFilter);

        filterChanged();
    }

    /**
     * Perform any updates required when {@code filterEnabled} changes.
     */
    protected void filterChanged()
    {
        // Noop
    }

    /**
     * Get the row index for a point on the table.  If the filter is enabled, the row will be converted from view to
     * actual model row.
     */
    protected int rowForPoint(Point p)
    {
        return groupTable.convertRowIndexToModel(groupTable.rowAtPoint(p));
    }

    /**
     * Get the table.  Only useful for extending classes.
     */
    protected T getGroupTable()
    {
        return groupTable;
    }

    /**
     * Set a 'constant' pixel width for a table column.  This is 'constant' in that users will not be able to resize
     * the column.
     */
    protected void setColumnWidth(int column, int width)
    {
        TableColumn tableColumn = groupTable.getColumnModel().getColumn(column);
        tableColumn.setMinWidth(width);
        tableColumn.setPreferredWidth(width);
        tableColumn.setMaxWidth(width);
    }

    /**
     * A concrete implementation of a {@link RowFilter} to provide custom filtering capabilities
     */
    protected static class TablePatternFilter extends RowFilter<DefaultTableModel, Integer>
    {
        private final JXTable groupTable;
        private final TableModel tableModel;
        private final TableColumn selectedColumn;
        private Pattern pattern;

        public TablePatternFilter(JXTable groupTable)
        {
            this.groupTable = groupTable;
            tableModel = groupTable.getModel();
            selectedColumn = groupTable.getColumnModel().getColumn(LayerViewUtils.Column.TITLE.ordinal());
            pattern = null;
        }

        public void setPattern(Pattern pattern)
        {
            this.pattern = pattern;
        }

        @Override
        public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry)
        {
            if (pattern == null || pattern.toString().isEmpty() || pattern.toString().equals(".*"))
            {
                return true;
            }

            int row = entry.getIdentifier();
            if (selectedColumn == null)
            {
                for (TableColumn col : groupTable.getColumns(false))
                {
                    if (isColumnMatch(row, col)) return true;
                }
            } else
            {
                return isColumnMatch(row, selectedColumn);
            }
            return false;
        }

        private boolean isColumnMatch(int row, TableColumn col)
        {
            Object value = tableModel.getValueAt(row, col.getModelIndex());
            return value != null && pattern.matcher(value.toString()).matches();
        }
    }

    /**
     * An extension of {@link AbstractTableModel} to provide custom interaction capabilities
     * @param <T> The type of data being interacted with in the table
     */
    public static abstract class AGroupTableModel<T> extends AbstractTableModel
    {
        /**
         * Only the layer visibility checkbox is editable.  (Note that the favorites star has similar on/off functionality,
         * but it is managed directly by a mouse listener in the LayerGroupPanel).
         * todo: should the favorites functionality also be managed by this table model?
         */
        @Override
        public boolean isCellEditable(int row, int column)
        {
            return LayerViewUtils.Column.VALUES[column] == LayerViewUtils.Column.VISIBLE;
        }

        /**
         * Column classes are specified by the LayerViewUtils.Column enum.
         */
        @Override
        public Class<?> getColumnClass(int column)
        {
            return LayerViewUtils.Column.VALUES[column].type;
        }

        /**
         * Number of columns are specified by the LayerViewUtils.Column enum.
         */
        @Override
        public int getColumnCount()
        {
            return LayerViewUtils.Column.VALUES.length;
        }

        /**
         * Set the value at a location in the table.  Only visibility is editable in this table.
         */
        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex)
        {
            if (columnIndex != LayerViewUtils.Column.VISIBLE.ordinal() || !(value instanceof Boolean)) return;
            setEntryVisible(rowIndex, (Boolean) value);
        }

        /**
         * Set the visibility of the given table entry (object).
         *
         * @param rowIndex Index of entry to update
         * @param visible {@code true} to make the entry visible, {@code false} otherwise
         */
        protected abstract void setEntryVisible(int rowIndex, boolean visible);

        /**
         * Get the backing object for a row in this table.
         *
         * @param rowIndex Index of row to retrieve
         * @return backing object
         */
        protected abstract T getRowObject(int rowIndex);
    }
}
