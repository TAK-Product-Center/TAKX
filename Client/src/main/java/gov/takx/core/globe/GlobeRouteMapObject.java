package gov.takx.core.globe;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.MeasurableLength;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.measure.LengthMeasurer;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.api.mapobject.MapToolType;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.api.shapes.IRoute;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.dataservices.shapes.Route;
import gov.takx.platform.utils.WWUtils;

import java.util.Arrays;
import java.util.List;

import static gov.tak.api.util.StandardAttributeNames.CALLSIGN;

/**
 * A  {@link gov.takx.api.globe.IGlobeMapObject} that is used to draw a {@link Route} and its waypoints on the globe.
 *
 * @since 3.3
 */
@PseudoBean
public class GlobeRouteMapObject extends GlobeShapeMapObject implements MeasurableLength
{
    private static final List<String> PROPERTIES_TO_WATCH = Arrays.asList(
            IMapObject.NAME,
            ShapeProperties.EDITABLE,
            ShapeProperties.LINE_COLOR,
            ShapeProperties.LINE_WIDTH,
            ShapeProperties.LOCATIONS,
            ShapeProperties.VISIBILITY, // unused locally
            CALLSIGN,
            IRoute.METHOD,
            IRoute.DIRECTION,
            IRoute.ROUTE_PRECEDENCE_TYPE,
            IRoute.ROUTE_ORDER,
            IRoute.WAYPOINT_ADDED,
            IRoute.WAYPOINT_REMOVED,
            IRoute.WAYPOINT_NAME,
            IRoute.PREFIX);

    protected GlobeRouteMapObject(IRouteMapObject mapObject)
    {
        super(mapObject);
    }

    protected GlobeRouteMapObject()
    {
        super(MapToolType.DRAWING_ROUTE);
    }

    @Override
    public void setLocation(ILocation location)
    {
        IMutableShape shapeModel = getShapeModel();
        if (shapeModel != null) shapeModel.setLocation(location);
    }

    @Override
    public double getLength(Globe globe)
    {
        IRoute route = requireShapeModel();
        final LengthMeasurer measurer = new LengthMeasurer();
        measurer.setFollowTerrain(true);
        measurer.setPathType(WorldWind.GREAT_CIRCLE);
        measurer.setPositions(route.getVertices().stream().map(WWUtils::locationToPosition).toList());

        return measurer.getLength(globe);
    }

    @Override
    protected List<String> getShapePropertiesToWatch()
    {
        return PROPERTIES_TO_WATCH;
    }
}
