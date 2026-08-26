package gov.takx.api.event;

import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.PluginCategory;
import jakarta.annotation.Nonnull;

/**
 * Event fired when a plugin is added to the registry at runtime, i.e., hot deployed.
 * <p/>
 * Note that this event is never fired during service startup.  It is only fired when a plugin jar is hot deployed.
 *
 * @since 5.7 (Moved from Platform)
 * @see gov.takx.core.view.gui.maps.importers.PluginJarImporter
 */
public record PluginAdded(IPluginDefinition<?> pluginDefinition)
{
    @Nonnull
    public PluginCategory getPluginCategory()
    {
        return pluginDefinition.getPluginCategory();
    }
}
