package gov.takx.data;

import gov.takx.api.dataportal.IRaptorDataListener;
import gov.takx.api.dataportal.IRaptorDataPortal;
import gov.takx.api.dataportal.IRaptorDataPortalController;
import gov.takx.api.gateway.IDeviceGateway;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IMapObjectOfflineMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.plugin.gateway.IGatewayController;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.extension.DependsOn;
import gov.takx.test.extension.DependsOnOrdering;
import gov.takx.test.mock.plugin.device.TestDevicePlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test functionality of the DataPortal and DataPortalController listeners.
 *
 * @since 3.0
 */
@TestMethodOrder(DependsOnOrdering.class)
public class DataPortalListenerTest extends AIntegrationTest
{
    private static final String UNIT_ID_1 = "1";
    private static final String UNIT_ID_2 = "2";

    private static final String ON_DATA_MESSAGE = "onDataMessage";
    private static final String ON_DISCONNECT = "onDisconnect";
    private static final String ON_MAP_OBJECT_ADDED = "onMapObjectAdded";
    private static final String ON_MAP_OBJECT_DEFINED = "onMapObjectDefined";
    private static final String ON_MAP_OBJECT_OFFLINE = "onMapObjectOffline";

    private final DataListener lowDataListener = new DataListener();
    private final DataListener midDataListener = new DataListener();
    private final DataListener highDataListener = new DataListener();

    private TestDevicePlugin plugin;

    public DataPortalListenerTest()
    {
        setPluginsToRegister(TestDevicePlugin.class);
    }

    /**
     * Register our listeners on the data portal.
     */
    @Override
    protected void preLoadState()
    {
        final IRaptorDataPortal dataPortal = getBean(IRaptorDataPortal.class);

        dataPortal.addListener(lowDataListener, IRaptorDataPortalController.LOW_PRIORITY);
        dataPortal.addListener(midDataListener);
        dataPortal.addListener(highDataListener, IRaptorDataPortalController.HIGH_PRIORITY);

        plugin = getPluginInstance(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME);
    }

    @AfterAll
    public void cleanupAfterTest()
    {
        deleteAllMapObjects();
    }

    @Test
    public void testMapEntityNotification()
    {
        // Verify the MapEntity notifications (they should have happened when the project opened)
        testTimesOnDataListeners(ON_MAP_OBJECT_DEFINED);
    }

    @Test
    @DependsOn("testMapEntityNotification")
    public void testMessageEvents()
    {
        final IGatewayController gc = getBean(IDeviceGateway.class).getController(plugin.getFamily(), plugin.getType(), UNIT_ID_1);

        // Send an RDM which will also cause the MO to be created
        gc.processParserMessage(plugin.generateMessage(UNIT_ID_1));

        testTimesOnDataListeners(ON_MAP_OBJECT_ADDED); // Verify the MO Added notifications
        testTimesOnDataListeners(ON_DATA_MESSAGE); // Verify the RDM notifications
    }

    @Test
    @DependsOn("testMessageEvents")
    public void testCreateSuppressDeleteMapObject()
    {
        final IMapObjectManager mapObjectManager = getBean(IMapObjectManager.class);

        resetDataListenerTimes();

        final MapObjectSpec spec = MapObjectSpec.builder(plugin.getFamily(), plugin.getType(), UNIT_ID_2).build();
        final IMapObject mapObject = mapObjectManager.getOrCreateMapObject(spec);

        testTimesOnDataListeners(ON_MAP_OBJECT_ADDED); // Verify the MO Added notifications

        // Test delete
        resetDataListenerTimes();
        mapObjectManager.deleteMapObject(mapObject); // Delete that MO
        testTimesOnDataListeners(ON_MAP_OBJECT_OFFLINE); // Verify the MO Offline notifications
    }

    @Test
    @DependsOn("testCreateSuppressDeleteMapObject")
    public void testShutdownEvents()
    {
        shutdown();

        testTimesOnDataListeners(ON_DISCONNECT);
    }

    /**
     * Validate the times for the given event on the data listeners.
     */
    private void testTimesOnDataListeners(String eventName)
    {
        testTimesOnListeners(eventName, lowDataListener, midDataListener, highDataListener);
    }

    /**
     * Validate that the given event time has been set (non-zero) and that the timing of receipt is in the proper order.
     */
    private void testTimesOnListeners(String eventName, TimingListener low, TimingListener mid, TimingListener high)
    {
        final long lowTime = low.getTime(eventName);
        final long midTime = mid.getTime(eventName);
        final long highTime = high.getTime(eventName);

        assertThat(lowTime).isGreaterThan(0);
        assertThat(midTime).isGreaterThan(0);
        assertThat(highTime).isGreaterThan(0);

        assertThat(lowTime).isGreaterThan(midTime);
        assertThat(midTime).isGreaterThan(highTime);
    }

    private void resetDataListenerTimes()
    {
        lowDataListener.reset();
        midDataListener.reset();
        highDataListener.reset();
    }

    /**
     * Data listener to record the time of receipt on all events.
     */
    private static final class DataListener extends TimingListener implements IRaptorDataListener
    {
        @Override
        public boolean matchMapObject(IMapObject mapObject)
        {
            return true;
        }

        @Override
        public void onMapObjectDefined(IMapEntity mapEntity)
        {
            setTime(ON_MAP_OBJECT_DEFINED);
        }

        @Override
        public void onMapObjectAdded(IMapObject mapObject, IRaptorDataPortalController controller)
        {
            setTime(ON_MAP_OBJECT_ADDED);
        }

        @Override
        public void onDataMessage(String topic, IRaptorDataMessage msg, IRaptorDataStructure struct)
        {
            setTime(ON_DATA_MESSAGE);
        }

        @Override
        public void onDisconnect(IRaptorDataPortalController controller)
        {
            setTime(ON_DISCONNECT);
        }

        @Override
        public void onMapObjectOffline(IMapObjectOfflineMessage msg, IRaptorDataPortalController controller, boolean wipe)
        {
            setTime(ON_MAP_OBJECT_OFFLINE);
        }
    }

    /**
     * Base class to support recording the time of event notification.
     */
    private static abstract class TimingListener
    {
        final Map<String, Long> times = new HashMap<>();

        void setTime(String eventName)
        {
            times.put(eventName, System.nanoTime());
        }

        long getTime(String eventName)
        {
            return times.getOrDefault(eventName, 0L);
        }

        void reset()
        {
            times.clear();
        }
    }
}
