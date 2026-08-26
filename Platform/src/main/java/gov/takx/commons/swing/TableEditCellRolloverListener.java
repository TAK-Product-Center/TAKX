package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import org.jdesktop.swingx.rollover.RolloverProducer;

import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;

import java.awt.Point;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A {@link PropertyChangeListener} for the {@link RolloverProducer#ROLLOVER_KEY} property that activates table editing for
 * cells in a given column when the mouse is over that column. This is useful when cells contain complex components that need
 * to receive mouse events immediately rather than after the user clicks within the cell. This allows for things like LAF
 * style changes when hovering over buttons. This class is likely not suitable for cells that expect user input because the
 * nature of rollover events means that cells can go into and out of edit mode based on mouse movement. This can result in
 * premature saving or even loss of user input, depending on how the editor is implemented. Be careful when using it in those
 * scenarios.
 *
 * @since 5.6
 */
@IncubatingApi(since = "5.6")
public class TableEditCellRolloverListener implements PropertyChangeListener, ListSelectionListener, TableModelListener // TODO DCW TAKX-4111 rollover issues when switching panels
{
    // Make sure this collection always has good iteration performance since it must be scanned every time
    //  we roll over a new table cell
    private final List<Integer> editModelColumns = new ArrayList<>();
    private final JTable table;

    /**
     * @since 5.8
     */
    private TableEditCellRolloverListener(@Nonnull JTable table)
    {
        this.table = table;
        table.addPropertyChangeListener(RolloverProducer.ROLLOVER_KEY, this);
        table.getSelectionModel().addListSelectionListener(this); // Forces a repaint if we select the row during an edit
        table.getModel().addTableModelListener(this);
    }

    /**
     * @param columns the table column model indices to add to this listener
     * @since 5.7
     */
    private void addColumns(@Nonnull Collection<Integer> columns)
    {
        editModelColumns.addAll(columns.stream().filter(i -> !editModelColumns.contains(i)).toList());
    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        var point = (Point) event.getNewValue();
        if (point != null)
        {
            var table = (JTable) event.getSource();
            var modelColumn = table.convertColumnIndexToModel(point.x);
            if (modelColumn > -1 && editModelColumns.contains(modelColumn))
            {
                table.editCellAt(point.y, point.x);
            } else
            {
                table.editingCanceled(null);
            }
        }
    }

    /**
     * This listener ensures that cells that were sent into edit mode are re-rendered when the table selection changes. This
     * can be necessary to apply selected row styles, which normally would not be applied while a cell is in edit mode.
     *
     * @param event the event
     * @implNote This operation could easily be done in the table itself, however, re-rendering edited cells can have
     * undesirable consequences if the cell allows user input (See note in class doc). So we only want to do this if the
     * consumer has explicitly enabled rollover edits by applying this class to a column.
     * @since 5.8
     */
    @Override
    public void valueChanged(ListSelectionEvent event)
    {
        if (editModelColumns.contains(table.convertColumnIndexToModel(table.getEditingColumn())))
        {
            var editingRow = table.getEditingRow();
            if (IntStream.of(table.getSelectedRows()).filter(i -> i == editingRow).findFirst().isPresent())
            {
                table.editCellAt(table.getEditingRow(), table.getEditingColumn());
            }
        }
    }

    /**
     * This listener ensures that cells that were sent into edit mode are re-rendered when their underlying value changes.
     * This functionality is desirable because this class is starting the edit without user interaction, so we want
     * to keep the user informed if the value changes. This functionality would be counter-intuitive for classic cell edit
     * scenarios, but this class uses edit mode merely to display complex components, as opposed to actually editing cell
     * values. In this case, re-rendering the cell does not risk losing user input.
     *
     * @param event the event
     * @since 5.8
     */
    @Override
    public void tableChanged(@Nonnull TableModelEvent event)
    {
        // This logic only works for update events. Insert / Delete events will cause issues because the table component listener is always fired last
        //  which means that the model <-> view row mapping is technically out of sync in this listener
        if (event.getType() == TableModelEvent.UPDATE)
        {
            var tableEditColumn = table.getEditingColumn();
            if (tableEditColumn != -1)
            {
                var changeModelColumn = event.getColumn();
                var tableEditModelColumn = table.convertColumnIndexToModel(tableEditColumn);
                if (changeModelColumn == TableModelEvent.ALL_COLUMNS || editModelColumns.contains(tableEditModelColumn))
                {
                    var tableEditRow = table.getEditingRow();
                    var tableEditModelRow = table.convertRowIndexToModel(tableEditRow);
                    if (event.getFirstRow() <= tableEditModelRow && event.getLastRow() >= tableEditModelRow && tableEditRow < table.getModel().getRowCount())
                    {
                        table.editCellAt(tableEditRow, tableEditColumn);
                    }
                }
            }
        }
    }

    /**
     * This class only works when there is one instance added to a given table. This method ensures that by
     * first searching for that instance and modifying it rather than creating a new instance.
     *
     * @param table the table component
     * @param columns the columns to add
     * @since 5.7
     */
    public static void addForColumns(@Nonnull JTable table, @Nonnull TableColumn... columns)
    {
        var listener = Stream.of(table.getPropertyChangeListeners(RolloverProducer.ROLLOVER_KEY))
                .filter(TableEditCellRolloverListener.class::isInstance)
                .map(TableEditCellRolloverListener.class::cast)
                .findFirst()
                .orElseGet(() -> new TableEditCellRolloverListener(table));
        listener.addColumns(Stream.of(columns).map(TableColumn::getModelIndex).toList());
    }
}
