package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import java.awt.Component;

/**
 * A Table cell renderer that adjusts html color styling when a row is selected, to avoid unreadable text.
 *
 * @implNote Don't do too much in this class, as renderers can have performance implications. Adjusting html for selected
 * rows is likely not an issue, but it would be a bad idea to expand this class to do anything that doesn't explicitly
 * require the rendering parameters.
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class ColorMappingRenderer extends DefaultTableCellRenderer
{
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
        var html = (String) value;
        if (html != null)
        {
            if (isSelected) html = html.replaceAll("color:#.*?;", "");

        }
        return super.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column);
    }
}
