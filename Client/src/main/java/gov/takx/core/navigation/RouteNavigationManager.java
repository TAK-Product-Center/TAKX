package gov.takx.core.navigation;

import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.core.location.LocationTracker;
import gov.takx.api.mapobject.IPointMapObject;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.api.navigation.IRouteNavigationManager;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.shapes.IRoute;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static gov.tak.api.util.StandardAttributeNames.UID;

/**
 * Manager for the ability to navigate the current device to a route, and along the waypoints for that route.
 *
 * @since 3.4 (Moved from {@code java.gov.takx.core.view.gui.routes.navigation} in 5.7)
 */
@ApplicationScoped
public class RouteNavigationManager implements IRouteNavigationManager // TODO DCW TAKX-4314 Deprecate this when exposing NavigationManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private NavigationManager navigationManager;
    @Inject
    private LocationTracker locationTracker;
    @Inject
    private INotificationService notificationService;

    private RouteNavigationConfiguration currentNavigation;

    @Override
    public void navigateToRoute(@Nonnull IRoute route, @Nullable String waypointUid)
    {
        if (route.getWaypoints().isEmpty())
        {
            logger.error("Tried to navigate to an invalid route without waypoints");
            throw new IllegalStateException("Tried to navigate an invalid route without waypoints.");
        }
        if (waypointUid != null &&
                route.getWaypoints().stream()
                        .noneMatch(waypoint -> waypoint.getMetaData().getStringAttribute(UID).equals(waypointUid)))
        {
            logger.error("The provided waypoint UID {} was not present on route {}", waypointUid, route);
            throw new IllegalArgumentException("The provided waypoint UID was not present on the route.");
        }

        IRouteMapObject routeMo = route.getShapeMapObject();
        var self = (IPointMapObject) locationTracker.getMarkerMapObject();
        if (self == null || !self.hasLocation())
        {
            notificationService.show(Notification.builder("Self location required to navigate", Notification.Type.Warning).build());
            return;
        }
        currentNavigation = (RouteNavigationConfiguration) navigationManager.createNavigationConfiguration(self, routeMo);
        if (waypointUid != null) currentNavigation.setCurrentTarget(waypointUid);
        navigationManager.navigate(currentNavigation);
    }

    @Override
    public void endNavigation()
    {
        if (currentNavigation != null)
        {
            navigationManager.removeConfiguration(currentNavigation);
            navigationManager.navigate(null);
            currentNavigation = null;
        }
    }

    @Nullable
    @Override
    public IRoute getRouteBeingNavigated()
    {
        return currentNavigation != null ? currentNavigation.getTargetMapObject().getAssociatedShape() : null;
    }

    @Nullable
    @Override
    public GeoPointMetaData getCurrentNavigationWaypoint()
    {
        return currentNavigation != null ? currentNavigation.getCurrentTarget().getTarget().getGeoPointMetaData() : null;
    }

    @Override
    public void setReversedNavigation(boolean reversedNavigation)
    {
        if (currentNavigation != null) currentNavigation.setReversed(reversedNavigation);
    }
}
