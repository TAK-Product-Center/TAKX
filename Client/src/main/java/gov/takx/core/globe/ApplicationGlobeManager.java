
package gov.takx.core.globe;

import com.atakmap.android.maps.tilesets.TilesetLayerInfoSpi;
import com.atakmap.map.formats.cdn.StreamingContentDatasetDescriptorSpi;
import com.atakmap.map.formats.kmz.KMZTileReaderSpi;
import com.atakmap.map.formats.kmz.KmzLayerInfoSpi;
import com.atakmap.map.gdal.GdalLibrary;
import com.atakmap.map.layer.Layers;
import com.atakmap.map.layer.feature.FeatureDataSourceContentFactory;
import com.atakmap.map.layer.feature.FeatureDataStore2.FeatureQueryParameters;
import com.atakmap.map.layer.feature.falconview.FalconViewFeatureDataSource;
import com.atakmap.map.layer.feature.ogr.OgrFeatureDataSource;
import com.atakmap.map.layer.raster.DatasetDescriptorFactory2;
import com.atakmap.map.layer.raster.apass.ApassLayerInfoSpi;
import com.atakmap.map.layer.raster.gdal.GdalLayerInfo;
import com.atakmap.map.layer.raster.gpkg.GeoPackageLayerInfoSpi;
import com.atakmap.map.layer.raster.gpkg.GeoPackageMosaicDatabase;
import com.atakmap.map.layer.raster.gpkg.opengl.GLGeoPackageTilesLayer2;
import com.atakmap.map.layer.raster.mbtiles.MBTilesMosaicDatabase;
import com.atakmap.map.layer.raster.mbtiles.MOMAPMosaicDatabase;
import com.atakmap.map.layer.raster.mobac.MobacMapSourceLayerInfoSpi;
import com.atakmap.map.layer.raster.mosaic.ATAKMosaicDatabase3;
import com.atakmap.map.layer.raster.mosaic.MosaicDatabaseFactory2;
import com.atakmap.map.layer.raster.mosaic.opengl.GLMosaicMapLayer2;
import com.atakmap.map.layer.raster.nativeimagery.NativeImageryMosaicDatabase2;
import com.atakmap.map.layer.raster.opengl.GLMapLayerFactory;
import com.atakmap.map.layer.raster.osm.OSMDroidMosaicDatabase;
import com.atakmap.map.layer.raster.pfps.PfpsLayerInfoSpi;
import com.atakmap.map.layer.raster.tilereader.TileReaderFactory;
import com.atakmap.net.AtakAuthenticationCredentials;
import com.atakmap.net.AtakAuthenticationDatabase;
import com.atakmap.net.AtakAuthenticationHandlerHTTP;
import com.atakmap.util.ConfigOptions;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.tak.api.engine.map.MapRendererFactory;
import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.event.ShuttingDown;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.globemessage.HideGlobeMessageEvent;
import gov.takx.api.event.globemessage.ShowGlobeMessageEvent;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IGlobeLifeCycleListener;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.configuration.KernelConfigOptionConstants;
import gov.takx.core.globe.tak.JOGLMapRendererSpi;
import gov.takx.core.globe.tak.ww.ApplicationSceneController;
import gov.takx.core.globe.tak.ww.NoOpTessellator;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.core.view.gui.docking.DockingManager;
import gov.takx.platform.logging.NullProgressMonitor;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import javax.swing.SwingUtilities;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.MouseInfo;
import java.awt.Point;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Manages the set of {@link IApplicationGlobe} for TAK globes.
 *
 * @since 3.4
 */
@ApplicationScoped
public class ApplicationGlobeManager implements IExtendedGlobeManager
{
    private final boolean platformUnderTest;
    private final boolean headless;
    private final DockingManager dockingManager;
    private final boolean multipleGlobesEnabled;
    private final PluginRendererManager pluginRendererManager;
    private final Event<ShowGlobeMessageEvent> showGlobeMessageEvent;
    private final Event<HideGlobeMessageEvent> hideGlobeMessageEvent;

    /**
     * Track all globes by id.
     */
    private final Map<Integer, ApplicationGlobe> globesById = new ConcurrentHashMap<>();

    /**
     * Keep a reference to the primary globe since it will be needed to create additional globes.
     */
    private ApplicationGlobe primaryGlobe;

    /**
     * The last globe to have focus. May be {@code null} if no globe has acquired focus.
     */
    private volatile IApplicationGlobe lastFocusedGlobe;

    /**
     * List of listeners that will be notified of various globe lifecycle events (not just the initial globe).
     */
    private final List<IGlobeLifeCycleListener> globeLifeCycleListeners = new CopyOnWriteArrayList<>();

    /**
     * Track the next available globe id. Reset with each new project.
     */
    private int nextGlobeId = IApplicationGlobe.PRIMARY_GLOBE_ID;

    /**
     * The list of handlers to be invoked as each globe becomes ready.
     */
    private final List<Consumer<IGlobeDelegate>> readyHandlers = new CopyOnWriteArrayList<>();

    // Tracks whether we can construct plugin renderers as globes are created
    private boolean pluginRenderersAvailable;

    private BiConsumer<ApplicationGlobe, IProgressMonitor> cleanupMethod = ApplicationGlobe::onClearState;

    /**
     * constructor.
     */
    @Inject
    ApplicationGlobeManager(@PlatformUnderTest boolean platformUnderTest, @Headless boolean headless, DockingManager dockingManager,
                            @ProgramPreference(PreferenceEntry.GLOBE_ENABLE_MULTIPLE_GLOBES) boolean multipleGlobesEnabled,
                            PluginRendererManager pluginRendererManager,
                            Event<ShowGlobeMessageEvent> showGlobeMessageEvent, Event<HideGlobeMessageEvent> hideGlobeMessageEvent)
    {
        this.platformUnderTest = platformUnderTest;
        this.headless = headless;
        this.dockingManager = dockingManager;
        this.multipleGlobesEnabled = multipleGlobesEnabled;
        this.pluginRendererManager = pluginRendererManager;
        this.showGlobeMessageEvent = showGlobeMessageEvent;
        this.hideGlobeMessageEvent = hideGlobeMessageEvent;
    }

    /**
     * All of this code used to be in a static initializer block.  This could cause problems when running individual
     * tests that had a class dependency chain that ended up here.  The initial case was in JOGLMapRendererSPI and
     * it caused a recursive invocation of computeIfAbsent inside {@link gov.takx.api.aspects.PostInitInjector} when
     * {@link #registerProviders()} was called.
     * <p/>
     * It seemed better to make this operation only occur when this class instance is fully realized instead of when
     * this class is loaded/initialized.
     */
    @PostConstruct
    private void postConstruct()
    {
        GdalLibrary.init();
        org.gdal.gdal.gdal.SetConfigOption("CPL_LOG_ERRORS", "OFF");

        // This option allows importing SHP files without requiring an associated SHX file
        org.gdal.gdal.gdal.SetConfigOption("SHAPE_RESTORE_SHX", "YES");

        // `enable-vsync` syncs the renderer framerate to the display refresh
        if ("true".equals(System.getProperty("enable-vsync", "false"))) ConfigOptions.setOption(KernelConfigOptionConstants.ENABLE_VSYNC, 1);

        // Configure display of on-screen diagnostics related to the globe
        if ("true".equals(System.getProperty("diagnostics", "false"))) ConfigOptions.setOption(KernelConfigOptionConstants.SHOW_RENDER_DIAGNOSTICS, 1);

        // Suppress some excessive duplicate logging
        ConfigOptions.setOption(KernelConfigOptionConstants.SUPPRESS_DUPLICATE_LOGS, 1);

        // Set the font atlas path
        ConfigOptions.setOption(KernelConfigOptionConstants.DEFAULT_FONT_ATLAS_URI, "asset://fonts");
        ConfigOptions.setOption(KernelConfigOptionConstants.DEFAULT_FONT_NAME, "Arial");

        // Support a custom EGM96 data file
        final String egm96Datafile = System.getProperty("egm96.datafile");
        if (egm96Datafile != null) ConfigOptions.setOption(KernelConfigOptionConstants.EGM96_DATA_FILE, new File(egm96Datafile).getAbsolutePath());

        // provider registration
        registerProviders();

        // Set the HTTP authentication handler
        AtakAuthenticationHandlerHTTP.setCallback(new AuthenticationCallbackHandler());

        Configuration.setValue(AVKey.SCENE_CONTROLLER_CLASS_NAME, ApplicationSceneController.class.getName());
        Configuration.setValue(AVKey.TESSELLATOR_CLASS_NAME, NoOpTessellator.class.getName());
    }

    private synchronized void startService(@Observes @Priority(ServiceOrdering.START_GlobeService) StartProjectServices event)
    {
        event.getProgressMonitor().setText("Loading Globe");

        updateWorldWindProxyFromPreferences();

        primaryGlobe = getPrimaryGlobeDelegate();
        primaryGlobe.initialize();
    }

    /**
     * Once the plugin containers have been created, we need to load any plugin provided renderers and install them on
     * all existing globes.
     * 
     * @since 5.2 
     */
    private synchronized void loadPluginRenderers(@Observes @Priority(ServiceOrdering.START_DeploymentContainerManager + 1) StartProjectServices ignored)
    {
        // The globes aren't fully realized at this point (no GL context), so postpone actual construction until they are
        globesById.values().forEach(globe -> globe.doWhenReady(() -> {
            globe.setPluginRenderers(pluginRendererManager.constructRenderers(globe));
            pluginRenderersAvailable = true;
        }));
    }

    private synchronized void stopService(@Observes @Priority(ServiceOrdering.STOP_GlobeService) StopProjectServices event)
    {
        final var progressMonitor = event.getProgressMonitor();

        // Clear secondary globes first.
        globesById.values().stream()
                .filter(globe -> !globe.isPrimaryGlobe())
                .forEach(globe -> cleanupMethod.accept(globe, progressMonitor));

        // Clear the primary globe.
        if (primaryGlobe != null) cleanupMethod.accept(primaryGlobe, progressMonitor);

        globesById.clear();
        readyHandlers.clear();
        nextGlobeId = IApplicationGlobe.PRIMARY_GLOBE_ID;
        primaryGlobe = null;
        lastFocusedGlobe = null;
        pluginRenderersAvailable = false;
    }

    private void onShuttingDown(@Observes ShuttingDown ignored)
    {
        cleanupMethod = ApplicationGlobe::onShutdown;
    }

    /**
     * Update the WW Proxy configuration according to the user preferences.
     *
     * @since 2.35
     */
    private void updateWorldWindProxyFromPreferences()
    {
        boolean proxyEnabled = UserPreferenceEntry.WW_PROXY_ENABLED.getBooleanValue();
        String proxyHost = UserPreferenceEntry.WW_PROXY_HOST.getStringValue();
        String proxyPort = UserPreferenceEntry.WW_PROXY_PORT.getStringValue();
        String proxyType = UserPreferenceEntry.WW_PROXY_TYPE.getStringValue();

        if (!proxyEnabled || StringUtils.isEmpty(proxyHost) || StringUtils.isEmpty(proxyPort) || StringUtils.isEmpty(proxyType))
        {
            Configuration.removeKey(AVKey.URL_PROXY_HOST);
            Configuration.removeKey(AVKey.URL_PROXY_PORT);
            Configuration.removeKey(AVKey.URL_PROXY_TYPE);
        } else
        {
            Configuration.setValue(AVKey.URL_PROXY_HOST, proxyHost);
            Configuration.setValue(AVKey.URL_PROXY_PORT, proxyPort);
            Configuration.setValue(AVKey.URL_PROXY_TYPE, proxyType);
        }
    }

    @Override
    public void setLastFocusedGlobe(IApplicationGlobe globe)
    {
        lastFocusedGlobe = globe;
    }

    @Override
    public void stopTracking(IMapObject mapObject)
    {
        // Loop through all the globes and stop tracking on this MO
        for (IApplicationGlobe globe : globesById.values())
        {
            if (globe.isCenterOn(mapObject)) globe.stopCenterOn();
            if (globe.isKeepInView(mapObject)) globe.removeKeepInView(mapObject);
        }
    }

    @Override
    public void setCursorOnAllGlobes(Cursor cursor)
    {
        if (!headless) globesById.values().forEach(globe -> globe.setCursor(cursor));
    }

    @Override
    public void showGlobeMessage(@Nonnull String message, @Nullable IGlobeDelegate globe)
    {
        showGlobeMessageEvent.fire(new ShowGlobeMessageEvent(message, globe));
    }

    @Override
    public void hideGlobeMessage(@Nullable IGlobeDelegate globe)
    {
        hideGlobeMessageEvent.fire(new HideGlobeMessageEvent(globe));
    }

    @Override
    public boolean isAdditionalGlobeCreationEnabled()
    {
        return multipleGlobesEnabled;
    }

    @Nonnull
    @Override
    public synchronized IApplicationGlobe getOrCreateGlobe(int globeId) throws IllegalStateException
    {
        return globesById.containsKey(globeId)
                ? globesById.get(globeId)
                : createGlobe(globeId);
    }

    @Nonnull
    @Override
    public synchronized IApplicationGlobe createGlobe() throws IllegalStateException
    {
        return createGlobe(nextGlobeId);
    }

    @Nonnull
    @Override
    public synchronized IApplicationGlobe createGlobe(int globeId) throws IllegalStateException
    {
        boolean isPrimaryGlobe = globeId == IGlobeDelegate.PRIMARY_GLOBE_ID;

        // Do some checks to make sure we can create the globe as requested.
        if (!isPrimaryGlobe && !multipleGlobesEnabled)
        {
            throw new IllegalStateException("Multiple globes are not permitted due to " +
                    PreferenceEntry.GLOBE_ENABLE_MULTIPLE_GLOBES.getKey() + " program preference.");
        }

        if (!isPrimaryGlobe && (primaryGlobe == null || !primaryGlobe.isGlobeComponentReady()))
        {
            throw new IllegalStateException("Unable to create additional globes before primary globe is initialized.");
        }

        if (globesById.containsKey(globeId))
        {
            throw new IllegalStateException("Globe already exists: " + globeId);
        }

        // Create the new globe. If it's the primary globe, primaryGlobe will just be null.
        ApplicationGlobe globe = new ApplicationGlobe(globeId, primaryGlobe);

        globesById.put(globeId, globe);
        nextGlobeId = Math.max(nextGlobeId + 1, globeId + 1); // Ensure that the next globe id is set above this id

        if (!isPrimaryGlobe) globe.onLoadState(new NullProgressMonitor());

        if (pluginRenderersAvailable) globe.setPluginRenderers(pluginRendererManager.constructRenderers(globe));

        readyHandlers.forEach(handler -> globe.doWhenReady(() -> handler.accept(globe)));

        return globe;
    }

    @Override
    public void doWhenGlobeReady(@Nonnull Consumer<IGlobeDelegate> handler)
    {
        readyHandlers.add(handler);

        globesById.values().forEach(globe -> globe.doWhenReady(() -> handler.accept(globe)));
    }

    @Nonnull
    @Override
    public synchronized IGlobeDelegate createGlobeDockable() throws IllegalStateException
    {
        return createGlobeDockable(nextGlobeId, null);
    }

    @Nonnull
    @Override
    public synchronized IGlobeDelegate createGlobeDockable(String title) throws IllegalStateException
    {
        return createGlobeDockable(nextGlobeId, title);
    }

    @Nonnull
    @Override
    public synchronized IGlobeDelegate createGlobeDockable(int globeId) throws IllegalStateException
    {
        return createGlobeDockable(globeId, null);
    }

    @Nonnull
    @Override
    public synchronized IGlobeDelegate createGlobeDockable(int globeId, String title) throws IllegalStateException
    {
        // Create a new globe.
        final IApplicationGlobe globe = createGlobe(globeId);

        // Create the dockable component.
        dockingManager.createAndAddGlobeDockable(globe, title);

        // Return the original globe to the caller for reference.
        return globe;
    }

    @Nonnull
    @Override
    public Set<Integer> getGlobeIds()
    {
        return platformUnderTest
                ? Collections.emptySet()
                : new TreeSet<>(globesById.keySet()); // Return them sorted
    }

    @Override
    public int getGlobeCount()
    {
        return globesById.size();
    }

    @Override
    public int getPrimaryGlobeId()
    {
        return IGlobeDelegate.PRIMARY_GLOBE_ID;
    }

    @Nonnull
    @Override
    public ApplicationGlobe getPrimaryGlobeDelegate()
    {
        if (primaryGlobe == null)
        {
            primaryGlobe = (ApplicationGlobe) createGlobe(IGlobeDelegate.PRIMARY_GLOBE_ID);
        }

        return primaryGlobe;
    }

    @Nullable
    @Override
    public IApplicationGlobe getGlobeDelegate(int globeId)
    {
        return globesById.get(globeId);
    }

    @Nonnull
    @Override
    public List<IApplicationGlobe> getAllGlobeDelegates()
    {
        return platformUnderTest
                ? Collections.emptyList()
                : new ArrayList<>(globesById.values());
    }

    @Nullable
    @Override
    public IApplicationGlobe getGlobeForComponent(@Nonnull WorldWindow wwd)
    {
        // There are never many globes, so just scan the set.  If we find this is ever too slow, we can create
        // another map.

        return globesById.values()
                .stream()
                .filter(globe -> globe.getWorldWindow() == wwd)
                .findFirst()
                .orElse(null);
    }

    @Override
    public synchronized void deleteGlobe(int globeId)
    {
        if (globeId == IGlobeDelegate.PRIMARY_GLOBE_ID)
        {
            throw new UnsupportedOperationException("You can not remove the primary globe");
        }

        dockingManager.removeGlobeDockableById(globeId);

        IApplicationGlobe globe = globesById.remove(globeId);

        if (globe == lastFocusedGlobe)
        {
            lastFocusedGlobe = null;
        }

        if (globe != null)
        {
            globe.deactivate();
        } else
        {
            throw new IllegalStateException("This globe has already been removed");
        }
    }

    @Nullable
    @Override
    public IApplicationGlobe getLastFocusedGlobe()
    {
        return lastFocusedGlobe != null ? lastFocusedGlobe : primaryGlobe;
    }

    @Override
    public void requestRedraw(@Nonnull String reason, boolean immediate)
    {
        if (!headless) globesById.values().forEach(globe -> globe.redraw(immediate));
    }

    @Override
    public void addGlobeLifeCycleListener(@Nonnull IGlobeLifeCycleListener listener)
    {
        if (!globeLifeCycleListeners.contains(listener))
        {
            globeLifeCycleListeners.add(listener);
        }
    }

    @Override
    public void removeGlobeLifeCycleListener(@Nonnull IGlobeLifeCycleListener listener)
    {
        globeLifeCycleListeners.remove(listener);
    }

    @Nonnull
    @Override
    public List<IGlobeLifeCycleListener> getGlobeLifeCycleListeners()
    {
        return Collections.unmodifiableList(globeLifeCycleListeners);
    }

    @Override
    public void repaintAllGlobes()
    {
        if (!headless) globesById.values().forEach(globe -> globe.redraw(true));
    }

    @Override
    public void setSelectListenersActive(boolean active)
    {
        getAllGlobeDelegates().forEach(globe -> globe.setSelectListenerActive(active));
    }

    @Override
    public boolean mouseLocationInAnyGlobe()
    {
        final Point mouseLocation = MouseInfo.getPointerInfo().getLocation();

        // Iterate through all the globes and see if the point is in one of them
        return getAllGlobeDelegates().stream().anyMatch(globe -> {
            final Container container = globe.getWorldWindow().getComponent().getParent();
            final Point globePoint = new Point(mouseLocation);
            SwingUtilities.convertPointFromScreen(globePoint, container);

            return container.contains(globePoint);
        });
    }

    private void registerProviders()
    {
        // Register Interop
        new FeatureQueryParameters();

        // TilesetMapComponent
        GLMapLayerFactory.registerSpi(GLGeoPackageTilesLayer2.SPI);

        DatasetDescriptorFactory2.register(TilesetLayerInfoSpi.INSTANCE);
        DatasetDescriptorFactory2.register(MobacMapSourceLayerInfoSpi.INSTANCE);
        DatasetDescriptorFactory2.register(GeoPackageLayerInfoSpi.INSTANCE);
        DatasetDescriptorFactory2.register(StreamingContentDatasetDescriptorSpi.INSTANCE);
        DatasetDescriptorFactory2.register(KmzLayerInfoSpi.INSTANCE);

        // NativeRenderingMapComponent
        GLMapLayerFactory.registerSpi(GLMosaicMapLayer2.SPI);

        DatasetDescriptorFactory2.register(GdalLayerInfo.INSTANCE);
        DatasetDescriptorFactory2.register(ApassLayerInfoSpi.INSTANCE);
        DatasetDescriptorFactory2.register(PfpsLayerInfoSpi.INSTANCE);

        // AtakMapView.registerServiceProviders()
        // FeatureDataSource
        FeatureDataSourceContentFactory.register(new FalconViewFeatureDataSource(), 2);
        FeatureDataSourceContentFactory.register(new OgrFeatureDataSource(), 1);

        // MosaicDatabaseSpi
        MosaicDatabaseFactory2.register(NativeImageryMosaicDatabase2.SPI);
        MosaicDatabaseFactory2.register(GeoPackageMosaicDatabase.SPI);
        MosaicDatabaseFactory2.register(OSMDroidMosaicDatabase.SPI);
        MosaicDatabaseFactory2.register(MBTilesMosaicDatabase.SPI);
        MosaicDatabaseFactory2.register(MOMAPMosaicDatabase.SPI);
        MosaicDatabaseFactory2.register(ATAKMosaicDatabase3.SPI);

        TileReaderFactory.registerSpi(KMZTileReaderSpi.INSTANCE);

        Layers.registerAll();

        MapRendererFactory.registerSpi(new JOGLMapRendererSpi());
    }

    private static class AuthenticationCallbackHandler implements AtakAuthenticationHandlerHTTP.OnAuthenticateCallbackV2
    {
        @Override
        public String[] getBasicAuth(URL url, int previousStatus, Map<String, String> properties)
        {
            AtakAuthenticationCredentials credentials = AtakAuthenticationDatabase.getCredentials(AtakAuthenticationCredentials.TYPE_HTTP_BASIC_AUTH, url.getHost());
            if (credentials != null && credentials.username != null && credentials.password != null)
            {
                return new String[]{credentials.username, credentials.password};
            }
            return null;
        }

        @Override
        public String[] getBasicAuth(URL url, int previousStatus)
        {
            return getBasicAuth(url, previousStatus, null);
        }
    }
}
