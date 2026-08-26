package gov.takx.core.layer;

import com.atakmap.filesystem.HashingUtils;
import com.atakmap.map.formats.cdn.StreamingTiles;
import com.atakmap.map.layer.raster.DatasetDescriptorFactory2;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Streams;
import gov.tak.platform.client.eud.EndPoints;
import gov.tak.platform.client.eud.MapSources;
import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.layer.ILayerService;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.LayerConfiguration;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.layer.builder.OnlineImageryLayerBuilder;
import gov.takx.api.layer.builder.StreamingElevationLayerBuilder;
import gov.takx.api.persist.ILayerPersist;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.dataservices.persist.RaptorLayer;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Responsible for loading imagery layer definitions from tak.gov and synchronizing them on project startup.
 *
 * @since 5.0 (name changed in 5.1)
 */
@ApplicationScoped
class LayerDefinitionSyncManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String CACHED_MAP_SOURCES = "cachedMapSources";
    private static final String DEFAULT_VISIBLE_LAYER = "USGS IMAGERY TOPO";

    // Timeout for reading Map Sources from tak.gov
    private static final Duration TIMEOUT_DURATION = Duration.ofSeconds(5);

    private static final String CONTENT_IMAGERY_TYPE = "imagery";
    private static final String CONTENT_TERRAIN_TYPE = "terrain";

    private final RaptorLayerManager raptorLayerManager;
    private final ICoreExecutorService executorService;
    private final ILayerPersist layerPersist;
    private final ILayerService layerService;
    private final boolean closedNetwork;
    private final boolean underTest;

    private final Path cachedMapSourcesDir;

    @Inject
    LayerDefinitionSyncManager(RaptorLayerManager raptorLayerManager, ICoreExecutorService executorService,
                               ILayerPersist layerPersist, ILayerService layerService,
                               @VFSManagerScope(VFSScope.Global) IVirtualFileSystemManager vfsManager,
                               @ProgramPreference(PreferenceEntry.CLOSED_NETWORK) boolean closedNetwork,
                               @PlatformUnderTest boolean underTest)
    {
        this.raptorLayerManager = raptorLayerManager;
        this.executorService = executorService;
        this.layerPersist = layerPersist;
        this.layerService = layerService;
        this.closedNetwork = closedNetwork;
        this.underTest = underTest;

        cachedMapSourcesDir = vfsManager.getVirtualFileSystem(CACHED_MAP_SOURCES).getRootDir().toPath();
    }

    /**
     * Adds imagery layers from the TAK Map Sources API, and removes layers that are no longer returned by the API
     * (assuming they are not user-created).  In the event that the Map Sources API is unreachable, the current
     * layer list is left unmodified.
     */
    void loadImageryLayers()
    {
        // Avoid trying to contact tak.gov while running tests or when running on a closed network
        if (!underTest && !closedNetwork) syncImageryLayers();
    }

    /**
     * Sync the imagery layers with tak.gov.
     * <p/>
     * This method is separated from the loadImageryLayers above so that it can be called directly during tests,
     * and otherwise is not invoked during the RaptorLayerManger startup when under test.
     *
     * @since 5.1
     */
    @VisibleForTesting
    void syncImageryLayers()
    {
        executorService.submit(() -> {
            Thread.currentThread().setName("Layer Definition Sync");

            final MapSources mapSources;
            try
            {
                logger.info("Syncing map sources with tak.gov");
                mapSources = getMapSources();
            } catch (IOException e)
            {
                final Throwable exceptionToLog = underTest ? null : e;
                logger.warn("Failed to retrieve map sources from tak.gov", exceptionToLog);
                return;
            }

            if (mapSources != null)
            {
                final var imageryLayers = raptorLayerManager.getLayerList(LayerGroup.IMAGERY);
                final var elevationLayers = raptorLayerManager.getLayerList(LayerGroup.ELEVATION);
                final var layerList = Streams.concat(imageryLayers.stream(), elevationLayers.stream())
                        .map(RaptorLayer.class::cast)
                        .toList();

                updateLayersFromMapSources(layerList, mapSources);
                removeStaleLayers(layerList, mapSources);
            }
        });
    }

    /**
     * This method exists so that it can be stubbed in tests.
     *
     * @return The parsed map sources. or {@code null} if they fail to parse
     * @since 5.1
     */
    @VisibleForTesting
    @Nullable
    MapSources getMapSources() throws IOException
    {
        final var mapSourcesJson = getFile(EndPoints.MAP_SOURCES);
        final var mapSources = MapSources.parse(mapSourcesJson);
        if (mapSources == null) logger.warn("Failed to parse tak.gov map sources: {}", mapSourcesJson);
        return mapSources;
    }

    /**
     * Add or update imagery and elevation layers according to the definitions in {@code mapSources}.
     *
     * @param existingLayers The list of existing layers (both imagery and elevation)
     * @param mapSources The map sources to create/update layers from
     */
    private void updateLayersFromMapSources(@Nonnull List<RaptorLayer> existingLayers, @Nonnull MapSources mapSources)
    {
        final var layersByKey = existingLayers.stream().collect(Collectors.toMap(IRaptorLayer::getKey, Function.identity()));

        // Create or update layers according to the new map source elements
        for (var mapSourceElement : mapSources.elements)
        {
            final String mapSourceName = mapSourceElement.name; // This is the layer key

            try
            {
                final String mapSourceConfig = getFile(mapSourceElement.file_src);
                final boolean hasChanged = updateCache(mapSourceElement.file_src, mapSourceConfig);

                if (layersByKey.containsKey(mapSourceName))
                {
                    final var existingLayer = layersByKey.get(mapSourceName);

                    // If the existing layer is not system managed, then we don't want to mess with it
                    if (!existingLayer.isSystemManaged())
                    {
                        logger.warn("Existing layer '{}' conflicts with tak.gov map source '{}'; skipping update", existingLayer, mapSourceName);
                    } else if (hasChanged)
                    {
                        recreateLayer(existingLayer, mapSourceConfig);
                    }
                } else
                {
                    createLayer(mapSourceName, mapSourceConfig, true);
                }
            } catch (IOException e)
            {
                final Throwable exceptionToLog = underTest ? null : e;
                logger.warn("Failed to sync map source '{}' with tak.gov", mapSourceName, exceptionToLog);
            }
        }
    }

    /**
     * Removes stale layers from the layer manager. A layer is considered to be "stale" if it is system managed and a
     * corresponding map source cannot be found for it.
     *
     * @param existingLayers The list of existing layers
     * @param mapSources The map sources to compare against when deciding if an existing layer is stale
     */
    private void removeStaleLayers(List<RaptorLayer> existingLayers, MapSources mapSources)
    {
        final var mapSourceNames = Stream.of(mapSources.elements).map(element -> element.name).toList();

        existingLayers.stream()
                .filter(layer -> layer.isSystemManaged() && !mapSourceNames.contains(layer.getName()))
                .peek(layer -> logger.debug("Remove stale layer {}", layer))
                .forEach(layer -> raptorLayerManager.removeLayer(layer, false));
    }

    /**
     * Recreate a system managed layer, preserving these user-modifiable properties:
     * <ul>
     *     <li>Visible</li>
     *     <li>Display name</li>
     *     <li>Opacity</li>
     *     <li>Detail Hint (split scale)</li>
     *     <li>Visible altitude min/max</li>
     * </ul>
     *
     * @param existingLayer Existing layer being replaced
     * @param mapSourceConfig New map source configuration
     * @since 5.6
     */
    private void recreateLayer(@Nonnull IRaptorLayer existingLayer, @Nonnull String mapSourceConfig) throws IOException
    {
        final String layerName = existingLayer.getName();
        logger.debug("Recreating layer '{}'", layerName);

        raptorLayerManager.removeLayer(existingLayer, false);

        final var newLayer = createLayer(layerName, mapSourceConfig, false);
        if (newLayer != null)
        {
            newLayer.setVisible(existingLayer.isVisible());
            newLayer.setDisplayName(existingLayer.getDisplayName());
            newLayer.setOpacity(existingLayer.getOpacity());
            newLayer.setSplitScale(existingLayer.getSplitScale());
            newLayer.setMinActiveAltitudeEnabled(existingLayer.isMinActiveAltitudeEnabled());
            newLayer.setMinActiveAltitude(existingLayer.getMinActiveAltitude());
            newLayer.setMaxActiveAltitudeEnabled(existingLayer.isMaxActiveAltitudeEnabled());
            newLayer.setMaxActiveAltitude(existingLayer.getMaxActiveAltitude());

            layerPersist.persistLayer(newLayer);
        }
    }

    /**
     * Create a new layer with the given name and configuration XML/JSON.
     *
     * @param mapSourceName Name of map source (layer key)
     * @param mapSourceConfig New map source configuration
     * @param persist Pass {@code true} to persist the new layer after setting properties, {@code false} to not
     * @return new layer, or {@code null} if the layer couldn't be created for any reason
     * @since 5.6
     */
    @Nullable
    private IRaptorLayer createLayer(@Nonnull String mapSourceName, @Nonnull String mapSourceConfig, boolean persist) throws IOException
    {
        logger.debug("Creating layer for map source '{}'", mapSourceName);
        logger.debug("Source config: {}", mapSourceConfig);

        final var configuration = makeLayerConfiguration(mapSourceName, mapSourceConfig);
        if (configuration != null)
        {
            final var newLayer = (RaptorLayer) layerService.addLayer(configuration);
            newLayer.setPermanent(true);
            newLayer.setSystemManaged();

            if (persist) layerPersist.persistLayer(newLayer);
            return newLayer;
        }
        return null;
    }

    /**
     * @return the layer configuration according to the {@code mapSourceConfig}, or {@code null} if unknown type
     * @since 5.6
     */
    @Nullable
    private LayerConfiguration makeLayerConfiguration(@Nonnull String mapSourceName, @Nonnull String mapSourceConfig) throws IOException
    {
        final var tileSource = StreamingTiles.parse(mapSourceConfig);
        final boolean streamingImagery = tileSource != null && CONTENT_IMAGERY_TYPE.equals(tileSource.content);
        final boolean streamingTerrain = tileSource != null && CONTENT_TERRAIN_TYPE.equals(tileSource.content);

        if (streamingTerrain)
        {
            logger.debug("Detected streaming elevation source");
            return StreamingElevationLayerBuilder.builder()
                    .setName(mapSourceName)
                    .setConfigurationJson(mapSourceConfig)
                    .setVisible(false)
                    .build();
        } else if (streamingImagery || DatasetDescriptorFactory2.isSupported(createTemporaryConfigFile(mapSourceConfig)))
        {
            logger.debug(streamingImagery ? "Detected streaming imagery source" : "Detected DatasetDescriptorFactory2 source");
            return OnlineImageryLayerBuilder.builder()
                    .setName(mapSourceName)
                    .setConfigurationXml(mapSourceConfig)
                    .setVisible(mapSourceName.equals(DEFAULT_VISIBLE_LAYER))
                    .build();
        } else
        {
            logger.warn("Don't know how to process map source '{}'", mapSourceName);
            return null;
        }
    }

    /**
     * Update our cached map sources, and determine if the definition of a map source has changed by comparing the hash
     * of the {@code mapSourceConfig} against the hash of the previously downloaded map source, if one exists.  If the
     * previously downloaded map source does not exist, or is different, then the given {@code mapSourceConfig} is
     * written to our cache directory for future comparisons.
     *
     * @return {@code true} if the map source has changed or was not previously recorded
     * @since 5.6
     */
    private boolean updateCache(@Nonnull String mapSourcePath, @Nonnull String mapSourceConfig) throws IOException
    {
        final var filename = mapSourcePath.substring(mapSourcePath.lastIndexOf('/') + 1);
        final var cachedSource = cachedMapSourcesDir.resolve(filename);

        if (Files.exists(cachedSource))
        {
            final var localHash = HashingUtils.sha256sum(cachedSource.toFile());
            final var remoteHash = HashingUtils.sha256sum(mapSourceConfig);

            if (localHash.equals(remoteHash))
            {
                logger.debug("source '{}' unchanged", filename);
                return false;
            } else
            {
                logger.debug("source '{}' hash different", filename);
            }
        } else
        {
            logger.debug("No cached source for '{}'", filename);
        }

        Files.writeString(cachedSource, mapSourceConfig, StandardCharsets.UTF_8);
        return true;
    }

    /**
     * Get the contents of the given URI.
     * <p/>
     * This replaces the use of {@link MapSources} to load the map source definitions.  The underlying {@code libcurl}
     * library would randomly hang trying to read the map source files, and even configuring appropriate timeouts
     * would not resolve it, it still hung randomly.
     * <p/>
     * This implementation uses the Java {@link HttpClient}, and it seems completely stable with no hangs observed
     * under stress testing.
     *
     * @param fileUri String URI of the file to read
     * @return Contents of the file at the specified URI
     * @since 5.1
     */
    @VisibleForTesting
    String getFile(String fileUri) throws IOException
    {
        logger.debug("getFile {}", fileUri);

        try
        {
            final HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .connectTimeout(TIMEOUT_DURATION)
                    .build();

            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(fileUri))
                    .timeout(TIMEOUT_DURATION)
                    .GET()
                    .build();

            return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception e)
        {
            throw new IOException("Failed to get: " + fileUri, e);
        }
    }

    private static File createTemporaryConfigFile(String data) throws IOException
    {
        final var configFile = File.createTempFile("config", ".xml", null);
        configFile.deleteOnExit();
        Files.writeString(configFile.toPath(), data, StandardCharsets.UTF_8);
        return configFile;
    }
}
