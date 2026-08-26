package gov.takx.core.layer;

import gov.tak.platform.client.eud.MapSources;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.core.globe.layers.mobac.CustomMobacTiledImageLayer;
import gov.takx.dataservices.persist.RaptorLayer;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.utils.CdiTestUtils;
import gov.takx.test.utils.SwingTestUtils;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

/**
 * Integration test class that targets imagery layer loading behavior via the {@link RaptorLayerManager} and
 * {@link LayerDefinitionSyncManager}.
 *
 * @since 5.0
 */
public class LayerDefinitionSyncManagerTest extends AIntegrationTest
{
    // These must match the current definitions in DefaultGlobeLayers.xml
    private static final Set<String> DEFAULT_MANAGED_LAYERS = Set.of(
            "USGS IMAGERY",
            "USGS IMAGERY TOPO",
            "NRL DRG",
            "NRL DRG MOSAIC",
            "NRL FAA SECTIONALS",
            "NRL FAA TACS",
            "NRL OPEN STREET MAP");

    private static final Set<String> TEST_MANAGED_LAYERS = Set.of("TAK LAYER 1", "TAK LAYER 2", "TAK LAYER 3");

    private static final Set<String> DEFAULT_AND_TEST_MANAGED_LAYERS =
            Stream.concat(DEFAULT_MANAGED_LAYERS.stream(), TEST_MANAGED_LAYERS.stream()).collect(Collectors.toSet());

    private final RaptorLayerManager raptorLayerManager;
    private final MapSources.Element[] mapSourceElements = new MapSources.Element[3];

    private LayerDefinitionSyncManager layerDefinitionSyncManagerSpy;

    public LayerDefinitionSyncManagerTest()
    {
        raptorLayerManager = getBean(RaptorLayerManager.class);

        // Use names that should never match any of the layers we have in DefaultGlobeLayers.xml
        mapSourceElements[0] = createMapSourceElement(1, "TAK LAYER 1", "/layers/mobac/NRL-OpenStreetMap.xml");
        mapSourceElements[1] = createMapSourceElement(2, "TAK LAYER 2", "/layers/mobac/USGSImageryOnly.xml");
        mapSourceElements[2] = createMapSourceElement(3, "TAK LAYER 3", "/layers/mobac/USGSImageryTopo.xml");
    }

    @BeforeEach
    public void setup() throws InterruptedException, URISyntaxException, IOException
    {
        configureMapSourcesMock();
        clearAllLayers();

        // Stop the layer manager service before each test so that each test has the opportunity to assert state before
        // and after the service has started.
        stopRaptorLayerManagerService();
    }

    @AfterEach
    public void teardown()
    {
        clearAllLayers();
    }

    /**
     * Clears all layers from the layer manager and delete them from persistence.
     */
    private void clearAllLayers()
    {
        raptorLayerManager.removeLayers(raptorLayerManager.getLayers(), false);
        raptorLayerManager.removeLayers(raptorLayerManager.getModelLayers(), false);

        getBean(IPersistServiceProvider.class).getPersistService().executeNativeUpdateSQL("delete from ClientLayer");
    }

    @Test
    public void layersCreatedFromValidConfigsLoadedFromMapSources()
    {
        startRaptorLayerManagerService(DEFAULT_AND_TEST_MANAGED_LAYERS, DEFAULT_MANAGED_LAYERS);
        validateExistenceOfMapSourceLayers(mapSourceElements.length);
    }

    @Test
    public void staleLayersRemovedWhenConfigsLoadedFromMapSources()
    {
        final String staleLayerNamePrefix = "Stale Layer ";
        final List<String> staleLayerNames = new ArrayList<>();
        final int numStaleLayers = 3;

        // Add the layers that will act as "stale" layers, and assert that those layers exist in the imagery layer list
        // given by the Raptor layer manager.
        for (int i = 0; i < numStaleLayers; i++)
        {
            final String staleLayerName = staleLayerNamePrefix + i;
            final IRaptorLayer raptorLayer =
                    raptorLayerManager.addLayer(staleLayerName, staleLayerName, CustomMobacTiledImageLayer.class.getName(),
                            false, false, null, LayerGroup.IMAGERY, null, null, false);

            ((RaptorLayer) raptorLayer).setSystemManaged();

            staleLayerNames.add(staleLayerName);
        }

        // Since the layer manager service was initially empty and hasn't been started yet, the only layers that should
        // be present are the "stale" layers that we just added.
        assertThat(raptorLayerManager.getLayerList(LayerGroup.IMAGERY))
                .hasSize(staleLayerNames.size())
                .extracting(IRaptorLayer::getName)
                .containsAll(staleLayerNames);

        // Start the layer manager service and assert that the "stale" layers were removed.
        final var expectedRemovedNames = new HashSet<>(DEFAULT_MANAGED_LAYERS);
        expectedRemovedNames.addAll(staleLayerNames);

        startRaptorLayerManagerService(null, expectedRemovedNames);
        validateExistenceOfMapSourceLayers(mapSourceElements.length);
    }

    @Test
    public void userCreatedLayersPersistBetweenProjectSessions() throws IOException
    {
        // Start the layer manager service first to mimic an open project session.
        startRaptorLayerManagerService(null, null);

        final String userCreatedLayerNamePrefix = "User-created Layer ";
        final List<String> userCreatedLayerNames = new ArrayList<>();
        final int numUserCreatedLayers = 3;

        // Add the layers that will act as user-created layers, and assert that those layers exist in the imagery layer list
        // given by the Raptor layer manager.
        for (int i = 0; i < numUserCreatedLayers; i++)
        {
            final String userCreatedLayerName = userCreatedLayerNamePrefix + i;

            raptorLayerManager.addLayer(userCreatedLayerName, userCreatedLayerName,
                    CustomMobacTiledImageLayer.class.getName(), false, false, null,
                    LayerGroup.IMAGERY, null, null, false, 0.0,
                    IRaptorLayer.DEFAULT_MIN_ACTIVE_ALTITUDE, IRaptorLayer.DEFAULT_MAX_ACTIVE_ALTITUDE);

            userCreatedLayerNames.add(userCreatedLayerName);
        }

        final List<IRaptorLayer> layersBeforeProjectRestart = raptorLayerManager.getLayerList(LayerGroup.IMAGERY);
        assertThat(layersBeforeProjectRestart).hasSize(mapSourceElements.length + numUserCreatedLayers);

        validateUserCreatedLayers(userCreatedLayerNames);

        stopRaptorLayerManagerService();

        // Restart the project session
        shutdown();
        reboot();

        configureMapSourcesMock();
        layerDefinitionSyncManagerSpy.syncImageryLayers();

        validateExistenceOfMapSourceLayers(mapSourceElements.length + numUserCreatedLayers);

        // Assert user created (non-system managed) layers are still in the imagery layer list.
        validateUserCreatedLayers(userCreatedLayerNames);
    }

    private void validateUserCreatedLayers(List<String> userCreatedLayerNames)
    {
        final var userCreatedLayers = raptorLayerManager.getLayerList(LayerGroup.IMAGERY).stream()
                .filter(layer -> !((RaptorLayer) layer).isSystemManaged())
                .toList();

        assertThat(userCreatedLayers)
                .extracting(IRaptorLayer::getName)
                .containsExactlyInAnyOrderElementsOf(userCreatedLayerNames);
    }

    @Test
    @SuppressLogging(LayerDefinitionSyncManager.class)
    public void defaultLayersUnchangedWhenMapSourcesApiUnreachable() throws IOException
    {
        // Configure MapSources.get() to throw an IOException as a means of simulating an unreachable tak.gov.
        doThrow(new IOException()).when(layerDefinitionSyncManagerSpy).getMapSources();

        startRaptorLayerManagerService(DEFAULT_MANAGED_LAYERS, Set.of());

        // Assert that the default set of imagery layers are all present, and no others.
        assertThat(raptorLayerManager.getLayerList(LayerGroup.IMAGERY))
                .hasSize(DEFAULT_MANAGED_LAYERS.size())
                .extracting(IRaptorLayer::getName)
                .containsAll(DEFAULT_MANAGED_LAYERS);
    }

    /**
     * Configures {@link #mockedMapSources} so that the test map source elements are returned, thus allowing for
     * testing of map sources without needing to make network calls.
     *
     * @throws IOException If unable to read the bytes of a config file
     */
    private void configureMapSourcesMock() throws IOException
    {
        final LayerDefinitionSyncManager layerDefinitionSyncManager = getBean(LayerDefinitionSyncManager.class);
        layerDefinitionSyncManagerSpy = spy((LayerDefinitionSyncManager) CdiTestUtils.getInstance(layerDefinitionSyncManager));

        final MapSources mapSources = new MapSources();
        mapSources.elements = mapSourceElements;

        doReturn(mapSources).when(layerDefinitionSyncManagerSpy).getMapSources();

        for (MapSources.Element mapSourceElement : mapSourceElements)
        {
            doReturn(Files.readString(new File(mapSourceElement.file_src).toPath()))
                    .when(layerDefinitionSyncManagerSpy).getFile(eq(mapSourceElement.file_src));
        }
    }

    /**
     * Creates a map source element using the given id, name, and file source. Each parameter below represents a key/value
     * in the actual JSON response returned by the tak.gov Map Sources API.
     *
     * @param id The integer ID of the element to create
     * @param name The name of the element to create
     * @param resource The path to the config file resource
     * @return The created map source element
     */
    @SuppressWarnings("DataFlowIssue")
    private MapSources.Element createMapSourceElement(int id, String name, String resource)
    {
        final MapSources.Element element = new MapSources.Element();

        element.id = id;
        element.name = name;
        element.file_src = getClass().getResource(resource).getPath();

        return element;
    }

    /**
     * Starts the RaptorLayerManager service, asserting that expected layers were added/removed.
     */
    private void startRaptorLayerManagerService(@Nullable Set<String> expectedLayersAdded, @Nullable Set<String> expectedLayersRemoved)
    {
        // Register our test listener so that we know when the expected number of layers have been added and/or removed.
        final var listener = new TestRaptorLayerManagerListener(true);

        raptorLayerManager.addRaptorLayerManagerListener(listener);

        // Start the RaptorLayerManager service to trigger the loading of imagery from map sources.
        startService(RaptorLayerManager.class);
        layerDefinitionSyncManagerSpy.syncImageryLayers(); // Force this as it doesn't normally get called when under test

        SwingTestUtils.waitForEdtEvents();

        if (expectedLayersAdded != null) assertThat(listener.getLayerNamesAdded()).containsExactlyInAnyOrderElementsOf(expectedLayersAdded);
        if (expectedLayersRemoved != null) assertThat(listener.getLayerNamesRemoved()).containsExactlyInAnyOrderElementsOf(expectedLayersRemoved);

        raptorLayerManager.removeRaptorLayerManagerListener(listener);
    }

    /**
     * Stops the RaptorLayerManager service.
     */
    private void stopRaptorLayerManagerService()
    {
        // Ensure the RaptorLayerManager is stopped and has cleared all imagery layers.
        stopService(RaptorLayerManager.class);
        assertThat(raptorLayerManager.getLayerList(LayerGroup.IMAGERY)).isEmpty();
    }

    /**
     * Asserts that a given number of imagery layers are returned by the layer manager, and that a layer was created for
     * each map source element.
     *
     * @param expectedNumLayers The number of imagery layers that are expected to be returned by the layer manager
     */
    private void validateExistenceOfMapSourceLayers(int expectedNumLayers)
    {
        // Assert that the correct number of layers were added, and that a layer was created for each map source.
        final List<IRaptorLayer> imageryLayers = raptorLayerManager.getLayerList(LayerGroup.IMAGERY);
        assertThat(imageryLayers).hasSize(expectedNumLayers);

        imageryLayers.stream()
                .filter(layer -> ((RaptorLayer) layer).isSystemManaged())
                .map(layer -> getElement(layer.getName(), mapSourceElements))
                .forEach(element -> assertThat(element).isNotNull());
    }

    /**
     * Gets an element by name from an array of map source elements.
     *
     * @param name The name of the element to get
     * @param mapSourceElements The array of map source elements to search through
     * @return The map source element that has the given name, or null if no matching element is found
     */
    private MapSources.Element getElement(String name, MapSources.Element[] mapSourceElements)
    {
        for (MapSources.Element currentElement : mapSourceElements)
        {
            if (currentElement.name.equals(name)) return currentElement;
        }

        return null;
    }
}
