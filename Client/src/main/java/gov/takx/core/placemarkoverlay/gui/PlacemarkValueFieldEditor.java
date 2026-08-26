package gov.takx.core.placemarkoverlay.gui;

import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;

import java.awt.Component;
import java.util.EventObject;

/**
 */
public class PlacemarkValueFieldEditor implements TableCellEditor
{
    private JTextField textField = new JTextField();

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
    {
        return value instanceof JTextField ? (JTextField) value : textField;
    }

    @Override
    public Object getCellEditorValue()
    {
        return null;
    }

    @Override
    public boolean isCellEditable(EventObject anEvent)
    {
        return true;
    }

    @Override
    public boolean shouldSelectCell(EventObject anEvent)
    {
        return true;
    }

    @Override
    public boolean stopCellEditing()
    {
        return true;
    }

    @Override
    public void cancelCellEditing()
    {

    }

    @Override
    public void addCellEditorListener(CellEditorListener l)
    {

    }

    @Override
    public void removeCellEditorListener(CellEditorListener l)
    {

    }
}
