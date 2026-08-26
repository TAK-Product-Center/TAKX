package gov.takx.core.globe;

import com.atakmap.map.layer.MultiLayer;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.Earth;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.TiledImageLayer;
import gov.nasa.worldwind.terrain.CompoundElevationModel;
import gov.nasa.worldwind.terrain.ZeroElevationModel;
import gov.nasa.worldwind.util.LevelSet;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IGlobeLayerManager;
import gov.takx.api.globe.IWorldWindowGL;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.core.globe.layers.IGlobeLayer;
import gov.takx.core.terrain.experimental.TerrainService;
import gov.takx.core.view.gui.maps.FileStoreLocationManager;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Abstract base class for globe layer managers. Since, currently, they all need to handle elevation models, those
 * layers are handled here.  Other (imagery) layers are only handled by the concrete subclasses that need them.
 *
 * @since 1.50
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public abstract class AGlobeLayerManager implements IGlobeLayerManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    protected IRaptorLayerManager layerManager;

    @Inject
    protected TerrainService terrainService;

    protected final ApplicationGlobe globe;
    protected final IWorldWindowGL wwd;
    protected boolean primaryGlobe;
    protected final GlobeLayerFactory globeLayerFactory;

    private static final AtomicInteger nextInstance = new AtomicInteger(1);
    protected final int instance;

    /**
     * Construct the globe layer manager.
     *
     * @param globe The globe we are the layer manager for
     * @param primaryGlobe Pass {@code true} if we are handling the primary globe
     */
    protected AGlobeLayerManager(IApplicationGlobe globe, boolean primaryGlobe)
    {
        instance = nextInstance.getAndIncrement();
        this.primaryGlobe = primaryGlobe;
        this.globe = (ApplicationGlobe) globe;

        wwd = globe.getWorldWindow();
        globeLayerFactory = new GlobeLayerFactory(globe);

        // Configure initial ElevationModel containing a single default low-res zero model
        if (primaryGlobe)
        {
            // Zero elevation model added so that when real elevation models are all switched off, the view won't
            // exhibit strange display errors, such as the ability to go under the globe layers.

            final CompoundElevationModel cem = new CompoundElevationModel();
            cem.addElevationModel(new LowResZeroElevationModel());

            wwd.getModel().getGlobe().setElevationModel(cem);
        }
    }

    @PostConstruct
    private void postConstruct()
    {
        addUserFileStores();
    }

    /**
     * Dump globe layer details to the log, if debug is enabled. If the layer isn't an instance of
     * {@link TiledImageLayer}, then no data is displayed.
     *
     * @param globeLayer Layer to dump, must be
     */
    protected void logLayerResolution(IGlobeLayer globeLayer)
    {
        if (!logger.isDebugEnabled() || globeLayer == null) return;

        Object layer = globeLayer.getLayer();

        final LevelSet levelSet = (layer instanceof TiledImageLayer imageLayer)
                ? imageLayer.getLevels()
                : null;

        if (levelSet != null)
        {
            int numLevels = levelSet.getNumLevels();
            double resolution = levelSet.getLastLevel().getTexelSize() *
                    globe.getWorldWindow().getModel().getGlobe().getRadius();
            logger.debug("Layer: {}, num levels: {}, resolution: {} m", globeLayer.getName(), numLevels, String.format("%.4f", resolution));
        }
    }

    /**
     * Return the list of globe layers from the WW model.
     *
     * @return list of globe layers
     */
    @Override
    public List<IGlobeLayer> getGlobeLayers()
    {
        Stream<?> layerStream = Stream.concat(getTakGlobeLayers(globe.getNativeGlobe().getLayers()).stream(), wwd.getModel().getLayers().stream());
        return layerStream
                .filter(IGlobeLayer.class::isInstance)
                .map(IGlobeLayer.class::cast)
                .distinct()
                .toList();
    }

    /**
     * Get the globe layer associated with the given {@link IRaptorLayer}.
     *
     * @param raptorLayer The layer of interest
     * @return Associated globe layer, {@code null} if not found
     */
    @Override
    public IGlobeLayer getGlobeLayer(IRaptorLayer raptorLayer)
    {
        IGlobeLayer takGlobeLayer = getTakGlobeLayers(globe.getNativeGlobe().getLayers()).stream()
                .filter(l -> l.getRaptorLayer().equals(raptorLayer))
                .findFirst().orElse(null);
        if (takGlobeLayer != null) return takGlobeLayer;

        for (Layer layer : wwd.getModel().getLayers())
        {
            if (layer instanceof IGlobeLayer globeLayer && globeLayer.getRaptorLayer().equals(raptorLayer)) return globeLayer;
        }
        return null;
    }

    private static List<IGlobeLayer> getTakGlobeLayers(List<com.atakmap.map.layer.Layer> layers)
    {
        List<IGlobeLayer> globeLayers = new ArrayList<>();
        for (com.atakmap.map.layer.Layer layer : layers)
        {
            if (layer instanceof IGlobeLayer globeLayer) globeLayers.add(globeLayer);
            if (layer instanceof MultiLayer multiLayer)
            {
                globeLayers.addAll(getTakGlobeLayers(multiLayer.getLayers()));
            }
        }
        return globeLayers;
    }

    /**
     * Get all the file stores recorded in the user preferences and add them to the World Wind file stores.
     */
    private void addUserFileStores()
    {
        String directories = UserPreferenceEntry.FILE_STORES_DEFAULT_LOCATIONS.getStringValue();
        String[] directoryNames = directories.split(",");
        if (directoryNames.length > 0 && directories.length() > 1)
        {
            for (String location : directoryNames)
            {
                addLocationToWorldWind(location);
            }
        }
    }

    /**
     * Convenience method for adding a file store location to WorldWind.
     *
     * @param fileStoreLocation a String location to a file store location
     * @since 1.25.3
     */
    protected void addLocationToWorldWind(String fileStoreLocation)
    {
        fileStoreLocation = fileStoreLocation.trim();
        File curFile = new File(fileStoreLocation);
        if (curFile.exists())
        {
            FileStoreLocationManager.getInstance().addLocationWithParent(fileStoreLocation);
        }
    }

    /**
     * CORE-3296:
     * The terrain service needs to be restored once the globe layers (incl. elevation models) have initialized.
     */
    protected void restoreTerrainService()
    {
        terrainService.restore();

        // CORE-3402: Terrain service needs to get RaptorLayerManager events (i.e. layer added, layer removed).
        // TODO: TAKX-3931 This is gross, the terrain service should handle its own registrations
        layerManager.addRaptorLayerManagerListener(terrainService);
    }

    @Override
    public void onShutdown(IProgressMonitor progressMonitor)
    {
        // TODO: TAKX-3931 This is gross, the terrain service should handle its own registrations
        layerManager.removeRaptorLayerManagerListener(terrainService);
    }

    /**
     * Simple elevation model that returns all zeros and has a very low resolution.
     */
    protected static final class LowResZeroElevationModel extends ZeroElevationModel
    {
        private static final double RESOLUTION = 10_000d / Earth.WGS84_EQUATORIAL_RADIUS;

        private LowResZeroElevationModel()
        {
            setNetworkRetrievalEnabled(false);
        }

        @Override
        public double getBestResolution(Sector sector)
        {
            // Return a very low resolution (roughly equal to 10 km).  This is because we want WW to prefer
            // using other elevation models over this.
            return RESOLUTION;
        }
    }
}
