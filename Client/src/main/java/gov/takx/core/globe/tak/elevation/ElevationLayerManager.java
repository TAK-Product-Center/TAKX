
package gov.takx.core.globe.tak.elevation;

import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.map.elevation.ElevationSourceBuilder;
import com.atakmap.map.elevation.ElevationSourceManager;
import com.atakmap.map.layer.MultiLayer;
import com.atakmap.map.layer.control.Controls;
import com.atakmap.map.layer.control.ElevationSourceControl;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.raster.ImageryFileType;
import com.atakmap.map.layer.raster.osm.OSMUtils;
import com.atakmap.map.layer.raster.tilematrix.TileMatrix;
import com.atakmap.map.layer.raster.tilematrix.TileProxy;
import com.atakmap.map.opengl.ElMgrTerrainRenderService;
import com.atakmap.util.ConfigOptions;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.Earth;
import gov.nasa.worldwind.terrain.CompoundElevationModel;
import gov.nasa.worldwind.terrain.ZeroElevationModel;
import gov.tak.api.util.Disposable;
import gov.takx.api.event.ConfigureGLMapView;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IGlobeLayerManager;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerListener;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.IRaptorLayerManagerListener;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.terrain.experimental.ITakElevationModel;
import gov.takx.core.configuration.ProgramEnvironment;
import gov.takx.core.globe.layers.GlobeLayer;
import gov.takx.core.globe.tak.elevation.ElevationLayerFactory.ElevationLayerPair;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.core.terrain.experimental.TerrainService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A service that installs and manages an {@link ITakElevationModel} in each globe. It is responsible for maintaining
 * a TAK {@link MultiLayer} group that contains the representative TAK layers for the TAKX {@link LayerGroup#ELEVATION}
 * group.
 *
 * @since 3.4
 */
@ApplicationScoped
public class ElevationLayerManager implements IRaptorLayerManagerListener, IRaptorLayerListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int DEFAULT_CACHE_LIMIT_BYTES = 40_000_000;

    private final IExtendedGlobeManager globeManager;
    private final ElevationLayerFactory elevationLayerFactory;
    private final IRaptorLayerManager raptorLayerManager;
    private final TerrainService terrainService;
    private final ElevationDownloaderManager elevationDownloaderManager;
    private final File cacheDir;
    private final Map<IRaptorLayer, ElevationLayerPair> elevationLayerPairMap = new HashMap<>();
    private final Set<ElevationSource> visibleElevationSources = new LinkedHashSet<>();

    private MultiLayer layer;
    private ElevationSource terrainRenderSource;
    private boolean serviceStarted;

    @Inject
    ElevationLayerManager(IExtendedGlobeManager globeManager, ElevationLayerFactory elevationLayerFactory,
                          IRaptorLayerManager raptorLayerManager, TerrainService terrainService,
                          ElevationDownloaderManager elevationDownloaderManager, ProgramEnvironment programEnvironment)
    {
        this.globeManager = globeManager;
        this.elevationLayerFactory = elevationLayerFactory;
        this.raptorLayerManager = raptorLayerManager;
        this.terrainService = terrainService;
        this.elevationDownloaderManager = elevationDownloaderManager;
        cacheDir = new File(programEnvironment.getProjectDataDirectory(), "OfflineCache/Elevation/CLOD");
        if (!cacheDir.exists() && !cacheDir.mkdirs()) logger.error("Could not create cache directory at {}", cacheDir.getAbsolutePath());
    }

    private void startService(@Observes @Priority(ServiceOrdering.START_CreateTAKLayers) StartProjectServices event)
    {
        event.getProgressMonitor().setText("Starting elevation services");

        serviceStarted = true;
        layer = new MultiLayer("TAK Elevation Layers");

        // We want the content changed dispatcher layer to always be visible so we can get update notifications.
        visibleElevationSources.add(elevationDownloaderManager.getElevationContentChangedDispatcher());

        // Clear out WW elevation models and add initial TAK elevation models
        globeManager.doWhenGlobeReady(globe -> {
            CompoundElevationModel compoundModel = (CompoundElevationModel) globe.getWorldWindow().getModel().getGlobe().getElevationModel();
            compoundModel.getElevationModels().stream().filter(m -> !(m instanceof ITakElevationModel)).forEach(compoundModel::removeElevationModel);
            compoundModel.addElevationModel(new LowResZeroElevationModel());
            // Primary globe needs to attach events to ElevationLayerManager
            boolean isPrimaryGlobe = globeManager.getPrimaryGlobeDelegate() == globe;
            if (isPrimaryGlobe)
            {
                raptorLayerManager.getModelLayers().forEach(this::raptorLayerAdded);
                raptorLayerManager.addRaptorLayerManagerListener(this);
            } else
            {
                // Non-primary globes just need call to attachElevationModel
                elevationLayerPairMap.values().forEach(p -> attachElevationModel(globe, p.elevationModel()));
                pushVisibleSourcesToElMgr();
            }
        });
    }

    private void stopService(@Observes @Priority(ServiceOrdering.STOP_TAKLayers) StopProjectServices event)
    {
        serviceStarted = false;
        if (terrainRenderSource != null) disposeElevationSource(terrainRenderSource);
        // Make a copy of the elevation layers and call raptorModelRemoved on each
        new ArrayList<>(elevationLayerPairMap.keySet()).forEach(this::raptorLayerRemoved);
        elevationLayerPairMap.clear();
        visibleElevationSources.clear();
    }

    private void onGLEventInitialized(@Observes ConfigureGLMapView event)
    {
        // In this event if the GLMapView has just been configured for the first time
        // then it hasn't been received by the ApplicationGlobe yet, so push to event queue
        event.glMapView().getRenderContext().queueEvent(this::pushVisibleSourcesToElMgr);
    }

    @Override
    public void raptorLayerAdded(IRaptorLayer raptorLayer)
    {
        if (raptorLayer.getLayerGroup() != LayerGroup.ELEVATION) return;
        ElevationLayerPair pair = elevationLayerFactory.create(raptorLayer);
        if (pair != null)
        {
            attachElevationLayer(raptorLayer, pair);
            // Extract sector and propagate through layers
            ITakElevationModel elevationModel = getElevationModel(raptorLayer);
            Sector sector = elevationModel != null ? elevationModel.getSector() : null;
            if (sector != null)
            {
                IGlobeLayerManager globeLayerManager = globeManager.getPrimaryGlobeDelegate().getGlobeLayerManager();
                if (globeLayerManager.getGlobeLayer(raptorLayer) instanceof GlobeLayer globeLayer) globeLayer.setSector(sector);
            }
        }
    }

    private void attachElevationLayer(IRaptorLayer raptorLayer, ElevationLayerPair pair)
    {
        raptorLayer.addPropertyChangeListener(this);
        ElevationLayerPair existingPair = elevationLayerPairMap.get(raptorLayer);
        if (existingPair != null) detachElevationLayer(raptorLayer, existingPair);
        ElevationSourceManager.attach(pair.elevationSource());
        pair.elevationModel().addPropertyChangeListener(this);
        globeManager.getAllGlobeDelegates().forEach(g -> attachElevationModel(g, pair.elevationModel()));
        elevationLayerPairMap.put(raptorLayer, pair);
        setVisible(raptorLayer, raptorLayer.isVisible());
        terrainService.raptorLayerAdded(raptorLayer);
    }

    private void attachElevationModel(@Nonnull IGlobeDelegate globe, @Nonnull ITakElevationModel elevationModel)
    {
        CompoundElevationModel compoundModel = (CompoundElevationModel) globe.getWorldWindow().getModel().getGlobe().getElevationModel();
        compoundModel.addElevationModel(elevationModel);
    }

    @Override
    public void raptorLayerRemoved(IRaptorLayer raptorLayer)
    {
        if (raptorLayer.getLayerGroup() != LayerGroup.ELEVATION) return;
        ElevationLayerPair pair = elevationLayerPairMap.get(raptorLayer);
        if (pair != null) detachElevationLayer(raptorLayer, pair);
    }

    private void disposeElevationSource(ElevationSource elevationSource)
    {
        if (elevationSource instanceof Controls controls)
        {
            TileMatrix tileMatrix = controls.getControl(TileMatrix.class);
            if (tileMatrix != null) tileMatrix.dispose();
            TileProxy tileProxy = controls.getControl(TileProxy.class);
            if (tileProxy != null)
            {
                ExecutorService executorService = tileProxy.getControl(ExecutorService.class);
                if (executorService != null)
                {
                    try
                    {
                        executorService.awaitTermination(3, TimeUnit.SECONDS);
                    } catch (InterruptedException e)
                    {
                        logger.debug("Interrupted {} tile proxy shutdown.", tileProxy.getName(), e);
                    }
                }
            }
        }
        if (elevationSource instanceof Disposable disposable) disposable.dispose();
    }

    private void detachElevationLayer(IRaptorLayer raptorLayer, ElevationLayerPair pair)
    {
        raptorLayer.removePropertyChangeListener(this);
        ElevationSourceManager.detach(pair.elevationSource());
        disposeElevationSource(pair.elevationSource());
        pair.elevationModel().removePropertyChangeListener(this);
        globeManager.getAllGlobeDelegates().forEach(g -> detachElevationModel(g, pair.elevationModel()));
        setVisible(raptorLayer, false);
        elevationLayerPairMap.remove(raptorLayer);
        terrainService.raptorLayerRemoved(raptorLayer);
    }

    private void detachElevationModel(@Nonnull IGlobeDelegate globe, @Nonnull ITakElevationModel elevationModel)
    {
        CompoundElevationModel compoundModel = (CompoundElevationModel) globe.getWorldWindow().getModel().getGlobe().getElevationModel();
        compoundModel.removeElevationModel(elevationModel);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        switch (evt.getPropertyName())
        {
            case IRaptorLayer.VISIBLE ->
            {
                IRaptorLayer raptorLayer = (IRaptorLayer) evt.getSource();
                setVisible(raptorLayer, (boolean) evt.getNewValue());
            }
            case AVKey.ELEVATION_MODEL -> globeManager.getAllGlobeDelegates().forEach(IGlobeDelegate::redraw);
        }
    }

    private synchronized void setVisible(IRaptorLayer raptorLayer, boolean visible)
    {
        ElevationLayerPair pair = elevationLayerPairMap.get(raptorLayer);
        if (pair != null)
        {
            boolean needsUpdate = visible ? visibleElevationSources.add(pair.elevationSource()) : visibleElevationSources.remove(pair.elevationSource());
            if (needsUpdate) pushVisibleSourcesToElMgr();
            pair.elevationModel().setEnabled(visible);
        }
    }

    private synchronized void pushVisibleSourcesToElMgr()
    {
        if (!serviceStarted) return;
        List<ElevationSource> elevationSources = new ArrayList<>();
        List<ElevationSource> dtedSources = new ArrayList<>();

        for (ElevationSource elevationSource : visibleElevationSources)
        {
            boolean isDted = elevationLayerPairMap.values().stream()
                    .filter(p -> p.elevationSource() == elevationSource)
                    .map(p -> p.elevationModel() instanceof DtedElevationModel)
                    .findFirst()
                    .orElse(false);
            (isDted ? dtedSources : elevationSources).add(elevationSource);
        }
        ElevationSource oldTerrainRenderSource = terrainRenderSource;
        if (!dtedSources.isEmpty())
        {
            terrainRenderSource = createMultiplexedDtedElevationSource(dtedSources);
            elevationSources.add(terrainRenderSource);
        }

        ElevationSource multiplexSource = ElevationSourceBuilder.multiplex("Elevation Layer Manager", elevationSources.toArray(ElevationSource[]::new));
        doElevationSourceControl(c -> c.setElevationSource(multiplexSource));
        if (oldTerrainRenderSource != null) disposeElevationSource(oldTerrainRenderSource);
    }

    /**
     * Returns the {@link ElevationSource} linked to the {@link IRaptorLayer}
     * @param raptorLayer The layer to get the ElevationSource for
     * @return The ElevationSource linked to the raptorLayer
     * @since 5.1
     */
    public ElevationSource getElevationSource(IRaptorLayer raptorLayer)
    {
        ElevationLayerPair pair = elevationLayerPairMap.get(raptorLayer);
        return pair != null ? pair.elevationSource() : null;
    }

    /**
     * Returns the {@link ITakElevationModel} linked to the {@link IRaptorLayer}
     * @param raptorLayer The layer to get the ITakElevationModel for
     * @return The ITakElevationModel linked to the raptorLayer
     * @since 5.3
     */
    public ITakElevationModel getElevationModel(IRaptorLayer raptorLayer)
    {
        ElevationLayerPair pair = elevationLayerPairMap.get(raptorLayer);
        return pair != null ? pair.elevationModel() : null;
    }

    /**
     * A MultiLayer containing the TAK elevation layers.
     *
     * @return the layer
     */
    public MultiLayer getLayer()
    {
        return layer;
    }

    private void doElevationSourceControl(Consumer<ElevationSourceControl> action)
    {
        globeManager.getAllGlobeDelegates().forEach(globe -> {
            ElevationSourceControl control = globe.getPortableMapRenderer().getControl(ElevationSourceControl.class);
            if (control != null) action.accept(control);
        });
    }

    /**
     * Determine if a {@link File} is a supported DTED file.
     * @return {@code true} if supported, or {@code false}
     * @since 5.2
     */
    public static boolean isSupportedDtedFile(File file)
    {
        ImageryFileType.AbstractFileType fileType = ImageryFileType.getFileType(file);
        return fileType != null && fileType.getID() == ImageryFileType.DTED;
    }

    /**
     * Get the relative root directory for the valid DTED file.
     * @param dtedFile a valid DTED file
     * @return the relative root directory, or {@code null} if one could not be found
     * @since 5.3
     */
    @Nullable
    public static File getRootDtedPath(@Nonnull File dtedFile)
    {
        // DTED paths are generally in <root path>/<lon path/<lat path>.dt<level> format
        return dtedFile.getParentFile() != null ? dtedFile.getParentFile().getParentFile() : null;
    }

    @Nonnull
    private ElevationSource createMultiplexedDtedElevationSource(List<ElevationSource> dtedSources)
    {
        File offlineCacheDb;
        try
        {
            offlineCacheDb = IOProviderFactory.createTempFile("dted", ".sqlite", cacheDir);
            offlineCacheDb.deleteOnExit();
        } catch (IOException e)
        {
            logger.error("Could not create temporary file at {}", cacheDir, e);
            offlineCacheDb = null;
        }

        return new MultiplexedDtedElevationSource(dtedSources, offlineCacheDb);
    }

    /**
     * Simple elevation model that returns all zeros and has a very low resolution.
     *
     * @since 3.4.1
     */
    private static final class LowResZeroElevationModel extends ZeroElevationModel
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

    /**
     * An {@link ElevationSource elevation source} that multiplexes a set of DTED elevation sources to create a source suitable
     * for performant terrain rendering. The source combines a static low-resolution source, a mid-resolution continuous level
     * of detail source from the provided DTED sources, and a high-resolution continuous level of detail source from the provided
     * DTED sources. This class will properly dispose of all generated sources.
     *
     * @since 5.5.1
     */
    private static class MultiplexedDtedElevationSource extends ElevationSource implements Disposable, Controls
    {
        private final ElevationSource impl;
        final ElevationSource lowResSource;
        final ElevationSource lowResConstrained;
        final ElevationSource midResSource;
        final ElevationSource midResConstrained;
        final ElevationSource highResSource;
        final ElevationSource highResConstrained;

        private MultiplexedDtedElevationSource(List<ElevationSource> dtedSources, File offlineCacheDb)
        {
            List<ElevationSource> sources = new ArrayList<>();
            ElevationSource dtedElevationSource = ElevationSourceBuilder.multiplex("DTED Multiplex", dtedSources.toArray(ElevationSource[]::new));
            if (ConfigOptions.getOption("elmgr.lores-dted-enabled", 1) != 0)
            {
                lowResSource = ElMgrTerrainRenderService.createDefaultLoResElevationSource();
                if (lowResSource != null)
                {
                    // The elevation sources use a nominal zoom level, so don't overlap min/maxResolutions.
                    lowResConstrained = ElevationSourceBuilder.constrainResolution(lowResSource, Double.NaN, OSMUtils.mapnikTileResolution(5));
                    sources.add(lowResConstrained);
                } else
                {
                    lowResConstrained = null;
                }
            } else
            {
                lowResSource = null;
                lowResConstrained = null;
            }

            midResSource = new ElevationSourceBuilder.CLOD(dtedElevationSource)
                    .setStrategy(ElevationManager.HeightmapStrategy.LowFillHoles)
                    .setPointSampler(true)
                    .enableCaching(offlineCacheDb, DEFAULT_CACHE_LIMIT_BYTES, true)
                    .build();
            midResConstrained = ElevationSourceBuilder.constrainResolution(midResSource, OSMUtils.mapnikTileResolution(6), OSMUtils.mapnikTileResolution(7));
            sources.add(midResConstrained);
            highResSource = new ElevationSourceBuilder.CLOD(dtedElevationSource)
                    .setStrategy(ElevationManager.HeightmapStrategy.HighestResolution)
                    .setPointSampler(false)
                    .enableCaching(offlineCacheDb, DEFAULT_CACHE_LIMIT_BYTES, false)
                    .build();
            highResConstrained = ElevationSourceBuilder.constrainResolution(highResSource, OSMUtils.mapnikTileResolution(8), Double.NaN);
            sources.add(highResConstrained);
            impl = ElevationSourceBuilder.multiplex(dtedElevationSource.getName() + " CLOD", sources.toArray(ElevationSource[]::new));
        }

        @Override
        public String getName()
        {
            return impl.getName();
        }

        @Override
        public Cursor query(QueryParameters params)
        {
            return impl.query(params);
        }

        @Override
        public Envelope getBounds()
        {
            return impl.getBounds();
        }

        @Override
        public void addOnContentChangedListener(OnContentChangedListener l)
        {
            impl.addOnContentChangedListener(l);
        }

        @Override
        public void removeOnContentChangedListener(OnContentChangedListener l)
        {
            impl.removeOnContentChangedListener(l);
        }

        @Override
        public void dispose()
        {
            if (impl instanceof Disposable disposable) disposable.dispose();
            if (lowResSource instanceof Disposable disposable) disposable.dispose();
            if (lowResConstrained instanceof Disposable disposable) disposable.dispose();
            if (midResSource instanceof Disposable disposable) disposable.dispose();
            if (midResConstrained instanceof Disposable disposable) disposable.dispose();
            if (highResSource instanceof Disposable disposable) disposable.dispose();
            if (highResConstrained instanceof Disposable disposable) disposable.dispose();
        }

        @Override
        public <T> T getControl(Class<T> controlClazz)
        {
            return impl instanceof Controls c ? c.getControl(controlClazz) : null;
        }

        @Override
        public void getControls(Collection<Object> controls)
        {
            if (impl instanceof Controls c) c.getControls(controls);
        }
    }
}