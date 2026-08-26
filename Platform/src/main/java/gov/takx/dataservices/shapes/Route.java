package gov.takx.dataservices.shapes;

import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.ExtentHolder;
import gov.nasa.worldwind.globes.Globe;
import gov.tak.api.cot.route.RouteDirection;
import gov.tak.api.cot.route.RouteMethod;
import gov.tak.api.cot.route.RouteOrder;
import gov.tak.api.cot.route.RouteType;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.api.util.AttributeSet;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.shapes.IMutableIrregularShape;
import gov.takx.api.shapes.IRoute;
import gov.takx.commons.utils.UUIDUtils;
import gov.takx.platform.utils.ShapeUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static gov.tak.api.maps.coords.GeoPointMetaData.CHECKSUM;
import static gov.tak.api.util.StandardAttributeNames.CALLSIGN;
import static gov.tak.api.util.StandardAttributeNames.UID;
import static gov.takx.api.shapes.ShapeProperties.LOCATIONS;

/**
 * Implementation of the {@link IRoute} interface.
 *
 * @since 3.3
 */
public class Route extends AShape implements IRoute, IMutableIrregularShape, PropertyChangeListener, ExtentHolder
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private RouteDirection direction = RouteDirection.INFILTRATION;
    private RouteMethod method = RouteMethod.DRIVING;
    private RouteType routeType = RouteType.PRIMARY;
    private RouteOrder routeOrder = RouteOrder.ASCENDING;
    private String prefix = "CP";

    private List<GeoPointMetaData> vertices = new CopyOnWriteArrayList<>();

    /**
     * Ensures that this {@link Route} is registered as a property change listener for each vertex.
     */
    public void initializeListeners()
    {
        // Removing listeners first for when this gets called other than after restoration.
        vertices.stream().map(GeoPointMetaData::getMetaData).forEach(metaData -> {
            metaData.removePropertyChangeListener(this);
            metaData.addPropertyChangeListener(this);
        });
    }

    /**
     * Overridden to move all the points of this route according to the new starting location.
     * <p>
     * {@inheritDoc}
     * @since 5.5
     */
    @Override
    public void setLocation(ILocation location)
    {
        final var currentVertices = getVertices();
        if (GeoPoint.UNKNOWN_POINT.sameAs(location) || currentVertices.isEmpty() || currentVertices.get(0).equals(location)) return;

        super.setLocation(location);
        setVertices(ShapeUtils.movePoints(location, currentVertices, getAltitudeMode()));
    }

    @Nonnull
    @Override
    public List<ILocation> getVertices()
    {
        return vertices.stream().map(geoPointMetaData -> (ILocation) geoPointMetaData.get()).toList();
    }

    @Override
    public void setVertices(@Nonnull List<? extends ILocation> vertices)
    {
        final int size = vertices.size();
        if (size != this.vertices.size())
        {
            // We don't actually want to do anything if we get a set of locations that doesn't match our current route length
            // We shouldn't reach this case normally, but if it did happen there isn't a reliable way to match a new set of
            // vertices with a different length set of control points without losing data about waypoints, etc.
            throw new IllegalArgumentException(String.format("Vertex count mismatch %d != %d", size, this.vertices.size()));
        }

        final List<GeoPointMetaData> newPoints = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
        {
            newPoints.add(copyGeoPointMetaDataWithNewLocation(this.vertices.get(i), new GeoPoint(vertices.get(i))));
        }
        setPoints(newPoints, true);
    }

    @Nonnull
    @Override
    public List<GeoPointMetaData> getWaypoints()
    {
        return vertices.stream().filter(this::isWaypoint).toList();
    }

    @Nonnull
    @Override
    public List<GeoPointMetaData> getControlPoints()
    {
        return new ArrayList<>(vertices);
    }

    @Override
    public void addWaypoint(@Nonnull GeoPoint geoPoint, int index)
    {
        addPoint(geoPoint, index, true);
    }

    @Override
    public void addControlPoint(@Nonnull GeoPoint controlPoint, int index)
    {
        addPoint(controlPoint, index, false);
    }

    /**
     * Adds a {@link GeoPoint} to the route at the provided index, setting whether it is a waypoint or not.
     *
     * @param geoPoint The position of the point to be added
     * @param index index in the route to add this position at
     * @param waypoint true if this point is a waypoint, otherwise false
     */
    private void addPoint(@Nonnull GeoPoint geoPoint, int index, boolean waypoint)
    {
        GeoPointMetaData geoPointMetaData = new GeoPointMetaData(geoPoint);
        geoPointMetaData.getMetaData().setAttribute(IS_A_WAYPOINT, waypoint);

        addPoint(geoPointMetaData, index);
    }

    @Override
    public void addPoint(@Nonnull GeoPointMetaData geoPoint)
    {
        addPoint(geoPoint, getWaypoints().size());
    }

    @Override
    public void addPoint(@Nonnull GeoPointMetaData geoPoint, int index)
    {
        validateControlPointIndex(index, true);

        final AttributeSet metaData = geoPoint.getMetaData();

        if (!metaData.containsAttribute(UID)) metaData.setAttribute(UID, UUIDUtils.generateType1UUID().toString());
        final String uid = metaData.getStringAttribute(UID);

        if (getPointByUid(uid).isPresent())
        {
            logger.debug("Tried to add a point to the route that already is present, uid : {}.", uid);
            return;
        }

        // The first point is always a waypoint
        if (index == 0) metaData.setAttribute(IS_A_WAYPOINT, true);

        final List<GeoPointMetaData> newControlPoints = getControlPoints(); // get a copy
        newControlPoints.add(index, geoPoint);
        setPoints(newControlPoints, false);

        if (isWaypoint(geoPoint)) propertyChangeSupport.firePropertyChange(WAYPOINT_ADDED, null, geoPoint);
    }

    @Override
    public void removePointByUid(@Nonnull String uid, boolean removeControlPoint)
    {
        getPointByUid(uid).ifPresent(point -> {
            if (removeControlPoint)
            {
                final List<GeoPointMetaData> controlPoints = getControlPoints(); // get a copy
                controlPoints.remove(point);
                setPoints(controlPoints);
            } else
            {
                // If not removing a control point, all we have to do is flag this as not a waypoint
                point.getMetaData().setAttribute(IS_A_WAYPOINT, false);
            }

            propertyChangeSupport.firePropertyChange(WAYPOINT_REMOVED, point, null);
        });
    }

    @Override
    public void removePointByIndex(int index)
    {
        final List<GeoPointMetaData> controlPoints = getControlPoints(); // get a copy
        controlPoints.remove(validateControlPointIndex(index, false));
        setPoints(controlPoints, false);
    }

    @Nonnull
    @Override
    public GeoPointMetaData getPointByIndex(int index)
    {
        return vertices.get(validateControlPointIndex(index, false));
    }

    @Override
    public Optional<GeoPointMetaData> getPointByUid(@Nonnull String uid)
    {
        return getWaypoints().stream()
                .filter(geoPointMetaData -> geoPointMetaData.getMetaData().getStringAttribute(UID).equals(uid))
                .findFirst();
    }

    @Override
    public void updatePointLocationByUid(@Nonnull String uid, @Nonnull GeoPoint newLocation)
    {
        int indexToReplace = -1;
        for (int i = 0, size = vertices.size(); i < size; i++)
        {
            if (vertices.get(i).getMetaData().getStringAttribute(UID).equals(uid))
            {
                indexToReplace = i;
                break;
            }
        }

        if (indexToReplace < 0) throw new IllegalArgumentException("Control point with UID : " + uid + " not found in route " + getKey());

        updatePointLocationByIndex(indexToReplace, newLocation);
    }

    @Override
    public void updatePointLocationByIndex(int index, @Nonnull GeoPoint newLocation)
    {
        // if locations are the same, do nothing
        final GeoPointMetaData existingPoint = getPointByIndex(index);
        if (existingPoint.get().equals(newLocation)) return;

        final List<GeoPointMetaData> newControlPoints = getControlPoints(); // Get a copy
        final GeoPointMetaData oldPoint = newControlPoints.remove(index);
        newControlPoints.add(index, copyGeoPointMetaDataWithNewLocation(oldPoint, newLocation));
        setPoints(newControlPoints, false);
    }

    @Override
    public void setPoints(@Nonnull List<GeoPointMetaData> points)
    {
        setPoints(points, true);
    }

    /**
     * Internal call to set a new list of points.  Can also verify that the route has enough points, and ensure the
     * first and last points are set to waypoints, if finalize is set to {@code true}.
     *
     * @param points The new list of all the points to use for this route
     * @param finalize if {@code true}, ensure size of the route and that the first/last points are waypoints
     */
    private void setPoints(@Nonnull List<GeoPointMetaData> points, boolean finalize)
    {
        if (finalize && points.size() < 2)
        {
            throw new IllegalArgumentException("Routes must have at least 2 control points, got " + points.size());
        }

        List<ILocation> oldControlPoints = getVertices();

        vertices.forEach(vertex -> vertex.getMetaData().removePropertyChangeListener(this));
        vertices = new CopyOnWriteArrayList<>(points);
        vertices.forEach(vertex -> vertex.getMetaData().addPropertyChangeListener(this));

        updateWaypointNumbering();
        if (finalize) finalizeRoute();
        propertyChangeSupport.firePropertyChange(LOCATIONS, oldControlPoints, getVertices());

        // If we no longer have any vertices, set our location to unknown, otherwise our first vertex
        super.setLocation(vertices.isEmpty() ? GeoPoint.UNKNOWN_POINT : vertices.get(0).get());
    }

    /**
     * Ensure that the first and last points of a route are set to be waypoints
     */
    private void finalizeRoute()
    {
        GeoPointMetaData firstPoint = vertices.get(0);
        setLocation(firstPoint.get());
        convertToWaypointIfNeeded(firstPoint);
        convertToWaypointIfNeeded(vertices.get(vertices.size() - 1)); // last point
    }

    @Override
    public void promoteToWaypoint(int index)
    {
        convertToWaypointIfNeeded(getPointByIndex(index));
    }

    /**
     * Ensure that a given point is a waypoint, updating metadata and waypoint numbering if not.
     * @since 3.4
     */
    private void convertToWaypointIfNeeded(GeoPointMetaData point)
    {
        if (!isWaypoint(point))
        {
            point.getMetaData().setAttribute(IS_A_WAYPOINT, true);
            updateWaypointNumbering();
            propertyChangeSupport.firePropertyChange(WAYPOINT_ADDED, null, point);
        }
    }

    @Override
    public void demoteToControlPoint(int index)
    {
        if (index == 0 || index == vertices.size() - 1)
        {
            logger.debug("Tried to demote the point at : {} but you cannot demote the first or last waypoint of a route.", index);
            return;
        }

        final GeoPointMetaData point = getPointByIndex(index);
        if (isWaypoint(point))
        {
            // We only need to fire off a property change if we actually make a change
            point.getMetaData().setAttribute(IS_A_WAYPOINT, false);
            updateWaypointNumbering();
            propertyChangeSupport.firePropertyChange(WAYPOINT_REMOVED, point, null);
        }
    }

    @Override
    @Nonnull
    public RouteOrder getRouteOrder()
    {
        return routeOrder;
    }

    @Override
    public void setRouteOrder(@Nonnull RouteOrder routeOrder)
    {
        propertyChangeSupport.updateAndFire(ROUTE_ORDER, this.routeOrder, routeOrder,
                () -> {
                    this.routeOrder = routeOrder;
                    updateWaypointNumbering();
                });
    }

    @Override
    @Nonnull
    public RouteDirection getDirection()
    {
        return direction;
    }

    @Override
    public void setDirection(@Nonnull RouteDirection direction)
    {
        propertyChangeSupport.updateAndFire(DIRECTION, this.direction, direction, () -> this.direction = direction);
    }

    @Override
    @Nonnull
    public RouteType getRouteType()
    {
        return routeType;
    }

    @Override
    public void setRouteType(@Nonnull RouteType routeType)
    {
        propertyChangeSupport.updateAndFire(ROUTE_PRECEDENCE_TYPE, this.routeType, routeType, () -> this.routeType = routeType);
    }

    @Override
    @Nonnull
    public RouteMethod getMethod()
    {
        return method;
    }

    @Override
    public void setMethod(@Nonnull RouteMethod method)
    {
        propertyChangeSupport.updateAndFire(METHOD, this.method, method, () -> {
            this.method = method;
            updateWaypointNumbering();
        });
    }

    @Nonnull
    @Override
    public String getPrefix()
    {
        return prefix;
    }

    @Override
    public void setPrefix(@Nonnull String prefix)
    {
        propertyChangeSupport.updateAndFire(PREFIX, this.prefix, prefix, () -> this.prefix = prefix);
    }

    /**
     * Check if a given index is valid for accessing the control points list.
     *
     * @param index            The index to validate.
     * @param allowEqualToSize True if the index can be equal to the size of the list. If false, index must be less than list size.
     * @return Input index, if valid
     */
    private int validateControlPointIndex(int index, boolean allowEqualToSize)
    {
        final int size = vertices.size();
        if (index < 0 || index >= (allowEqualToSize ? size + 1 : size))
        {
            throw new IndexOutOfBoundsException("index " + index + " not valid for route " + getKey() + "; route length: " + size);
        }
        return index;
    }

    /**
     * Updates the stock naming scheme of the Waypoints, where way points other than the route start/stop are named "<PREFIX> X"
     */
    private void updateWaypointNumbering()
    {
        List<GeoPointMetaData> waypoints = getWaypoints();

        if (waypoints.isEmpty()) return;

        final int lastIndex = waypoints.size() - 1;
        final boolean finalPointIsWaypoint = waypoints.get(lastIndex).equals(vertices.get(vertices.size() - 1));
        final int numberOfWaypoints = finalPointIsWaypoint ? lastIndex : waypoints.size();

        // Set the name for the first and last waypoints
        updateDisplayName(waypoints.get(0), getName() + START_POINT_SUFFIX);

        // Possible for the last waypoint to not be the endpoint when creating a route
        if (finalPointIsWaypoint) updateDisplayName(waypoints.get(lastIndex), method.getEndpointDefaultLabel());

        if (numberOfWaypoints > 1)
        {
            // subList returns an immutable list, so load to an ArrayList in case we need to reverse it
            List<GeoPointMetaData> middleWaypoints = new ArrayList<>(waypoints.subList(1, numberOfWaypoints));

            // If order is descending, the waypoint closest to the end starts numbering at '1'
            if (getRouteOrder() == RouteOrder.DESCENDING) Collections.reverse(middleWaypoints);

            int waypointCount = 1;
            for (GeoPointMetaData geoPoint : middleWaypoints)
            {
                updateDisplayName(geoPoint, prefix + " " + waypointCount++);
            }
        }
    }

    /**
     * Given a {@link GeoPointMetaData} update the display name with either the point's specified callsign,
     * or the provided default if the point has no specified callsign.
     *
     * @param point The point whose display name we are updating
     * @param defaultName The name to use if the given waypoint doesn't have a callsign set
     */
    private void updateDisplayName(@Nonnull GeoPointMetaData point, String defaultName)
    {
        final AttributeSet metaData = point.getMetaData();
        final String nameToAssign = metaData.containsAttribute(CALLSIGN) ? metaData.getStringAttribute(CALLSIGN) : defaultName;
        metaData.setAttribute(DISPLAY_NAME, nameToAssign);
    }

    /**
     * @return {@code true} if the given point is a waypoint
     * @since 3.4
     */
    private boolean isWaypoint(GeoPointMetaData point)
    {
        return point.getMetaData().getBooleanAttribute(IS_A_WAYPOINT, false);
    }

    /**
     * @return a new {@link GeoPointMetaData} with the given new location, copying all pre-existing metadata
     * @since 3.4
     */
    private GeoPointMetaData copyGeoPointMetaDataWithNewLocation(GeoPointMetaData source, GeoPoint newLocation)
    {
        final GeoPointMetaData newGeoPointMetaData = new GeoPointMetaData(newLocation);
        final AttributeSet metaData = newGeoPointMetaData.getMetaData();

        // We need to specifically reset the CHECKSUM, since the old attribute set has a checksum for the old GeoPoint location
        final String newChecksum = metaData.getStringAttribute(CHECKSUM);
        metaData.putAll(source.getMetaData());
        metaData.setAttribute(CHECKSUM, newChecksum);

        return newGeoPointMetaData;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (CALLSIGN.equals(evt.getPropertyName()))
        {
            updateWaypointNumbering();
            propertyChangeSupport.firePropertyChange(WAYPOINT_NAME, evt.getOldValue(), evt.getNewValue());
        }
    }

    @Override
    public Extent getExtent(Globe globe, double verticalExaggeration)
    {
        return ExtentFactory.createExtent(globe, getVertices());
    }
}
