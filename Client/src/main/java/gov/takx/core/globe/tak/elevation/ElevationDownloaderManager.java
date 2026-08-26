package gov.takx.core.globe.tak.elevation;

import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.map.elevation.ElevationSourceManager;
import com.atakmap.map.layer.control.SurfaceRendererControl;
import com.atakmap.map.layer.feature.geometry.Envelope;
import gov.tak.api.commons.resources.IResourceManager;
import gov.tak.api.engine.map.MapRenderer;
import gov.tak.platform.client.dted.ElevationDownloader;
import gov.tak.platform.client.dted.ElevationDownloader.OnContentChangedListener;
import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IGlobeLifeCycleListener;
import gov.takx.api.layer.IRaptorLayerListener;
import gov.takx.api.preferences.IPreferenceEntryChangeListener;
import gov.takx.api.preferences.PreferenceEntryChange;
import gov.takx.core.configuration.ProgramEnvironment;
import gov.takx.core.globe.ApplicationGlobeManager;
import gov.takx.core.preferences.ProjectPreferenceManager;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.ProjectPreferenceEntry;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * A service that installs and manages an {@link ElevationDownloader}.
 *
 * @since 3.4
 */
@ApplicationScoped
public class ElevationDownloaderManager implements MapRenderer.OnCameraChangedListener, OnContentChangedListener, IPreferenceEntryChangeListener, IRaptorLayerListener, IGlobeLifeCycleListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ProjectPreferenceManager preferenceManager;
    private final IResourceManager resourceManager;
    private final ApplicationGlobeManager globeManager;
    private final boolean headless;
    private final ElevationContentChangedDispatcher elevationContentChangedDispatcher = new ElevationContentChangedDispatcher("Content Changed Dispatcher");
    private final File dtedDir;
    private ElevationDownloader elevationDownloader;

    @Inject
    ElevationDownloaderManager(ApplicationGlobeManager globeManager, ProgramEnvironment programEnvironment,
                               ProjectPreferenceManager preferenceManager, IResourceManager resourceManager,
                               @Headless boolean headless)
    {
        this.preferenceManager = preferenceManager;
        this.resourceManager = resourceManager;
        this.headless = headless;
        this.globeManager = globeManager;
        dtedDir = new File(programEnvironment.getProjectDataDirectory(), "OfflineCache/Elevation/DTED");
        if (!dtedDir.exists() && !dtedDir.mkdirs()) logger.error("Failed to create directory: {}", dtedDir);
    }

    private void startService(@Observes @Priority(ServiceOrdering.START_InstallTAKLayers) StartProjectServices event)
    {
        if (headless) return;

        elevationDownloader = new ElevationDownloader(resourceManager, dtedDir);
        elevationDownloader.setEnabled(isDownloadEnabled());
        elevationDownloader.addListener(this);
        elevationDownloader.setDownloadServer(ProjectPreferenceEntry.DTED_STREAM_SERVER.getStringValue());

        globeManager.addGlobeLifeCycleListener(this);
        preferenceManager.addPreferenceEntryChangeListener(this);
        ElevationSourceManager.attach(elevationContentChangedDispatcher);
        onContentChanged();
    }

    private void stopService(@Observes @Priority(ServiceOrdering.STOP_GenericCoreService) StopProjectServices event)
    {
        if (headless) return;

        ElevationSourceManager.detach(elevationContentChangedDispatcher);
        preferenceManager.removePreferenceEntryChangeListener(this);
        globeManager.removeGlobeLifeCycleListener(this);

        if (elevationDownloader != null)
        {
            elevationDownloader.removeListener(this);
            elevationDownloader.dispose();
        }
    }

    @Override
    public void globeCreated(IGlobeDelegate globe)
    {
        globe.getPortableMapRenderer().addOnCameraChangedListener(this);
    }

    @Override
    public void globeRemoved(IGlobeDelegate globe)
    {
        globe.getPortableMapRenderer().removeOnCameraChangedListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        globeManager.repaintAllGlobes();
    }

    @Override
    public void onCameraChanged(MapRenderer renderer)
    {
        updateCurrentAreaOfInterest();
    }

    @Override
    public void preferenceEntryChanged(PreferenceEntryChange change)
    {
        if (change.getPreferenceEntry() == ProjectPreferenceEntry.DTED_STREAM ||
                change.getPreferenceEntry() == ProjectPreferenceEntry.WMS_OFFLINE_MODE)
        {
            elevationDownloader.setEnabled(isDownloadEnabled());
            onContentChanged();
        } else if (change.getPreferenceEntry() == ProjectPreferenceEntry.DTED_STREAM_SERVER)
        {
            elevationDownloader.setDownloadServer(change.getStringValue());
        }
    }

    @InvokeOnEDT
    @Override
    public void onContentChanged()
    {
        elevationContentChangedDispatcher.contentChanged();
        globeManager.repaintAllGlobes();
    }

    /**
     * Gets the {@link ElevationContentChangedDispatcher} that is used to signal new DTED files have been downloaded. This elevation source must be made visible
     * for DTED notifications to work.
     * @return the {@link ElevationContentChangedDispatcher}
     * @since 5.2
     */
    ElevationContentChangedDispatcher getElevationContentChangedDispatcher()
    {
        return elevationContentChangedDispatcher;
    }

    /**
     * @return the {@link File} that points to where downloaded DTED files are saved
     * @since 5.2
     */
    public File getDtedDir()
    {
        return dtedDir;
    }

    private boolean isDownloadEnabled()
    {
        return ProjectPreferenceEntry.DTED_STREAM.getBooleanValue() &&
                !ProjectPreferenceEntry.WMS_OFFLINE_MODE.getBooleanValue() &&
                !PreferenceEntry.CLOSED_NETWORK.getBooleanValue();
    }

    @InvokeOnEDT
    private void updateCurrentAreaOfInterest()
    {
        List<Envelope> envelopes = new ArrayList<>();
        globeManager.getAllGlobeDelegates().forEach(globe -> {
            SurfaceRendererControl control = globe.getPortableMapRenderer().getControl(SurfaceRendererControl.class);
            if (control != null)
            {
                envelopes.addAll(control.getSurfaceBounds());
            }
        });
        GeoBounds[] aois = envelopes.stream()
                .map(e -> new GeoBounds(e.minY, e.minX, e.minZ, e.maxY, e.maxX, e.maxZ))
                .toArray(GeoBounds[]::new);
        elevationDownloader.setCurrentAreasOfInterest(aois);

        // Skip call to scanCurrentAreas to prevent trigger of LimitingThread
        if (isDownloadEnabled()) elevationDownloader.scanCurrentAreas();
    }
}
