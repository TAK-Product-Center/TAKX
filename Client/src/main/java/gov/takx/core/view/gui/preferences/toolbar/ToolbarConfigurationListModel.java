package gov.takx.core.view.gui.preferences.toolbar;

import gov.takx.core.toolbar.ToolbarConfiguration;
import jakarta.annotation.Nonnull;

import javax.swing.DefaultListModel;

/**
 * Model for toolbar configuration list, which maintains the configurations in sorted order.
 *
 * @since 2.0 (Renamed in 3.4)
 */
public class ToolbarConfigurationListModel extends DefaultListModel<ToolbarConfiguration>
{
    private final boolean showDefaultConfiguration;

    public ToolbarConfigurationListModel(@Nonnull ToolbarConfiguration defaultConfiguration, boolean showDefaultConfiguration)
    {
        super();
        this.showDefaultConfiguration = showDefaultConfiguration;
        if (showDefaultConfiguration)
        {
            super.addElement(defaultConfiguration);
        }
    }

    @Override
    public void add(int index, @Nonnull ToolbarConfiguration element)
    {
        insertElementAt(element, 0);
    }

    @Override
    public void addElement(@Nonnull ToolbarConfiguration element)
    {
        insertElementAt(element, 0);
    }

    @Override
    public void insertElementAt(@Nonnull ToolbarConfiguration newConfiguration, int index)
    {
        int size = getSize();

        // Determine the correct location for the new element. If present, the first element should always be the
        // default configuration, so start searching for the correct location at index 1.
        int startingIndex = showDefaultConfiguration ? 1 : 0;
        final String newConfigurationName = newConfiguration.getName();
        for (index = startingIndex; index < size; index++)
        {
            ToolbarConfiguration configuration = get(index);

            // Order based upon name
            if (configuration.getName().compareTo(newConfigurationName) > 0) break;
        }

        super.insertElementAt(newConfiguration, index);
    }
}
