package gov.takx.core.view.gui.preferences.toolbar;

import gov.takx.core.toolbar.ToolbarButton;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import java.awt.Component;

/**
 * Renderer for a list cell containing a {@link ToolbarButton}
 * which displays the button icon and text as the value of the cell.
 *
 * @since 2.0 (Renamed in 3.4)
 */
public class ToolbarButtonListCellRenderer extends DefaultListCellRenderer
{
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
    {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof ToolbarButton button)
        {
            final String identificationString = button.getIdentificationString();
            setText(button.isValid()
                    ? identificationString
                    : "<html><body><span style='text-decoration: line-through;'>" + identificationString + "</span></body></html>");
            setIcon(button.getIcon());
        }

        return this;
    }
}
