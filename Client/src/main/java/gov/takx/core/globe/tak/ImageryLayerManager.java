
package gov.takx.core.globe.tak;

import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.Layer2;
import com.atakmap.map.layer.MultiLayer;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.raster.DatasetDescriptorFactory2;
import com.atakmap.map.layer.raster.DatasetDescriptorSpi;
import com.atakmap.map.layer.raster.DatasetDescriptorSpiArgs;
import com.atakmap.map.layer.raster.PersistentRasterDataStore;
import com.atakmap.map.layer.raster.controls.ImageryRelativeScaleControl;
import com.atakmap.map.layer.raster.mobileimagery.MobileImageryRasterLayer2;
import com.atakmap.map.layer.raster.nativeimagery.NativeImageryRasterLayer2;
import com.atakmap.map.layer.raster.service.OnlineImageryExtension;
import com.atakmap.spi.InteractiveServiceProvider;
import com.atakmap.util.ConfigOptions;
import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.Headless;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.IRaptorLayerManagerListener;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.layer.builder.LocalImageryLayerBuilder;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.project.IProjectManager;
import gov.takx.core.configuration.KernelConfigOptionConstants;
import gov.takx.core.configuration.ProgramEnvironment;
import gov.takx.core.globe.ApplicationGlobeManager;
import gov.takx.core.globe.layers.GlobeLayer;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.platform.preferences.ProjectPreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * A service that manages the list of configured imagery data set renderers in TAK. It is responsible for maintaining
 * a TAK {@link MultiLayer} group that contains the representative TAK layers for the TAKX {@link LayerGroup#IMAGERY}
 * group. It also is responsible for providing a {@link PersistentRasterDataStore} for rendering local imagery.
 *
 * @since 3.4
 */
@ApplicationScoped
public class ImageryLayerManager implements IRaptorLayerManagerListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @DeprecatedApi(since = "5.8", forRemoval = true, removeAt = "5.11")
    @Deprecated(since = "5.8", forRemoval = true)
    public static final String NATIVE_IMAGERY_RASTER_CLASS = NativeImageryRasterLayer2.class.getName();
    public static final String IMAGERY_LAYER_MANAGER_CLASS = ImageryLayerManager.class.getName();

    private final ApplicationGlobeManager globeManager;
    private final IRaptorLayerManager raptorLayerManager;
    private final IProjectManager projectManager;
    private final ProgramEnvironment programEnvironment;
    private final boolean headless;
    private final List<LayerAndEnvelope> layersNeedingSectors = new ArrayList<>();

    private final IPreferenceMonitor<Boolean> wmsOfflineModeMonitor = ProjectPreferenceEntry.WMS_OFFLINE_MODE.newBooleanMonitor(this::wmsOfflineModeChanged, false);

    private PersistentRasterDataStore dataStore;
    private MultiLayer layer;

    @Inject
    ImageryLayerManager(ApplicationGlobeManager globeManager, IRaptorLayerManager raptorLayerManager,
                        IProjectManager projectManager, ProgramEnvironment programEnvironment, @Headless boolean headless)
    {
        this.globeManager = globeManager;
        this.raptorLayerManager = raptorLayerManager;
        this.projectManager = projectManager;
        this.programEnvironment = programEnvironment;
        this.headless = headless;
    }

    private void startService(@Observes @Priority(ServiceOrdering.START_CreateTAKLayers) StartProjectServices event)
    {
        if (headless) return;
        event.getProgressMonitor().setText("Starting raster services");

        File projectDir = new File(projectManager.getProjectDirectory());

        File storeDbFile = new File(projectDir, "/Databases/layers3.sqlite");
        File workingDir = new File(projectDir, "/Databases/.layersprivate");
        workingDir.mkdirs();
        File offlineCacheDir = new File(programEnvironment.getProjectDataDirectory(), "OfflineCache/Imagery");
        offlineCacheDir.mkdirs();
        ConfigOptions.setOption(KernelConfigOptionConstants.IMAGERY_OFFLINE_CACHE_DIR, offlineCacheDir.getAbsolutePath());

        dataStore = new PersistentRasterDataStore(storeDbFile, workingDir);
        layer = new MultiLayer("TakRasterLayers");

        raptorLayerManager.addRaptorLayerManagerListener(this);
        wmsOfflineModeMonitor.start();

        globeManager.doWhenGlobeReady(g -> {
            g.getPortableMapRenderer().getControl(ImageryRelativeScaleControl.class).setRelativeScale(0.5f);

            // Now that the globe is ready, set the bounding sectors on imagery layers that need it
            final var globeLayerManager = globeManager.getPrimaryGlobeDelegate().getGlobeLayerManager();
            layersNeedingSectors.forEach(layerAndEnvelope -> {
                if (globeLayerManager.getGlobeLayer(layerAndEnvelope.layer) instanceof GlobeLayer globeLayer) globeLayer.setSector(layerAndEnvelope.envelope);
            });
            layersNeedingSectors.clear();
        });
    }

    private void stopService(@Observes @Priority(ServiceOrdering.STOP_TAKLayers) StopProjectServices event)
    {
        if (headless) return;
        wmsOfflineModeMonitor.stop();
        raptorLayerManager.removeRaptorLayerManagerListener(this);
        if (dataStore != null)
        {
            dataStore.dispose();
        }
    }

    @Override
    public void raptorLayerAdded(IRaptorLayer raptorLayer)
    {
        if (NATIVE_IMAGERY_RASTER_CLASS.equals(raptorLayer.getClassName()) || IMAGERY_LAYER_MANAGER_CLASS.equals(raptorLayer.getClassName()))
        {
            final var filePaths = LocalImageryLayerBuilder.parseRestorableString(raptorLayer.getRestorableState());
            final var fileList = filePaths.stream().map(File::new).toList();

            final var envelopeBuilder = new Envelope.Builder();

            // GDAL gets a little noisy when calling dataStore.add()
            org.gdal.gdal.gdal.PushErrorHandler();

            fileList.forEach(file -> {
                try
                {
                    dataStore.add(file);

                    final var geometry = dataStore.getCoverage(file.getName(), null);
                    if (geometry != null) envelopeBuilder.add(geometry.getEnvelope());
                } catch (IOException | IllegalArgumentException e)
                {
                    logger.error("Error while importing map imagery at {}", file.getPath(), e);
                }
            });

            org.gdal.gdal.gdal.PopErrorHandler();

            final var envelope = envelopeBuilder.build();

            if (envelope != null)
            {
                final var globeLayerManager = globeManager.getPrimaryGlobeDelegate().getGlobeLayerManager();

                // This service starts before the globe is fully initialized, and the GlobeLayerManager will not have
                // been created if this method is called during initial startup.  In this case, we need to record
                // the layer and envelope so it can be processed once the project is fully open.
                //
                // TODO: Rework this startup ordering, TAKX-4138
                if (globeLayerManager == null)
                {
                    layersNeedingSectors.add(new LayerAndEnvelope(raptorLayer, envelope));
                } else
                {
                    if (globeLayerManager.getGlobeLayer(raptorLayer) instanceof GlobeLayer globeLayer) globeLayer.setSector(envelope);
                }
            }
        }

        if (raptorLayer.getLayerGroup() == LayerGroup.IMAGERY)
        {
            boolean offlineOnly = ProjectPreferenceEntry.WMS_OFFLINE_MODE.getBooleanValue();
            for (Layer childLayer : layer.getLayers())
            {
                if (childLayer instanceof GlobeLayer globeLayer)
                {
                    if (globeLayer.getRaptorLayer() == raptorLayer) setOfflineOnlyMode(globeLayer.getTakLayer(), offlineOnly);
                }
            }
        }
    }

    @Override
    public void raptorLayerRemoved(IRaptorLayer raptorLayer)
    {
        if (NATIVE_IMAGERY_RASTER_CLASS.equals(raptorLayer.getClassName()) || IMAGERY_LAYER_MANAGER_CLASS.equals(raptorLayer.getClassName()))
        {
            File file = new File(raptorLayer.getRestorableState());
            dataStore.remove(file);
        }
    }

    private void wmsOfflineModeChanged(boolean offline)
    {
        for (Layer childLayer : layer.getLayers())
        {
            if (childLayer instanceof GlobeLayer globeLayer) setOfflineOnlyMode(globeLayer.getTakLayer(), offline);
        }
    }

    private void setOfflineOnlyMode(Layer layer, boolean offlineOnly)
    {
        if (layer instanceof Layer2 layer2)
        {
            //noinspection deprecation
            OnlineImageryExtension onlineImageryExtension = layer2.getExtension(OnlineImageryExtension.class);
            if (onlineImageryExtension != null) onlineImageryExtension.setOfflineOnlyMode(offlineOnly);
        }
    }

    /**
     * A data store for rendering local imagery.
     *
     * @return the data store
     */
    public PersistentRasterDataStore getDataStore()
    {
        return dataStore;
    }

    /**
     * A MultiLayer containing the TAK imagery layers.
     *
     * @return the layer
     */
    public MultiLayer getLayer()
    {
        return layer;
    }

    /**
     * Simple holder to record a layer and its associated bounding envelope.
     * @since 5.6
     */
    private record LayerAndEnvelope(IRaptorLayer layer, Envelope envelope)
    {
    }

    /**
     * Determine if a {@link File} represents Mobile imagery or Native imagery.
     * Mobile imagery is multi-resolution and serviced by {@link MobileImageryRasterLayer2} and Native imagery is single resolution and serviced by {@link NativeImageryRasterLayer2}.
     * @param file the file being probed
     * @return {@code true} if the file is mobile imagery, {@code false} if native
     * @since 5.8
     */
    public static boolean isMobileImagery(@Nonnull File file)
    {
        var mobileTypes = new HashSet<String>();
        MobileImageryRasterLayer2.getDefaultDatasetTypes(mobileTypes);
        for (String datasetType : mobileTypes)
        {
            DatasetDescriptorSpi spi = DatasetDescriptorFactory2.getRegisteredSpi(datasetType);
            if (spi != null)
            {
                ProbeOnlyCallback callback = new ProbeOnlyCallback(500);
                spi.create(new DatasetDescriptorSpiArgs(file, null), callback);
                if (callback.probeSuccess) return true;
            }
        }
        return false;
    }

    /**
     * An implementation of {@link InteractiveServiceProvider.Callback} that only probes the SPI to determine if it supports the data.
     * @since 5.8
     */
    private final static class ProbeOnlyCallback implements InteractiveServiceProvider.Callback
    {
        boolean probeSuccess;
        private final int limit;

        ProbeOnlyCallback(int limit)
        {
            this.limit = limit;
            probeSuccess = false;
        }

        @Override
        public boolean isCanceled()
        {
            return false;
        }

        @Override
        public boolean isProbeOnly()
        {
            return true;
        }

        @Override
        public int getProbeLimit()
        {
            return limit;
        }

        @Override
        public void setProbeMatch(boolean match)
        {
            probeSuccess = match;
        }

        @Override
        public void errorOccurred(String msg, Throwable t)
        {
        }

        @Override
        public void progress(int progress)
        {
        }
    }
}
