package gov.takx.test.mock.plugin.app;

import gov.takx.api.plugin.annotations.AppPlugin;
import gov.takx.api.plugin.annotations.PluginDescriptor;
import gov.takx.api.plugin.annotations.PluginSpecification;
import gov.takx.api.plugin.app.AAppPlugin;
import gov.takx.api.plugin.app.IAppPluginDelegate;

/**
 * Second app plugin for integration testing.
 *
 * @since 2.38
 */
@AppPlugin(
        spec = @PluginSpecification(
                family = TestAppPlugin2.FAMILY,
                type = TestAppPlugin2.TYPE,
                version = TestAppPlugin2.VERSION,
                iconPath = "icons/32x32.png"),
        descriptor = @PluginDescriptor(
                name = TestAppPlugin2.TYPE))
public class TestAppPlugin2 extends AAppPlugin
{
    public static final String FAMILY = "TestFamily";
    public static final String TYPE = "TestV2AppPlugin2";
    public static final String VERSION = "1.0.0";

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
