package gov.takx.core.view.gui.preferences.toolbar;

import gov.takx.core.toolbar.ToolbarConfiguration;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import java.awt.Component;

/**
 * Renderer for a list cell containing a {@link ToolbarConfiguration}
 * which displays the configuration name.
 *
 * @since 2.0 (Renamed in 3.4)
 */
public class ToolbarConfigurationListCellRenderer extends DefaultListCellRenderer
{
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
    {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof ToolbarConfiguration configuration)
        {
            setText(configuration.getName());
        }

        return this;
    }
}
