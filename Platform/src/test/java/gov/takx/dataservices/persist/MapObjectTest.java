package gov.takx.dataservices.persist;

import gov.tak.api.maps.MapItem;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.core.persist.MapObjectPersist;
import gov.takx.platform.KeyUtils;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.mock.mapobject.TestMapObjectPropertyChangeListener;
import gov.takx.test.mock.plugin.device.TestDevicePlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MapObject unit-level tests. Note: there already exists a separate MapObjectTest, but it seems to be for integration
 * tests, and this one needs to be in the same package as MapObject to be able to easily test protected methods.
 *
 * @since 3.0
 */
public class MapObjectTest extends AIntegrationTest
{
    private IMapObjectManager mapObjectManager;

    @Override
    protected void postProjectOpen()
    {
        mapObjectManager = getBean(IMapObjectManager.class);
    }

    @AfterEach
    public void cleanupAfterMethod()
    {
        deleteAllMapObjects();
    }

    @SuppressLogging({MapObject.class, MapObjectPersist.class})
    @Test
    public void test_find_map_object_by_id()
    {
        final MapObject mapObject = (MapObject) createMapObject("main");

        final IMapObject mo123 = createMapObject("123");

        IIconMapObject foundIconMO = mapObject.findMapObjectById(mo123.getId(), "test", IIconMapObject.class);
        assertThat(foundIconMO).isNotNull().isSameAs(mo123);

        // Return null on not found
        foundIconMO = mapObject.findMapObjectById(123456789, "test", IIconMapObject.class);
        assertThat(foundIconMO).isNull();

        // Return null on null id
        foundIconMO = mapObject.findMapObjectById(null, "test", IIconMapObject.class);
        assertThat(foundIconMO).isNull();

        // Requesting the wrong type should return null
        final IRouteMapObject foundRouteMO = mapObject.findMapObjectById(mo123.getId(), "test", IRouteMapObject.class);
        assertThat(foundRouteMO).isNull();
    }

    @SuppressLogging(MapObject.class)
    @Test
    public void test_find_map_object_by_key()
    {
        final MapObject mapObject = (MapObject) createMapObject("main");

        final IMapObject mo123 = createMapObject("123");

        IIconMapObject foundIconMO = mapObject.findMapObjectByKey(mo123.getKey(), "test", IIconMapObject.class);
        assertThat(foundIconMO).isNotNull().isSameAs(mo123);

        // Return null on not found
        foundIconMO = mapObject.findMapObjectByKey("random/key/123", "test", IIconMapObject.class);
        assertThat(foundIconMO).isNull();

        // Return null on null key
        foundIconMO = mapObject.findMapObjectByKey(null, "test", IIconMapObject.class);
        assertThat(foundIconMO).isNull();

        // Requesting the wrong type should return null
        final IRouteMapObject foundRouteMO = mapObject.findMapObjectByKey(mo123.getKey(), "test", IRouteMapObject.class);
        assertThat(foundRouteMO).isNull();
    }

    @Test
    public void test_encode_and_find_map_objects_by_csv_ids()
    {
        final MapObject mapObject = (MapObject) createMapObject("main");

        List<IMapObject> mapObjects = Arrays.asList(createMapObject("1"), createMapObject("2"), createMapObject("3"));
        final String ids = mapObjects.stream().map(IMapObject::getId).map(id -> Integer.toString(id)).collect(Collectors.joining(","));
        assertThat(mapObjects).hasSize(3);

        final String csvIds = mapObject.encodeMapObjectsAsCsvIds(mapObjects);
        assertThat(csvIds).isNotEmpty().isEqualTo(ids);

        final List<IMapObject> foundMapObjects = mapObject.findMapObjectsByCsvIds(csvIds);
        assertThat(foundMapObjects).hasSize(3).isEqualTo(mapObjects);
    }

    @SuppressLogging({MapObject.class, MapObjectPersist.class})
    @Test
    public void test_relink_map_object_calls_handler_when_found()
    {
        final MapObject mapObject = (MapObject) createMapObject("main");

        final IMapObject mo123 = createMapObject("123");

        final TestRelinkHandler handler = new TestRelinkHandler();
        mapObject.relinkMapObject(mo123.getId(), "test", handler);
        assertThat(handler.getCount()).isEqualTo(1);
        assertThat(handler.getConsumedMO()).isEqualTo(mo123);

        // Handler should not be called if not found
        handler.reset();
        mapObject.relinkMapObject(123456789, "test", handler);
        assertThat(handler.getCount()).isEqualTo(0);

        // Handler should not be called if provided id was null
        handler.reset();
        mapObject.relinkMapObject(null, "test", handler);
        assertThat(handler.getCount()).isEqualTo(0);
    }

    @Test
    void whenDeleted_deletionHandlersAreCalled()
    {
        final var mapObject = createMapObject("forDeletion");
        final var handler1Called = new AtomicBoolean(false);
        final var handler2Called = new AtomicBoolean(false);

        mapObject.addDeletionHandler(m -> handler1Called.set(true));
        mapObject.addDeletionHandler(m -> handler2Called.set(true));
        mapObjectManager.deleteMapObject(mapObject);
        assertThat(handler1Called.get()).isTrue();
        assertThat(handler2Called.get()).isTrue();
    }

    @Test
    void whenDeleted_removedHandlerIsNotCalled()
    {
        final var mapObject = createMapObject("forDeletion");
        final var handler1Called = new AtomicBoolean(false);
        final var handler2Called = new AtomicBoolean(false);
        final Consumer<MapItem> handler1 = m -> handler1Called.set(true);

        mapObject.addDeletionHandler(handler1);
        mapObject.addDeletionHandler(m -> handler2Called.set(true));
        mapObject.removeDeletionHandler(handler1);

        mapObjectManager.deleteMapObject(mapObject);
        assertThat(handler1Called.get()).isFalse();
        assertThat(handler2Called.get()).isTrue();
    }

    /**
     * Test that a call in to change the location with a specific time results in the most recent communication time
     * for the map object to change.  The time should change whether the actual location changes or not.
     * The exception to this rule is if the new location is a manual location update.  Also test that the map object
     * property change is fired appropriately.
     */
    @Test
    public void testMostRecentCommunicationTime()
    {
        final ILocation location = new RaptorLocation(10, 10);

        final MapObjectSpec spec = MapObjectSpec.builder(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME).location(location).build();
        final IMapObject mapObject = mapObjectManager.getOrCreateMapObject(spec);

        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        mapObject.addPropertyChangeListener(Collections.singletonList(IMapObject.MOST_RECENT_COMMUNICATION_TIME), listener);

        // Should start at -1
        assertThat(mapObject.getMostRecentCommunicationTime()).isEqualTo(-1);
        assertThat(listener.getReceivedEventTypes().size()).isEqualTo(0);

        // New non-manual location, PCL should be notified and time should be updated.
        mapObject.setLocation(new RaptorLocation(11, 11), 100, false);
        assertThat(mapObject.getMostRecentCommunicationTime()).isEqualTo(100);
        assertThat(listener.getCountForEventType(IMapObject.MOST_RECENT_COMMUNICATION_TIME)).isEqualTo(1);
        assertThat(listener.getReceivedEventTypes().size()).isEqualTo(1);

        // New location call, but in same location.  Time should still be updated and PCL notified.
        listener.clearEvents();
        mapObject.setLocation(new RaptorLocation(11, 11), 150, false);
        assertThat(mapObject.getMostRecentCommunicationTime()).isEqualTo(150);
        assertThat(listener.getCountForEventType(IMapObject.MOST_RECENT_COMMUNICATION_TIME)).isEqualTo(1);
        assertThat(listener.getReceivedEventTypes().size()).isEqualTo(1);

        // New location but a manual one.  No event should be fired and the time should be the same.
        listener.clearEvents();
        mapObject.setLocation(new RaptorLocation(12, 12), 200, true);
        assertThat(mapObject.getMostRecentCommunicationTime()).isEqualTo(150);
        assertThat(listener.getReceivedEventTypes().size()).isEqualTo(0);

        // New location but earlier in time.  Should not result in a PCE being fired or the most recent time being updated.
        listener.clearEvents();
        mapObject.setLocation(new RaptorLocation(13, 13), 100, false);
        assertThat(mapObject.getMostRecentCommunicationTime()).isEqualTo(150);
        assertThat(listener.getReceivedEventTypes().size()).isEqualTo(0);
    }

    /**
     * Test that the heading is updated and the appropriate events are fired.
     */
    @Test
    public void testSetHeading()
    {
        final ILocation location = new RaptorLocation(10, 10);

        final MapObjectSpec spec = MapObjectSpec.builder(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME).location(location).build();
        final IMapObject mapObject = mapObjectManager.getOrCreateMapObject(spec);

        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        mapObject.addPropertyChangeListener(IMapObject.HEADING, listener);

        // Should start without a heading
        assertThat(mapObject.hasHeading()).isFalse();
        assertThat(listener.getCountForEventType(IMapObject.HEADING)).isEqualTo(0);
        assertThat(listener.getReceivedEventTypes().size()).isEqualTo(0);

        double heading = 0;
        mapObject.setHeading(heading);
        assertThat(mapObject.hasHeading()).isTrue();
        assertThat(mapObject.getHeading()).isEqualTo(heading);
        assertThat(listener.getReceivedEventTypes().size()).isEqualTo(1);
        assertThat(listener.getCountForEventType(IMapObject.HEADING)).isEqualTo(1);

        // After the first time, if the value is the same, no events will fire.
        listener.clearEvents();
        mapObject.setHeading(heading);
        assertThat(listener.getReceivedEventTypes().size()).isEqualTo(0);

        // If the value is different, the heading event should fire
        listener.clearEvents();
        heading = 45;
        mapObject.setHeading(heading);
        assertThat(mapObject.hasHeading()).isTrue();
        assertThat(mapObject.getHeading()).isEqualTo(heading);
        assertThat(listener.getReceivedEventTypes().size()).isEqualTo(1);
        assertThat(listener.getCountForEventType(IMapObject.HEADING)).isEqualTo(1);

        // Verify that sending an RDM with a heading updates the MO
        heading = 99;
        final IPrePersistRaptorDataMessage locationRdm = mapObject.createLocationDataMessage();
        locationRdm.setHeading(heading);
        mapObject.getController().sendMessage(locationRdm);
        assertThat(mapObject.getHeading()).isEqualTo(heading);
    }

    /**
     * Test that the speed is updated and the appropriate events are fired.
     */
    @Test
    public void testSetSpeed()
    {
        final ILocation location = new RaptorLocation(10, 10);

        final MapObjectSpec spec = MapObjectSpec.builder(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME).location(location).build();
        final IMapObject mapObject = mapObjectManager.getOrCreateMapObject(spec);

        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        mapObject.addPropertyChangeListener(IMapObject.SPEED, listener);

        // Should start without a speed
        assertThat(mapObject.hasSpeed()).isFalse();
        assertThat(listener.getCountForEventType(IMapObject.SPEED)).isEqualTo(0);
        assertThat(listener.getReceivedEventTypes().size()).isEqualTo(0);

        double speed = 0;
        mapObject.setSpeedInMps(speed);
        assertThat(mapObject.hasSpeed()).isTrue();
        assertThat(mapObject.getSpeedInMps()).isEqualTo(speed);
        assertThat(listener.getReceivedEventTypes().size()).isEqualTo(1);
        assertThat(listener.getCountForEventType(IMapObject.SPEED)).isEqualTo(1);

        // After the first time, if the value is the same, no events will fire.
        listener.clearEvents();
        mapObject.setSpeedInMps(speed);
        assertThat(listener.getReceivedEventTypes().size()).isEqualTo(0);

        // If the value is different, the speed event should fire
        listener.clearEvents();
        speed = 45;
        mapObject.setSpeedInMps(speed);
        assertThat(mapObject.hasSpeed()).isTrue();
        assertThat(mapObject.getSpeedInMps()).isEqualTo(speed);
        assertThat(listener.getReceivedEventTypes().size()).isEqualTo(1);
        assertThat(listener.getCountForEventType(IMapObject.SPEED)).isEqualTo(1);

        // Verify that sending an RDM with a speed updates the MO
        speed = 99;
        final IPrePersistRaptorDataMessage locationRdm = mapObject.createLocationDataMessage();
        locationRdm.setSpeedInMps(speed);
        mapObject.getController().sendMessage(locationRdm);
        assertThat(mapObject.getSpeedInMps()).isEqualTo(speed);
    }

    /**
     * Create a red pushpin map object with the provided unit id.
     *
     * @param unitId The unit id for the map object
     * @return The created map object
     */
    private IMapObject createMapObject(String unitId)
    {
        final MapObjectSpec spec = MapObjectSpec.builder(KeyUtils.RESOURCE_ICON_FAMILY, "Red").uid(unitId).build();
        return mapObjectManager.getOrCreateMapObject(spec);
    }

    private static class TestRelinkHandler implements Consumer<IMapObject>
    {
        private int count = 0;
        private IMapObject consumedMO = null;

        @Override
        public void accept(IMapObject mapObject)
        {
            count++;
            consumedMO = mapObject;
        }

        /**
         * @return The number of times the handler was called
         */
        int getCount()
        {
            return count;
        }

        /**
         * @return The MO provided to the consumer
         */
        IMapObject getConsumedMO()
        {
            return consumedMO;
        }

        void reset()
        {
            count = 0;
            consumedMO = null;
        }
    }
}
