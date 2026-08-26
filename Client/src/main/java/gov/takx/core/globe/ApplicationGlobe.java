package gov.takx.core.globe;

import com.atakmap.map.Globe;
import com.atakmap.map.LegacyAdapters;
import com.atakmap.map.MapRenderer3;
import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.layer.control.SurfaceRendererControl;
import com.atakmap.map.opengl.GLMapView;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.Animator;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.SceneController;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.AWTInputHandler;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.RenderingListener;
import gov.nasa.worldwind.formats.tiff.GeotiffImageReaderSpi;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.Earth;
import gov.nasa.worldwind.globes.EarthFlat;
import gov.nasa.worldwind.globes.FlatGlobe;
import gov.nasa.worldwind.globes.projections.ProjectionEquirectangular;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.SurfacePolyline;
import gov.nasa.worldwind.util.BasicDragger;
import gov.nasa.worldwind.view.BasicView;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;
import gov.nasa.worldwind.view.orbit.OrbitView;
import gov.nasa.worldwind.view.orbit.OrbitViewLimits;
import gov.nasa.worldwindx.examples.util.HotSpotController;
import gov.tak.api.engine.map.IMapRendererEnums.DisplayMode;
import gov.tak.api.engine.map.IMapRendererEnums.DisplayOrigin;
import gov.tak.api.engine.map.IMapRendererEnums.InverseMode;
import gov.tak.api.engine.map.IMapRendererEnums.InverseResult;
import gov.tak.api.engine.map.MapRenderer;
import gov.tak.api.engine.map.MapRendererFactory;
import gov.tak.api.engine.math.PointD;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.platform.events.MessageTapModel;
import gov.tak.platform.events.TakxGlobeEventService;
import gov.tak.platform.widgets.factory.MapMenuWidgetControllerFactory;
import gov.tak.platform.widgets.factory.MapMenuWidgetFactory;
import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.event.project.ProjectSaving;
import gov.takx.api.event.tak.WWViewInitialized;
import gov.takx.api.globe.GlobeNavigationStyle;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IExtendedPluginRendererManager;
import gov.takx.api.globe.IFavorite;
import gov.takx.api.globe.IGlobeComponentListener;
import gov.takx.api.globe.IGlobeEyeElevationListener;
import gov.takx.api.globe.IGlobeLayerManager;
import gov.takx.api.globe.IGlobeMapObject;
import gov.takx.api.globe.IGlobeMapObjectChangeListener;
import gov.takx.api.globe.IGlobeMapObjectManager;
import gov.takx.api.globe.IProjectFavoritesManager;
import gov.takx.api.globe.IRenderingEngine;
import gov.takx.api.globe.ISnapshotListener;
import gov.takx.api.globe.IWorldWindowGL;
import gov.takx.api.globe.render.RendererConstants;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectSelectListener;
import gov.takx.api.mapobject.MapToolType;
import gov.takx.api.ping.IGeoPingService;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.project.IProjectManager;
import gov.takx.api.ui.event.GlobeCreatedEvent;
import gov.takx.api.ui.event.GlobeRemovedEvent;
import gov.takx.api.ui.event.RenderingEngineChangedEvent;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.EscapeKeyListener;
import gov.takx.core.configuration.ProgramEnvironment;
import gov.takx.core.globe.layers.IGlobeLayer;
import gov.takx.core.globe.layers.worldwind.LayerFactory;
import gov.takx.core.globe.layers.worldwind.NoopWorldwindElevationModelFactory;
import gov.takx.core.globe.render.NullRenderingEngine;
import gov.takx.core.globe.render.RenderingContext;
import gov.takx.core.globe.render.SpatialRenderer;
import gov.takx.core.globe.render.TopDownRenderer;
import gov.takx.core.globe.tak.WidgetLayerManager;
import gov.takx.core.globe.tak.ww.ApplicationOrbitView;
import gov.takx.core.globe.tak.ww.ApplicationSceneController;
import gov.takx.core.globe.tak.ww.TakEarthGlobe;
import gov.takx.core.mapobject.gui.widgets.ContextMapMenuWidgetController;
import gov.takx.core.mapobject.gui.widgets.ContextMenuMapMenuWidgetSpi;
import gov.takx.core.mapobject.gui.widgets.ContextMenuModel;
import gov.takx.core.view.DeleteKeyListener;
import gov.takx.core.view.gui.screenimageutils.SnapshotUtils;
import gov.takx.platform.globe.Favorite;
import gov.takx.platform.logging.NullProgressMonitor;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.PreferencesNode;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.TimeBoundedTask;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.commons.lang3.NotImplementedException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of {@link IApplicationGlobe} for TAK globe.
 *
 * @since 3.4
 */
@PseudoBean
public class ApplicationGlobe implements IApplicationGlobe, RenderingListener, PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /*
     * Provide our own Imagery Layer and Elevation Model factories for the globe.
     */
    static
    {
        Configuration.setValue(AVKey.LAYER_FACTORY, LayerFactory.class.getName());
        Configuration.setValue(AVKey.ELEVATION_MODEL_FACTORY, NoopWorldwindElevationModelFactory.class.getName());
        Configuration.setValue(AVKey.MODEL_CLASS_NAME, MessageTapModel.class.getName());
        Configuration.setValue(AVKey.GLOBE_CLASS_NAME, TakEarthGlobe.class.getName());

        // Ensure factory SPI is registered for IMapMenuWidget and controller creation for ContextMenuModel instances
        MapMenuWidgetFactory.registerSpi(new ContextMenuMapMenuWidgetSpi(), ContextMenuModel.class, 0);
        MapMenuWidgetControllerFactory.registerSpi(ContextMapMenuWidgetController.SPI, ContextMenuModel.class, 0);
    }

    // The maximum pitch allowed while rendering in Top Down mode
    public static final double MAX_TOP_DOWN_PITCH = 45.0;

    // Min zoom prevents infinite zoom-in and perceived unresponsiveness; see WW OrbitViewInputHandler.computeNewZoom()
    private static final Double MIN_ZOOM_METERS = 0.1;

    // Determines in ms how often to repaint the globe
    public static final int REDRAW_DELAY = 950;

    // Time thresholds (in ms) for responding to requests to redraw the globe
    private static final int INITIAL_REDRAW_DELAY = 100;
    private static final int MAX_REDRAW_DELAY = 300;

    @Inject
    @ProgramPreference(PreferenceEntry.TRACK_PRIMARY_RENDERING_MODE)
    private String configuredRenderingMode;

    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    private IExtendedPluginRendererManager pluginRendererManager;

    @Inject
    private SnapshotUtils snapshotUtils;

    @Inject
    private ProgramEnvironment programEnvironment;

    @Inject
    @Headless
    private boolean headless;

    @Inject
    @PlatformUnderTest
    private boolean platformUnderTest;

    @Inject
    private IProjectFavoritesManager projectViewBookmarkManager;

    @Inject
    private IRaptorLayerManager layerManager;

    @Inject
    private IProjectManager projectManager;

    @Inject
    private IGeoPingService pingService;

    @Inject
    private EventBus eventBus;

    @Inject
    @UiEventBus
    private EventBus uiEventBus;

    @Inject
    private TerrainPopupMenuProvider terrainPopupMenuProvider;

    @Inject
    private WidgetLayerManager widgetLayerManager;

    @Inject
    private Event<PrimaryGlobeCreatedEvent> globeEvent;

    private String renderingMode; // Current rendering mode
    private RenderingContext renderingContext;
    private IRenderingEngine primaryRenderingEngine;
    private List<IRenderingEngine> pluginRenderers = List.of();

    // Redraw request handler to throttle the actual redraw to avoid thrashing the rendering operations
    private final TimeBoundedTask redrawRequestHandler = new TimeBoundedTask("globe redraw", INITIAL_REDRAW_DELAY, MAX_REDRAW_DELAY, this::doRedraw);

    private final Globe nativeGlobe = new Globe();
    private final int globeId;
    private ApplicationGlobe primaryGlobe; // Non-null if this is a secondary globe
    private MapRenderer<?> renderer;
    private Component component;
    private IWorldWindowGL wwd;

    private final AtomicBoolean globeComponentReady = new AtomicBoolean(false);
    private final List<Runnable> readyHandlers = new CopyOnWriteArrayList<>();
    private IGlobeLayerManager globeLayerManager;
    private GlobeViewController globeViewController;
    private GlobeMapObjectManager globeMapObjectManager;

    private Animator animator;
    private Timer repaintTimer;
    private boolean repaint = true;
    private DragImageController dragImageController;

    private WWSelectListener wwdSelectListener;

    private final List<IGlobeEyeElevationListener> elevationListeners = new ArrayList<>();

    private final List<IGlobeComponentListener> componentListeners = new ArrayList<>();

    // Keeps us from fire elevation change events if the elevation hasn't changed
    private double previousEyeAltitude;

    private PingHandler pingHandler;

    private GlobePreviewAnnotationManager previewAnnotationManager;

    private TrackingGlobeViewController trackingGlobeViewController;

    private BasicOrbitView initialView;
    private TakViewInputHandler viewInputHandler;
    private gov.nasa.worldwind.globes.Globe initialGlobe;

    // Handler to watch for clicks on terrain to do "center on" or put up the terrain context menu
    private final MouseAdapter globeClickHandler = new GlobeClickHandler();

    private boolean keyboardNavLock;
    private boolean mouseNavLock;

    // Special handler for Ctrl-C on globe - copy the current location to the clipboard
    private final KeyListener copyKeyListener = new CopyKeyHandler();

    private final KeyListener deleteKeyListener = new DeleteKeyListener();

    private final MouseMotionListener navLockMotionListener = new ConsumeAllMouseMotionEvents();
    private final KeyListener navLockKeyListener = new ConsumeAllKeyEvents();
    private final MouseWheelListener navLockWheelListener = InputEvent::consume;

    private final KeyListener escapeListener = new EscapeKeyListener(() -> widgetLayerManager.closeOpenRadialMenus());

    /**
     * We want to keep track of the last globe to have focus so we can properly direct navigation and various other
     * globe-specific operations to the proper globe.
     */
    private final FocusListener globeFocusHandler = new FocusAdapter()
    {
        @Override
        public void focusGained(FocusEvent e)
        {
            globeManager.setLastFocusedGlobe(ApplicationGlobe.this);
        }
    };

    private IFavorite lastView;

    @Inject
    private TakxGlobeEventService globeEventService;

    /**
     * Constructor used to create a "secondary" globe. This one will share the same underlying globe, but maintain
     * a separate model and layer list. The "sharable" layers from the original globe will be copied into this
     * secondary globe.
     */
    public ApplicationGlobe()
    {
        this(PRIMARY_GLOBE_ID, null);
    }

    public ApplicationGlobe(int globeId, ApplicationGlobe primaryGlobe)
    {
        if (globeId != PRIMARY_GLOBE_ID)
        {
            if (primaryGlobe == null)
            {
                throw new IllegalArgumentException("primaryGlobe must not be null");
            }

            this.primaryGlobe = primaryGlobe;
        }

        this.globeId = globeId;
        UserPreferenceEntry.GLOBE_NAVIGATION_STYLE.newMonitor(GlobeNavigationStyle.class, this::setGlobeNavigationStyle);
    }

    @Override
    public int getGlobeId()
    {
        return globeId;
    }

    @Override
    public IApplicationGlobe getGlobeForComponent(WorldWindow wwd)
    {
        return globeManager.getGlobeForComponent(wwd);
    }

    @Override
    public int getGlobeCount()
    {
        return globeManager.getGlobeCount();
    }

    @Override
    public Set<Integer> getGlobeIds()
    {
        return globeManager.getGlobeIds();
    }

    @Override
    public boolean isPrimaryGlobe()
    {
        return primaryGlobe == null;
    }

    @Override
    public IMapObject getCenterOn()
    {
        if (trackingGlobeViewController == null) // Should only happen under test or headless
        {
            if (!headless) throw new IllegalStateException("Globe not initialized");
            return null;
        }

        return trackingGlobeViewController.getCenterOn();
    }

    @Override
    public void startCenterOn(IMapObject mapObject)
    {
        if (trackingGlobeViewController == null) // Should only happen under test or headless
        {
            if (!headless) throw new IllegalStateException("Globe not initialized");
            return;
        }

        trackingGlobeViewController.startCenterOn(mapObject);
    }

    @Override
    public void stopCenterOn()
    {
        if (trackingGlobeViewController == null) // Should only happen under test or headless
        {
            if (!headless) throw new IllegalStateException("Globe not initialized");
            return;
        }

        trackingGlobeViewController.stopCenterOn();
    }

    @Override
    public boolean isCenterOn(IMapObject mapObject)
    {
        if (trackingGlobeViewController == null) // Should only happen under test
        {
            if (!headless) throw new IllegalStateException("Globe not initialized");
            return false;
        }

        return trackingGlobeViewController.isCenterOn(mapObject);
    }

    @Override
    public void addKeepInView(IMapObject mapObject)
    {
        if (trackingGlobeViewController == null) // Should only happen under test or headless
        {
            if (!headless) throw new IllegalStateException("Globe not initialized");
            return;
        }

        trackingGlobeViewController.addKeepInView(mapObject);
    }

    @Override
    public void removeKeepInView(IMapObject mapObject)
    {
        if (trackingGlobeViewController == null) // Should only happen under test or headless
        {
            if (!headless) throw new IllegalStateException("Globe not initialized");
            return;
        }

        trackingGlobeViewController.removeKeepInView(mapObject);
    }

    @Override
    public boolean isKeepInView(IMapObject mapObject)
    {
        if (trackingGlobeViewController == null) // Should only happen under test or headless
        {
            if (!headless) throw new IllegalStateException("Globe not initialized");
            return false;
        }

        return trackingGlobeViewController.isKeepInView(mapObject);
    }

    @Override
    public void copyPositionToClipboard()
    {
        Position position = wwd.getCurrentPosition();

        if (position != null)
        {
            copyPositionToClipboard(position);
        } else
        {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection("off globe"), null);
        }
    }

    @Override
    public void copyLocationToClipboard(@Nonnull ILocation location)
    {
        // Convert to a string using the proper coordinate system
        final String positionString = CoordinateSystem.getLocationString(location, UserPreferenceEntry.COORDINATE_SYSTEM.getValueAs(CoordinateSystem.class));

        // Send the string to the clipboard
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(positionString), null);
    }

    @Override
    public void addGlobeEyeElevationListener(IGlobeEyeElevationListener globeEyeElevationListener)
    {
        elevationListeners.add(globeEyeElevationListener);
    }

    @Override
    public void removeGlobeEyeElevationListener(IGlobeEyeElevationListener globeEyeElevationListener)
    {
        elevationListeners.remove(globeEyeElevationListener);
    }

    @Override
    public void setCursor(@Nonnull Cursor cursor)
    {
        getWorldWindow().getComponent().setCursor(cursor);
    }

    @Override
    public void setMapTool(MapToolType mapToolType)
    {
        if (null != globeMapObjectManager) globeMapObjectManager.setMapTool(mapToolType);
    }

    @Override
    public IWorldWindowGL getWorldWindow()
    {
        return wwd;
    }

    @Override
    public Globe getNativeGlobe()
    {
        return nativeGlobe;
    }

    @Override
    @Nonnull
    public GlobeViewController getGlobeViewController()
    {
        return globeViewController;
    }

    /**
     * @return the {@link TakViewInputHandler} for this globe
     * @since 5.1
     */
    public TakViewInputHandler getViewInputHandler()
    {
        return viewInputHandler;
    }

    @Override
    public MapRenderer<?> getPortableMapRenderer()
    {
        return renderer;
    }

    @Override
    public MapRenderer3 getMapRenderer()
    {
        return getGLMapView();
    }

    @Override
    public GLMapView getGLMapView()
    {
        return LegacyAdapters.adapt(renderer);
    }

    @Override
    public RenderingContext getRenderingContext()
    {
        return renderingContext;
    }

    @Override
    public IRenderingEngine getPrimaryRenderingEngine()
    {
        return primaryRenderingEngine;
    }

    @Override
    public JPanel createFocusHandlingPanel()
    {
        return new FocusHandlingPanel(wwd.getComponent());
    }

    @Override
    public void redraw(boolean immediate)
    {
        redrawRequestHandler.request();
        if (immediate) redrawRequestHandler.processNow(false);
    }

    /**
     * Perform the actual redraw on the globe.
     */
    void doRedraw()
    {
        if (wwd != null)
        {
            wwd.redraw();
            MapRenderer<?> renderer = getPortableMapRenderer();
            if (renderer != null)
            {
                renderer.getRenderContext().queueEvent(() -> renderer.getControl(SurfaceRendererControl.class).markDirty());
            }
        }
    }

    @Override
    public synchronized void addGlobeComponentListener(IGlobeComponentListener l)
    {
        if (!componentListeners.contains(l)) componentListeners.add(l);

        // If we are already initialized, then immediately signal the listener
        if (globeComponentReady.get()) SwingUtilities.invokeLater(() -> safeInvoke(l::globeInitialized, "globeInitialized"));
    }

    @Override
    public void removeGlobeComponentListener(IGlobeComponentListener l)
    {
        componentListeners.remove(l);
    }

    @Override
    public void addSelectListener(IMapObjectSelectListener listener)
    {
        wwdSelectListener.addListener(listener);
    }

    @Override
    public void removeSelectListener(IMapObjectSelectListener listener)
    {
        if (wwdSelectListener != null) wwdSelectListener.removeListener(listener);
    }

    @Override
    public boolean isGlobeComponentReady()
    {
        return globeComponentReady.get();
    }

    @Override
    public synchronized void doWhenReady(Runnable handler)
    {
        if (globeComponentReady.get())
        {
            SwingUtilities.invokeLater(() -> safeInvoke(handler, "ready handler"));
        } else
        {
            readyHandlers.add(handler);
        }
    }

    @Override
    public IFavorite getViewBookmark()
    {
        final BasicView view = wwd != null ? (BasicView) wwd.getView() : null;

        if (view == null) return null;

        double zoomOrElevation = (view instanceof OrbitView orbitView)
                ? orbitView.getZoom()
                : view.getEyePosition().elevation;

        Position position = view.getCenterPosition();

        return Favorite.createViewBookmarkAsUser("Camera Eye", position.getLatitude().getDegrees(),
                position.getLongitude().getDegrees(), position.getElevation(), view.getHeading().getDegrees(),
                view.getPitch().getDegrees(), zoomOrElevation);
    }

    @Override
    public void setMouseNavLock(boolean lock)
    {
        logger.debug("Setting Mouse Nav Lock:{}", lock);

        if (mouseNavLock != lock)
        {
            if (isPrimaryGlobe()) PreferenceEntry.GLOBE_DISABLE_MOUSE_GLOBE_NAVIGATION.put(lock);
            internalSetMouseNavLock(lock);
        }
    }

    /**
     * Internal mechanics of setting the "mouse navigation lock" parameter.
     */
    private void internalSetMouseNavLock(boolean lock)
    {
        mouseNavLock = lock;

        if (lock)
        {
            addMouseMotionListener(navLockMotionListener);
            addMouseWheelListener(navLockWheelListener);
        } else
        {
            removeMouseMotionListener(navLockMotionListener);
            removeMouseWheelListener(navLockWheelListener);
        }
    }

    private void setGlobeNavigationStyle(GlobeNavigationStyle globeNavigationStyle)
    {
        TakViewInputHandler inputHandler = getViewInputHandler();
        switch (globeNavigationStyle)
        {
            case WinTAK ->
            {
                inputHandler.setConvertMouseTiltToRotate(true);
                inputHandler.setConvertMouseButtonZoomToTilt(true);
                inputHandler.setConvertKeyRotateToZoom(true);
                inputHandler.setConvertKeyZoomToCenter(true);
            }
            case TAKX ->
            {
                inputHandler.setConvertMouseTiltToRotate(false);
                inputHandler.setConvertMouseButtonZoomToTilt(false);
                inputHandler.setConvertKeyRotateToZoom(false);
                inputHandler.setConvertKeyZoomToCenter(false);
            }
        }
    }

    @Override
    public void setKeyboardNavLock(boolean lock)
    {
        logger.debug("Setting Keyboard Nav Lock:{}", lock);

        if (keyboardNavLock != lock)
        {
            if (isPrimaryGlobe()) PreferenceEntry.GLOBE_DISABLE_KEYBOARD_GLOBE_NAVIGATION.put(lock);
            internalSetKeyboardNavLock(lock);
        }
    }

    /**
     * Internal mechanics of setting the "keyboard navigation lock" parameter.
     */
    private void internalSetKeyboardNavLock(boolean lock)
    {
        keyboardNavLock = lock;

        if (lock)
        {
            wwd.getInputHandler().addKeyListener(navLockKeyListener);
        } else
        {
            wwd.getInputHandler().removeKeyListener(navLockKeyListener);
        }
    }

    @Override
    public void setDisableCenterGlobeOnClick(boolean lock)
    {
        internalSetDisableCenterGlobeOnClick(lock);
        if (isPrimaryGlobe()) PreferenceEntry.GLOBE_DISABLE_CENTER_GLOBE_ON_CLICK.put(lock);
    }

    /**
     * Internal mechanics of setting the "disable center globe on click" parameter.
     */
    private void internalSetDisableCenterGlobeOnClick(boolean lock)
    {
        viewInputHandler.setCenterOnClickNavLock(lock);
    }

    @Override
    public void createSnapshot(ILocation minLocation, ILocation maxLocation, SnapshotFormat snapshotImageFormat, File outputFile, ISnapshotListener snapshotListener)
    {
        snapshotUtils.createSnapshot(this, minLocation, maxLocation, outputFile, snapshotImageFormat, snapshotListener, false, true);
    }

    @Override
    public void createSnapshotWithFileChooser(ILocation minLocation, ILocation maxLocation, ISnapshotListener snapshotListener)
    {
        JFileChooser fileChooser = snapshotUtils.getSnapshotFileChooser();
        int returnValue = fileChooser.showSaveDialog(JFrame.getFrames()[0]);
        if (returnValue == JFileChooser.APPROVE_OPTION)
        {
            final File selectedFile = fileChooser.getSelectedFile();
            SnapshotFormat snapshotFormat = SnapshotFormat.getValueByExtension(FileUtils.getExtension(selectedFile));
            if (snapshotFormat == null)
            {
                snapshotFormat = SnapshotFormat.GEOTIFF;
            }
            createSnapshot(minLocation, maxLocation, snapshotFormat, selectedFile, snapshotListener);
        }
    }

    @Override
    public void goToImmediate(Sector sector)
    {
        globeViewController.goToImmediate(sector);
    }

    @Override
    public GeoPoint getCenterLocation()
    {
        final Position position = getCenterPosition();
        return new GeoPoint(position.getLatitude().getDegrees(), position.getLongitude().getDegrees(), position.getElevation());
    }

    @Override
    public Position getCenterPosition()
    {
        return ((BasicView) wwd.getView()).getCenterPosition();
    }

    @Override
    public GeoPoint getGlobeLocationFromPoint(Point p)
    {
        final var geoPoint = gov.tak.api.engine.map.coords.GeoPoint.createMutable();
        final var inverse = renderer.inverse(new PointD(p.x, p.y, 0), geoPoint, InverseMode.RayCast, 0, DisplayOrigin.UpperLeft);

        if (inverse != InverseResult.None)
        {
            // The ray cast intersects with the displayed terrain, but because of LOD handling the returned
            // altitude may be inaccurate.  So, get it from the elevation manager.
            final double latitude = geoPoint.getLatitude();
            final double longitude = geoPoint.getLongitude();
            final double hae = ElevationManager.getElevation(latitude, longitude, null);
            return new GeoPoint(latitude, longitude, hae);
        } else
        {
            return null;
        }
    }

    @Override
    public IGlobeMapObject getGlobeMapObject(String key)
    {
        return globeMapObjectManager.getGlobeMapObject(key);
    }

    @Override
    public void setFlatWorldMode(boolean flatGlobe)
    {
        if (flatGlobe)
        {
            initialGlobe = wwd.getModel().getGlobe();
            wwd.getModel().setGlobe(new EarthFlat());

            // Update globe projection - Lat/Lon projection works well for captured GeoTIFFs
            ((FlatGlobe) wwd.getModel().getGlobe()).setProjection(new ProjectionEquirectangular());
            getPortableMapRenderer().setDisplayMode(DisplayMode.Flat);
        } else
        {
            wwd.getModel().setGlobe(Objects.requireNonNullElseGet(initialGlobe, Earth::new));
            wwd.setView(initialView);
            getPortableMapRenderer().setDisplayMode(DisplayMode.Globe);
        }
        wwd.redraw();
    }

    @Override
    public void addGlobeMapObjectChangeListener(IGlobeMapObjectChangeListener globeMapObjectChangeListener)
    {
        globeMapObjectManager.addMapObjectChangeListener(globeMapObjectChangeListener);
    }

    @Override
    public void removeGlobeMapObjectChangeListener(IGlobeMapObjectChangeListener globeMapObjectChangeListener)
    {
        if (globeMapObjectManager != null)
        {
            globeMapObjectManager.removeMapObjectChangeListener(globeMapObjectChangeListener);
        }
    }

    @Override
    public void setSelectListenerActive(boolean setActive)
    {
        if (wwdSelectListener != null)
        {
            wwdSelectListener.setArmed(setActive);
        }
    }

    @Override
    public void addMouseListener(MouseListener listener)
    {
        if (wwd != null) wwd.getInputHandler().addMouseListener(listener);
    }

    @Override
    public void removeMouseListener(MouseListener listener)
    {
        if (wwd != null) wwd.getInputHandler().removeMouseListener(listener);
    }

    @Override
    public void addMouseMotionListener(MouseMotionListener listener)
    {
        if (wwd != null) wwd.getInputHandler().addMouseMotionListener(listener);
    }

    @Override
    public void removeMouseMotionListener(MouseMotionListener listener)
    {
        if (wwd != null) wwd.getInputHandler().removeMouseMotionListener(listener);
    }

    @Override
    public void addMouseWheelListener(MouseWheelListener listener)
    {
        if (wwd != null) wwd.getInputHandler().addMouseWheelListener(listener);
    }

    @Override
    public void removeMouseWheelListener(MouseWheelListener listener)
    {
        if (wwd != null) wwd.getInputHandler().removeMouseWheelListener(listener);
    }

    @Override
    public IGlobeMapObjectManager getGlobeMapObjectManager()
    {
        return globeMapObjectManager;
    }

    @Override
    public IGlobeLayerManager getGlobeLayerManager()
    {
        return globeLayerManager;
    }

    @Override
    @Nullable
    public IGlobeLayer getGlobeLayer(IRaptorLayer raptorLayer)
    {
        return globeLayerManager != null ? globeLayerManager.getGlobeLayer(raptorLayer) : null;
    }

    @Override
    public File getImageryDirectoryFile()
    {
        throw new NotImplementedException();
    }

    @Override
    public List<File> getImageryDirectoryFileExtra()
    {
        throw new NotImplementedException();
    }

    @Override
    public boolean isInstallLocation(String s)
    {
        throw new NotImplementedException();
    }

    @Override
    public IRaptorLayer addImageryLayer(Layer layer)
    {
        return addLayer(layer, LayerGroup.IMAGERY, true);
    }

    @Override
    public IRaptorLayer addLayer(Layer layer, LayerGroup layerGroup)
    {
        return addLayer(layer, layerGroup, true);
    }

    @Override
    public IRaptorLayer addLayer(Layer layer, LayerGroup layerGroup, boolean sharable)
    {
        int layerGlobeId = sharable ? IRaptorLayer.ALL_GLOBES : globeId;
        return layerManager.addLayer(layer, layerGroup, layerGlobeId);
    }

    @Override
    public IRaptorLayer addLayer(com.atakmap.map.layer.Layer layer, LayerGroup layerGroup)
    {
        return addLayer(layer, layerGroup, true);
    }

    @Override
    public IRaptorLayer addLayer(com.atakmap.map.layer.Layer layer, LayerGroup layerGroup, boolean sharable)
    {
        int layerGlobeId = sharable ? IRaptorLayer.ALL_GLOBES : globeId;
        return layerManager.addLayer(layer, layerGroup, layerGlobeId);
    }

    @Override
    public IRaptorLayer getLayer(String name)
    {
        if (name == null) return null;

        for (IRaptorLayer layer : layerManager.getLayers())
        {
            if (name.equals(layer.getName()))
            {
                return layer;
            }
        }
        for (IRaptorLayer layer : layerManager.getModelLayers())
        {
            if (name.equals(layer.getName()))
            {
                return layer;
            }
        }
        return null;
    }

    @Override
    public IRaptorLayer getLayer(String name, LayerGroup group)
    {
        if (name == null) return null;

        for (IRaptorLayer layer : layerManager.getLayerList(group))
        {
            if (name.equals(layer.getName())) return layer;
        }

        return null;
    }

    @Override
    public void setWMSOfflineMode(boolean offlineMode)
    {
        logger.debug("Setting Network Offline Mode: {}", offlineMode);
        WorldWind.getNetworkStatus().setOfflineMode(offlineMode);
    }

    @Override
    public void removeLayer(IRaptorLayer layer)
    {
        layerManager.removeLayer(layer, false);
    }

    @Override
    public void setZOrder(IMapObject mapObject, int zOrder)
    {
        primaryRenderingEngine.setZOrder(mapObject, zOrder);
        pluginRenderers.forEach(r -> r.setZOrder(mapObject, zOrder));
    }

    /**
     * Called when the TAK scene is initialized and the WW View object is valid and synced with the TAK scene.
     */
    @Subscribe
    public void worldWindowLoaded(WWViewInitialized event)
    {
        if (event.view() == wwd.getView()) SwingUtilities.invokeLater(this::worldWindowLoadedEDT);
    }

    /**
     * Must be called on the EDT.
     */
    private void worldWindowLoadedEDT()
    {
        final boolean isPrimaryGlobe = isPrimaryGlobe(); // Is this the primary globe

        wwd.addRenderingListener(this);

        final SceneController sceneController = wwd.getSceneController();
        // Prevent OGLRenderToTextureSupport from using an offscreen framebuffer
        // since it doesn't restore an incoming offscreen buffer properly.
        sceneController.getGLRuntimeCapabilities().setFramebufferObjectEnabled(false);

        //noinspection ResultOfObjectAllocationIgnored
        new HotSpotController(wwd);

        // Construct our "primary" renderer, and then all plugin-provided renderers

        renderingContext = isPrimaryGlobe() ? new RenderingContext() : primaryGlobe.renderingContext;

        createRendererForMode(configuredRenderingMode); // Construct initial rendering engine

        // Property changes can cause a reference to 'renderingMode', so register AFTER we've set up the initial renderer
        sceneController.addPropertyChangeListener(this);

        if (isPrimaryGlobe)
        {
            globeLayerManager = new GlobeLayerManager(this);
            pingHandler = new PingHandler();
            pingService.addListener(pingHandler);
        } else
        {
            // Similar to the way that WW windows can be constructed to share key data structures (like the model),
            // we do the same for the GlobeLayerManager.
            globeLayerManager = new GlobeLayerManager(this, primaryGlobe.globeLayerManager);
            pingHandler = null;
        }

        if (!platformUnderTest)
        {
            dragImageController = new DragImageController(wwd);

            DragSource dragSource = DragSource.getDefaultDragSource();

            dragSource.addDragSourceListener(dragImageController);
            dragSource.addDragSourceMotionListener(dragImageController);
        }

        if (isPrimaryGlobe) globeMapObjectManager.worldWindowLoaded();

        initViewLimits();

        internalSetDisableCenterGlobeOnClick(PreferenceEntry.GLOBE_DISABLE_CENTER_GLOBE_ON_CLICK.getBooleanValue());
        internalSetKeyboardNavLock(PreferenceEntry.GLOBE_DISABLE_KEYBOARD_GLOBE_NAVIGATION.getBooleanValue());
        internalSetMouseNavLock(PreferenceEntry.GLOBE_DISABLE_MOUSE_GLOBE_NAVIGATION.getBooleanValue());
        setGlobeNavigationStyle(UserPreferenceEntry.GLOBE_NAVIGATION_STYLE.getValueAs(GlobeNavigationStyle.class));

        setInitialView();

        // We need to give the elevation listeners the initial eye altitude
        if (!platformUnderTest) notifyElevationListeners(wwd.getView().getEyePosition().getElevation());

        // start sending unified API globe interaction events
        globeEventService.addEventSource(wwd, getGlobeId());

        // Track continuous render setting changes
        PreferenceEntry.DEV_MODE_CONTINUOUS_RENDERING_ENABLED.addPreferenceEntryChangeListener(change -> onContinuousRenderChanged());

        globeComponentReady.set(true);

        fireGlobeInitialized();
    }

    /**
     * @param pluginRenderers Collection of plugin provided renderers to use
     * @since 5.2
     */
    void setPluginRenderers(List<IRenderingEngine> pluginRenderers)
    {
        this.pluginRenderers = pluginRenderers;
        redrawRequestHandler.request();
    }

    /**
     * Load a rendering engine based on mode string.
     *
     * @param newMode mode name, i.e. "TopDown".
     * @see RendererConstants#RENDER_MODE_TOP_DOWN
     * @see RendererConstants#RENDER_MODE_SPATIAL
     * @since 2.29
     */
    private void createRendererForMode(String newMode)
    {
        if (primaryRenderingEngine != null) primaryRenderingEngine.deactivate();

        renderingMode = newMode;

        if (renderingMode.equalsIgnoreCase(RendererConstants.RENDER_MODE_SPATIAL))
        {
            primaryRenderingEngine = new SpatialRenderer(this, renderingContext);
        } else if (renderingMode.equalsIgnoreCase(RendererConstants.RENDER_MODE_TOP_DOWN))
        {
            primaryRenderingEngine = new TopDownRenderer(this, renderingContext);
        } else if (renderingMode.equalsIgnoreCase(RendererConstants.RENDER_MODE_NONE))
        {
            primaryRenderingEngine = new NullRenderingEngine(this);
        } else
        {
            primaryRenderingEngine = new NullRenderingEngine(this); // So we avoid NPEs
            throw new IllegalArgumentException("Unknown rendering mode [" + renderingMode + "]");
        }

        // Fire the event that tells the rest of the core that the engine has changed
        eventBus.post(new RenderingEngineChangedEvent(primaryRenderingEngine, this));
    }

    /**
     * Set the initial view on the globe. If configured, restore the last view from the saved bookmark. Otherwise,
     * goto the default bookmark, and if that is not set, show the region of the current timezone.
     */
    protected void setInitialView()
    {
        boolean usingLastView = false;

        if (UserPreferenceEntry.RESTORE_LAST_VIEW.getBooleanValue())
        {
            if (lastView == null) lastView = projectViewBookmarkManager.getLast();
            if (lastView != null)
            {
                goTo(lastView);
                usingLastView = true;
            }
        }

        if (!usingLastView)
        {
            IFavorite defaultView = projectViewBookmarkManager.getDefault();

            if (defaultView != null)
            {
                goTo(defaultView);
            } else if (wwd.getView() instanceof OrbitView orbitView) // Can only work on OrbitViews
            {
                // default to computer local timezone
                TimeZone tz = programEnvironment.getLocalTimeZone();
                if (tz != null)
                {
                    double lon = Angle.fromDegrees(180.0 * tz.getOffset(System.currentTimeMillis()) / (12.0 * 3.6e6)).degrees;
                    Position position = orbitView.getCenterPosition();

                    IFavorite favorite = Favorite.createViewBookmark("", "Default", Configuration.getDoubleValue(AVKey.INITIAL_LATITUDE),
                            lon, position.getElevation(), orbitView.getHeading().getDegrees(),
                            orbitView.getPitch().getDegrees(), orbitView.getZoom());
                    goTo(favorite);
                }
            }
        }
    }

    @Override
    public void stageChanged(RenderingEvent event)
    {
        if (event == null || event.getStage() == null)
        {
            return;
        }

        if (event.getStage().equals(RenderingEvent.AFTER_BUFFER_SWAP) && event.getSource() instanceof WorldWindow
                && wwd != null && wwd.getSceneController() != null && wwd.getSceneController().getDrawContext() != null)
        {
            // If the data structures owned by region manager are expired, then update those data structures now. We
            // delay updating region manager until the rendering listener for two reasons:
            // 1. The call to updateRegions() may exit without doing anything, and we'll need to attempt to update again
            //    during the next frame.
            // 2. updateRegions() must be called after the view state is made current. The view state is guaranteed
            //    current after the SceneController calls apply(). However many view property change events are
            //    generated during apply(), so we cannot invoke updateRegions() in the property listener. Therefore
            //    rendering listeners are a natural place for TAKX to invoke per-frame behavior.

            final DrawContext drawContext = wwd.getSceneController().getDrawContext();

            if (primaryRenderingEngine.isViewStale()) primaryRenderingEngine.updateView(drawContext);

            if (pluginRenderers != null)
            {
                for (IRenderingEngine renderer : pluginRenderers)
                {
                    if (renderer.isViewStale()) renderer.updateView(drawContext);
                }
            }
        }
    }

    @Override
    public void preRenderRenderers(DrawContext dc)
    {
        primaryRenderingEngine.preRender(dc);
        pluginRenderers.forEach(renderer -> renderer.preRender(dc));
    }

    @Override
    public void renderRenderers(DrawContext dc)
    {
        primaryRenderingEngine.render(dc);
        pluginRenderers.forEach(renderer -> renderer.render(dc));
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt == null || evt.getPropertyName() == null)
        {
            return;
        }

        if (evt.getPropertyName().equals(AVKey.VIEW))
        {
            final double pitch = wwd.getView().getPitch().degrees;

            // If we are in Top Down rendering and the pitch is greater than the limit, then we need
            // to switch to Spatial rendering mode. Similarly, if we are in Spatial mode due to exceeding
            // the pitch limit and the pitch is now below the limit, switch back to Top Down rendering.

            boolean isTopDown = RendererConstants.RENDER_MODE_TOP_DOWN.equals(renderingMode);
            boolean wantTopDown = RendererConstants.RENDER_MODE_TOP_DOWN.equals(configuredRenderingMode);

            if (isTopDown && pitch > MAX_TOP_DOWN_PITCH)
            {
                createRendererForMode(RendererConstants.RENDER_MODE_SPATIAL); // Switch to Spatial rendering
            } else if (wantTopDown && !configuredRenderingMode.equals(renderingMode) && pitch <= MAX_TOP_DOWN_PITCH)
            {
                createRendererForMode(RendererConstants.RENDER_MODE_TOP_DOWN); // Switch back to Top Down rendering
            } else
            {
                // The globe view has changed, indicate that the rendering engines view is stale and should
                // be re-computed at the next available opportunity (in the above stageChanged()).
                primaryRenderingEngine.setViewStale(true);

                double alt;

                View view = wwd.getView();

                // OrbitView.getZoom() is preferred. It is lighter weight than View.getCurrentEyePosition and should
                // be set by the time the view change event is fired after a user changes zoom level.
                if (view instanceof OrbitView orbitView)
                {
                    alt = orbitView.getZoom();
                } else
                {
                    // Use View.getCurrentEyePosition rather than View.getEyePosition to force a recalculation.
                    // The value returned by View.getEyePosition is stale at this point.
                    alt = view.getCurrentEyePosition().getElevation();
                }

                if (previousEyeAltitude != alt)
                {
                    notifyElevationListeners(alt);
                }
            }
        }
    }

    private void notifyElevationListeners(double alt)
    {
        previousEyeAltitude = alt;
        for (IGlobeEyeElevationListener eyeElevationListener : elevationListeners)
        {
            eyeElevationListener.elevationChanged(alt);
        }
    }

    /**
     * Notify all listeners that the globe is initialized and ready for use. Since IGlobeComponentListener is
     * part of the published API, we need to catch any exceptions plugins might be throwing that would prevent
     * core TAKX components from receiving the notification.
     * <p/>
     * Also, notify any globe lifecycle listeners that a globe has been created.
     * <p/>
     * This method, and thus the various callbacks, always occurs on the EDT.
     */
    private synchronized void fireGlobeInitialized()
    {
        // Work off of a copy of the list, as the listener may remove itself as a result of the call.
        new ArrayList<>(componentListeners)
                .forEach(handler -> safeInvoke(handler::globeInitialized, "globeInitialized"));

        readyHandlers.forEach(handler -> safeInvoke(handler, "doWhenReady"));
        readyHandlers.clear();

        globeManager.getGlobeLifeCycleListeners()
                .forEach(listener -> safeInvoke(() -> listener.globeCreated(this), "globeCreated"));

        if (isPrimaryGlobe())
        {
            globeEvent.fire(new PrimaryGlobeCreatedEvent());
        }
    }

    /**
     * Notify all listeners that the globe is initialized and ready for use. Since IGlobeComponentListener is
     * part of the published API, we need to catch any exceptions plugins might be throwing that would prevent
     * core TAKX components from receiving the notification.
     * <p/>
     * Also, notify any globe lifecycle listeners that this globe is about to be shutdown.
     */
    private void fireGlobeAboutToShutdown()
    {
        // Work off of a copy of the list, as the listener may remove itself as a result of the call.
        new ArrayList<>(componentListeners)
                .forEach(handler -> safeInvoke(handler::globeAboutToShutdown, "globeAboutToShutdown"));

        globeManager.getGlobeLifeCycleListeners()
                .forEach(listener -> safeInvoke(() -> listener.globeAboutToBeRemoved(this), "globeAboutToBeRemoved"));
    }

    /**
     * Notify all listeners that the globe is initialized and ready for use. Since IGlobeComponentListener is
     * part of the published API, we need to catch any exceptions plugins might be throwing that would prevent
     * core TAKX components from receiving the notification.
     * <p/>
     * Also, notify any globe lifecycle listeners that this globe has been removed.
     */
    private void fireGlobeShutdown()
    {
        // Work off of a copy of the list, as the listener may remove itself as a result of the call.
        new ArrayList<>(componentListeners)
                .forEach(handler -> safeInvoke(handler::globeShutdown, "globeShutdown"));

        globeManager.getGlobeLifeCycleListeners()
                .forEach(listener -> safeInvoke(() -> listener.globeRemoved(this), "globeRemoved"));
    }

    /**
     * Invoke an operation and catch any exception that might be thrown.
     *
     * @param operation   Operation to perform
     * @param description Description to use in error message
     */
    private void safeInvoke(Runnable operation, String description)
    {
        try
        {
            operation.run();
        } catch (Exception e)
        {
            logger.error("exception invoking {}", description, e);
        }
    }

    /**
     * Construct the appropriate World Window, either headless or the appropriate flavor depending on the platform.
     */
    private void makeWwd()
    {
        if (headless)
        {
            wwd = new HeadlessWorldWindow();
        } else
        {
            Class<?> surfaceType = System.getProperty("heavyweight-globe", "false").equals("true") ? Canvas.class : JComponent.class;
            renderer = MapRendererFactory.create(nativeGlobe, null, surfaceType);
            component = (Component) renderer.getRenderContext().getRenderSurface();
            wwd = (IWorldWindowGL) component;
        }
    }

    /**
     * Initialize our environment when running in headless mode. This cranks up the models, but no view management
     * or input handling.
     */
    private void initHeadlessGlobeComponent()
    {
        if (!isPrimaryGlobe()) throw new IllegalStateException("Only 1 globe allowed in headless mode!");

        // Always force WMS offline when under test - this avoids random exceptions being logged during tests
        boolean offlineMode = platformUnderTest || projectManager.getCurrentOpenProject().isWMSOfflineMode();

        setWMSOfflineMode(offlineMode);

        final Model model = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);

        //start with an empty layer list until layer manager has added layers
        model.setLayers(new LayerList());
        wwd.setModel(model);

        globeMapObjectManager = new GlobeMapObjectManager();
        wwdSelectListener = new WWSelectListener(wwd, null);
        globeLayerManager = new HeadlessGlobeLayerManager(this);
        globeComponentReady.set(true);

        globeMapObjectManager.worldWindowLoaded();

        globeViewController = new GlobeViewController(wwd, null);
    }

    /**
     * Initialize the WW environment when we are running with a standard UI.
     */
    private void initRealGlobeComponent()
    {
        if (isPrimaryGlobe()) removeWorldWindTiffReader();

        globeViewController = new GlobeViewController(wwd, renderer);
        viewInputHandler = new TakViewInputHandler(globeViewController, renderer);
        initialView = new ApplicationOrbitView(renderer, eventBus);
        initialView.setViewInputHandler(viewInputHandler);
        wwd.setView(initialView);
        wwd.getSceneController().getDrawContext().setValue(ApplicationSceneController.GLOBE_DELEGATE, this);

        final AWTInputHandler inputHandler = new AWTInputHandler();
        wwd.setInputHandler(inputHandler);
        inputHandler.setEventSource(wwd);

        wwd.addFocusListener(globeFocusHandler);

        boolean offlineMode = projectManager.getCurrentOpenProject().isWMSOfflineMode();
        setWMSOfflineMode(offlineMode);

        Model model = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
        wwd.setModel(model);

        // Configure our view limits to avoid several issues:
        //
        // - limit pitch to 89 degrees to avoid click/drag issues at 90 degrees
        // - limit how far the user can zoom in to prevent infinite zoom-in and perceived unresponsiveness
        // - limit user from scrolling to 90N to prevent rendering artifacts
        if (wwd.getView() instanceof OrbitView orbitView) // Can only work on OrbitViews
        {
            final var limits = orbitView.getOrbitViewLimits();
            limits.setPitchLimits(Angle.ZERO, Angle.fromDegrees(89));
            limits.setZoomLimits(MIN_ZOOM_METERS, Double.MAX_VALUE);
            limits.setCenterLocationLimits(new Sector(Angle.NEG90, Angle.POS90.subtract(Angle.SECOND), Angle.NEG180, Angle.POS180));
        }

        wwdSelectListener = new WWSelectListener(wwd, new BasicDragger(wwd));
        wwd.addSelectListener(wwdSelectListener);
        addMouseListener(wwdSelectListener);

        addMouseListener(globeClickHandler);

        globeMapObjectManager = isPrimaryGlobe() ? new GlobeMapObjectManager() : primaryGlobe.globeMapObjectManager;

        previewAnnotationManager = new GlobePreviewAnnotationManager(wwd);

        inputHandler.addKeyListener(deleteKeyListener);
        inputHandler.addKeyListener(copyKeyListener); // Ctrl-C to copy mouse position to clipboard
        inputHandler.addKeyListener(escapeListener); // Close radial menus on ESC

        // Ensure that the normal focus traversal keys don't work when the globe is focused
        // This prevents unwanted behaviors like Tab moving the focus away from the globe.

        wwd.getComponent().setFocusTraversalKeysEnabled(false);

        // Construct the view controller that handles "center on" and "keep in view"
        trackingGlobeViewController = new TrackingGlobeViewController(this);

        //noinspection ResultOfObjectAllocationIgnored
        new DropTarget(wwd.getComponent(), DnDConstants.ACTION_COPY, new GlobeDropTargetHandler(this));

        getWorldWindow().getSceneController().setGpuResourceCache(TakWorldWindowImpl.createGpuResourceCache());

        // Start repainting here to trigger firing a WWViewInitialized event in ApplicationOrbitView.
        startRepaint();
    }

    /**
     * The WW {@link GeotiffImageReaderSpi} is busted, and can't properly read TIFF files.  However, the one provided
     * by JAI imageIO works, so unregister the WW SPI so it is not used.
     * 
     * @since 5.7
     */
    private void removeWorldWindTiffReader()
    {
        final var registry = IIORegistry.getDefaultInstance();
        final var category = ImageReaderSpi.class;
        final String geotiffReaderClassName = GeotiffImageReaderSpi.class.getName();

        // Unregistering the provider within the iterator loop causes a CME, so record the spi and remove later 
        ImageReaderSpi providerToRemove = null;

        final var providers = registry.getServiceProviders(category, false);
        while (providers.hasNext())
        {
            final ImageReaderSpi provider = providers.next();

            if (provider.getClass().getName().equals(geotiffReaderClassName))
            {
                providerToRemove = provider;
                break;
            }
        }

        if (providerToRemove != null) registry.deregisterServiceProvider(providerToRemove, category);
    }

    /**
     * Construct and initialize the WW environment.
     */
    private void initGlobeComponent()
    {
        makeWwd();

        if (headless)
        {
            initHeadlessGlobeComponent();
        } else
        {
            initRealGlobeComponent();
        }

        // And fire the globe created event
        uiEventBus.post(new GlobeCreatedEvent(globeId));
    }

    /**
     * Read the view limits from the ProgramPreferences.xml file. If view limits are set, apply them to the
     * Worldwind viewer, and add a layer with a polyline marking the viewable area.
     */
    private void initViewLimits()
    {
        final PreferencesNode viewLimits = PreferenceEntry.VIEW_LIMITS_NODE.node();

        final String minLatStr = viewLimits.get("minLat", null);
        final String minLonStr = viewLimits.get("minLon", null);
        final String maxLatStr = viewLimits.get("maxLat", null);
        final String maxLonStr = viewLimits.get("maxLon", null);

        logger.debug("View limits are ({}, {}) x ({}, {})", minLatStr, minLonStr, maxLatStr, maxLonStr);

        if (minLatStr != null && minLonStr != null && maxLatStr != null && maxLonStr != null)
        {
            Sector sector = Sector.fromDegrees(Double.parseDouble(minLatStr),
                    Double.parseDouble(maxLatStr),
                    Double.parseDouble(minLonStr),
                    Double.parseDouble(maxLonStr));

            updateViewLimits(sector);

            SurfacePolyline viewLimitPolyline = new SurfacePolyline();
            viewLimitPolyline.setClosed(true);
            BasicShapeAttributes shapeAttributes = new BasicShapeAttributes();
            shapeAttributes.setOutlineMaterial(new Material(Color.RED));
            shapeAttributes.setOutlineWidth(2.0);
            viewLimitPolyline.setAttributes(shapeAttributes);
            viewLimitPolyline.setLocations(sector.asList());
            viewLimitPolyline.setDragEnabled(false);

            RenderableLayer viewLimitBounds = new RenderableLayer();
            viewLimitBounds.setName("View Bounds");

            viewLimitBounds.addRenderable(viewLimitPolyline);

            layerManager.addLayer(viewLimitBounds, LayerGroup.TOP);
        } else if (minLatStr != null || minLonStr != null
                || maxLatStr != null || maxLonStr != null)
        {
            logger.warn("ProgramProperties does not specify all sides of view limit box. You must "
                    + "provide all four constraints: min latitude, min longitude, max latitude, max longitude");
        }
    }

    /**
     * Update the view limit in the globe view.
     *
     * @param sector The viewable sector.
     */
    private void updateViewLimits(@Nonnull Sector sector)
    {
        if (!(wwd.getView() instanceof BasicOrbitView view)) return; // Can only work on OrbitViews

        OrbitViewLimits viewLimits = view.getOrbitViewLimits();
        if (viewLimits == null) return;

        viewLimits.setCenterLocationLimits(sector);

        // Calculate the max zoom so that the user can only see the sector
        // in their view limits, and cannot zoom out to globe view.
        gov.nasa.worldwind.globes.Globe globe = wwd.getModel().getGlobe();
        double ve = wwd.getSceneController().getVerticalExaggeration();
        Extent extent = Sector.computeBoundingBox(globe, ve, sector);

        double maxZoom = extent.getRadius() / view.getFieldOfView().tanHalfAngle();

        viewLimits.setZoomLimits(MIN_ZOOM_METERS, maxZoom);

        view.setCenterPosition(viewLimits.limitCenterPosition(view, view.getCenterPosition()));
        view.setHeading(viewLimits.limitHeading(view, view.getHeading()));
        view.setPitch(viewLimits.limitPitch(view, view.getPitch()));
        view.setZoom(viewLimits.limitZoom(view, view.getZoom()));

        double centerLat = 0.5 * (sector.getMinLatitude().getDegrees() + sector.getMaxLatitude().getDegrees());
        double centerLon = 0.5 * (sector.getMinLongitude().getDegrees() + sector.getMaxLongitude().getDegrees());

        view.setCenterPosition(Position.fromDegrees(centerLat, centerLon, 0));

        view.firePropertyChange(AVKey.VIEW, null, view);
    }

    private void onContinuousRenderChanged()
    {
        stopRepaint();
        startRepaint();
    }

    private void startRepaint()
    {
        if (PreferenceEntry.DEV_MODE_CONTINUOUS_RENDERING_ENABLED.getBooleanValue())
        {
            if (animator == null)
            {
                animator = new Animator((GLAutoDrawable) component);
                animator.setRunAsFastAsPossible(true);
            }
            animator.start();
        } else
        {
            // Even with continuous rendering, we do a periodic redraw
            // to handle some change detection
            ActionListener repaintAction = e -> {
                if (repaint && wwd != null) wwd.getComponent().repaint();
            };

            repaintTimer = new Timer(REDRAW_DELAY, repaintAction);
            repaintTimer.start();
        }
    }

    public void stopRepaint()
    {
        repaint = false;
        if (repaintTimer != null) repaintTimer.stop();
        if (animator != null) animator.stop();
    }

    /**
     * Initialize the internal state of this globe.
     */
    void initialize()
    {
        if (headless)
        {
            initGlobeComponent();
        } else
        {
            // This class only listens on the non-UI event bus
            eventBus.register(this);

            initGlobeComponentOnEDT();
        }
    }

    @InvokeAndWaitOnEDT
    private void initGlobeComponentOnEDT()
    {
        initGlobeComponent();
    }

    @RemoveObserver
    @InvokeAndWaitOnEDT
    void tearDown(IProgressMonitor progressMonitor)
    {
        final boolean isPrimaryGlobe = isPrimaryGlobe(); // Is this the primary globe

        fireGlobeAboutToShutdown();

        globeComponentReady.set(false);

        // This class does not register itself in headless mode
        if (eventBus.isRegistered(this)) eventBus.unregister(this);

        // If this is the primary globe, then shutdown all the secondary globes first
        // Moved to GlobeManager. Check timing of clear call.

        stopRepaint();

        if (wwd != null) globeEventService.removeEventSource(wwd);

        // TODO: It seems a little confusing to call onShutdown from onClearState in the calls below. Should we use a different interface for these calls?

        if (globeLayerManager != null)
        {
            globeLayerManager.onShutdown(progressMonitor);
            globeLayerManager = null;
        }

        globeViewController = null;

        if (isPrimaryGlobe && renderingContext != null)
        {
            renderingContext.onShutdown(progressMonitor);
            renderingContext = null;
        }

        if (primaryRenderingEngine != null)
        {
            primaryRenderingEngine.shutdown(isPrimaryGlobe);
            primaryRenderingEngine = null;
        }

        if (previewAnnotationManager != null)
        {
            previewAnnotationManager.dispose();
            previewAnnotationManager = null;
        }

        if (trackingGlobeViewController != null)
        {
            trackingGlobeViewController.onShutdown(progressMonitor);
            trackingGlobeViewController = null;
        }

        // Only shut down the primary instance
        if (pingHandler != null)
        {
            pingService.removeListener(pingHandler);
            pingHandler.onShutdown(progressMonitor);
            pingHandler = null;
        }

        // Tear down the GMOM last since other services might try to use it as they shutdown
        if (globeMapObjectManager != null)
        {
            if (isPrimaryGlobe) // Only shut down the primary instance
            {
                globeMapObjectManager.onShutdown(progressMonitor);
            }
            globeMapObjectManager = null;
        }

        if (!headless)
        {
            DragSource dragSource = DragSource.getDefaultDragSource();

            if (dragImageController != null)
            {
                dragSource.removeDragSourceListener(dragImageController);
                dragSource.removeDragSourceMotionListener(dragImageController);

                dragImageController = null;
            }
        }

        if (wwd != null)
        {
            wwd.removeKeyListener(copyKeyListener);
            wwd.removeKeyListener(deleteKeyListener);
            wwd.removeRenderingListener(this);
            wwd.getSceneController().removePropertyChangeListener(this);

            if (wwdSelectListener != null)
            {
                wwd.removeSelectListener(wwdSelectListener);
                wwd.getInputHandler().removeMouseListener(wwdSelectListener);
                wwd.getInputHandler().removeMouseListener(wwdSelectListener);

                wwdSelectListener.onShutdown(progressMonitor);
                wwdSelectListener = null;
            }

            wwd.getModel().getLayers().clear();
            nativeGlobe.removeAllLayers();

            final Component wwdComponent = wwd.getComponent();

            if (wwdComponent != null)
            {
                for (MouseListener mouseListener : wwdComponent.getMouseListeners())
                {
                    wwdComponent.removeMouseListener(mouseListener);
                }
                for (MouseMotionListener mouseMotionListener : wwdComponent.getMouseMotionListeners())
                {
                    wwdComponent.removeMouseMotionListener(mouseMotionListener);
                }
                for (PropertyChangeListener propertyChangeListener : wwdComponent.getPropertyChangeListeners())
                {
                    wwd.removePropertyChangeListener(propertyChangeListener);
                }
            }

            wwd.shutdown();
            wwd = null;
        }

        fireGlobeShutdown();

        componentListeners.clear(); // Remove all listeners

        // If this is the primary globe, then shutdown WW
        if (isPrimaryGlobe)
        {
            WorldWind.shutDown();
        } else
        {
            // And fire the globe removed event
            uiEventBus.post(new GlobeRemovedEvent(globeId));
        }
    }

    @Override
    public void saveLastViewBookmark()
    {
        final IFavorite vb = getViewBookmark();

        if (vb != null)
        {
            projectViewBookmarkManager.setLast(vb);
        } else
        {
            if (!headless) logger.warn("Failed to get LAST bookmark");
        }
    }

    /**
     * Handle the saving event, save our state.
     *
     * @since 2.37
     */
    private void handleSaving(@SuppressWarnings("unused") @Observes ProjectSaving event)
    {
        saveLastViewBookmark();
    }

    @Override
    public void onShutdown(IProgressMonitor progressMonitor)
    {
        onClearState(progressMonitor);
    }

    @Override
    public void onLoadState(IProgressMonitor progressMonitor)
    {
        initialize();
    }

    @InvokeAndWaitOnEDT
    @Override
    public void onClearState(final IProgressMonitor progressMonitor)
    {
        tearDown(progressMonitor);
    }

    @InvokeAndWaitOnEDT
    @Override
    public void deactivate()
    {
        deactivatePluginRenderers();

        tearDown(new NullProgressMonitor());
    }

    @Override
    public void deactivatePluginRenderers()
    {
        if (pluginRenderers != null)
        {
            pluginRendererManager.shutdownRenderers(this);
            pluginRenderers = null;
        }
    }

    @Override
    public String toString()
    {
        return "ApplicationGlobe{" +
                "globeId=" + globeId +
                '}';
    }

    /**
     * KeyListener to consume all events.
     *
     * @since 2.37
     */
    private static final class ConsumeAllKeyEvents implements KeyListener
    {
        @Override
        public void keyTyped(KeyEvent e)
        {
            e.consume();
        }

        @Override
        public void keyPressed(KeyEvent e)
        {
            e.consume();
        }

        @Override
        public void keyReleased(KeyEvent e)
        {
            e.consume();
        }
    }

    /**
     * MouseMotionListener to consume all events.
     *
     * @since 2.37
     */
    private static final class ConsumeAllMouseMotionEvents implements MouseMotionListener
    {
        @Override
        public void mouseDragged(MouseEvent e)
        {
            e.consume();
        }

        @Override
        public void mouseMoved(MouseEvent e)
        {
            e.consume();
        }
    }

    /**
     * Key adapter to watch for Ctrl-C key and copy the current mouse position to the clipboard.
     */
    private final class CopyKeyHandler extends KeyAdapter
    {
        @Override
        public void keyPressed(KeyEvent e)
        {
            if (e.getKeyCode() == KeyEvent.VK_C && e.isControlDown())
            {
                copyPositionToClipboard();
            }
        }
    }

    /**
     * Simple mouse adapter to watch for a clicks on terrain and handle the popup menu.
     */
    private final class GlobeClickHandler extends MouseAdapter
    {
        private boolean wasPopupTrigger;
        private double pitch;
        private double heading;

        @Override
        public void mousePressed(MouseEvent e)
        {
            // on Mac OS, the mouse release is not marked "isPopupTrigger", so we must record
            // whether the mouse press was.
            wasPopupTrigger = e.isPopupTrigger();

            // Store the pitch and heading at the time of the mouse down event.
            pitch = wwd.getView().getPitch().degrees;
            heading = wwd.getView().getHeading().degrees;
        }

        /**
         * Display popup menu for copying location if the user hasn't changed the pitch and heading with
         * the right mouse button down. Basically, a click and release without a move.
         *
         * @param e the mouse event
         */
        @Override
        public void mouseReleased(MouseEvent e)
        {
            // If the select listener is not armed, it means that something else is directly working with mouse
            // events, like one of our shape editors, so skip processing here in that case.
            if (!wwdSelectListener.isArmed()) return;

            final View view = wwd.getView();
            final boolean sameView = pitch == view.getPitch().degrees && heading == view.getHeading().degrees;
            final boolean isPopupTrigger = wasPopupTrigger || e.isPopupTrigger();

            // If the user clicks on the globe while our menu is visible, the top picked object will not be terrain.
            // So, we need to also look for the top object being the WidgetLayerManager, which indicates that our menu
            // is displayed, and we need to move it.

            final var topPickedObject = wwd.getObjectsAtCurrentPosition().getTopPickedObject();
            final boolean shouldShow = topPickedObject != null && (topPickedObject.isTerrain() || topPickedObject.getObject() instanceof WidgetLayerManager);

            if (isPopupTrigger && sameView && shouldShow)
            {
                // The elevation reported in the current position can be stale, so fetch it directly
                final var position = wwd.getCurrentPosition();
                if (position != null)
                {
                    final double latitude = position.latitude.degrees;
                    final double longitude = position.longitude.degrees;
                    final var location = new GeoPoint(latitude, longitude, ElevationManager.getElevation(latitude, longitude, null));
                    goToAndTranslateMouseEvent(location, e);

                    final var popupMenu = terrainPopupMenuProvider.createMenu(ApplicationGlobe.this, location);
                    widgetLayerManager.showRadialMenu(ApplicationGlobe.this, null, popupMenu, e.getX(), e.getY());
                }
            }
        }
    }

    /**
     * Panel to hold the globe component and provide a border that changes color when the globe has focus.
     */
    private static final class FocusHandlingPanel extends JPanel
    {
        FocusHandlingPanel(Component globeComponent)
        {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(3, 3, 3, 3));

            add(globeComponent);

            // Track focus and set the background color accordingly
            globeComponent.addFocusListener(new FocusHandler());
        }

        /**
         * Watch for focus gain/loss on the globe component and set the background to give a visual cue as to which
         * globe panel has the focus.
         */
        private final class FocusHandler implements FocusListener
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                setBackground(UIResources.BORDER_HIGHLIGHT_COLOR); // Add the visual cue
            }

            @Override
            public void focusLost(FocusEvent e)
            {
                Container parent = getParent();
                if (parent != null)
                {
                    Color parentBackground = parent.getBackground();
                    setBackground(parentBackground);
                }
            }
        }
    }
}
