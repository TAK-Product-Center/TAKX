package gov.takx.platform.ui.devicelog;

import gov.takx.commons.swing.TableColumnModelListenerAdapter;
import jakarta.annotation.Nonnull;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.table.DefaultTableColumnModelExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import java.lang.invoke.MethodHandles;
import java.util.Comparator;
import java.util.List;

/**
 * Custom combo box control to manage the selection of the column to which the filter text is applied.
 *
 * @since 2.16
 */
public class FilterColumnSelector extends JComboBox<String>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String NONE = "None";

    private JXTable table;

    /**
     * Default constructor.
     */
    public FilterColumnSelector()
    {
        setModel(makeDefaultModel());
    }

    /**
     * Set the table on which our content is to be derived.  This control will contain all visible columns in its
     * model, handling dynamic user actions to hide/show columns.
     *
     * @param table The table from which to pull our column names
     */
    public void setModelFromTable(@Nonnull JXTable table)
    {
        // Track our table, and don't add our listener more than once
        if (table != this.table)
        {
            this.table = table;
            table.getColumnModel().addColumnModelListener(new ColumnListener());
            table.addPropertyChangeListener("model", evt -> rebuildModel());
        }

        rebuildModel();
    }

    /**
     * Select the "None" entry.
     */
    public void selectNone()
    {
        setSelectedItem(NONE);
    }

    /**
     * Rebuild our model based on the currently visible columns of the table.
     */
    private void rebuildModel()
    {
        logger.debug("rebuildModel");

        final String selectedColumn = (String) getSelectedItem(); // So we can re-install it below

        // Get the visible columns, and sort them into model order
        final List<TableColumn> visibleColumns = table.getColumns(false);
        visibleColumns.sort(Comparator.comparingInt(TableColumn::getModelIndex));

        // Ok, this is just stupid, but when the table model is being replaced, calls are made to remove
        // the existing columns.  And, for some reason, some of those calls result in "addColumn" calls
        // with the "visibility change" set true.  And, wait for it, now that we're here, and we call
        // table.getColumns(false), we get columns that no longer exist in the model. Kill me.
        //
        // So, best I an figure out, we need to filter out any columns with an index greater than the current
        // column count.

        final int columnCount = table.getColumnCount(true);
        final TableModel tableModel = table.getModel();

        final DefaultComboBoxModel<String> comboBoxModel = makeDefaultModel();

        // And add each column name to our new model
        visibleColumns.stream()
                .filter(c -> c.getModelIndex() < columnCount)
                .map(c -> tableModel.getColumnName(c.getModelIndex()))
                .forEach(comboBoxModel::addElement);

        setModel(comboBoxModel); // And install it

        // By initially selecting "None" and then the previously selected item, we reset to the
        // None state, and then, when we try to re-select the previously selected element, it will have
        // no affect if the element is no longer in the model (i.e., it was the one that was removed).

        selectNone();
        if (selectedColumn != null) setSelectedItem(selectedColumn);
    }

    /**
     * @return The selected column name, {@code null} if there is no selection, or "None" is selected
     */
    public String getSelectedColumn()
    {
        final int index = getSelectedIndex();
        return index > 0 ? getItemAt(index) : null;
    }

    /**
     * @return A new model instance with the None entry already added.
     */
    private DefaultComboBoxModel<String> makeDefaultModel()
    {
        return new DefaultComboBoxModel<>(new String[]{NONE});
    }

    /**
     * Handler for when columns are made visible/invisible.  When this happens we need to rebuild our
     * combo box model.
     */
    private class ColumnListener extends TableColumnModelListenerAdapter
    {
        @Override
        public void columnAdded(TableColumnModelEvent e)
        {
            final int index = e.getToIndex();

            // We're only watching for columns being un-hidden
            if (((DefaultTableColumnModelExt) e.getSource()).isAddedFromInvisibleEvent(index))
            {
                rebuildModel();
            }
        }

        @Override
        public void columnRemoved(TableColumnModelEvent e)
        {
            // We're only watching for columns being hidden
            if (((DefaultTableColumnModelExt) e.getSource()).isRemovedToInvisibleEvent(e.getFromIndex()))
            {
                rebuildModel();
            }
        }
    }
}
