package gov.takx.core.navigation;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IPointMapObject;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.api.mapobject.IShapeArrowMapObject;
import gov.takx.api.shapes.IRoute;
import gov.takx.api.shapes.ShapeProperties;
import jakarta.annotation.Nonnull;

import java.util.List;

import static gov.takx.api.shapes.IRoute.WAYPOINT_ADDED;

/**
 * NavigationConfiguration object for the navigation of a route by a point map object. Creates navigation targets based on route waypoints
 * and keeps the targets in sync if the route is updated.
 *
 * @since 5.7
 */
public class RouteNavigationConfiguration extends NavigationConfiguration
{
    /**
     * Construct a new configuration that tracks navigation from the first map object along the waypoints of the given route.
     *
     * @param sourceMapObject the source map object
     * @param route the route to navigate
     */
    public RouteNavigationConfiguration(@Nonnull IPointMapObject sourceMapObject, @Nonnull IRouteMapObject route)
    {
        super(sourceMapObject, route);

        listenerManager.addPropertyChangeListener(route, List.of(ShapeProperties.LOCATIONS, WAYPOINT_ADDED), ignored -> updateRouteLocations());

        updateRouteLocations();
        var currentTarget = getCurrentTarget();
        currentTarget.setVisible(true);
        currentTarget.update();
        update();
    }

    @Nonnull
    @Override
    public IRouteMapObject getTargetMapObject()
    {
        return (IRouteMapObject) super.getTargetMapObject();
    }

    /**
     * @param waypointUid the waypoint id to use as the current navigation target
     * @throws IllegalArgumentException if the waypoint uid is not found in the target list
     */
    public void setCurrentTarget(@Nonnull String waypointUid) throws IllegalArgumentException
    {
        var target = targets.stream()
                .filter(t -> t.getTarget().getGeoPointMetaData().getMetaData().getStringAttribute(StandardAttributeNames.UID).equals(waypointUid))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Waypoint " + waypointUid + " not found in route"));
        setCurrentTarget(target);
    }

    @Override
    protected boolean arrivedAt(@Nonnull IMapObject target)
    {
        var arrivalRadius = navigationManager.getArrivalRadius(getTargetMapObject().getMethod());
        return sourceMapObject.getLocation().distanceTo(target.getLocation()) <= arrivalRadius;
    }

    private void updateRouteLocations()
    {
        var waypoints = getTargetMapObject().getWaypoints();
        var waypointCount = waypoints.size();
        for (var i = 0; i < waypointCount; i++)
        {
            var metaData = waypoints.get(i);
            createOrUpdateRouteWaypoint(i, metaData);
        }
        while (targets.size() > waypointCount)
        {
            var target = targets.remove(waypointCount);
            target.releaseResources();
            listenerManager.unregister(target);
        }
        if (getCurrentTargetIndex() >= waypointCount) setCurrentTarget(waypointCount - 1);
        propertyChangeSupport.firePropertyChange(ShapeProperties.LOCATIONS, null, null);
    }

    private void createOrUpdateRouteWaypoint(int i, GeoPointMetaData point)
    {
        NavigationTarget navigationTarget;
        if (targets.size() <= i)
        {
            var targetPoint = navigationManager.createTargetPoint(point);
            var targetArrow = navigationManager.createTargetArrow(getSourceMapObject(), targetPoint);
            navigationTarget = new RouteNavigationTarget(this, targetPoint, targetArrow, point);
            navigationTarget.addMapObject(targetPoint);
            listenerManager.addPropertyChangeListener(navigationTarget, List.of(ETA), propertyChangeSupport::firePropertyChangeCopy);
            targets.add(navigationTarget);

        } else
        {
            navigationTarget = targets.get(i);
            navigationTarget.getTarget().setGeoPointMetaData(point);
        }
        navigationTarget.getTarget().setName(point.getMetaData().getStringAttribute(IRoute.DISPLAY_NAME));
    }

    @VisibleForTesting
    static class RouteNavigationTarget extends NavigationTarget
    {
        @VisibleForTesting
        final GeoPointMetaData waypoint;

        public RouteNavigationTarget(@Nonnull NavigationConfiguration navigation, @Nonnull IPointMapObject target, @Nonnull IShapeArrowMapObject targetArrow, @Nonnull GeoPointMetaData waypoint)
        {
            super(navigation, target, targetArrow);
            this.waypoint = waypoint;
        }
    }
}
