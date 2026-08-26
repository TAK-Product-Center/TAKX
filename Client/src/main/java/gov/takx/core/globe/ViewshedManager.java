package gov.takx.core.globe;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.layer.ITakLayerManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectOption;
import gov.takx.api.mapobject.MapObjectOptions;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.core.globe.tak.layer.ViewshedLayer;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;

/**
 * Manages all Viewshed layers.
 *
 * @since 5.6
 */
@ApplicationScoped
public class ViewshedManager implements ViewshedLayer.OnPointOfInterestChangedListener
{
    private static final URI VIEWSHED_ICON_URI = UIResources.getUri(ResourceDirectory.TAK_ASSETS_ICONS, "viewshed_eye.png");

    private static final MapObjectOptions MAP_OBJECT_OPTIONS = new MapObjectOptions(
            MapObjectOption.NoPersistence, MapObjectOption.NoAlarms, MapObjectOption.NoStyledTracks,
            MapObjectOption.NoPluginState, MapObjectOption.NoEventLogs, MapObjectOption.ExcludeFromExplorer);

    private final ITakLayerManager takLayerManager;
    private final IMapObjectManager mapObjectManager;

    private final List<ViewshedLayer> layers = new CopyOnWriteArrayList<>();
    private final List<ViewshedListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<ViewshedLayer, IMapObject> mapObjectsByLayer = new ConcurrentHashMap<>();

    private int viewshedLayerNameCounter;

    @Inject
    ViewshedManager(ITakLayerManager takLayerManager, IMapObjectManager mapObjectManager)
    {
        this.takLayerManager = takLayerManager;
        this.mapObjectManager = mapObjectManager;
    }

    /**
     * Create a {@link ViewshedLayer}.
     * @param forRoute if this viewshed is intended for a route
     * @return The layer showing a viewshed
     * @since 5.7
     */
    @Nonnull
    public ViewshedLayer createViewshed(boolean forRoute)
    {
        final var layerName = forRoute ? "Viewshed Route" : "Viewshed " + (++viewshedLayerNameCounter);
        final var layer = new ViewshedLayer(layerName, forRoute);
        layer.addOnPointOfInterestChangedListener(this);

        if (!forRoute) createMapObjectForLayer(layer);

        layers.add(layer);
        takLayerManager.addLayer(layer);

        ExceptionUtils.safeDoOperation(listeners, l -> l.viewshedCreated(layer));
        return layer;
    }

    /**
     * Delete the given {@link ViewshedLayer}.
     * @param layer the layer to delete
     * @since 5.7
     */
    public void deleteViewshed(@Nonnull ViewshedLayer layer)
    {
        deleteViewshed(layer, true);
    }

    /**
     * Delete the given {@link ViewshedLayer}.
     * @param layer the layer to delete
     * @param deleteMapObject Pass {@code true} if the Map Object representing this layer should also be deleted
     * @since 5.7
     */
    private void deleteViewshed(@Nonnull ViewshedLayer layer, boolean deleteMapObject)
    {
        if (layers.remove(layer))
        {
            final var mapObject = mapObjectsByLayer.remove(layer);
            if (deleteMapObject && mapObject != null) mapObjectManager.deleteMapObject(mapObject);

            layer.setVisible(false);
            layer.removeOnPointOfInterestChangedListener(this);
            takLayerManager.removeLayer(layer);

            ExceptionUtils.safeDoOperation(listeners, l -> l.viewshedDeleted(layer));
        }
    }

    /**
     * When the POI of the layer changes, we need to move the MapObject representing the viewshed accordingly.
     */
    @Override
    public void onPointOfInterestChanged(@Nonnull ViewshedLayer layer)
    {
        final var mapObject = mapObjectsByLayer.get(layer);
        if (mapObject != null)
        {
            final var moLocation = mapObject.getLocation();
            final var layerLocation = layer.getMarkerLocation();

            // If the MO didn't move, ignore the change
            if (Double.compare(moLocation.getLatitude(), layerLocation.getLatitude()) != 0
                    || Double.compare(moLocation.getLongitude(), layerLocation.getLongitude()) != 0)
            {
                mapObject.setLocation(layerLocation, true);
            }
        }

        ExceptionUtils.safeDoOperation(listeners, l -> l.viewshedLocationChanged(layer));
    }

    /**
     * Construct the Map Object that represents the given viewshed.  This Map Object gives the user a mechanism
     * to move the location by dragging the marker.
     */
    private void createMapObjectForLayer(@Nonnull ViewshedLayer layer)
    {
        final var specs = MapObjectSpec.builder(KeyUtils.TOOL_FAMILY, "Viewshed")
                .name(layer.getName())
                .mapObjectOptions(MAP_OBJECT_OPTIONS)
                .initializer(mo -> {
                    mo.setMetaBoolean(StandardAttributeNames.VIEWSHED, true);
                    mo.setMetaBoolean(StandardAttributeNames.NEVER_COT, true);
                    mo.setIconImage(VIEWSHED_ICON_URI);
                    mo.setCotType(CotTypes.VIEWSHED_MARKER_TYPE);
                    mo.addDeletionHandler(mi -> deleteViewshed(layer, false));
                    mo.addPropertyChangeListener(IMapObject.LOCATION, evt -> mapObjectLocationChanged(layer, mo));
                })
                .build();
        final var mapObject = mapObjectManager.getOrCreateMapObject(specs);
        mapObjectsByLayer.put(layer, mapObject);
    }

    /**
     * Update the layer's marker location when the MO location changes.
     */
    private void mapObjectLocationChanged(@Nonnull ViewshedLayer layer, @Nonnull IMapObject mapObject)
    {
        boolean updating = mapObject.getMetaBoolean(StandardAttributeNames.DRAGGING, false);
        layer.setMarkerLocation(mapObject.getLocation(), updating);
    }

    /**
     * Register a {@link ViewshedListener} with this manager to receive events when a viewshed is added/removed/moved.
     * @param listener the listener to add
     * @since 5.7
     */
    public void addViewshedListener(@Nonnull ViewshedListener listener)
    {
        listeners.add(listener);
    }

    /**
     * Unregister a {@link ViewshedListener} with this manager to stop receiving events.
     * @param listener the listener to remove
     * @since 5.7
     */
    public void removeViewshedListener(@Nonnull ViewshedListener listener)
    {
        listeners.remove(listener);
    }

    private void stopService(@Observes @Priority(STOP_GenericCoreService) StopProjectServices event)
    {
        listeners.clear();
        layers.clear();
        mapObjectsByLayer.clear();
        viewshedLayerNameCounter = 0;
    }

    /**
     * Listener for changes to Viewshed layers.
     * @since 5.7
     */
    public interface ViewshedListener
    {
        /**
         * Called when a new viewshed layer is created.
         * @param layer the newly added layer
         */
        default void viewshedCreated(@Nonnull ViewshedLayer layer)
        {
        }

        /**
         * Called when a viewshed layer is deleted.
         * @param layer the deleted layer
         */
        default void viewshedDeleted(@Nonnull ViewshedLayer layer)
        {
        }

        /**
         * Called when the viewshed location changes.
         * @param layer the layer whose location changed
         */
        default void viewshedLocationChanged(@Nonnull ViewshedLayer layer)
        {
        }
    }
}
