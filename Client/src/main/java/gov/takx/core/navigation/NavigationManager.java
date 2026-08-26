package gov.takx.core.navigation;

import gov.tak.api.icons.TakColor;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.breadcrumb.IBreadcrumbManager;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.api.mapobject.IPointMapObject;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.api.mapobject.IShapeArrowMapObject;
import gov.takx.api.mapobject.MapObjectOption;
import gov.takx.api.mapobject.MapObjectOptions;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.shapes.ShapeType;
import gov.takx.api.ui.ComponentPosition;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.commons.swing.ActionPopupFactory;
import gov.takx.core.view.gui.IExtendedUISpaceManager;
import gov.takx.core.view.gui.navigation.NavigationPanel;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.ui.ComponentDescriptor;
import gov.takx.platform.ui.ComponentDescriptorBuilder;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_AUTO_KEEP_IN_VIEW;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_AUTO_TARGET_BREADCRUMBS;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_DISPLAY_INTERCEPT;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_FLASH;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_FLASH_ETA_COLOR;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_FLASH_ETA_THRESHOLD;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_INNER_ETA_COLOR;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_INNER_ETA_THRESHOLD;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_LOW_SPEED_COLOR;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_MIDDLE_ETA_COLOR;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_MIDDLE_ETA_THRESHOLD;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_OUTER_ETA_COLOR;

/**
 * Manager for {@link NavigationConfiguration} instances and the {@link NavigationPanel}.
 *
 * @since 5.6 (Renamed from BloodhoundController in 5.7)
 */
@ApplicationScoped
public class NavigationManager // TODO DCW TAKX-4314 expose as interface and deprecate IRouteNavigationManager
{
    private static final short INTERCEPT_LINE_STIPPLE = (short) (-1 << 8);
    private static final String CANCEL_POPUP_STRING_FORMAT = "<html>Cancel Navigation <b>%s<b>?</html>";

    private static final MapObjectOptions NAVIGATION_MAP_OBJECT_OPTIONS = new MapObjectOptions(
            MapObjectOption.NoPersistence, MapObjectOption.NoAlarms, MapObjectOption.NoStyledTracks,
            MapObjectOption.NoPluginState, MapObjectOption.NoEventLogs, MapObjectOption.ExcludeFromExplorer);

    private final IPreferenceMonitor<Long> flashThreshold = BLOODHOUND_FLASH_ETA_THRESHOLD.newMonitor(Long.class, this::updateAllConfigurations);
    private final IPreferenceMonitor<TakColor> outerColor = BLOODHOUND_OUTER_ETA_COLOR.newMonitor(TakColor.class, this::updateAllConfigurations);
    private final IPreferenceMonitor<TakColor> middleColor = BLOODHOUND_MIDDLE_ETA_COLOR.newMonitor(TakColor.class, this::updateAllConfigurations);
    private final IPreferenceMonitor<TakColor> innerColor = BLOODHOUND_INNER_ETA_COLOR.newMonitor(TakColor.class, this::updateAllConfigurations);
    private final IPreferenceMonitor<TakColor> unreachableColor = BLOODHOUND_LOW_SPEED_COLOR.newMonitor(TakColor.class, this::updateAllConfigurations);
    private final IPreferenceMonitor<Long> middleThreshold = BLOODHOUND_MIDDLE_ETA_THRESHOLD.newMonitor(Long.class, this::updateAllConfigurations);
    private final IPreferenceMonitor<Long> innerThreshold = BLOODHOUND_INNER_ETA_THRESHOLD.newMonitor(Long.class, this::updateAllConfigurations);
    private final IPreferenceMonitor<Boolean> displayIntercept = BLOODHOUND_DISPLAY_INTERCEPT.newMonitor(Boolean.class, this::updateAllConfigurations);
    private final IPreferenceMonitor<Boolean> flashEnabled = BLOODHOUND_FLASH.newMonitor(Boolean.class, this::updateAllConfigurations);
    private final IPreferenceMonitor<TakColor> flashColor = BLOODHOUND_FLASH_ETA_COLOR.newMonitor(TakColor.class, this::updateAllConfigurations);

    private final List<NavigationConfiguration> configurationList = new CopyOnWriteArrayList<>();
    private final List<NavigationManagerListener> configurationListeners = new ArrayList<>();

    private final IMapObjectManager mapObjectManager;
    private final IExtendedGlobeManager globeManager;
    private final IBreadcrumbManager breadcrumbManager;
    private final IExtendedUISpaceManager uiSpaceManager;
    private final ComponentDescriptor navigationPanelDescriptor;

    private NavigationPanel navigationPanel;
    private NavigationConfiguration currentNavigation;

    /**
     * Construct a new controller using the given managers for creating map objects and handling keep-in-view and breadcrumbs.
     *
     * @param mapObjectManager the map object manager to use when creating map objects
     * @param globeManager the globe manager to use for keep-in-view
     * @param breadcrumbManager the breadcrumb manager to use for enabled breadcrumbs
     */
    @Inject
    NavigationManager(IMapObjectManager mapObjectManager, IExtendedGlobeManager globeManager, IBreadcrumbManager breadcrumbManager, IExtendedUISpaceManager uiSpaceManager)
    {
        this.mapObjectManager = mapObjectManager;
        this.globeManager = globeManager;
        this.breadcrumbManager = breadcrumbManager;
        this.uiSpaceManager = uiSpaceManager;

        navigationPanelDescriptor = new ComponentDescriptorBuilder(NavigationPanel.class, NavigationPanel.PANEL_TITLE)
                .setComponentPosition(ComponentPosition.RIGHT)
                .setIcon(NavigationConstants.NAVIGATE_TO_ICON_URL)
                .setToolbarButtonVisible(false)
                .setPinned(true)
                .build();
    }

    /**
     * @return a List of all current configurations
     */
    @Nonnull
    public List<NavigationConfiguration> getConfigurations()
    {
        return List.copyOf(configurationList);
    }

    /**
     * Toggles the keep-in-view state of the given navigation configuration.
     * 
     * @param configuration the navigation configuration
     */
    public void toggleKeepInView(@Nonnull NavigationConfiguration configuration)
    {
        var globe = globeManager.getLastFocusedGlobe();
        if (!isKeepInView(configuration))
        {
            globe.addKeepInView(configuration.getTargetMapObject());
            globe.addKeepInView(configuration.getSourceMapObject());
        } else
        {
            globe.removeKeepInView(configuration.getTargetMapObject());
            globe.removeKeepInView(configuration.getSourceMapObject());
        }
    }

    /**
     * @param navigation a navigation instance
     * @return {@code true} if the given configuration is currently kept in view, otherwise {@code false}
     */
    public boolean isKeepInView(@Nonnull NavigationConfiguration navigation)
    {
        var globe = globeManager.getLastFocusedGlobe();
        return globe.isKeepInView(navigation.getSourceMapObject()) && globe.isKeepInView(navigation.getTargetMapObject());
    }

    /**
     * Create a navigation instance for the given source and target objects.
     *
     * @param source the source point
     * @param target the target object
     * @return a navigation instance
     * @since 5.7
     */
    public NavigationConfiguration createNavigationConfiguration(@Nonnull IPointMapObject source, @Nonnull IMapObject target)
    {
        var configuration = findNavigationConfiguration(source, target);
        if (configuration != null) return configuration;
        if (target instanceof IRouteMapObject route)
        {
            return createRouteNavigationConfiguration(source, route);
        } else if (target instanceof IPointMapObject point)
        {
            return createPointNavigationConfiguration(source, point);
        } else
        {
            throw new IllegalArgumentException("Cannot create NavigationConfiguration Configuration for MapObject type: " + target.getClass().getCanonicalName());
        }
    }

    /**
     * Create a navigation instance between the given source and target points.
     * 
     * @param source the source item
     * @param target the target item
     * @return the new navigation instance
     * @since 5.7
     */
    public PointNavigationConfiguration createPointNavigationConfiguration(@Nonnull IPointMapObject source, @Nonnull IPointMapObject target)
    {
        var configuration = new PointNavigationConfiguration(source, target);
        initNavigation(configuration);
        return configuration;
    }

    /**
     * Create a navigation instance for the given source along the given route.
     *
     * @param source the source object
     * @param route the route to navigate
     * @return the new navigation instance
     * @since 5.7
     */
    @Nonnull
    public RouteNavigationConfiguration createRouteNavigationConfiguration(@Nonnull IPointMapObject source, @Nonnull IRouteMapObject route)
    {
        var configuration = new RouteNavigationConfiguration(source, route);
        initNavigation(configuration);
        return configuration;
    }

    /**
     * @param listener a Listener to receive events when navigation configurations are added or removed
     */
    public void addConfigurationListener(@Nonnull NavigationManagerListener listener)
    {
        configurationListeners.add(listener);
    }

    /**
     * @param listener the listener that should no longer to receive blood hound configuration events
     */
    @SuppressWarnings("unused")
    public void removeConfigurationListener(@Nonnull NavigationManagerListener listener)
    {
        configurationListeners.remove(listener);
    }

    /**
     * @param configuration the navigation configuration to delete
     */
    public void removeConfiguration(@Nonnull NavigationConfiguration configuration)
    {
        configuration.releaseResources();
        configurationList.remove(configuration);
        configuration.getMapObjects().forEach(mapObjectManager::deleteMapObject);
        ExceptionUtils.safeDoOperation(configurationListeners, l -> l.removed(configuration));
    }

    /**
     * Show a yes/no popup and remove the configuration if the use selects yes.
     *
     * @param configuration the navigation configuration to delete
     * @param popupSource the component to use as the source of a popup
     * @since 5.7
     */
    public void removeConfiguration(@Nonnull NavigationConfiguration configuration, @Nonnull Component popupSource)
    {
        var name = configuration.getSourceMapObject().getName() + " → " + configuration.getTargetMapObject().getName();
        ActionPopupFactory
                .yesNo(CANCEL_POPUP_STRING_FORMAT.formatted(name), popupSource, () -> removeConfiguration(configuration))
                .show();
    }

    /**
     * Search for a configuration with the given source and target.
     * 
     * @param source the source object
     * @param target the target object
     * @return the navigation found, or {@code null} if one does not exist
     * @since 5.7
     */
    @Nullable
    public NavigationConfiguration findNavigationConfiguration(@Nonnull IMapObject source, @Nonnull IMapObject target)
    {
        return configurationList.stream()
                .filter(config -> config.getSourceMapObject() == source && config.getTargetMapObject() == target)
                .findFirst()
                .orElse(null);
    }

    /**
     * @param etaSec an eta value in seconds
     * @return the proper color of a navigation line having the given eta
     * @since 5.7
     */
    @Nonnull
    public Color getArrowColor(double etaSec)
    {
        TakColor color;
        if (etaSec < innerThreshold.getCurrentValue())
        {
            color = innerColor.getCurrentValue();
        } else if (etaSec < middleThreshold.getCurrentValue())
        {
            color = middleColor.getCurrentValue();
        } else if (Double.isNaN(etaSec))
        {
            color = unreachableColor.getCurrentValue();
        } else
        {
            color = outerColor.getCurrentValue();
        }
        return color.getColor();
    }

    /**
     * @return the Color that navigational lines should be when flashing
     * @since 5.7
     */
    public Color getFlashColor()
    {
        return flashColor.getCurrentValue().getColor();
    }

    /**
     * @return {@code true} if navigational lines should flash, otherwise {@code false}
     * @since 5.7
     */
    public boolean isFlashEnabled()
    {
        return flashEnabled.getCurrentValue();
    }

    /**
     * @return {@code true} if navigation instances should display intercept points and lines, otherwise {@code false}
     * @since 5.7
     */
    public boolean displayIntercept()
    {
        return displayIntercept.getCurrentValue();
    }

    /**
     * @param etaSec the eta of a navigation target
     * @return the delay in seconds between line flashing
     * @since 5.7
     */
    public int getFlashDelay(long etaSec)
    {
        return etaSec < innerThreshold.getCurrentValue() ? 3 : etaSec < middleThreshold.getCurrentValue() ? 5 : etaSec < flashThreshold.getCurrentValue() ? 7 : -1;
    }

    /**
     * Creates a navigational R&B arrow between two map objects.
     *
     * @param from the start object for the arrow
     * @param to the end object for the arrow
     * @return an arrow map object between the two given objects
     * @since 5.7
     */
    @Nonnull
    public IShapeArrowMapObject createTargetArrow(@Nonnull IMapObject from, @Nonnull IMapObject to)
    {
        return createArrow(from, to, "NavigationConfiguration Target Arrow", null);
    }

    /**
     * Creates a navigational R&B arrow between two map objects, styled as an intercept arrow.
     *
     * @param source the start object for the arrow
     * @param target the end object for the arrow
     * @return an arrow map object between the two given objects
     * @since 5.7
     */
    @Nonnull
    public IShapeArrowMapObject createInterceptArrow(@Nonnull IMapObject source, @Nonnull IMapObject target)
    {
        return createArrow(source, target, "NavigationConfiguration Intercept Arrow", INTERCEPT_LINE_STIPPLE);
    }

    /**
     * Creates an intercept point for the given source and target object.
     *
     * @param source the source object
     * @param target the target object
     * @return the created marker
     * @since 5.7
     */
    @Nonnull
    public IMarkerMapObject createInterceptPoint(@Nonnull IMapObject source, @Nonnull IMapObject target)
    {
        var interceptPointSpec = MapObjectSpec.builder(source.getFamily(), source.getType())
                .location(target.getLocation())
                .name(target.getName())
                .initializer(mo -> {
                    var marker = (IMarkerMapObject) mo;
                    marker.setAllowDrag(false);
                    marker.setIconImage(target.getIconImageURI());
                    marker.setAllowDeviceToBeContainer(false);
                    marker.setSelectable(false);
                    marker.setVisible(false);
                })
                .mapObjectOptions(NAVIGATION_MAP_OBJECT_OPTIONS)
                .build();
        return (IMarkerMapObject) mapObjectManager.getOrCreateMapObject(interceptPointSpec);
    }

    /**
     * Create a target map object from a metadata instance.
     *
     * @param geoPointMetaData the metadata to get a location from
     * @return a point map object at the given location
     * @since 5.7
     */
    @Nonnull
    public IPointMapObject createTargetPoint(GeoPointMetaData geoPointMetaData)
    {
        var spec = MapObjectSpec.builder(KeyUtils.TOOL_FAMILY, IPointMapObject.POINT_TYPE)
                .name("Target Point")
                .initializer(mo -> {
                    var marker = (IPointMapObject) mo;
                    marker.setAllowDrag(false);
                    marker.setIconImage(UIResources.getUri(ResourceDirectory.PLATFORM_ICONS, "circle.png"));
                    marker.setAllowDeviceToBeContainer(false);
                    marker.setSelectable(false);
                    marker.setVisible(false);
                    marker.setGeoPointMetaData(geoPointMetaData);
                })
                .mapObjectOptions(NAVIGATION_MAP_OBJECT_OPTIONS)
                .build();

        return (IPointMapObject) mapObjectManager.getOrCreateMapObject(spec);
    }

    /**
     * Populates the navigation panel with the given navigation information.
     *
     * @param navigation the navigation configuration to use, or {@code null} to clear the panel and hide it
     * @since 5.7
     */
    public void navigate(@Nullable NavigationConfiguration navigation)
    {
        if (currentNavigation != navigation)
        {
            currentNavigation = navigation;
            getNavigationPanel().setNavigation(navigation);
        }
        if (currentNavigation != null)
        {
            showNavigationPanel();
        } else
        {
            hideNavigationPanel();
        }
    }

    /**
     * @return the navigation currently visible on the navigation panel
     * @since 5.7
     */
    @Nullable
    public NavigationConfiguration getCurrentNavigation()
    {
        return currentNavigation;
    }

    /**
     * @param mapObject the map object to check
     * @return {@code true} if the map object can be navigated, otherwise {@code false}
     * @since 5.7
     */
    public boolean isNavigable(@Nonnull IMapObject mapObject)
    {
        return mapObject instanceof IPointMapObject || mapObject instanceof IRouteMapObject;
    }

    /**
     * Clear and hide the navigation panel.
     * @since 5.7
     */
    public void hideNavigationPanel()
    {
        getNavigationPanel().setNavigation(null);
        uiSpaceManager.removeComponent(getNavigationPanel());
    }

    @Nonnull
    private NavigationPanel getNavigationPanel()
    {
        if (navigationPanel == null) navigationPanel = new NavigationPanel();
        return navigationPanel;
    }

    private void initNavigation(NavigationConfiguration configuration)
    {
        configurationList.add(configuration);
        if (BLOODHOUND_AUTO_KEEP_IN_VIEW.getBooleanValue()) toggleKeepInView(configuration);
        if (BLOODHOUND_AUTO_TARGET_BREADCRUMBS.getBooleanValue() && configuration.getTargetMapObject() instanceof IMarkerMapObject) breadcrumbManager.startBreadcrumbs(configuration.getTargetMapObject().getUid(), null);
        ExceptionUtils.safeDoOperation(configurationListeners, l -> l.added(configuration));
        configuration.getSourceMapObject().addDeletionHandler(item -> removeConfiguration(configuration));
        configuration.getTargetMapObject().addDeletionHandler(item -> removeConfiguration(configuration));
    }

    private void showNavigationPanel()
    {
        var panel = getNavigationPanel();
        uiSpaceManager.addComponent(navigationPanelDescriptor, panel);
        uiSpaceManager.showComponent(panel);
    }

    private void updateAllConfigurations(Object ignored)
    {
        configurationList.forEach(NavigationConfiguration::update);
    }

    private IShapeArrowMapObject createArrow(@Nonnull IMapObject from, @Nonnull IMapObject to, @Nonnull String name, @Nullable Short stipple)
    {
        var primaryArrowSpec = MapObjectSpec.builder(KeyUtils.TOOL_FAMILY, ShapeType.ARROW.getShapeType())
                .name(name)
                .initializer(mo -> {
                    var arrow = (IShapeArrowMapObject) mo;
                    arrow.setAllowDeviceToBeContainer(false);
                    arrow.setAllowDrag(false);
                    arrow.setEditable(false);
                    arrow.setMetaBoolean(StandardAttributeNames.RANGE_BEARING, true);
                    arrow.getAssociatedShape().setName(arrow.getName());
                    arrow.setStartAnchor(from);
                    arrow.setSelectable(false);
                    arrow.setVisible(false);
                    if (stipple != null)
                    {
                        arrow.setLineStippleFactor(1);
                        arrow.setLineStipplePattern(stipple);
                    }
                    // TODO TAKX-2503 opacity doesn't work
                    //arrow.setOpacity(50);

                    arrow.setRangeAnchor(to);
                })
                .mapObjectOptions(NAVIGATION_MAP_OBJECT_OPTIONS)
                .build();
        return (IShapeArrowMapObject) mapObjectManager.getOrCreateMapObject(primaryArrowSpec);
    }

    /**
     * Listener for changes to drifter configurations.
     */
    public interface NavigationManagerListener
    {
        /**
         * @param configuration the newly added configuration
         */
        void added(@Nonnull NavigationConfiguration configuration);

        /**
         * @param configuration the deleted configuration
         */
        void removed(@Nonnull NavigationConfiguration configuration);
    }
}
