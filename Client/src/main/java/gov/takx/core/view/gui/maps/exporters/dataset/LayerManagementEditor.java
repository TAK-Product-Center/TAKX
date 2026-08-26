package gov.takx.core.view.gui.maps.exporters.dataset;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import java.awt.Component;

/**
 * An editor for MapDataWrapper size management.
 *
 * @since 1.43
 */
public class LayerManagementEditor extends AbstractCellEditor implements TableCellEditor
{

    private LayerSizeManagementPanel editPanel;

    private MapDataWrapper value;

    @Override
    public Object getCellEditorValue()
    {
        return this.value;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
    {
        if (value instanceof MapDataWrapper)
        {
            this.value = (MapDataWrapper) value;
            editPanel = new LayerSizeManagementPanel();
            editPanel.configure(this.value);
        }

        return editPanel;
    }
}
