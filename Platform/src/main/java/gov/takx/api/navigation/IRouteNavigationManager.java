package gov.takx.api.navigation;

import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.shapes.IRoute;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Interface for access to the route navigation manager, which displays directions to an {@link IRoute} and to each of
 * the waypoints on that route.
 *
 * @since 3.4
 */
@NoPluginImplementations
public interface IRouteNavigationManager
{
    /**
     * Navigate to a particular route, starting at the waypoint identified by the provided UID
     * @param route The route to navigate to
     * @param waypointUid The UID for the waypoint within the route to navigate to.  If {@code null} will default to the first waypoint in the route.
     * @throws IllegalArgumentException if there is no waypoint with the provided UID on the route
     * @throws IllegalStateException if the system does not have a valid location provider configured
     */
    void navigateToRoute(@Nonnull IRoute route, @Nullable String waypointUid);

    /**
     * Ends the current navigation, if any is underway.
     */
    void endNavigation();

    /**
     * @return The route we are currently navigating, or {@code null} if we are not navigating.
     */
    @Nullable
    IRoute getRouteBeingNavigated();

    /**
     * @return The waypoint we are currently navigating to, or {@code null} if we are not navigating.
     */
    @Nullable
    GeoPointMetaData getCurrentNavigationWaypoint();

    /**
     * Sets whether we are navigating the waypoints of this route in order, or reversed.
     *
     * @param reversedNavigation {@code true} if the route waypoints should be displayed and navigated in reverse order.
     * @throws IllegalStateException if we ar enot currently navigating a route
     */
    void setReversedNavigation(boolean reversedNavigation);
}
