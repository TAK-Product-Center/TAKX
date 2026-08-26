package gov.takx.test.mock.plugin.app;

import gov.takx.api.plugin.annotations.AppPlugin;
import gov.takx.api.plugin.annotations.PluginDescriptor;
import gov.takx.api.plugin.annotations.PluginSpecification;
import gov.takx.api.plugin.app.AAppPlugin;
import gov.takx.api.plugin.app.IAppPluginDelegate;

/**
 * A simple app plugin used for integration testing.  It can generate data messages and push them into the core.
 *
 * @since 2.0.0
 */
@AppPlugin(
        spec = @PluginSpecification(
                family = TestAppPlugin.FAMILY,
                type = TestAppPlugin.TYPE,
                version = TestAppPlugin.VERSION,
                iconPath = ""),
        descriptor = @PluginDescriptor(
                name = "Test App Plugin"))
public class TestAppPlugin extends AAppPlugin
{
    public static final String FAMILY = "gov.takx.TestApp";
    public static final String TYPE = "TestAppPlugin";
    public static final String VERSION = "1.0.0";

    /**
     * Constructor.
     */
    public TestAppPlugin()
    {
    }

    @Override
    public void start()
    {
    }

    @Override
    public void stop()
    {
    }

    @Override
    public void shutdown()
    {
    }

    public IAppPluginDelegate getDelegate()
    {
        return delegate;
    }
}
