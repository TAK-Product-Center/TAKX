package gov.takx.test.mock.plugin.connection;

import gov.takx.api.plugin.annotations.ConnectionPlugin;
import gov.takx.api.plugin.annotations.PluginDescriptor;
import gov.takx.api.plugin.annotations.PluginSpecification;
import gov.takx.api.plugin.connection.AConnectionPlugin;
import gov.takx.api.plugin.connection.IConnectionPluginController;

/**
 * A simple connection plugin for integration testing.
 *
 * @since 2.0.0
 */
@ConnectionPlugin(
        spec = @PluginSpecification(
                family = TestConnectionPlugin.FAMILY,
                type = TestConnectionPlugin.TYPE,
                version = TestConnectionPlugin.VERSION,
                iconPath = TestConnectionPlugin.ICON),
        descriptor = @PluginDescriptor(
                name = TestConnectionPlugin.NAME),
        editSupported = true,
        maxSupportedConnections = TestConnectionPlugin.MAX_SUPPORTED_CONNECTIONS)
public class TestConnectionPlugin extends AConnectionPlugin
{
    public static final String FAMILY = "gov.takx.test";
    public static final String TYPE = "TestV2ConnectionPlugin";
    public static final String VERSION = "1.0.0";
    public static final String ICON = "icons/32x32.png";
    public static final String NAME = "Test Connection";
    public static final int MAX_SUPPORTED_CONNECTIONS = 10;

    @Override
    public IConnectionPluginController createConnectionController()
    {
        return new TestConnectionPluginController(FAMILY, TYPE);
    }
}
