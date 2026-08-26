package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.rollover.RolloverProducer;
import org.jdesktop.swingx.rollover.TableRolloverProducer;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.swingx.table.TableColumnModelExt;

import javax.swing.JComponent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Extension of {@link JXTable} with added features:
 * <p>
 * <ul>
 *  <li>Highlighters can run on cells that are in edit mode when {@link #editorHighlightsEnabled} is enabled.</li>
 *  <li>Editors are re-rendered if their model data changes.</li>
 *  <li>Columns can be configured to be hidden automatically when they contain only {@code null} values. See {@link #setAutoHideColumns(boolean, int...)}.</li>
 *  <li>Columns can be configured to resize automatically when content changes. See {@link #setAutoSizeColumns(boolean, int...)}.</li>
 *  <li>Row height can be derived from a known number of text lines. See {@link #setTextLinesForRowHeight(int)}</li>
 * </ul>
 *
 * @since 5.6
 */
@IncubatingApi(since = "5.6")
public class JXTableExt extends JXTable
{
    protected boolean editorHighlightsEnabled;
    private List<Integer> autoHideColumns;
    private List<Integer> autoSizeColumns;
    private ToolTipProvider toolTipProvider;
    private int textLinesForRowHeight;

    /**
     * Construct a new instance with defaults.
     */
    public JXTableExt()
    {
    }

    /**
     * Construct a new instance with the given model.
     * @param model the table model to use
     */
    @SuppressWarnings("unused")
    public JXTableExt(@Nullable TableModel model)
    {
        super(model);
    }

    /**
     * @param dataModel the new data source for this table
     * @since 5.7
     */
    @Override
    public void setModel(@Nonnull TableModel dataModel)
    {
        // Yes, initializing fields here is odd, but swing has a bad habit of calling non-private methods from constructors
        //  and so this is the best place to catch the constructor call and initialize these, to prevent excessive null checking
        if (autoHideColumns == null) autoHideColumns = new ArrayList<>();
        if (autoSizeColumns == null) autoSizeColumns = new ArrayList<>();
        super.setModel(dataModel);
    }

    @Override
    public void tableChanged(@Nonnull TableModelEvent event)
    {
        super.tableChanged(event);
        List<TableColumn> columns = null;
        var eventColumn = event.getColumn();

        if (!autoHideColumns.isEmpty())
        {
            List<Integer> check = eventColumn == TableModelEvent.ALL_COLUMNS ? autoHideColumns : autoHideColumns.contains(eventColumn) ? List.of(eventColumn) : Collections.emptyList();
            columns = ((TableColumnModelExt) getColumnModel()).getColumns(true);
            for (var modelColumn : check)
            {
                autoHide(event, (TableColumnExt) columns.get(modelColumn), modelColumn);
            }
        }

        if (!autoSizeColumns.isEmpty())
        {
            List<Integer> check = eventColumn == TableModelEvent.ALL_COLUMNS ? autoSizeColumns : autoSizeColumns.contains(eventColumn) ? List.of(eventColumn) : Collections.emptyList();
            if (columns == null) columns = ((TableColumnModelExt) getColumnModel()).getColumns(true);
            for (var modelColumn : check)
            {
                autoSize(columns.get(modelColumn));
            }
        }
    }

    /**
     * Set model columns to auto hide when all values are {@code null}.
     *
     * @param autoHide {@code true} to auto hide the columns, otherwise {@code false}
     * @param columns the model column indices
     * @since 5.7
     */
    @SuppressWarnings("unused")
    public void setAutoHideColumns(boolean autoHide, int... columns)
    {
        for (var column : columns)
        {
            if (autoHide && !autoHideColumns.contains(column))
            {
                autoHideColumns.add(column);
            } else
            {
                autoHideColumns.remove(column);
            }
        }
    }

    /**
     * Clears all auto-hide column flags.
     *
     * @since 5.7
     */
    public void clearAutoHideColumns()
    {
        autoHideColumns.clear();
    }

    /**
     * @return the {@code List} of columns to auto hide
     * @since 5.7
     */
    @Nonnull
    public List<Integer> getAutoHideColumns()
    {
        return List.copyOf(autoHideColumns);
    }

    /**
     * Set model columns to auto size based on contents.
     *
     * @param autoSize {@code true} to auto size the columns, otherwise {@code false}
     * @param columns the model column indices
     * @since 5.7
     */
    public void setAutoSizeColumns(boolean autoSize, int... columns)
    {
        for (var column : columns)
        {
            if (autoSize && !autoSizeColumns.contains(column))
            {
                autoSizeColumns.add(column);
            } else
            {
                autoSizeColumns.remove(column);
            }
        }
    }

    /**
     * Setting a tool tip provider allows overriding renderer generated tool tips with those from a single provider. If the
     * provider returns {@code null} then the normal tooltip logic will apply.
     * 
     * @param toolTipProvider the provider to generate tool tip text for cells
     * @since 5.8
     */
    public void setToolTipProvider(@Nonnull ToolTipProvider toolTipProvider)
    {
        this.toolTipProvider = toolTipProvider;
    }

    /**
     * @return all visible columns in this table
     * @since 5.7
     */
    @SuppressWarnings("unchecked")
    public List<TableColumnExt> getColumnExts()
    {
        return (List<TableColumnExt>) (List<?>) getColumns();
    }

    /**
     * Moves the given column to the new index in the underlying column model. See
     * {@link javax.swing.table.TableColumnModel#moveColumn(int, int)}.
     * 
     * @param column the table column
     * @param newIndex the new index
     */
    public void moveColumn(@Nonnull TableColumnExt column, int newIndex)
    {
        getColumnModel().moveColumn(getColumnModel().getColumnIndex(column.getIdentifier()), newIndex);
    }

    /**
     * Sets table columns visible or invisible.
     * 
     * @param visible {@code true} if columns should be visible, otherwise {@code false}
     * @param identifiers column identifiers
     * @since 5.7
     */
    public void setColumnsVisible(boolean visible, Object... identifiers)
    {
        for (var id : identifiers) getColumnExt(id).setVisible(visible);
    }

    /**
     * @return {@code true} if highlighters should run on cell editors, otherwise {@code false}
     */
    public boolean isEditorHighlightsEnabled()
    {
        return editorHighlightsEnabled;
    }

    /**
     * @param editorHighlightsEnabled {@code true} if highlighters should run on cell editors, otherwise {@code false}
     */
    public void setEditorHighlightsEnabled(boolean editorHighlightsEnabled)
    {
        this.editorHighlightsEnabled = editorHighlightsEnabled;
    }

    /**
     * This implementation mimics the {@link JXTable#prepareRenderer(TableCellRenderer, int, int)} method and applies
     * highlighters to the editor components, which is not done by default.
     *
     * @param editor the {@code TableCellEditor} to set up
     * @param row the row of the cell to edit, where 0 is the first row
     * @param column the column of the cell to edit, where 0 is the first column
     * @return the component being edited
     */
    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column)
    {
        var comp = super.prepareEditor(editor, row, column);
        // Copied from JXTable#prepareRenderer(TableCellRenderer, int, int)
        if (editorHighlightsEnabled)
        {
            var adapter = getComponentAdapter(row, column);
            if (compoundHighlighter != null)
            {
                comp = compoundHighlighter.highlight(comp, adapter);
            }
            var columnExt = getColumnExt(column);
            if (columnExt != null)
            {
                for (var highlighter : columnExt.getHighlighters())
                {
                    comp = highlighter.highlight(comp, adapter);

                }
            }
        }
        return comp;
    }

    /**
     * This implementation first checks the tables {@link ToolTipProvider} before falling back to the cell renderer tooltip.
     *
     * @param event the {@code MouseEvent} that initiated the {@code ToolTip} display
     * @return the tooltip for the given mouse location
     * @since 5.8
     */
    @Override
    public String getToolTipText(@Nonnull MouseEvent event)
    {
        String tooltip = null;
        if (toolTipProvider != null)
        {
            var point = event.getPoint();
            var row = rowAtPoint(point);
            if (row > -1)
            {
                var col = columnAtPoint(point);
                if (col > -1)
                {
                    tooltip = toolTipProvider.getToolTipText(row, col);
                }
            }
        }
        if (tooltip == null) tooltip = super.getToolTipText(event);
        return tooltip;
    }

    /**
     * Sets the row height based on the maximum expected lines of text in a row and font metrics for the component.
     *
     * @param numLines the number of expected lines of text
     * @since 5.8
     */
    public void setTextLinesForRowHeight(int numLines)
    {
        textLinesForRowHeight = numLines;
        var metrics = getFontMetrics(getFont());
        setRowHeight((metrics.getHeight() * numLines) + (metrics.getDescent() * 2)); // each row + descent sized padding above and below
    }

    /**
     * @see #setTextLinesForRowHeight(int)
     * @return the number of expected lines of text
     * @since 5.8
     */
    public int getTextLinesForRowHeight()
    {
        return textLinesForRowHeight;
    }

    @Override
    protected RolloverProducer createRolloverProducer()
    {
        return new TableRolloverProducerExt();
    }

    private void autoHide(TableModelEvent event, TableColumnExt column, int modelColumn)
    {
        if (event.getType() == TableModelEvent.INSERT)
        {
            if (!column.isVisible())
            {
                for (var i = event.getFirstRow(); i <= event.getLastRow(); i++)
                {
                    if (getModel().getValueAt(i, modelColumn) != null)
                    {
                        column.setVisible(true);
                        break;
                    }
                }
            }
        } else if (event.getType() == TableModelEvent.DELETE || event.getType() == TableModelEvent.UPDATE)
        {
            if (event.getType() == TableModelEvent.UPDATE || column.isVisible()) // Deletes cannot change a column from invisible -> visible
            {
                var empty = true;
                var rows = getModel().getRowCount();
                for (var i = 0; i < rows; i++)
                {
                    if (getModel().getValueAt(i, modelColumn) != null)
                    {
                        empty = false;
                        break;
                    }
                }
                column.setVisible(!empty);
            }
        }
    }

    private void autoSize(TableColumn column)
    {
        var width = 0;
        var col = convertColumnIndexToView(column.getModelIndex());
        if (col > -1)
        {
            for (var row = 0; row < getRowCount(); row++)
            {
                var renderer = getCellRenderer(row, col);
                var comp = prepareRenderer(renderer, row, col);
                width = Math.max(width, comp.getPreferredSize().width);
            }
            column.setMinWidth(width);
            column.setMaxWidth(width);
            column.setPreferredWidth(width);
        }
    }

    /**
     * Extension of {@link TableRolloverProducer} that hides the mouse exited event as long as the mouse is still over the
     * component. This is necessary to allow rollover to function with cell editors.
     */
    public static class TableRolloverProducerExt extends TableRolloverProducer
    {
        @Override
        public void mouseExited(MouseEvent e)
        {
            if (!((JComponent) e.getSource()).contains(e.getPoint()))
            {
                super.mouseExited(e);
            } else
            {
                mouseMoved(e);
            }
        }
    }

    /**
     * A provider interface for providing tool tips for cells in a table. See {@link #setToolTipProvider(ToolTipProvider)}.
     *
     * @since 5.8
     */
    @FunctionalInterface
    public interface ToolTipProvider
    {
        /**
         * @param row a valid table row
         * @param column a valid table column
         * @return the text to be used as the tooltip
         */
        @Nullable
        String getToolTipText(int row, int column);
    }
}
