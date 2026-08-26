package gov.takx.core.layer;

import gov.nasa.worldwind.avlist.AVList;
import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeLayerManager;
import gov.takx.api.layer.ILayerService;
import gov.takx.api.layer.IMobacLayerManager;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.IRaptorLayerManagerListener;
import gov.takx.api.layer.LayerGroup;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.globe.layers.IGlobeLayer;
import gov.takx.core.globe.layers.mobac.CustomMobacTiledImageLayer;
import gov.takx.core.globe.layers.worldwind.LayerFactory;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * MOBAC layer manager service, supporting the creation of layers from MOBAC configuration XML.
 *
 * @since 3.1
 * @deprecated Use {@link gov.takx.api.layer.builder.OnlineImageryLayerBuilder} and {@link ILayerService}
 */
@ApplicationScoped
@Deprecated(since = "5.4", forRemoval = true)
@DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
public class MobacLayerManager implements IMobacLayerManager, IRaptorLayerManagerListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Classname of layer type to be created
    private static final String LAYER_CLASS_NAME = CustomMobacTiledImageLayer.class.getName();

    // XML element that identifies a custom map source
    private static final String MAP_SOURCE_ELEMENT = "<" + LayerFactory.CUSTOM_MAP_SOURCE + ">";

    private final IRaptorLayerManager layerManager;
    private final IRaptorLayerManager raptorLayerManager;
    private final IExtendedGlobeManager globeManager;

    @Inject
    MobacLayerManager(IRaptorLayerManager layerManager, IRaptorLayerManager raptorLayerManager, IExtendedGlobeManager globeManager)
    {
        this.globeManager = globeManager;
        this.layerManager = layerManager;
        this.raptorLayerManager = raptorLayerManager;
    }

    private void startService(@Observes @Priority(ServiceOrdering.START_LayerViewManager) StartProjectServices event)
    {
        raptorLayerManager.addRaptorLayerManagerListener(this);
    }

    /**
     * Test if a file appears to be a map source XML config file.
     *
     * @param file File to test
     * @return {@code true} if the file contains the custom map source XML element
     */
    @Override
    public boolean isMapSourceXml(@Nonnull File file)
    {
        if (!"xml".equalsIgnoreCase(FilenameUtils.getExtension(file.getName()))) return false; // Must be XML

        try (InputStream inputStream = new FileInputStream(file))
        {
            return isMapSourceXml(inputStream);
        } catch (IOException e)
        {
            return false;
        }
    }

    @Override
    public boolean isMapSourceXml(@Nonnull String string)
    {
        return string.contains(MAP_SOURCE_ELEMENT);
    }

    /**
     * Test if an input stream appears to contain a map source XML config file.
     *
     * @param inputStream input stream to test
     * @return {@code true} if the input stream contains the custom map source XML element
     */
    @Override
    public boolean isMapSourceXml(@Nonnull InputStream inputStream)
    {
        // We only need the first block of the file to determine if it is one of our config files.
        // If it's valid, the element will always be within the first 512 characters.
        try
        {
            final byte[] bytes = inputStream.readNBytes(512);
            return new String(bytes).contains(MAP_SOURCE_ELEMENT);
        } catch (IOException e)
        {
            return false;
        }
    }

    @Nullable
    @Override
    public IRaptorLayer createLayerFromFile(@Nonnull File configFile)
    {
        try (InputStream inputStream = new FileInputStream(configFile))
        {
            final String layerName = FilenameUtils.getBaseName(configFile.getName());
            return createLayerFromConfigXml(layerName, IOUtils.toString(inputStream, StandardCharsets.UTF_8));
        } catch (Exception e)
        {
            logger.error("Unable to import: {}", configFile.getAbsolutePath(), e);
            return null;
        }
    }

    @Nullable
    @Override
    public IRaptorLayer createLayerFromConfigXml(@Nonnull String layerName, @Nonnull String configXml)
    {
        return layerManager.addLayer(layerName, layerName, LAYER_CLASS_NAME, false, false, null,
                LayerGroup.IMAGERY, null, configXml, false);
    }

    @Override
    public void raptorLayerRemoved(IRaptorLayer raptorLayer)
    {
        if (LAYER_CLASS_NAME.equals(raptorLayer.getClassName()))
        {
            final String raptorLayerName = raptorLayer.getName();
            logger.debug("MOBAC layer removed: {}", raptorLayerName);

            // Ensure that the globe is ready before attempting to use the primary globe delegate. It's possible for the
            // globe to not be ready in the event that imagery layers are removed from the layer manager on startup.
            globeManager.doWhenGlobeReady(globeDelegate -> {
                final IGlobeLayerManager globeLayerManager = globeManager.getPrimaryGlobeDelegate().getGlobeLayerManager();
                final IGlobeLayer globeLayer = globeLayerManager.getGlobeLayer(raptorLayer);

                if (globeLayer == null)
                {
                    logger.debug("No globe layer exists for Raptor layer {}", raptorLayerName);
                    return;
                }

                if (globeLayer.getLayer() instanceof CustomMobacTiledImageLayer mobacLayer
                        && globeLayerManager.getLayerUseCount(raptorLayer) <= 1)
                {
                    updateMobacServerList(mobacLayer, Set::remove);
                }
            });
        }
    }

    /**
     * Construct a layer from the {@code restorableState}.
     *
     * @param restorableState State string (XML) to process
     * @return new layer
     * @since 3.4
     */
    public CustomMobacTiledImageLayer makeImageryLayer(String restorableState)
    {
        return updateMobacServerList(new CustomMobacTiledImageLayer(restorableState), Set::add);
    }

    /**
     * Construct from a parsed DOM Element.
     *
     * @param configXml  The XML configuration (our restorable state)
     * @param domElement DOM Element parsed from {@code configXml}
     * @param params     Pre-configured AVList
     * @since 3.4
     */
    public CustomMobacTiledImageLayer makeImageryLayer(String configXml, Element domElement, AVList params)
    {
        return updateMobacServerList(new CustomMobacTiledImageLayer(configXml, domElement, params), Set::add);
    }

    /**
     * Update the {@link UserPreferenceEntry#MOBAC_SERVERS} preference content according to given updater.  Keeping this
     * list in sync is important since it controls the "test sites" used by {@link gov.takx.core.globe.utils.WMSNetworkStatus}
     * in determining if the network is "reachable".  Without this, and with all the WW servers effectively offline, the
     * user would be shown "No Network" in the status bar even when we are successfully talking to a MOBAC tile server.
     *
     * @param mobacLayer The layer causing the update
     * @param updater Update to be applied to the Set of server host strings
     * @return the input layer
     * @since 3.4
     */
    private CustomMobacTiledImageLayer updateMobacServerList(CustomMobacTiledImageLayer mobacLayer, BiConsumer<Set<String>, String> updater)
    {
        final String layerServer = mobacLayer.getBaseServerUrl();

        if (layerServer != null)
        {
            final String serversList = UserPreferenceEntry.MOBAC_SERVERS.getStringValue();
            final Set<String> servers = serversList.isBlank()
                    ? new HashSet<>()
                    : Stream.of(serversList.split(",")).collect(Collectors.toSet());

            updater.accept(servers, layerServer);
            UserPreferenceEntry.MOBAC_SERVERS.putAndSave(StringUtils.join(servers));
        }

        return mobacLayer;
    }
}
