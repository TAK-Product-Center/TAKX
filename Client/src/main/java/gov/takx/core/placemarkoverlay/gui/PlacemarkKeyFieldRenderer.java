package gov.takx.core.placemarkoverlay.gui;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import java.awt.Component;

/**
 */
public class PlacemarkKeyFieldRenderer implements TableCellRenderer
{
    private JLabel label = new JLabel();

    public PlacemarkKeyFieldRenderer()
    {
        label.setHorizontalAlignment(JLabel.RIGHT);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
        String text = value instanceof String ? (String) value : "";
        label.setText(text);
        return label;
    }
}
