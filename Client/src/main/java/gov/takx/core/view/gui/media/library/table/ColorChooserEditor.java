package gov.takx.core.view.gui.media.library.table;

import gov.takx.core.view.gui.media.library.MediaLibraryPanel;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionListener;

/**
 * Cell editor attached to the color column in the media library's table to allow the user to choose a color by
 * clicking in the cell.  The chosen color is then set as the background of the button, effectively setting the
 * background color of the cell.
 * <p>
 * Adapted from: http://www.java2s.com/Tutorial/Java/0240__Swing/ColorChooserEditor.htm
 *
 * @since 2.37
 */
public class ColorChooserEditor extends AbstractCellEditor implements TableCellEditor
{
    private final JButton colorChooserButton = new JButton();
    private Color savedColor;

    public ColorChooserEditor(MediaLibraryPanel mediaLibraryPanel)
    {
        ActionListener actionListener = actionEvent -> {
            Color color = JColorChooser.showDialog(colorChooserButton, "Color Chooser", savedColor);
            changeColor(color);
            mediaLibraryPanel.changeColor();
            stopCellEditing();
        };
        colorChooserButton.addActionListener(actionListener);
    }

    @Override
    public Object getCellEditorValue()
    {
        return savedColor;
    }

    private void changeColor(Color color)
    {
        if (color != null && !color.equals(savedColor))
        {
            savedColor = color;
            colorChooserButton.setBackground(color);
        }
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
    {
        changeColor((Color) value);
        return colorChooserButton;
    }
}
