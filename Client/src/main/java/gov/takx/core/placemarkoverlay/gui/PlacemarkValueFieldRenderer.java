package gov.takx.core.placemarkoverlay.gui;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellRenderer;

import java.awt.Component;

/**
 */
public class PlacemarkValueFieldRenderer implements TableCellRenderer
{
    private JTextField textField = new JTextField();
    private JLabel label = new JLabel();
    private boolean enabled = true;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
        JTextField field = value instanceof JTextField ? (JTextField) value : textField;
        if (enabled)
        {
            return field;
        } else
        {
            label.setText(":  " + field.getText());
            return label;
        }
    }

    /**
     * TODO: hacky, whole placemark dialog stuff is kind of hacky, may want to revisit it at some point and do it properly when theres some moola.
     * This is just a quick fix to have the text field display as a label when editing of values are disabled  in
     * placemark property panel.
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }
}
