package gov.takx.test.mock.plugin.device;

import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.plugin.annotations.DevicePlugin;
import gov.takx.api.plugin.annotations.PluginDescriptor;
import gov.takx.api.plugin.annotations.PluginSpecification;
import gov.takx.api.plugin.device.IDevicePluginController;
import gov.takx.api.plugin.gateway.GenericGatewayController;
import gov.takx.api.plugin.gateway.IGatewayController;
import gov.takx.api.plugin.gateway.IGatewayControllerDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * A simple plugin used for integration testing. It can generate data messages and push them into the core.
 *
 */
@DevicePlugin(
        spec = @PluginSpecification(
                family = ATestDevicePlugin.FAMILY,
                type = TestDevicePlugin.DEVICE_NAME,
                version = TestDevicePlugin.VERSION,
                iconPath = "icons/32x32.png"),
        descriptor = @PluginDescriptor(
                name = TestDevicePlugin.DEVICE_NAME))
@SuppressWarnings("Duplicates")
public class TestDevicePlugin extends ATestDevicePlugin
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String DEVICE_NAME = "TestPlugin";
    public static final String STRUCTURE_NAME = "TEST_STRUCTURE_P1";
    public static final String VERSION = "3.0.0";

    private boolean deleteCalled = false;

    /**
     * Construct - initialize message structure.
     */
    public TestDevicePlugin()
    {
        super(STRUCTURE_NAME);
        initStructures1();
    }

    @Override
    protected IDevicePluginController makeDeviceController(String unitID)
    {
        logger.debug("Construct TestPluginController for UnitId: {}", unitID);

        return new TestDevicePluginController(unitID);
    }

    @Override
    public IGatewayController createGatewayController(String unitID, IGatewayControllerDelegate delegate)
    {
        return new TestPluginGatewayController(unitID, delegate);
    }

    @Override
    public void deleteDevice(String unitID)
    {
        super.deleteDevice(unitID);
        deleteCalled = true;
    }

    public boolean isDeleteCalled()
    {
        return deleteCalled;
    }

    private static class TestPluginGatewayController extends GenericGatewayController
    {
        private TestPluginGatewayController(String unitID, IGatewayControllerDelegate delegate)
        {
            super(FAMILY, DEVICE_NAME, unitID, delegate);
        }

        @Override
        public void processParserMessage(IPrePersistRaptorDataMessage message)
        {
            if (message.getType().equals(DEVICE_NAME))
            {
                super.processParserMessage(message);
            } else
            {
                try
                {
                    delegate.sendToDevice(message, true, "");
                } catch (IllegalAccessException e)
                {
                    logger.error("Error sending RDM across plugin boundary", e);
                }
            }
        }
    }
}
