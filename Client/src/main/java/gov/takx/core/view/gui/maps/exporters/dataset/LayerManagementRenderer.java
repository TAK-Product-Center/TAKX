package gov.takx.core.view.gui.maps.exporters.dataset;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import java.awt.Component;

/**
 * A renderer for the MapDataWrapper size management panels.
 *
 * @since 1.43
 */
public class LayerManagementRenderer extends LayerSizeManagementPanel implements TableCellRenderer
{

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
        if (value instanceof MapDataWrapper)
        {
            configure((MapDataWrapper) value);
        }

        return this;
    }
}
