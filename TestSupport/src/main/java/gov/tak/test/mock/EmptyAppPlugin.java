package gov.tak.test.mock;

import gov.takx.api.plugin.annotations.AppPlugin;
import gov.takx.api.plugin.annotations.PluginDescriptor;
import gov.takx.api.plugin.annotations.PluginSpecification;
import gov.takx.api.plugin.app.AAppPlugin;

/**
 * Empty plugin, can be used to force scanning of the core container for beans that would normally be provided
 * in a plugin container.
 *
 * @since 5.1
 */
@AppPlugin(
        descriptor = @PluginDescriptor(name = "empty app plugin"),
        spec = @PluginSpecification(
                family = "test",
                type = "empty",
                version = "1.0",
                iconPath = ""))
public class EmptyAppPlugin extends AAppPlugin
{
    @Override
    public void start() throws Exception
    {
    }

    @Override
    public void stop()
    {
    }

    @Override
    public void shutdown() throws Exception
    {
    }
}
