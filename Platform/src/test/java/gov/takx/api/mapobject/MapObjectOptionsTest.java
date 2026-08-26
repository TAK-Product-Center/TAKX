package gov.takx.api.mapobject;

import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.gateway.IDeviceGateway;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.plugin.gateway.IGatewayController;
import gov.takx.core.ioservice.DataDelegate;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.mock.mapobject.TestMapObjectManagerListener;
import gov.takx.test.mock.plugin.device.TestDevicePluginWithOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to validate that basic Map Object Options are configured/maintained across various APIs.
 *
 * @since 2.29
 */
public class MapObjectOptionsTest extends AIntegrationTest
{
    private static final String FAMILY = TestDevicePluginWithOptions.FAMILY;
    private static final String TYPE = TestDevicePluginWithOptions.TYPE;

    private static final MapObjectOption[] SPECIFIC_OPTIONS_ARRAY = {MapObjectOption.NoStyledTracks, MapObjectOption.NoEventLogs};
    private static final MapObjectOptions SPECIFIC_OPTIONS = new MapObjectOptions(SPECIFIC_OPTIONS_ARRAY);
    private final TestMapObjectManagerListener listener = new TestMapObjectManagerListener();

    private IMapObjectManager mapObjectManager;
    private final DataDelegate dataDelegate = new MyDataDelegate();
    private int currentUnitId = 0;
    private IMapEntity mapEntity;

    public MapObjectOptionsTest()
    {
        setPluginsToRegister(TestDevicePluginWithOptions.class);
    }

    @Override
    protected void preLoadState()
    {
        mapObjectManager = getBean(IMapObjectManager.class);
        mapObjectManager.addMapObjectManagerListener(listener);
    }

    @Override
    protected void postProjectOpen()
    {
        mapEntity = dataDelegate.getOrCreateMapEntity(FAMILY, TYPE, Collections.emptyList());
        assertThat(mapEntity).isNotNull();
    }

    @AfterAll
    public void cleanupAfterTest()
    {
        deleteAllMapObjects();

        // May be null if this test was skipped
        if (mapObjectManager != null) mapObjectManager.removeMapObjectManagerListener(listener);
    }

    /**
     * Validate that MOs created via the gateway controller (by processing a message) have the options specified
     * in the plugin definition.
     */
    @Test
    public void testGatewayController()
    {
        // Create the gateway controller and send a message (which forces MO creation)
        final String unitId = nextUnitId();
        final IGatewayController controller = getBean(IDeviceGateway.class).getController(FAMILY, TYPE, unitId);
        final IRaptorDataStructure structure = DataStructureBuilder.createManualLocationStructure(FAMILY, TYPE);
        final IPrePersistRaptorDataMessage rdm = structure.createRaptorDataMessage(unitId);

        validateDefaultOptions(() -> controller.processParserMessage(rdm));
    }

    /**
     * Validate that a MO created via the DataDelegate with no options specified results in a MO with the options
     * specified in the plugin definition.
     */
    @Test
    public void testDataDelegateWithDefaultOptions()
    {
        validateDefaultOptions(() -> dataDelegate.getOrCreateMapObject(MapObjectSpec.builder(mapEntity, nextUnitId()).build()));
    }

    /**
     * Validate that a MO created via the DataDelegate with specified options results in a MO with those options.
     */
    @Test
    public void testDataDelegateWithSpecificOptions()
    {
        final MapObjectSpec spec = MapObjectSpec.builder(mapEntity, nextUnitId()).mapObjectOptions(SPECIFIC_OPTIONS).build();
        validateSpecificOptions(() -> dataDelegate.getOrCreateMapObject(spec));
    }

    /**
     * Validate that a MO created via the MapObjectManager with no options specified results in a MO with the options
     * specified in the plugin definition.
     */
    @Test
    public void testMapObjectManagerWithDefaultOptions()
    {
        validateDefaultOptions(() -> mapObjectManager.getOrCreateMapObject(MapObjectSpec.builder(mapEntity, nextUnitId()).build()));
    }

    /**
     * Validate that a MO created via the MapObjectManager with specified options results in a MO with those options.
     */
    @Test
    public void testMapObjectManagerWithSpecificOptions()
    {
        final MapObjectSpec spec = MapObjectSpec.builder(mapEntity, nextUnitId())
                .mapObjectOptions(SPECIFIC_OPTIONS).build();
        validateSpecificOptions(() -> mapObjectManager.getOrCreateMapObject(spec));
    }

    /**
     * Verify that a new MO was created and that the MO Options are the expected defaults from the plugin.
     *
     * @param creator Runnable to cause MO creation
     */
    private void validateDefaultOptions(Runnable creator)
    {
        final IMapObject previousLastMO = listener.getLastMapObjectAdded();

        creator.run();

        final IMapObject lastMapObjectAdded = listener.getLastMapObjectAdded();

        assertThat(lastMapObjectAdded).isNotSameAs(previousLastMO); // Ensure a new MO was added

        assertThat(lastMapObjectAdded.canHaveAlarms()).isFalse();
        assertThat(lastMapObjectAdded.shouldShowInExplorer()).isFalse();

        // And ensure no other options are set
        assertThat(lastMapObjectAdded.getMapObjectOptions().toList())
                .containsOnly(TestDevicePluginWithOptions.OPTIONS);
    }

    /**
     * Verify that a new MO was created and that the MO Options are the expected defaults from the plugin.
     *
     * @param creator Runnable to cause MO creation
     */
    private void validateSpecificOptions(Runnable creator)
    {
        final IMapObject previousLastMO = listener.getLastMapObjectAdded();

        creator.run();

        final IMapObject lastMapObjectAdded = listener.getLastMapObjectAdded();

        assertThat(lastMapObjectAdded).isNotSameAs(previousLastMO); // Ensure a new MO was added

        assertThat(lastMapObjectAdded.canHaveStyledTrackData()).isFalse();
        assertThat(lastMapObjectAdded.shouldCreateEventLogEntries()).isFalse();

        // And ensure no other options are set
        assertThat(lastMapObjectAdded.getMapObjectOptions().toList())
                .containsOnly(SPECIFIC_OPTIONS_ARRAY);
    }

    /**
     * @return Next unit Id to use
     */
    private String nextUnitId()
    {
        return "unit" + currentUnitId++;
    }

    /**
     * Need a concrete data delegate.
     */
    private static class MyDataDelegate extends DataDelegate
    {
        protected MyDataDelegate()
        {
            super(TestDevicePluginWithOptions.FAMILY, TestDevicePluginWithOptions.TYPE);
        }

        @Override
        public IVirtualFileSystem getVirtualFileSystem(VFSScope scope)
        {
            return null;
        }
    }
}
