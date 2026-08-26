package gov.takx.api.shapes;

import gov.tak.api.cot.route.RouteDirection;
import gov.tak.api.cot.route.RouteMethod;
import gov.tak.api.cot.route.RouteOrder;
import gov.tak.api.cot.route.RouteType;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Optional;

/**
 * Interface for a route.
 *
 * @since 3.3
 */
@NoPluginImplementations
public interface IRoute extends IMutableIrregularShape
{
    String DIRECTION = "DIRECTION";
    String DISPLAY_NAME = "DISPLAY_NAME";
    String IS_A_WAYPOINT = "IS_A_WAYPOINT"; // Indicates if a control point is a waypoint
    String METHOD = "METHOD";
    String PREFIX = "PREFIX";

    /**
     * @since 3.4
     */
    String ROUTE_ORDER = "ROUTE_ORDER";

    /**
     * @since 3.4
     */
    String ROUTE_PRECEDENCE_TYPE = "ROUTE_PRECEDENCE_TYPE";
    String WAYPOINT_ADDED = "WAYPOINT_ADDED";
    String WAYPOINT_NAME = "WAYPOINT_NAME";
    String WAYPOINT_REMOVED = "WAYPOINT_REMOVED";

    // Constants for standard transportation type values
    String TRANSPORTATION_TYPE_VEHICLE = "Vehicle";
    String TRANSPORTATION_TYPE_ON_FOOT = "On Foot";

    String START_POINT_SUFFIX = " SP";

    /**
     * @return The list of waypoints in the route, or an empty list if none present.  Does not return any of the control
     * points that are not waypoints
     */
    @Nonnull
    List<GeoPointMetaData> getWaypoints();

    /**
     * Fetch the list of all the control points, with attached metadata (waypoints will also be returned).
     *
     * @return list of route path control points.
     */
    @Nonnull
    List<GeoPointMetaData> getControlPoints();

    /**
     * Adds a waypoint to the route, associating it with the control point at the specified index.
     *
     * @param waypoint The waypoint to add
     * @param index    The index in the route to add the waypoint at
     * @throws IndexOutOfBoundsException if the index is not in range for the control points
     */
    void addWaypoint(@Nonnull GeoPoint waypoint, int index) throws IndexOutOfBoundsException;

    /**
     * Adds a new ControlPoint to the route at the provided index location
     *
     * @param controlPoint The {@link GeoPoint} representing the position of the new control point
     * @param index The index to insert the point at
     * @throws IndexOutOfBoundsException if the index is not in range for the control points
     */
    void addControlPoint(@Nonnull GeoPoint controlPoint, int index);

    /**
     * Adds a new point to the end of the route.  The {@link GeoPointMetaData} should contain in its {@link gov.tak.api.util.AttributeSet}
     * a {@link gov.tak.api.util.StandardAttributeNames#UID} string.  If it doesn't, a random uid will be generated and assigned to it
     *
     * @param geoPoint The location and associated metadata to add
     */
    void addPoint(@Nonnull GeoPointMetaData geoPoint);

    /**
     * Adds a new point to the route at the provided index.  The {@link GeoPointMetaData} should contain in its {@link gov.tak.api.util.AttributeSet}
     * a {@link gov.tak.api.util.StandardAttributeNames#UID} string.  If a point with the UID already exists, no changes will be made.
     *
     * @param geoPoint The location and associated metadata to add
     * @param index The position in the route to add this point at
     * @throws IndexOutOfBoundsException if the index is not in range for the control points
     */
    void addPoint(@Nonnull GeoPointMetaData geoPoint, int index);

    /**
     * Removes the waypoint and optionally its control point from the route.
     *
     * @param uid The uid of the waypoint or control point to remove
     * @param removeControlPoint True if the control point tied to the waypoint should be removed
     */
    void removePointByUid(@Nonnull String uid, boolean removeControlPoint);

    /**
     * Removes whichever point is at the given index, regardless of waypoint/control point status
     *
     * @param index The uid of the waypoint or control point to remove
     */
    void removePointByIndex(int index);

    /**
     * Returns the {@link GeoPointMetaData} for a point with the given uid, if one exists in this route.
     *
     * @param uid The uid for the {@link GeoPointMetaData} to retrieve
     * @return An {@link Optional} with the point for the provided uid, otherwise an empty {@link Optional}
     */
    Optional<GeoPointMetaData> getPointByUid(@Nonnull String uid);

    /**
     * Gets the point at the specified index.
     *
     * @param index The index of the desired control point
     * @return The control point.
     * @throws IndexOutOfBoundsException if the index is not in range for the control points
     */
    @Nonnull
    GeoPointMetaData getPointByIndex(int index);

    /**
     * Sets the points for the route path. This will finalize the route (auto promote first and last points if they are
     * not waypoints).
     *
     * @param points The route control points (vertices), with associated metadata for waypoint status, etc.
     */
    void setPoints(@Nonnull List<GeoPointMetaData> points);

    /**
     * Promotes a given control point to a way point if it isn't one already
     *
     * @param index The index of the control point to  promote
     * @throws IndexOutOfBoundsException if the index is not in range for the control points
     */
    void promoteToWaypoint(int index);

    /**
     * Demotes a given point to a control point if it isn't one already
     *
     * @param index The index of the control point to  demote
     * @throws IndexOutOfBoundsException if the index is not in range for the control points
     */
    void demoteToControlPoint(int index);

    /**
     * Updates the location of a particular control point (and associated waypoint if any)
     *
     * @param uid The UID of the control point to update
     * @param newLocation  The updated location of the given control point
     * @throws IllegalArgumentException if the  uid does not correspond to a control point or waypoint in this route
     */
    void updatePointLocationByUid(@Nonnull String uid, @Nonnull GeoPoint newLocation);

    /**
     * Updates the location of the point located at the provided index.
     *
     * @param index The index within the route of the point to update.
     * @param newLocation The updated location of the given control point
     * @throws IndexOutOfBoundsException if the index is not in range for the control points
     */
    void updatePointLocationByIndex(int index, @Nonnull GeoPoint newLocation);

    /**
     * @return The direction for the route (e.g. infil or exfil).
     */
    @Nonnull
    RouteDirection getDirection();

    /**
     * Sets the route direction (e.g. infil or exfil).
     *
     * @param direction The route direction
     */
    void setDirection(@Nonnull RouteDirection direction);

    /**
     * @return The transportation method for traversing the route.
     */
    @Nonnull
    RouteMethod getMethod();

    /**
     * Sets the transportation method for traversing the route.
     *
     * @param method The transportation method
     */
    void setMethod(@Nonnull RouteMethod method);

    /**
     * @return The type of the route, e.g. primary or secondary.
     * @since 3.4
     */
    @Nonnull
    RouteType getRouteType();

    /**
     * Sets the type of the route, e.g. primary or secondary.
     *
     * @since 3.4
     */
    void setRouteType(@Nonnull RouteType routeType);

    /**
     * @return The order in which waypoints are named.
     * @since 3.4
     */
    @Nonnull
    RouteOrder getRouteOrder();

    /**
     * Sets the order in which waypoints are named.
     *
     * @param routeOrder The waypoint naming order
     * @since 3.4
     */
    void setRouteOrder(@Nonnull RouteOrder routeOrder);

    /**
     * @return The prefix for naming new waypoints.
     */
    @Nonnull
    String getPrefix();

    /**
     * Sets the prefix for naming new waypoints, e.g. a value of "CP" would produce waypoints like "CP2".
     *
     * @param prefix The waypoint prefix
     */
    void setPrefix(@Nonnull String prefix);
}
