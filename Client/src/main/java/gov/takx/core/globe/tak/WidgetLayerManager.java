package gov.takx.core.globe.tak;

import com.atakmap.map.layer.MultiLayer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.jogamp.opengl.GLAutoDrawable;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.tak.api.commons.graphics.DisplaySettings;
import gov.tak.api.engine.map.MapRenderer;
import gov.tak.api.widgets.IMapMenuButtonWidget;
import gov.tak.api.widgets.IMapWidget;
import gov.tak.api.widgets.IMenuLayoutWidget;
import gov.tak.api.widgets.IParentWidget;
import gov.tak.api.widgets.opengl.IGLWidget;
import gov.tak.platform.commons.opengl.GLES30;
import gov.tak.platform.commons.opengl.Matrix;
import gov.tak.platform.graphics.PointF;
import gov.tak.platform.ui.IMotionEventHandler;
import gov.tak.platform.ui.MotionEvent;
import gov.tak.platform.widgets.LayoutWidget;
import gov.tak.platform.widgets.MenuLayoutWidget;
import gov.tak.platform.widgets.MenuLayoutWidgetController;
import gov.tak.platform.widgets.RootLayoutWidget;
import gov.tak.platform.widgets.TextWidget;
import gov.tak.platform.widgets.opengl.GLButtonWidget;
import gov.tak.platform.widgets.opengl.GLCenterBeadWidget;
import gov.tak.platform.widgets.opengl.GLDrawableWidget;
import gov.tak.platform.widgets.opengl.GLLayoutWidget;
import gov.tak.platform.widgets.opengl.GLLinearLayoutWidget;
import gov.tak.platform.widgets.opengl.GLMarkerIconWidget;
import gov.tak.platform.widgets.opengl.GLRadialButtonWidget;
import gov.tak.platform.widgets.opengl.GLRootLayoutWidget;
import gov.tak.platform.widgets.opengl.GLScaleWidget;
import gov.tak.platform.widgets.opengl.GLTextWidget;
import gov.tak.platform.widgets.opengl.GLWidget;
import gov.tak.platform.widgets.opengl.GLWidgetFactory;
import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.event.ShuttingDown;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IGlobeLifeCycleListener;
import gov.takx.api.globe.IWidget;
import gov.takx.api.globe.IWidgetInstance;
import gov.takx.api.globe.IWidgetLayerManager;
import gov.takx.api.globe.IWorldWindowGL;
import gov.takx.api.globe.WidgetLayoutPosition;
import gov.takx.api.layer.pick.IPickContext;
import gov.takx.api.layer.pick.IPickedObjectProvider;
import gov.takx.api.layer.pick.PickedObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.core.globe.tak.layer.WidgetLayer;
import gov.takx.core.globe.utils.MotionEventAdapter;
import gov.takx.core.globe.widgets.BloodhoundWidget;
import gov.takx.core.globe.widgets.CompassWidget;
import gov.takx.core.globe.widgets.EmergencyAlertsWidget;
import gov.takx.core.globe.widgets.GeoFenceAlertsWidget;
import gov.takx.core.globe.widgets.IsoKeyWidget;
import gov.takx.core.globe.widgets.MessageOverlayWidget;
import gov.takx.core.globe.widgets.NavigationWidget;
import gov.takx.core.globe.widgets.PositionWidget;
import gov.takx.core.globe.widgets.ProjectLabelWidgetManager;
import gov.takx.core.globe.widgets.ScaleWidget;
import gov.takx.core.globe.widgets.SelfLocationWidget;
import gov.takx.core.globe.widgets.TerrainProfilerWidget;
import gov.takx.core.globe.widgets.TrackColorScaleWidget;
import gov.takx.core.globe.widgets.WorldMapWidget;
import gov.takx.core.mapobject.gui.widgets.ContextMenuModel;
import gov.takx.core.project.ServiceOrdering;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPopupMenu;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A service that manages Widgets and is responsible for rendering and handling mouse events
 *
 * @since 3.5
 */
@ApplicationScoped
public class WidgetLayerManager implements IGlobeLifeCycleListener, IWidgetLayerManager, IPickedObjectProvider
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String WIDGETS_VFS_NS = "widgets";
    private static final String WIDGET_STATES_FILE_NAME = "widget_manager.json";
    private static final List<String> EXCLUDED_WIDGETS = List.of("LabelWidget", "MessageOverlayWidget", "IsoKeyWidget");

    private final float[] proj = new float[16];
    private final float[] model = new float[16];

    private final boolean headless;
    private final ProjectLabelWidgetManager projectLabelWidgetManager;
    private final Event<WidgetInstanceAdded> widgetInstanceAddedEvent;
    private final Event<WidgetInstanceRemoved> widgetInstanceRemovedEvent;
    private final Event<WidgetVisibilityChanged> widgetVisibilityChangedEvent;
    private final IExtendedGlobeManager globeManager;

    private final List<IRadialMenuListener> radialMenuListeners = new CopyOnWriteArrayList<>();
    private final IdentityHashMap<IGlobeDelegate, GlobeRegistration> globeRegistrations = new IdentityHashMap<>();
    private final IdentityHashMap<IWidgetFactory, WidgetLayoutPosition> widgetLayoutPositions = new IdentityHashMap<>();
    private final List<IWidgetFactory> widgetFactoryOrder = new ArrayList<>();
    private final MultiLayer layer = new MultiLayer("Widget Layers");
    private final Path stateFilePath;
    @VisibleForTesting
    final Map<String, List<WidgetState>> widgetStateMap = new HashMap<>();

    static
    {
        GLWidgetFactory.registerSpi(GLRadialButtonWidget.SPI);
        GLWidgetFactory.registerSpi(GLScaleWidget.SPI);
        GLWidgetFactory.registerSpi(GLLinearLayoutWidget.SPI);
        GLWidgetFactory.registerSpi(GLLayoutWidget.SPI);
        GLWidgetFactory.registerSpi(GLTextWidget.SPI);
        GLWidgetFactory.registerSpi(GLMarkerIconWidget.SPI);
        GLWidgetFactory.registerSpi(GLCenterBeadWidget.SPI);
        GLWidgetFactory.registerSpi(GLRootLayoutWidget.SPI);
        GLWidgetFactory.registerSpi(GLDrawableWidget.SPI);
        GLWidgetFactory.registerSpi(GLButtonWidget.SPI);
    }

    @Inject
    WidgetLayerManager(IExtendedGlobeManager globeManager, @Headless boolean headless, ProjectLabelWidgetManager projectLabelWidgetManager,
                       Event<WidgetInstanceAdded> widgetInstanceAddedEvent, Event<WidgetInstanceRemoved> widgetInstanceRemovedEvent,
                       Event<WidgetVisibilityChanged> widgetVisibilityChangedEvent,
                       @VFSManagerScope(VFSScope.Global) IVirtualFileSystemManager vfsManager)
    {
        this.headless = headless;
        this.projectLabelWidgetManager = projectLabelWidgetManager;
        this.globeManager = globeManager;
        this.widgetInstanceAddedEvent = widgetInstanceAddedEvent;
        this.widgetInstanceRemovedEvent = widgetInstanceRemovedEvent;
        this.widgetVisibilityChangedEvent = widgetVisibilityChangedEvent;
        layer.addLayer(new WidgetLayer());
        globeManager.addGlobeLifeCycleListener(this);
        stateFilePath = vfsManager.getVirtualFileSystem(WIDGETS_VFS_NS).getRootDir().toPath().resolve(WIDGET_STATES_FILE_NAME);
    }

    /**
     * Add all our default widget factories.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_CreateTAKLayers) StartProjectServices ignored)
    {
        if (headless) return;

        DisplaySettings.setDefaultFontSize(12);

        // Register early so they have layout preference (closer to edges) and are not
        // moved around significantly by other widgets.
        projectLabelWidgetManager.registerWidgetFactories(this);

        // Keep thee organized by position
        addWidgetFactory(MessageOverlayWidget::new, WidgetLayoutPosition.TOP_EDGE);

        addWidgetFactory(WorldMapWidget::new, WidgetLayoutPosition.TOP_RIGHT);

        addWidgetFactory(globe -> new TrackColorScaleWidget((IApplicationGlobe) globe), WidgetLayoutPosition.RIGHT_EDGE);

        addWidgetFactory(globe -> new CompassWidget((IApplicationGlobe) globe), WidgetLayoutPosition.TOP_LEFT);
        addWidgetFactory(NavigationWidget::new, WidgetLayoutPosition.TOP_LEFT);

        addWidgetFactory(SelfLocationWidget::new, WidgetLayoutPosition.BOTTOM_RIGHT);

        addWidgetFactory(PositionWidget::new, WidgetLayoutPosition.BOTTOM_LEFT);
        addWidgetFactory(ScaleWidget::new, WidgetLayoutPosition.BOTTOM_LEFT);
        addWidgetFactory(GeoFenceAlertsWidget::new, WidgetLayoutPosition.BOTTOM_LEFT);
        addWidgetFactory(EmergencyAlertsWidget::new, WidgetLayoutPosition.BOTTOM_LEFT);
        addWidgetFactory(TerrainProfilerWidget::new, WidgetLayoutPosition.BOTTOM_LEFT);
        addWidgetFactory(BloodhoundWidget::new, WidgetLayoutPosition.BOTTOM_LEFT);
        addWidgetFactory(IsoKeyWidget::new, WidgetLayoutPosition.BOTTOM_LEFT);

        restoreState();
    }

    /**
     * Clear out all factory registrations.
     * @since 5.5
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_TAKLayers) StopProjectServices ignored)
    {
        radialMenuListeners.clear();

        widgetStateMap.clear();
        storeState(getWidgetList());

        synchronized (globeRegistrations)
        {
            globeRegistrations.clear();
        }
        synchronized (widgetFactoryOrder)
        {
            widgetLayoutPositions.clear();
            widgetFactoryOrder.clear();
        }
    }

    @InvokeOnEDT
    private void shuttingDown(@Observes ShuttingDown event)
    {
        globeManager.removeGlobeLifeCycleListener(this);
    }

    @Override
    public void globeCreated(IGlobeDelegate globe)
    {
        register(createRegistration(globe), globe);
    }

    @Override
    public void globeAboutToBeRemoved(IGlobeDelegate globe)
    {
        final var globeRegistration = getGlobeRegistration(globe);
        if (globeRegistration != null) unregister(globeRegistration, globe);
    }

    private GlobeRegistration createRegistration(IGlobeDelegate globe)
    {
        final var root = new RootLayoutWidget();

        // Override _clearAllMenus so we can properly fire onMenuClosed events no matter how the menu is closed/cleared
        final var menuLayoutWidget = new MenuLayoutWidget()
        {
            @Override
            protected boolean _clearAllMenus()
            {
                final boolean result = super._clearAllMenus();
                if (result) notifyRadialMenuListeners(IRadialMenuListener::onMenuClosed);
                return result;
            }
        };

        final var absoluteLayoutWidget = new LayoutWidget();
        root.addChildWidget(menuLayoutWidget);
        root.addChildWidget(absoluteLayoutWidget);

        final var mapRenderer = globe.getPortableMapRenderer();
        final var impl = new GLRootLayoutWidget(root, mapRenderer);

        // Concurrent operations can occur on the widgetRegistrations, so use a ConcurrentHashMap instead of a plain HashMap
        return new GlobeRegistration(impl, root, mapRenderer, new MotionEventAdapter(), menuLayoutWidget, absoluteLayoutWidget, new ConcurrentHashMap<>());
    }

    @Override
    public void addWidgetFactory(@Nonnull IWidgetFactory widgetFactory, @Nonnull WidgetLayoutPosition layoutPosition)
    {
        synchronized (widgetFactoryOrder)
        {
            widgetLayoutPositions.put(widgetFactory, layoutPosition);
            if (!widgetFactoryOrder.contains(widgetFactory)) widgetFactoryOrder.add(widgetFactory);
        }

        synchronized (globeRegistrations)
        {
            globeRegistrations.forEach((globeDelegate, globeRegistration) -> {
                IWidget widget = widgetFactory.create(globeDelegate);
                if (widget != null) addWidgetToGlobe(widget, globeDelegate, globeRegistration, layoutPosition);
            });
        }
    }

    @Override
    public void removeWidgetFactory(@Nonnull IWidgetFactory widgetFactory)
    {
        synchronized (widgetFactoryOrder)
        {
            widgetLayoutPositions.remove(widgetFactory);
            widgetFactoryOrder.remove(widgetFactory);
        }
    }

    private void addWidgetToGlobe(@Nonnull IWidget widget, @Nonnull IGlobeDelegate globeDelegate, GlobeRegistration globeRegistration, WidgetLayoutPosition layoutPosition)
    {
        getLayoutWidget(globeRegistration, layoutPosition).addChildWidget(widget);
        globeRegistration.widgetRegistrations.put(widget, new WidgetRegistration(layoutPosition));

        final var widgetInstance = new WidgetInstance(widget, globeDelegate);

        // Monitor visibility changes to fire our event
        widget.addOnVisibleChangedListener(w -> widgetVisibilityChangedEvent.fire(new WidgetVisibilityChanged(widgetInstance)));

        widgetInstanceAddedEvent.fire(new WidgetInstanceAdded(widgetInstance, layoutPosition));
    }

    @Override
    public void removeWidget(@Nonnull IWidget widget)
    {
        synchronized (globeRegistrations)
        {
            globeRegistrations.forEach((globeDelegate, globeRegistration) -> removeWidgetFromGlobe(widget, globeDelegate, globeRegistration));
        }
    }

    @Override
    public void removeWidgets(@Nonnull Class<? extends IWidget> clazz)
    {
        synchronized (globeRegistrations)
        {
            globeRegistrations.forEach((globeDelegate, globeRegistration) -> {
                List<IWidget> matchingWidgets = globeRegistration.widgetRegistrations.keySet().stream()
                        .filter(widget -> clazz.isAssignableFrom(widget.getClass()))
                        .toList();
                matchingWidgets.forEach(widget -> removeWidgetFromGlobe(widget, globeDelegate, globeRegistration));
            });
        }
    }

    @Override
    public void removeWidgetsByName(@Nonnull String name)
    {
        synchronized (globeRegistrations)
        {
            globeRegistrations.forEach((globeDelegate, globeRegistration) -> {
                var matchingWidgets = globeRegistration.widgetRegistrations.keySet().stream()
                        .filter(widget -> widget.getName().equals(name))
                        .toList();
                matchingWidgets.forEach(widget -> removeWidgetFromGlobe(widget, globeDelegate, globeRegistration));
            });
        }
    }

    @Nonnull
    @Override
    public List<IWidgetInstance> getAllWidgetInstances()
    {
        return makeWidgetInstances((widget, ignored) -> true);
    }

    @Override
    @Nonnull
    public List<IWidgetInstance> getWidgetInstancesByName(@Nonnull String name)
    {
        return makeWidgetInstances((widget, ignored) -> name.equals(widget.getName()));
    }

    @Nonnull
    @Override
    public List<IWidgetInstance> getWidgetInstancesByGlobe(@Nonnull IGlobeDelegate globe)
    {
        return makeWidgetInstances((ignored, globeDelegate) -> globeDelegate == globe);
    }

    @Override
    public void addRadialMenuListener(@Nonnull IRadialMenuListener listener)
    {
        radialMenuListeners.add(listener);
    }

    @Override
    public void removeRadialMenuListener(@Nonnull IRadialMenuListener listener)
    {
        radialMenuListeners.remove(listener);
    }

    @Nonnull
    @Override
    public List<IWidgetInstance> getWidgetsAtPosition(@Nonnull IGlobeDelegate globe, @Nonnull WidgetLayoutPosition layoutPosition)
    {
        final var registration = getGlobeRegistration(globe);

        return registration != null
                ? registration.widgetRegistrations.entrySet().stream()
                        .filter(entry -> layoutPosition.equals(entry.getValue().layoutPosition))
                        .map(entry -> (IWidgetInstance) new WidgetInstance(entry.getKey(), globe))
                        .toList()
                : Collections.emptyList();
    }

    private void notifyRadialMenuListeners(Consumer<IRadialMenuListener> consumer)
    {
        ExceptionUtils.safeDoOperation(radialMenuListeners, consumer);
    }

    /**
     * Find the widgets, across all globes, that match the given {@code predicate}.
     * Create a {@link WidgetInstance} for each matched widget.
     * 
     * @since 5.4
     */
    @SuppressWarnings("unchecked")
    private <T extends IWidgetInstance> List<T> makeWidgetInstances(@Nonnull BiPredicate<IWidget, IGlobeDelegate> predicate)
    {
        final List<T> instances = new ArrayList<>();
        visitWidgets(predicate, (widget, globeDelegate) -> instances.add((T) new WidgetInstance(widget, globeDelegate)));
        return instances;
    }

    /**
     * Visit all the registered widgets (under required registration synchronization), applying the given visitor to
     * those that match the given {@code predicate}.
     *
     * @since 5.4
     */
    private void visitWidgets(@Nonnull BiPredicate<IWidget, IGlobeDelegate> predicate, @Nonnull BiConsumer<IWidget, IGlobeDelegate> visitor)
    {
        synchronized (globeRegistrations)
        {
            globeRegistrations.forEach((globe, globeRegistration) -> {
                globeRegistration.widgetRegistrations.keySet().forEach(widget -> {
                    if (predicate.test(widget, globe)) visitor.accept(widget, globe);
                });
            });
        }
    }

    private void removeWidgetFromGlobe(@Nonnull IWidget widget, @Nonnull IGlobeDelegate globeDelegate, GlobeRegistration globeRegistration)
    {
        WidgetRegistration widgetRegistration = globeRegistration.widgetRegistrations.remove(widget);
        if (widgetRegistration != null)
        {
            getLayoutWidget(globeRegistration, widgetRegistration.layoutPosition).removeChildWidget(widget);

            widgetInstanceRemovedEvent.fire(new WidgetInstanceRemoved(new WidgetInstance(widget, globeDelegate)));
        }
        widget.releaseResources();
    }

    /**
     *
     * @param globeRegistration The registration for the globe to check
     * @param layoutPosition    The {@link WidgetLayoutPosition} to get the layout container of
     * @return                  The containing {@link IMapWidget} at the provided position
     * @since 5.3
     */
    private LayoutWidget getLayoutWidget(GlobeRegistration globeRegistration, WidgetLayoutPosition layoutPosition)
    {
        return layoutPosition != WidgetLayoutPosition.ABSOLUTE
                ? globeRegistration.root.getLayout(layoutPosition.layoutValue)
                : globeRegistration.absolutePositionLayout;
    }

    private void register(GlobeRegistration globeRegistration, IGlobeDelegate globe)
    {
        IWorldWindowGL wwd = globe.getWorldWindow();
        Component wwdComponent = wwd.getComponent();

        synchronized (globeRegistrations)
        {
            GlobeRegistration oldGlobeRegistration = getGlobeRegistration(globe);
            if (oldGlobeRegistration != null) unregister(oldGlobeRegistration, globe);
            globeRegistrations.put(globe, globeRegistration);
        }

        synchronized (widgetFactoryOrder)
        {
            widgetFactoryOrder.forEach(factory -> {
                try
                {
                    IWidget widget = factory.create(globe);
                    if (widget != null)
                    {
                        addWidgetToGlobe(widget, globe, globeRegistration, widgetLayoutPositions.get(factory));

                        // Use stored state if available
                        final var state = getState(widget, globe.getGlobeId());
                        if (state != null)
                        {
                            widget.setVisible(state.visible);
                        }
                        // Copy state of existing widget
                        else
                        {
                            getWidgetList().stream()
                                    .filter(widget::sameKindAs)
                                    .findFirst()
                                    .ifPresent(existingWidget -> widget.setVisible(existingWidget.isVisible()));
                        }
                    }
                } catch (Exception e)
                {
                    logger.error("Failed to register widget", e);
                }
            });
        }

        globeRegistration.impl.start();
        globeRegistration.motionEventAdapter.setMotionEventHandler(globeRegistration);
        wwd.addSelectListener(globeRegistration);
        wwd.addMouseWheelListener(globeRegistration);
        wwdComponent.addFocusListener(globeRegistration);
        globeRegistration.menuLayoutWidget.addOnWidgetListChangedListener(new TooltipHandler(globe));
    }

    private void unregister(GlobeRegistration globeRegistration, IGlobeDelegate globe)
    {
        globeRegistration.impl.stop();
        globeRegistration.widgetRegistrations.keySet().forEach(IWidget::releaseResources);
        IWorldWindowGL wwd = globe.getWorldWindow();
        if (wwd != null)
        {
            Component wwdComponent = wwd.getComponent();
            wwd.removeSelectListener(globeRegistration);
            wwdComponent.removeFocusListener(globeRegistration);
        }
        synchronized (globeRegistrations)
        {
            globeRegistrations.remove(globe);
        }
    }

    /**
     * render all widgets
     * @param globe the globe
     */
    public void render(@Nonnull IGlobeDelegate globe)
    {
        GlobeRegistration globeRegistration = getGlobeRegistration(globe);
        if (globeRegistration != null)
        {
            GLAutoDrawable autoDrawable = globe.getWorldWindow();

            boolean restoreDepth = GLES30.glIsEnabled(GLES30.GL_DEPTH_TEST);
            GLES30.glDisable(GLES30.GL_DEPTH_TEST);

            int[] restoreProg = new int[1];
            GLES30.glGetIntegerv(GLES30.GL_CURRENT_PROGRAM, restoreProg, 0);
            int restoreProgId = restoreProg[0];

            Matrix.orthoM(proj, 0, 0, autoDrawable.getSurfaceWidth(), 0, autoDrawable.getSurfaceHeight(), -1, 1);

            Matrix.setIdentityM(model, 0);
            Matrix.translateM(model, 0, 0, autoDrawable.getSurfaceHeight(), 0);

            GLWidget.DrawState drawState = new GLWidget.DrawState(null);
            drawState.modelMatrix = model;
            drawState.projectionMatrix = proj;

            globeRegistration.impl.drawWidget(drawState);

            if (restoreDepth) GLES30.glEnable(GLES30.GL_DEPTH_TEST);
            GLES30.glUseProgram(restoreProgId);
        }
    }

    /**
     * Determines if a widget was clicked, and if so adds this layer to the pick list.
     *
     * @param context the pick context
     * @return a list of picked objects, empty if no globe registration was found
     * @since 5.2
     */
    @Nonnull
    @Override
    public List<PickedObject> pick(@Nonnull IPickContext context)
    {
        GlobeRegistration globeRegistration = getGlobeRegistration(context.globe());
        if (globeRegistration == null) return Collections.emptyList();
        Point pickPoint = context.rectangle().getLocation();

        List<PickedObject> picked = new ArrayList<>();
        if (globeRegistration.root.seekWidgetHit(null, pickPoint.x, pickPoint.y) != null ||
                globeRegistration.menuLayoutWidget.getChildWidgetCount() > 0)
        {
            // Construct new picked object and add to list
            picked.add(new PickedObject(null, this));
        }
        return picked;
    }

    /**
     * Get the {@link IMenuLayoutWidget} for the provided {@link IGlobeDelegate}.
     * @param globe the globe delegate
     * @return the menu layout widget or null if there is none
     */
    private IMenuLayoutWidget getMenuLayoutWidget(IGlobeDelegate globe)
    {
        GlobeRegistration globeRegistration = getGlobeRegistration(globe);
        return globeRegistration != null ? globeRegistration.menuLayoutWidget : null;
    }

    /**
     * Show the radial menu for the given Map Object (which may be {@code null} if showing the terrain popup menu).
     *
     * @param globe Globe on which to show the radial menu
     * @param mapObject The map object for whom the radial menu is being displayed, or {@code null} if none
     * @param popupMenu The popup menu to display as a radial menu
     * @param x X screen coordinate at which to show the menu
     * @param y Y screen coordinate at which to show the menu
     * @since 5.6
     */
    public void showRadialMenu(@Nonnull IGlobeDelegate globe, @Nullable IMapObject mapObject, @Nonnull JPopupMenu popupMenu, int x, int y)
    {
        final var menuLayoutWidget = getMenuLayoutWidget(globe);
        final var point = new PointF(x, y);
        new MenuLayoutWidgetController(menuLayoutWidget).openMenuOnItem(new ContextMenuModel(popupMenu), point);
        notifyRadialMenuListeners(l -> l.onMenuOpened(mapObject));
    }

    /**
     * Assumes all widgets are on all globes, and so returns the widgets on the primaryGlobe as it will be the same
     * @return The list of widgets that have been added
     * @since 5.0
     */
    @Nonnull
    public List<IWidget> getWidgetList()
    {
        GlobeRegistration globeRegistration = getGlobeRegistration(globeManager.getPrimaryGlobeDelegate());
        return globeRegistration != null ? new ArrayList<>(globeRegistration.widgetRegistrations.keySet()) : Collections.emptyList();
    }

    /**
     * @param widgetClass Class of widgets to retrieve
     * @return List of widgets of the given {@code widgetClass}
     * @since 5.4
     */
    @Nonnull
    public List<IWidget> getWidgetsOfClass(@Nonnull Class<? extends IWidget> widgetClass)
    {
        final List<IWidget> widgets = new ArrayList<>();
        visitWidgets((widget, ignored) -> widgetClass.isAssignableFrom(widget.getClass()), (widget, ignored) -> widgets.add(widget));
        return widgets;
    }

    /**
     * Set the visibility of all {@link IWidget widgets} of a given class, across all globes.
     *
     * @param widgetClass The class of widget to toggle
     * @param visible The value to set the widget visibility to
     * @since 5.0
     */
    public void setWidgetVisibility(@Nonnull Class<? extends IWidget> widgetClass, boolean visible)
    {
        visitWidgets((widget, ignored) -> widgetClass.isAssignableFrom(widget.getClass()), (widget, ignored) -> widget.setVisible(visible));
    }

    /**
     * A MultiLayer containing the TAK widget layers. This includes the widget renderer as well as any widget-added layers.
     *
     * @return the layer
     * @since 5.0
     */
    @Nonnull
    public MultiLayer getLayer()
    {
        return layer;
    }

    private GlobeRegistration getGlobeRegistration(IGlobeDelegate globeDelegate)
    {
        synchronized (globeRegistrations)
        {
            return globeRegistrations.get(globeDelegate);
        }
    }

    /**
     * Close any previously opened radial menu.
     * @since 5.2
     */
    public void closeOpenRadialMenus()
    {
        globeManager.getAllGlobeDelegates().forEach(globeDelegate -> {
            final var menuLayoutWidget = getMenuLayoutWidget(globeDelegate);
            if (menuLayoutWidget != null && menuLayoutWidget.getChildWidgetCount() > 0) menuLayoutWidget.clearMenu();
        });
    }

    /**
     * Stores the manager's state in the VFS file {@link #WIDGET_STATES_FILE_NAME}.
     *
     * @since 5.8
     */
    @VisibleForTesting
    void storeState(List<IWidget> widgets)
    {
        try
        {
            final var widgetStates = widgets.stream()
                    .filter(w -> !EXCLUDED_WIDGETS.contains(w.getClass().getSimpleName()))
                    .map(w -> new WidgetState(getWidgetStateKey(w), w.isVisible(), 1)) // TODO: use real id when supported
                    .toList();
            final var json = OBJECT_MAPPER.writeValueAsString(new WidgetsManagerState(widgetStates.toArray(new WidgetState[0])));

            Files.createDirectories(stateFilePath.getParent());
            Files.writeString(stateFilePath, json);
        } catch (IOException e)
        {
            logger.error("Error storing widget state", e);
        }
    }

    /**
     * Restores the manager's state from the VFS file {@link #WIDGET_STATES_FILE_NAME}.
     *
     * @since 5.8
     */
    @VisibleForTesting
    void restoreState()
    {
        if (!Files.exists(stateFilePath)) return;

        String json = null;

        try
        {
            json = Files.readString(stateFilePath);
            final var state = OBJECT_MAPPER.readValue(json, WidgetsManagerState.class);
            Stream.of(state.widgetStates).forEach(ws -> widgetStateMap.computeIfAbsent(ws.className, k -> new ArrayList<>()).add(ws));
        } catch (IOException e)
        {
            logger.error("Error reading widget state from JSON: {}", json, e);
        }
    }

    /**
     * @param widget The widget
     * @param globeId The globe id
     * @return The widget's state or {@code null} if a state does not exist for the widget/globe combination
     * @since 5.8
     */
    @Nullable
    private WidgetState getState(@Nonnull IWidget widget, int globeId)
    {
        final var states = widgetStateMap.get(getWidgetStateKey(widget));

        return states != null
                ? states.stream().filter(ws -> ws.globeId == globeId).findFirst().orElse(null)
                : null;
    }

    @VisibleForTesting
    static String getWidgetStateKey(@Nonnull IWidget widget)
    {
        return widget.getClass().getName();
    }

    /**
     * Holds information about a widget's state that will be persisted to JSON.
     *
     * @param className The widget's FQ class name
     * @param visible {@code true} if the widget is visible
     * @param globeId The globe id with which the widget is associated (to be used when per-globe visibility is supported)
     * @since 5.8
     */
    @VisibleForTesting
    record WidgetState(@Nonnull String className, boolean visible, int globeId)
    {
    }

    /**
     * Holds information about this manager that will be persisted to JSON.
     *
     * @param widgetStates The widget states
     */
    private record WidgetsManagerState(@Nonnull WidgetState[] widgetStates)
    {
    }

    /**
     * Handles tooltip display based on widget hover events. Tooltip handling is dynamically
     * managed in the widget hierarchy based on add/remove of children.
     */
    private final class TooltipHandler implements IParentWidget.OnWidgetListChangedListener,
            IMapWidget.OnHoverListener, IMapWidget.OnVisibleChangedListener
    {
        final IGlobeDelegate globe;
        final TextWidget tooltipWidget;

        /**
         * The last widget that was hovered over; cleared if the widget is removed.
         */
        IMapWidget hoveredWidget;

        TooltipHandler(IGlobeDelegate globe)
        {
            this.globe = globe;
            tooltipWidget = new TextWidget();
            tooltipWidget.setTouchable(false);
            hoveredWidget = null;
        }

        private void showTooltip(String text, float x, float y)
        {
            GlobeRegistration globeRegistration = getGlobeRegistration(globe);
            if (globeRegistration != null)
            {
                MenuLayoutWidget menuLayoutWidget = globeRegistration.menuLayoutWidget;
                if (!menuLayoutWidget.getChildren().contains(tooltipWidget))
                {
                    tooltipWidget.setText(text);
                    tooltipWidget.setPoint(x, y);
                    menuLayoutWidget.addChildWidget(tooltipWidget);
                }
            }
        }

        private void hideTooltip()
        {
            GlobeRegistration globeRegistration = getGlobeRegistration(globe);
            if (globeRegistration != null)
            {
                globeRegistration.menuLayoutWidget.removeChildWidget(tooltipWidget);
            }
        }

        @Override
        public void onMapWidgetHover(IMapWidget widget, MotionEvent event)
        {
            if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER)
            {
                // the widget was entered, set the tooltip
                showTooltip(widget.getName(), event.getX(), event.getY());
                // record the hover widget
                hoveredWidget = widget;
            } else if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT && hoveredWidget == widget)
            {
                // clear the tooltip if displaying for the current button
                hideTooltip();
                // clear the hover widget
                hoveredWidget = null;
            }
        }

        @Override
        public void onVisibleChanged(IMapWidget widget)
        {
            if (!widget.isVisible() && hoveredWidget == widget)
            {
                // clear the tooltip if displaying for the current button
                hideTooltip();
                // clear the hover widget
                hoveredWidget = null;
            }
        }

        @Override
        public void onWidgetAdded(IParentWidget parent, int index, IMapWidget child)
        {
            if (child instanceof IMapMenuButtonWidget)
            {
                // install hover and visibility callbacks on menu buttons
                child.addOnHoverListener(this);
                child.addOnVisibleChangedListener(this);
            } else if (child instanceof IParentWidget childParent)
            {
                // recursively propagate handler through the child hierarchy for non-menu buttons
                childParent.addOnWidgetListChangedListener(this);
                int grandchildIdx = 0;
                for (IMapWidget grandchild : childParent.getChildren())
                {
                    onWidgetAdded(childParent, grandchildIdx++, grandchild);
                }
            }
        }

        @Override
        public void onWidgetRemoved(IParentWidget parent, int index, IMapWidget child)
        {
            if (child instanceof IMapMenuButtonWidget)
            {
                // uninstall hover/visibility callback
                child.removeOnHoverListener(this);
                child.removeOnVisibleChangedListener(this);
                if (hoveredWidget == child)
                {
                    // clear the tooltip if displaying for the current button
                    hideTooltip();
                    // clear the hover widget
                    hoveredWidget = null;
                }
            } else if (child instanceof IParentWidget childParent)
            {
                // recursively uninstall handler through the child hierarchy for non-menu buttons
                childParent.removeOnWidgetListChangedListener(this);
                int grandchildIdx = 0;
                for (IMapWidget grandchild : childParent.getChildren())
                {
                    onWidgetRemoved(childParent, grandchildIdx++, grandchild);
                }
            }
        }
    }

    /**
     * A per-widget registration
     * @param layoutPosition the layout position
     * @since 5.1
     */
    private record WidgetRegistration(WidgetLayoutPosition layoutPosition)
    {
    }

    /**
     * A per-globe registration to track widgets on the globe.
     *
     * @param impl                   the root layout widget implementation
     * @param root                   the root layout widget
     * @param renderer               the renderer associated with this registration
     * @param motionEventAdapter     the motion event adaptor
     * @param menuLayoutWidget       the menu layout widget
     * @param absolutePositionLayout the layout widget for absolutely positioned widgets
     * @param widgetRegistrations    a map of widgets to their widget registration
     * @since 5.1
     */
    private record GlobeRegistration(IGLWidget impl, RootLayoutWidget root, MapRenderer<?> renderer, MotionEventAdapter motionEventAdapter, MenuLayoutWidget menuLayoutWidget,
            LayoutWidget absolutePositionLayout, Map<IWidget, WidgetRegistration> widgetRegistrations) implements SelectListener, IMotionEventHandler, FocusListener, MouseWheelListener
    {
        /**
         * Handle widget selection
         *
         * @param selectEvent selection even to handle
         */
        @Override
        public void selected(SelectEvent selectEvent)
        {
            motionEventAdapter.handleSelectEvent(selectEvent);
        }

        /**
         * Handle motion event
         *
         * @param e MotionEvent to handle
         * @return true if handled
         */
        @Override
        public boolean handleMotionEvent(MotionEvent e)
        {
            final boolean handled = root.handleMotionEvent(e);
            final boolean clearMenu = !handled && (e.getAction() == MotionEvent.ACTION_UP && menuLayoutWidget.getChildWidgetCount() > 0);
            if (clearMenu) menuLayoutWidget.clearMenu();
            return handled;
        }

        @Override
        public void focusLost(FocusEvent e)
        {
            if (menuLayoutWidget.getChildWidgetCount() > 0) menuLayoutWidget.clearMenu();
        }

        @Override
        public void focusGained(FocusEvent e)
        {
            // no-op
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e)
        {
            // clear the popup menu if we scroll the mouse wheel since zoom doesn't stay centered
            if (menuLayoutWidget.getChildWidgetCount() > 0) menuLayoutWidget.clearMenu();
        }
    }
}
