package gov.tak.core.location;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.location.ILocationProviderManager;
import gov.tak.api.location.ILocationTracker;
import gov.tak.api.maps.Marker;
import gov.tak.api.maps.PointMapItem;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.Headless;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.location.LocationChanged;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.api.mapobject.MapObjectOption;
import gov.takx.api.mapobject.MapObjectOptions;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.util.List;

import static gov.takx.core.project.ServiceOrdering.START_GenericCoreService;
import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;
import static gov.takx.platform.preferences.PreferenceEntry.SHOW_CURRENT_LOCATION_MARKER;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static jakarta.enterprise.event.Reception.IF_EXISTS;

/**
 * This service tracks the current location, as reported by the {@link ILocationProviderManager}, and displays
 * a Marker at that location if configured to do so (via {@link PreferenceEntry#SHOW_CURRENT_LOCATION_MARKER}).
 *
 * @see ILocationProviderManager#getCurrentLocation()
 * @since 3.0
 */
@ApplicationScoped
public class LocationTracker implements ILocationTracker
{
    private static final String MARKER_NAME = "Self Location Marker";
    private static final URI MARKER_ICON_URI = UIResources.getUri(PLATFORM_ICONS, "self.png");

    private static final MapObjectOptions MARKER_OPTIONS = new MapObjectOptions(
            MapObjectOption.NoAlarms, MapObjectOption.NoStyledTracks, MapObjectOption.NoPluginState,
            MapObjectOption.ExcludeFromExplorer);

    private static final List<ContextMenuEnum> MENU_ITEMS_TO_REMOVE = List.of(
            ContextMenuEnum.DEACTIVATE, ContextMenuEnum.DELETE, ContextMenuEnum.DEVICE_LOG, ContextMenuEnum.HIDE,
            ContextMenuEnum.OPEN, ContextMenuEnum.SEND_AS_COT);

    private final PropertyChangeListener locationChangeListener = e -> mapObjectMoved();

    private final boolean headless;
    private final IExtendedGlobeManager globeManager;
    private final ILocationProviderManager locationProviderManager;
    private final IMapObjectManager mapObjectManager;

    private final IPreferenceMonitor<Boolean> showMarkerMonitor = SHOW_CURRENT_LOCATION_MARKER.newBooleanMonitor(this::showMarkerChanged, false);

    private PointMapItem locationMarker;
    private boolean showMarker;
    private GeoPoint lastLocation;
    private volatile boolean movingMarker; // Indicates if we are moving the marker

    /**
     * CDI Constructor.
     */
    @Inject
    LocationTracker(IExtendedGlobeManager globeManager, ILocationProviderManager locationProviderManager,
                    IMapObjectManager mapObjectManager, @Headless boolean headless)
    {
        this.headless = headless;
        this.globeManager = globeManager;
        this.locationProviderManager = locationProviderManager;
        this.mapObjectManager = mapObjectManager;
    }

    @Override
    public PointMapItem getMarkerMapObject()
    {
        return locationMarker;
    }

    @Override
    public void setLocationMarkerIcon(@Nullable BufferedImage iconImage)
    {
        if (iconImage != null)
        {
            locationMarker.setExternalIconImage(iconImage);
        } else
        {
            locationMarker.setIconImage(MARKER_ICON_URI); // Back to the default
        }
    }

    @Override
    public void setLocationMarkerTooltipText(@Nullable String tooltipText)
    {
        locationMarker.setName(tooltipText != null ? tooltipText : MARKER_NAME);
    }

    /**
     * Start up, unless we are headless.
     */
    private void startService(@Observes @Priority(START_GenericCoreService) StartProjectServices event)
    {
        showMarkerMonitor.start();

        if (!headless) createMarker();
        showMarkerChanged(showMarkerMonitor.getCurrentValue());
    }

    /**
     * Shutdown.
     */
    private void stopService(@Observes @Priority(STOP_GenericCoreService) StopProjectServices event)
    {
        showMarkerMonitor.stop();

        if (locationMarker != null) locationMarker.removePropertyChangeListener(IMapObject.LOCATION, locationChangeListener);
    }

    /**
     * Handle a change to the location of our marker by updating the location provider configuration.
     * @since 5.3
     */
    private void mapObjectMoved()
    {
        final var location = locationMarker.getLocation();
        if (!movingMarker && !location.equals(lastLocation)) locationProviderManager.setFixedLocation(location);
    }

    /**
     * Set the location of the marker to the given location.
     *
     * @param newLocation new location, {@code null} (meaning no location) will hide the marker
     */
    private void updateMarkerLocation(@Nullable GeoPointMetaData newLocation)
    {
        if (locationMarker != null)
        {
            if (showMarker && newLocation != null)
            {
                movingMarker = true;
                locationMarker.setGeoPointMetaData(newLocation);
                movingMarker = false;

                // Extract and install the heading from the point metadata, if it exists
                final double heading = newLocation.getMetaData().getDoubleAttribute(StandardAttributeNames.BEARING, Double.NaN);

                if (!Double.isNaN(heading)) locationMarker.setHeading(heading);

                locationMarker.setVisible(true);
            } else
            {
                locationMarker.setVisible(false);
                movingMarker = true;
                locationMarker.setGeoPointMetaData(new GeoPointMetaData());
                movingMarker = false;
                if (newLocation != null) lastLocation = newLocation.get();
            }

            globeManager.repaintAllGlobes();
        }
    }

    /**
     * Create the marker that will display the current location.
     */
    @VisibleForTesting
    public void createMarker()
    {
        // Use a constant unit id, so we don't create a new marker every time the project is opened
        final MapObjectSpec spec = MapObjectSpec.builder(IMarkerMapObject.MARKER_FAMILY, "SelfLocation", "0")
                .name(MARKER_NAME)
                .mapObjectOptions(MARKER_OPTIONS)
                .cotType(CotTypes.SELF)
                .initializer(LocationTracker::setDefaultTrackerAttributes)
                .build();

        locationMarker = (PointMapItem) mapObjectManager.getOrCreateMapObject(spec);
        locationMarker.addPropertyChangeListener(IMapObject.LOCATION, locationChangeListener);

        // Handle opening an old project from when this marker used to be non-selectable. Make it selectable.
        if (!locationMarker.isSelectable()) locationMarker.setSelectable(true);

        // And, if our marker isn't identifying as the self marker, fix the CoT type.
        if (!locationMarker.isSelfMarker()) locationMarker.setCotType(CotTypes.SELF);

        // Remove all the menu items that are not applicable to "self"
        final var menuSpaceManager = locationMarker.getController().getMenuSpaceManager();

        MENU_ITEMS_TO_REMOVE.forEach(menuItem -> menuSpaceManager.setPopupMenuVisible(menuItem, false));
    }

    /**
     * Assigns default location tracker attributes to the {@link #locationMarker}.
     * @since 5.8
     */
    public void setDefaultTrackerAttributes()
    {
        if (locationMarker != null) setDefaultTrackerAttributes(locationMarker);
    }

    /**
     * Assigns default location tracker attributes to the given marker.
     * @param mo Map object to treat as our location marker.
     * @since 5.8
     */
    private static void setDefaultTrackerAttributes(@Nonnull IMapObject mo)
    {
        mo.setCotType(CotTypes.SELF);
        mo.setIconImage(MARKER_ICON_URI);
        mo.setAllowDrag(false);
        mo.setAllowDeviceToBeContainer(false);
        if (mo instanceof Marker marker)
        {
            marker.setStyle(marker.getStyle() | Marker.STYLE_ROTATE_HEADING_MASK);
            marker.setStyle(marker.getStyle() | Marker.STYLE_ROTATE_HEADING_NOARROW_MASK);
        }
    }

    /**
     * Current location has changed, update our marker.
     */
    private void onLocationChange(@Observes(notifyObserver = IF_EXISTS) LocationChanged event)
    {
        updateMarkerLocation(event.newLocation());
    }

    /**
     * Handle a change in the "show marker" preference.
     *
     * @param showMarker New preference value
     */
    private void showMarkerChanged(boolean showMarker)
    {
        this.showMarker = showMarker;

        updateMarkerLocation(locationProviderManager.getCurrentLocation());
    }
}
