package gov.takx.test.mock.plugin.device;

import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.plugin.annotations.DevicePlugin;
import gov.takx.api.plugin.annotations.PluginDescriptor;
import gov.takx.api.plugin.annotations.PluginSpecification;
import gov.takx.api.plugin.device.ADevicePluginController;
import gov.takx.api.plugin.device.IDevicePluginController;

/**
 */
@DevicePlugin(
        spec = @PluginSpecification(
                family = ATestDevicePlugin.FAMILY,
                type = TestDevicePlugin3.DEVICE_NAME,
                version = "3.0.0",
                iconPath = "icons/32x32.png"),
        descriptor = @PluginDescriptor(
                name = TestDevicePlugin3.DEVICE_NAME))
public class TestDevicePlugin3 extends ATestDevicePlugin
{
    public static final String DEVICE_NAME = "TestPlugin3";

    public TestDevicePlugin3()
    {
        super("Unused");
    }

    @Override
    public boolean isShareable()
    {
        return true;
    }

    @Override
    protected IDevicePluginController makeDeviceController(String unitID)
    {
        return new TestPlugin3Controller(unitID);
    }

    @Override
    public void initStructures1()
    {
    }

    @Override
    public void initStructures2()
    {
    }

    @Override
    public void initStructures3()
    {
    }

    @Override
    public IPrePersistRaptorDataMessage generateMessage(String unitId)
    {
        return null;
    }

    private static class TestPlugin3Controller extends ADevicePluginController
    {
        private TestPlugin3Controller(String unitID)
        {
            super(FAMILY, DEVICE_NAME, unitID);
        }

        @Override
        public void receiveMessage(IRaptorDataMessage rdm)
        {
        }

        @Override
        public boolean isShareable(String sourceDeviceFamily, String sourceDeviceType, String sourceDeviceId, String passkey)
        {
            return FAMILY.equals(sourceDeviceFamily);
        }
    }
}
