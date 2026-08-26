package gov.takx.test.utils;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.api.mapobject.IMultiPolylineMapObject;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.api.mapobject.IShapeArrowMapObject;
import gov.takx.api.mapobject.IShapePathMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.shapes.ShapeType;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.event.Observes;

import java.beans.PropertyChangeListener;
import java.util.function.Consumer;

/**
 * Common methods for map object tests.
 *
 * @since 3.0
 */
public class MapObjectTestUtils
{
    private final IMapObjectManager mapObjectManager;

    public MapObjectTestUtils(IMapObjectManager mapObjectManager)
    {
        this.mapObjectManager = mapObjectManager;
    }

    public IShapeArrowMapObject createArrowMapObject(@Nullable Consumer<IMapObject> initializer,
                                                     @Nullable ILocation location,
                                                     @Nullable PropertyChangeListener listener)
    {
        return createMapObject(KeyUtils.TOOL_FAMILY, ShapeType.ARROW.toString(), initializer, location, listener);
    }

    public IMarkerMapObject createMarkerMapObject(@Nullable Consumer<IMapObject> initializer,
                                                  @Nullable ILocation location,
                                                  @Nullable PropertyChangeListener listener)
    {
        return createMapObject(KeyUtils.RESOURCE_ICON_FAMILY, "Red", initializer, location, listener);
    }

    public IMultiPolylineMapObject createMultiPolylineMapObject(@Nullable Consumer<IMapObject> initializer,
                                                                @Nullable ILocation location,
                                                                @Nullable PropertyChangeListener listener)
    {
        return createMapObject(KeyUtils.COMPOUND_FAMILY, IMultiPolylineMapObject.MULTI_POLYLINE_TYPE, initializer, location, listener);
    }

    public IShapePathMapObject<?> createPathMapObject(@Nullable Consumer<IMapObject> initializer,
                                                      @Nullable ILocation location,
                                                      @Nullable PropertyChangeListener listener)
    {
        return createMapObject(KeyUtils.TOOL_FAMILY, ShapeType.POLYLINE.toString(), initializer, location, listener);
    }

    public IRouteMapObject createRouteMapObject(@Nullable Consumer<IMapObject> initializer,
                                                @Nullable ILocation location,
                                                @Nullable PropertyChangeListener listener)
    {
        return createMapObject(KeyUtils.TOOL_FAMILY, IRouteMapObject.ROUTE_TYPE, initializer, location, listener);
    }

    /**
     * Create a map object, adding a listener if one was supplied.
     *
     * @param <T>         The type of the map object
     * @param family      The map object family string
     * @param type        The map object type string
     * @param initializer An initializer for the map object (may be {@code null})
     * @param location    The location for the map object (may be {@code null})
     * @param listener    The listener to add (or {@code null} to skip adding a listener)
     * @return The map object of the specified type.
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public <T extends IMapObject> T createMapObject(@Nonnull String family, @Nonnull String type,
                                                    @Nullable Consumer<IMapObject> initializer,
                                                    @Nullable ILocation location,
                                                    @Nullable PropertyChangeListener listener)
    {
        final MapObjectSpec.Builder specBuilder = MapObjectSpec.builder(family, type);
        if (initializer != null) specBuilder.initializer(initializer);
        if (location != null) specBuilder.location(location);
        T mapObject = (T) mapObjectManager.getOrCreateMapObject(specBuilder.build());

        if (listener != null) mapObject.addPropertyChangeListener(listener);

        return mapObject;
    }

    /**
     * A listener to wait for the map object to be removed.
     */
    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @PseudoBean
    public static class MapObjectDeletionListener
    {
        private final IMapObject mapObject;
        private MapObjectRemoved event;

        public MapObjectDeletionListener(IMapObject mapObject)
        {
            this.mapObject = mapObject;
        }

        private void onMapObjectRemoved(@Observes MapObjectRemoved event)
        {
            if (event.getMapObject().equals(mapObject))
            {
                this.event = event;
            }
        }

        public MapObjectRemoved getEvent()
        {
            return event;
        }

        public void reset()
        {
            event = null;
        }
    }
}
