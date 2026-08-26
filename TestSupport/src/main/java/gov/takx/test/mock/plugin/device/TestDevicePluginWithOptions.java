package gov.takx.test.mock.plugin.device;

import gov.takx.api.mapobject.MapObjectOption;
import gov.takx.api.plugin.annotations.DevicePlugin;
import gov.takx.api.plugin.annotations.PluginDescriptor;
import gov.takx.api.plugin.annotations.PluginSpecification;
import gov.takx.api.plugin.device.ADevicePlugin;
import gov.takx.api.plugin.device.IDevicePluginController;
import gov.takx.api.plugin.gateway.IGatewayFramer;
import gov.takx.api.plugin.gateway.IGatewayFramerDelegate;
import gov.takx.api.plugin.gateway.IGatewayParser;
import gov.takx.api.plugin.gateway.IGatewayParserDelegate;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.core.plugin.DefaultDevicePluginController;

/**
 * Test device plugin that specifies map object options.
 *
 * @since 2.29
 */
@DevicePlugin(
        spec = @PluginSpecification(
                family = TestDevicePluginWithOptions.FAMILY,
                type = TestDevicePluginWithOptions.TYPE,
                version = TestDevicePluginWithOptions.VERSION,
                iconPath = "icons/32x32.png"),
        descriptor = @PluginDescriptor(
                name = "MOO"),
        dataStructureXml = "schemas/sample1.xml",
        mapObjectOptions = {MapObjectOption.ExcludeFromExplorer, MapObjectOption.NoAlarms})
public class TestDevicePluginWithOptions extends ADevicePlugin
{
    public static final String FAMILY = "TestMooFamily";
    public static final String TYPE = "TestMooType";
    public static final String VERSION = "1.0.0";
    public static final String MESSAGE_TYPE_0 = "MessageType0";
    public static final MapObjectOption[] OPTIONS = {MapObjectOption.ExcludeFromExplorer, MapObjectOption.NoAlarms};

    @Override
    protected IDevicePluginController makeDeviceController(String unitID)
    {
        return new DefaultDevicePluginController(family, type, unitID);
    }

    @Override
    public IGatewayParser createGatewayParser(CommServiceType sourceType, String sourceName, String format, IGatewayParserDelegate delegate)
    {
        return null;
    }

    @Override
    public IGatewayFramer createGatewayFramer(CommServiceType sourceType, String sourceName, String format, IGatewayFramerDelegate delegate)
    {
        return null;
    }
}
