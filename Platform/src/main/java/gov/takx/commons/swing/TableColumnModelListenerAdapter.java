package gov.takx.commons.swing;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;

/**
 * Simple adapter for TableColumnModelListener.  All methods are implemented with empty bodies.
 *
 * @since 2.16
 */
public class TableColumnModelListenerAdapter implements TableColumnModelListener
{
    @Override
    public void columnAdded(TableColumnModelEvent e)
    {
    }

    @Override
    public void columnRemoved(TableColumnModelEvent e)
    {
    }

    @Override
    public void columnMoved(TableColumnModelEvent e)
    {
    }

    @Override
    public void columnMarginChanged(ChangeEvent e)
    {
    }

    @Override
    public void columnSelectionChanged(ListSelectionEvent e)
    {
    }
}
