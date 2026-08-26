package gov.takx.dataservices.persist;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.route.RouteDirection;
import gov.tak.api.cot.route.RouteMethod;
import gov.tak.api.cot.route.RouteOrder;
import gov.tak.api.cot.route.RouteType;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.dataservices.shapes.Route;
import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;

import java.util.List;
import java.util.Optional;

import static gov.takx.api.shapes.ShapeProperties.LOCATIONS;

/**
 * A map object to represent a route. A route MO is made up of a {@link Route} that defines the route shape model. The
 * vertices of the route path are referred to as control points. Waypoints are simply named control points.
 * <p>
 * To make it possible to correlate control points and waypoints that exist at the same vertex of the path, the same UID
 * will be used for the waypoint and the control point. This works out well because from a TAK perspective, a vertex is
 * either a waypoint or a control point.
 * <p>
 *
 * @since 3.0
 */
@PseudoBean
@Entity
public class RouteMapObject extends AShapeMapObject<Route> implements IRouteMapObject
{
    protected RouteMapObject()
    {
    }

    public RouteMapObject(MapObjectSpec mapObjectSpec, IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        super(mapObjectSpec, pluginDefinition);

        setCotType(CotTypes.ROUTE);
    }

    @Override
    protected void prepareShape()
    {
        super.prepareShape();
        shape.initializeListeners();
    }

    @Override
    public void setAssociatedShape(@Nonnull IMutableShape associatedShape)
    {
        super.setAssociatedShape(associatedShape);
        propertyChangeSupport.firePropertyChange(LOCATIONS, null, shape.getVertices());
    }

    @Override
    public void updatePointLocationByUid(@Nonnull String uid, @Nonnull GeoPoint newLocation)
    {
        shape.updatePointLocationByUid(uid, newLocation);
    }

    @Override
    public void updatePointLocationByIndex(int index, @Nonnull GeoPoint newLocation)
    {
        shape.updatePointLocationByIndex(index, newLocation);
    }

    @Nonnull
    @Override
    public List<ILocation> getVertices()
    {
        return shape.getVertices();
    }

    @Override
    public void setVertices(@Nonnull List<? extends ILocation> vertices)
    {
        shape.setVertices(vertices);
    }

    @Override
    @Nonnull
    public List<GeoPointMetaData> getWaypoints()
    {
        return shape.getWaypoints();
    }

    @Override
    public Optional<GeoPointMetaData> getPointByUid(@Nonnull String uid)
    {
        return shape.getPointByUid(uid);
    }

    @Override
    public void addWaypoint(@Nonnull GeoPoint waypoint, int index) throws IndexOutOfBoundsException
    {
        shape.addWaypoint(waypoint, index);
    }

    @Override
    public void addControlPoint(@Nonnull GeoPoint controlPoint, int index)
    {
        shape.addControlPoint(controlPoint, index);
    }

    @Override
    public void addPoint(@Nonnull GeoPointMetaData geoPoint)
    {
        shape.addPoint(geoPoint);
    }

    @Override
    public void addPoint(@Nonnull GeoPointMetaData geoPoint, int index)
    {
        shape.addPoint(geoPoint, index);
    }

    @Override
    public void removePointByUid(@Nonnull String uid, boolean removeControlPoint)
    {
        shape.removePointByUid(uid, removeControlPoint);
    }

    @Override
    public void removePointByIndex(int index)
    {
        shape.removePointByIndex(index);
    }

    @Nonnull
    @Override
    public List<GeoPointMetaData> getControlPoints()
    {
        return shape.getControlPoints();
    }

    @Nonnull
    @Override
    public GeoPointMetaData getPointByIndex(int index)
    {
        return shape.getPointByIndex(index);
    }

    @Override
    public void setPoints(@Nonnull List<GeoPointMetaData> points)
    {
        shape.setPoints(points);
    }

    @Override
    public void promoteToWaypoint(int index)
    {
        shape.promoteToWaypoint(index);
    }

    @Override
    public void demoteToControlPoint(int index)
    {
        shape.demoteToControlPoint(index);
    }

    @Nonnull
    @Override
    public RouteDirection getDirection()
    {
        return shape.getDirection();
    }

    @Override
    public void setDirection(@Nonnull RouteDirection direction)
    {
        shape.setDirection(direction);
    }

    @Nonnull
    @Override
    public RouteMethod getMethod()
    {
        return shape.getMethod();
    }

    @Override
    public void setMethod(@Nonnull RouteMethod method)
    {
        shape.setMethod(method);
    }

    @Nonnull
    @Override
    public String getPrefix()
    {
        return shape.getPrefix();
    }

    @Override
    public void setPrefix(@Nonnull String prefix)
    {
        shape.setPrefix(prefix);
    }

    @Nonnull
    @Override
    public RouteType getRouteType()
    {
        return shape.getRouteType();
    }

    @Override
    public void setRouteType(@Nonnull RouteType routeType)
    {
        shape.setRouteType(routeType);
    }

    @Nonnull
    @Override
    public RouteOrder getRouteOrder()
    {
        return shape.getRouteOrder();
    }

    @Override
    public void setRouteOrder(@Nonnull RouteOrder routeOrder)
    {
        shape.setRouteOrder(routeOrder);
    }
}
