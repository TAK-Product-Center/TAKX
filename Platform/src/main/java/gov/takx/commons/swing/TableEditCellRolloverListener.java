package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import org.jdesktop.swingx.rollover.RolloverProducer;

import javax.swing.JTable;
import javax.swing.table.TableColumn;

import java.awt.Point;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * A {@link PropertyChangeListener} for the {@link RolloverProducer#ROLLOVER_KEY} property that activates
 * table editing for cells in a given column when the mouse is over that column. This is useful when cells contain complex
 * components that need to receive mouse events immediately rather than after the user clicks within the cell. This allows
 * for things like LAF style changes when hovering over buttons.
 *
 * @since 5.6
 */
@IncubatingApi(since = "5.6")
public class TableEditCellRolloverListener implements PropertyChangeListener // TODO DCW TAKX-4111 rollover issues when switching panels
{
    // Make sure this collection always has good iteration performance since it must be scanned every time
    //  we roll over a new table cell
    private final List<Integer> editModelColumns = new ArrayList<>();

    /**
     * @since 5.7
     */
    private TableEditCellRolloverListener()
    {
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
                .orElseGet(() -> {
                    var l = new TableEditCellRolloverListener();
                    table.addPropertyChangeListener(RolloverProducer.ROLLOVER_KEY, l);
                    return l;
                });
        listener.addColumns(Stream.of(columns).map(TableColumn::getModelIndex).toList());
    }
}
