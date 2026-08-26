package gov.takx.core.globe.tak.editor;

import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.shapes.IRoute;
import gov.takx.api.shapes.IShape.AltitudeMode;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.dataservices.shapes.Route;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static gov.takx.api.shapes.IRoute.DISPLAY_NAME;

/**
 * An implementation of {@link IShapeEditorSource} for {@link IGlobeShapeMapObject} that represent a route via an underlying {@link Route} object.
 *
 * @since 5.4
 */
public class GlobeRouteMapObjectEditorSource extends AShapeEditorSource<IGlobeShapeMapObject> implements IControlPointTextProvider
{
    /**
     * A collection of common properties that need to be monitored for route objects.
     */
    private static final List<String> ROUTE_PROPERTIES = List.of(
            IMapObject.NAME, ShapeProperties.EDITABLE, ShapeProperties.FILL_COLOR, ShapeProperties.LINE_COLOR,
            ShapeProperties.LINE_WIDTH, ShapeProperties.LOCATIONS, ShapeProperties.VISIBILITY, StandardAttributeNames.CALLSIGN,
            IRoute.METHOD, IRoute.DIRECTION, IRoute.ROUTE_PRECEDENCE_TYPE, IRoute.ROUTE_ORDER,
            IRoute.WAYPOINT_ADDED, IRoute.WAYPOINT_REMOVED, IRoute.WAYPOINT_NAME, IRoute.PREFIX);

    public GlobeRouteMapObjectEditorSource(IGlobeShapeMapObject source)
    {
        super(source);
    }

    @Override
    public void setArmed(boolean armed)
    {
        super.setArmed(armed);
        // Update control point editability
        onSourceUpdated(getSource());
    }

    @Override
    public List<ControlPointDefinition> createControlPointDefinitions()
    {
        Route route = getSource().requireShapeModel();
        List<ControlPointDefinition> controlPoints = new ArrayList<>();
        int index = 0;
        for (GeoPointMetaData controlPoint : route.getControlPoints())
        {
            boolean isWaypoint = controlPoint.getMetaData().getBooleanAttribute(IRoute.IS_A_WAYPOINT, false);
            String controlPointType = (isWaypoint ? RouteControlPointController.CONTROL_POINT_WAYPOINT_INDEX : RouteControlPointController.CONTROL_POINT_ROUTE_POINT_INDEX) + index++;
            boolean editable = route.isEditable() && getShapeEditor().isArmed();
            controlPoints.add(new ControlPointDefinition(controlPoint.get(), controlPointType, editable, AltitudeMode.ClampToGround));
        }
        return controlPoints;
    }

    @Override
    public void onControlPointDragged(@Nonnull String controlPointType, @Nonnull ILocation location)
    {
        Route route = getSource().requireShapeModel();
        int index = parseIndex(controlPointType);
        route.updatePointLocationByIndex(index, new GeoPoint(location));
        onSourceUpdated(getSource());
    }

    @Override
    public void onControlPointAdded(@Nonnull ILocation location, @Nonnull Map<String, Object> metadata)
    {
        if (!isEditable()) return;
        Route route = getSource().requireShapeModel();
        if (metadata.get(RouteControlPointController.PROMOTE_TO_WAYPOINT) instanceof IControlPoint selectedControlPoint)
        {
            int index = parseIndex(selectedControlPoint.getControlPointType());
            route.promoteToWaypoint(index);
            onSourceUpdated(getSource());
            invalidateAllControlPoints();
        } else if (metadata.containsKey(IControlPoint.MODIFIER_CTRL))
        {
            Integer index = insertIndexForPositionInPlace(route.getVertices(), location);
            if (index != null)
            {
                route.addControlPoint(new GeoPoint(location), index);
                onSourceUpdated(getSource());
            }
        } else
        {
            route.addControlPoint(new GeoPoint(location), route.getVertices().size());
            onSourceUpdated(getSource());
        }
        super.onControlPointAdded(location, metadata);
    }

    @Override
    public void onControlPointRemoved(@Nonnull IControlPoint controlPoint)
    {
        if (!isEditable()) return;
        Route route = getSource().requireShapeModel();
        int index = parseIndex(controlPoint.getControlPointType());
        route.removePointByIndex(index);
        onSourceUpdated(getSource());
        invalidateAllControlPoints();
        super.onControlPointRemoved(controlPoint);
    }

    @Override
    public void applyControlPointsToSource(List<IControlPoint> controlPoints)
    {
        // Make sure we "finalize" the route
        Route route = getSource().requireShapeModel();
        route.setPoints(route.getControlPoints());
    }

    @Override
    public String getControlPointText(@Nonnull IControlPoint controlPoint)
    {
        Route route = getSource().requireShapeModel();
        int index = parseIndex(controlPoint.getControlPointType());
        GeoPointMetaData routePoint = route.getPointByIndex(index);
        AttributeSet waypointAttributes = routePoint.getMetaData();
        return waypointAttributes.getStringAttribute(DISPLAY_NAME, null);
    }

    @Override
    protected void addSourcePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        getSource().addPropertyChangeListener(listener, ROUTE_PROPERTIES);
    }

    @Override
    protected void removeSourcePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        getSource().removePropertyChangeListener(listener, ROUTE_PROPERTIES);
    }

    /**
     * Parse the index from a route control point type.
     * @param controlPointType the control point type
     * @return the index of the control point type
     */
    private int parseIndex(String controlPointType)
    {
        int startIndex = 0;
        if (controlPointType.startsWith(RouteControlPointController.CONTROL_POINT_ROUTE_POINT_INDEX))
        {
            startIndex = RouteControlPointController.CONTROL_POINT_ROUTE_POINT_INDEX.length();
        } else if (controlPointType.startsWith(RouteControlPointController.CONTROL_POINT_WAYPOINT_INDEX))
        {
            startIndex = RouteControlPointController.CONTROL_POINT_WAYPOINT_INDEX.length();
        }
        return Integer.parseInt(controlPointType, startIndex, controlPointType.length(), 10);
    }

    // Slightly hacky way to force all control points to refresh and show updated text
    private void invalidateAllControlPoints()
    {
        for (IControlPoint controlPoint : getShapeEditor().getControlPointController().getControlPoints())
        {
            ILocation location = controlPoint.getLocation();
            controlPoint.setLocation(GeoPoint.UNKNOWN_POINT);
            controlPoint.setLocation(location);
        }
    }

    private boolean isEditable()
    {
        Route route = getSource().requireShapeModel();
        return route.isEditable() && getShapeEditor().isArmed();
    }
}
