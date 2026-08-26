package gov.takx.test.mock.plugin.device;

import gov.takx.api.plugin.annotations.DevicePlugin;
import gov.takx.api.plugin.annotations.PluginDescriptor;
import gov.takx.api.plugin.annotations.PluginSpecification;
import gov.takx.api.plugin.device.IDevicePluginController;
import gov.takx.core.plugin.DefaultDevicePluginController;

/**
 * A simple plugin used for integration testing.  It can generate data messages and push them into the core.
 *
 */
@DevicePlugin(
        spec = @PluginSpecification(
                family = ATestDevicePlugin.FAMILY,
                type = TestDevicePlugin2.DEVICE_NAME,
                version = "3.0.0",
                iconPath = "icons/32x32.png"),
        descriptor = @PluginDescriptor(
                name = TestDevicePlugin2.DEVICE_NAME))
public class TestDevicePlugin2 extends ATestDevicePlugin
{
    public static final String DEVICE_NAME = "TestPlugin2";
    public static final String STRUCTURE_NAME = "TEST_STRUCTURE_P2";

    public TestDevicePlugin2()
    {
        super(STRUCTURE_NAME);
        initStructures1();
    }

    @Override
    protected IDevicePluginController makeDeviceController(String unitID)
    {
        return new DefaultDevicePluginController(getFamily(), getType(), unitID);
    }
}
