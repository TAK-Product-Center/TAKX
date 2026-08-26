package gov.takx.core.mapobject;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.mapobject.CreationFailedException;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.IStructure;
import gov.takx.api.plugin.synthetic.ISyntheticPluginDefinition;
import gov.takx.commons.utils.UUIDUtils;
import gov.takx.core.registry.SyntheticPluginRegistry;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.mock.plugin.device.TestDevicePlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test {@link MapObjectManager} functionality
 */
public class MapObjectManagerTest extends AIntegrationTest
{
    private TestDevicePlugin plugin;
    private IMapObjectManager mapObjectManager;
    private SyntheticPluginRegistry syntheticPluginRegistry;

    public MapObjectManagerTest()
    {
        setPluginsToRegister(TestDevicePlugin.class);
    }

    @BeforeAll
    public void beforeClass()
    {
        mapObjectManager = getBean(IMapObjectManager.class);
        syntheticPluginRegistry = getBean(SyntheticPluginRegistry.class);
    }

    @Override
    protected void preLoadState()
    {
        plugin = getPluginInstance(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME);
    }

    /**
     * Ensure that {@link MapObjectManager#getOrCreateMapObject} executes properly when invoked by multiple concurrent threads.
     * <p/>
     * Note: This test is not a guarantee (since you can't prove a negative), but in testing it has never failed
     * to catch failures with concurrent access.
     *
     * @since 5.2
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void getOrCreateMapObject_whenRunConcurrently_succeeds() throws InterruptedException
    {
        final AtomicBoolean failure = new AtomicBoolean(false);
        final ThreadLocal<Boolean> threadFailure = ThreadLocal.withInitial(() -> false);
        final Set<String> unexpectedExceptions = ConcurrentHashMap.newKeySet();
        final ExecutorService service = Executors.newFixedThreadPool(5);
        final Set<IMapObject> created = ConcurrentHashMap.newKeySet();

        final IMapEntity mapEntity = mapObjectManager.getOrCreateMapEntity("concurrent", "test");

        for (int i = 0; i < 100; i++)
        {
            final int unitId = i / 10; // This should result in multiple threads trying to create the same MO
            final MapObjectSpec spec = MapObjectSpec.builder(mapEntity, Integer.toString(unitId)).build();

            final var ignored = service.submit(() -> {
                if (threadFailure.get()) return; // Skp once we have our fist failure

                try
                {
                    created.add(mapObjectManager.getOrCreateMapObject(spec));
                } catch (Exception e)
                {
                    threadFailure.set(true);
                    failure.set(true);

                    // No exceptions other than CreationFailedException should be thrown
                    if (!(e instanceof CreationFailedException)) unexpectedExceptions.add(e.getClass().getSimpleName());
                }
            });
        }

        service.shutdown();
        service.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(unexpectedExceptions).isEmpty();
        assertThat(failure).withFailMessage("Concurrent creation failed").isFalse();
        assertThat(created).hasSize(10);
    }

    /**
     * test ImapObjectManager setVisible method, both visible and not, validate with IMapObject
     */
    @Test
    public void testSetVisible()
    {
        ILocation location = new RaptorLocation(20, 30, false, 0);

        MapObjectSpec spec = MapObjectSpec.builder(plugin.getFamily(), plugin.getType()).location(location).build();
        IMapObject mapObject = mapObjectManager.getOrCreateMapObject(spec);

        mapObjectManager.setVisible(mapObject.getKey(), false);
        assertThat(mapObject.isVisible()).isFalse();

        mapObjectManager.setVisible(mapObject.getKey(), true);
        assertThat(mapObject.isVisible()).isTrue();
    }

    /**
     * test getPluginDefinition(), check optionalDefinition returned for empty and !empty cases
     */
    @Test
    public void testGetPluginDefinition()
    {
        String family = mapObjectManager.generateUniqueName("abc");
        ISyntheticPluginDefinition pluginDefinition = pluginDefinitionFactory.syntheticDeviceDefinitionFor(family, plugin.getType());
        assertThat(pluginDefinition.getFamily()).isEqualTo(family);

        // first assert definition is empty as unique plugin has not been registered/added,
        // this also adds cvg for last lines in GetPluginDefinition method
        assertThat(mapObjectManager.getPluginDefinition(family, plugin.getType())).isEmpty();

        // now register it
        assertThat(syntheticPluginRegistry.addPlugin(pluginDefinition)).isTrue();

        // assert definition is not empty as plugin has been added
        assertThat(mapObjectManager.getPluginDefinition(family, plugin.getType())).isPresent();
    }

    /**
     * test getSelectedMapObject, setup test with setSelected()
     */
    @Test
    public void testGetSelected()
    {
        ILocation location = new RaptorLocation(20, 30, false, 0);

        MapObjectSpec spec = MapObjectSpec.builder(plugin.getFamily(), plugin.getType()).location(location).build();
        IMapObject mapObject = mapObjectManager.getOrCreateMapObject(spec);

        mapObjectManager.setSelected(mapObject.getKey());
        assertThat(mapObject).isSameAs(mapObjectManager.getSelectedMapObject());
    }

    /**
     * test createDevice() in isolation, with longer strings, validate its returned boolean
     * also uses zero lat, long for variety in parameters and elevation 1.
     */
    @Test
    public void testCreateDeviceWithZeroLatLon()
    {
        ISyntheticPluginDefinition definition = pluginDefinitionFactory.syntheticDeviceDefinitionFor("abcdef ghijklm nopqrst uvwxyz",
                "TEST_STRUCTURE_A1 ADDITIONAL_TEXT_ST_A2 ADDITIONAL_TEXT_ST_A3");

        assertThat(mapObjectManager.createDevice(definition, new RaptorLocation(0, 0, true, 1))).isTrue();
    }

    /**
     * test isVisibilityLocked, setup uses setVisible to force lock setting and vis setting
     */
    @Test
    public void testisVisibilityLocked()
    {
        // test if map object is visible and locked
        ILocation location = new RaptorLocation(20, 30, false, 0);

        MapObjectSpec spec = MapObjectSpec.builder(plugin.getFamily(), plugin.getType()).location(location).build();
        IMapObject mapObject = mapObjectManager.getOrCreateMapObject(spec);

        mapObjectManager.setVisible(mapObject.getKey(), false, true);
        assertThat(mapObjectManager.isVisibilityLocked(mapObject.getKey())).isTrue();
    }

    @Test
    public void testCreationOfMapObjectWithLatLon()
    {
        final String unitId = UUIDUtils.generateType1UUID().toString();
        final ILocation location = new RaptorLocation(20, 30, false, 0);

        final MapObjectSpec spec = MapObjectSpec.builder(plugin.getFamily(), plugin.getType(), unitId)
                .location(location).build();
        final IMapObject mapObject = mapObjectManager.getOrCreateMapObject(spec);

        assertThat(mapObject.getFamily()).isEqualTo(plugin.getFamily());
        assertThat(mapObject.getType()).isEqualTo(plugin.getType());
        assertThat(mapObject.getUnitID()).isEqualTo(unitId);
        assertThat(mapObject.getLocation().sameAs(location)).isTrue();
//        TODO: Add this verify if group is needed on the API and adjust test accordingly
//        assertThat(mapObject.getGroupName()).isEqualTo(group);
    }

    @Test
    public void testCreationOfMapObjectWithLatLonElevation()
    {
        final String unitId = UUIDUtils.generateType1UUID().toString();
        final ILocation location = new RaptorLocation(20, 30, true, 45);

        final MapObjectSpec spec = MapObjectSpec.builder(plugin.getFamily(), plugin.getType(), unitId)
                .location(location).build();
        final IMapObject mapObject = mapObjectManager.getOrCreateMapObject(spec);

        assertThat(mapObject.getFamily()).isEqualTo(plugin.getFamily());
        assertThat(mapObject.getType()).isEqualTo(plugin.getType());
        assertThat(mapObject.getUnitID()).isEqualTo(unitId);
        assertThat(mapObject.getLocation().sameAs(location)).isTrue();
//        TODO: Add this verify if group is needed on the API and adjust test accordingly
//        assertThat(controller.getMapObject().getGroupName()).isEqualTo(group);
    }

    @Test
    public void getOrCreateMapEntity_whenNotExist_createsMapEntity()
    {
        // Use a unique "type" to ensure test order doesn't matter
        final String type = "type" + System.currentTimeMillis();

        final IRaptorDataStructure s1 = new DataStructureBuilder("TEST", type, "1.0", "M1").build();
        final IRaptorDataStructure s2 = new DataStructureBuilder("TEST", type, "1.0", "M2").build();

        final IMapEntity mapEntity = mapObjectManager.getOrCreateMapEntity("TEST", type, List.of(s1, s2));

        // Newly created MapEntity should have our given structures plus the manual location structure
        assertThat(mapEntity.getDataStructures()).hasSize(3)
                .map(IStructure::getMessageType)
                .containsExactlyInAnyOrder(DataStructureBuilder.LOCATION_MESSAGE_TYPE, "M1", "M2");
    }

    @Test
    public void getOrCreateMapEntity_whenExist_updatesMapEntity()
    {
        // Use a unique "type" to ensure test order doesn't matter
        final String type = "type" + System.currentTimeMillis();

        final IRaptorDataStructure s1 = new DataStructureBuilder("TEST", type, "1.0", "M1").build();
        final IRaptorDataStructure s2 = new DataStructureBuilder("TEST", type, "1.0", "M2").build();

        final IMapEntity mapEntity = mapObjectManager.getOrCreateMapEntity("TEST", type, List.of(s1, s2));

        // Newly created MapEntity should have our given structures plus the manual location structure
        assertThat(mapEntity.getDataStructures()).hasSize(3)
                .map(IStructure::getMessageType)
                .containsExactlyInAnyOrder(DataStructureBuilder.LOCATION_MESSAGE_TYPE, "M1", "M2");

        // This should add s3 to the existing MapEntity's structure list
        final IRaptorDataStructure s3 = new DataStructureBuilder("TEST", type, "1.0", "M3").build();
        final IMapEntity foundMapEntity = mapObjectManager.getOrCreateMapEntity("TEST", type, List.of(s1, s2, s3));

        assertThat(mapEntity).isSameAs(foundMapEntity);
        assertThat(foundMapEntity.getDataStructures()).hasSize(4)
                .map(IStructure::getMessageType)
                .containsExactlyInAnyOrder(DataStructureBuilder.LOCATION_MESSAGE_TYPE, "M1", "M2", "M3");
    }
}
