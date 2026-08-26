package gov.takx.core.playback;

import gov.takx.api.gateway.IDeviceGateway;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.playback.ITimeSpan;
import gov.takx.api.plugin.gateway.IGatewayController;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.playback.TimeSpan;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.mock.plugin.device.TestDevicePlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 1.48
 */
public abstract class APlaybackIntegrationTest extends AIntegrationTest
{
    private TestDevicePlugin plugin;

    protected final ITimeSpan timeSpan = new TimeSpan();

    protected static final String UNIT_ID_1 = "1";
    protected final String mapObject1Key = KeyUtils.getUniqueKey(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME, UNIT_ID_1);
    protected IMapObject mapObject1;
    protected IGatewayController gc1;

    protected static final String UNIT_ID_2 = "2";
    protected final String mapObject2Key = KeyUtils.getUniqueKey(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME, UNIT_ID_2);
    protected IMapObject mapObject2;
    protected IGatewayController gc2;

    protected final Map<String, IGatewayController> gcMap = new HashMap<>();

    /**
     * Construct with a registered plugin so we can use it to send messages.
     */
    protected APlaybackIntegrationTest()
    {
        setPluginsToRegister(TestDevicePlugin.class);

        timeSpan.setTimes(0, 5000); // 10 second time span, contains first 5 messages
    }

    @Override
    protected void postProjectOpen()
    {
        // Bring the plugin online
        final IMapObjectManager mom = getBean(IMapObjectManager.class);
        final IDeviceGateway gateway = getBean(IDeviceGateway.class);

        plugin = getPluginInstance(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME);

        // Bring MO 1 online and add messages
        gc1 = gateway.getController(plugin.getFamily(), plugin.getType(), UNIT_ID_1);
        gcMap.put(UNIT_ID_1, gc1);

        // Load our data
        long time = 1000;

        for (int i = 0; i < 10; i++)
        {
            sendMessage(UNIT_ID_1, time);
            time += 1000;
        }

        mapObject1 = mom.getMapObject(mapObject1Key);

        gc2 = gateway.getController(plugin.getFamily(), plugin.getType(), UNIT_ID_2);
        gcMap.put(UNIT_ID_2, gc2);

        // Load our data
        time = 10000;

        for (int i = 0; i < 10; i++)
        {
            sendMessage(UNIT_ID_2, time);
            time += 1000;
        }

        mapObject2 = mom.getMapObject(mapObject2Key);
    }

    protected void sendMessage(String unitId, long time)
    {
        final IPrePersistRaptorDataMessage rdm = plugin.generateMessage(unitId);
        rdm.setTime(time);
        gcMap.get(unitId).processParserMessage(rdm);
    }
}
