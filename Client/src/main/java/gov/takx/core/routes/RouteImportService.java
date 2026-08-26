package gov.takx.core.routes;

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.api.util.AttributeSet;
import gov.takx.api.routes.IRouteImportService;
import gov.takx.api.routes.IRoutesManager;
import gov.takx.api.shapes.IRoute;
import gov.takx.commons.xml.gpx.GPX;
import gov.takx.dataservices.shapes.Route;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.List;

import static gov.tak.api.util.StandardAttributeNames.CALLSIGN;
import static gov.tak.api.util.StandardAttributeNames.UID;
import static gov.takx.api.shapes.IRoute.DISPLAY_NAME;
import static gov.takx.api.shapes.IRoute.IS_A_WAYPOINT;

/**
 * A utility class to import a file into a {@link IRoute}.
 * @since 5.8
 */
@ApplicationScoped
public class RouteImportService implements IRouteImportService
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final XmlMapper XML_MAPPER = new XmlMapper(new WstxInputFactory(), new WstxOutputFactory());

    private final IRoutesManager routesManager;

    @Inject
    RouteImportService(IRoutesManager routesManager)
    {
        this.routesManager = routesManager;
    }

    @Override
    public void gpxToRoute(@Nonnull File gpx) throws RuntimeException
    {
        try (InputStream gpxIs = Files.newInputStream(gpx.toPath()))
        {
            gpxToRoute(gpxIs);
        } catch (IllegalArgumentException | UnsupportedOperationException | IOException e)
        {
            logger.error("Exception processing the GPX file", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void gpxToRoute(@Nonnull InputStream gpx)
    {
        GPX routeGpx;
        try
        {
            routeGpx = XML_MAPPER.readValue(gpx, GPX.class);
        } catch (IOException e)
        {
            logger.error("Exception processing the GPX InputStream", e);
            throw new RuntimeException(e);
        }
        GPX.GpxRoute gpxRoute = routeGpx.getRoute();
        List<GPX.GpxPoint> waypoints = routeGpx.getWaypoints();
        if (gpxRoute == null || gpxRoute.getPoints().isEmpty() || waypoints.isEmpty()) return;
        int waypointIndex = 0;
        GPX.GpxPoint nextWaypoint = waypoints.get(waypointIndex);
        IRoute routeModel = new Route();
        routeModel.setName(gpxRoute.getName());

        for (GPX.GpxPoint point : gpxRoute.getPoints())
        {
            GeoPoint geoPoint = new GeoPoint(point.getLat(), point.getLon(), point.getElevation());
            GeoPointMetaData gpmd = new GeoPointMetaData(geoPoint);
            if (point.samePosition(nextWaypoint))
            {
                AttributeSet gpmdAttributes = gpmd.getMetaData();
                gpmdAttributes.setAttribute(IS_A_WAYPOINT, true);
                gpmdAttributes.setAttribute(UID, nextWaypoint.getDesc());
                gpmdAttributes.setAttribute(CALLSIGN, nextWaypoint.getName());
                gpmdAttributes.setAttribute(DISPLAY_NAME, nextWaypoint.getName());
                nextWaypoint = (waypointIndex + 1) < waypoints.size() ? waypoints.get(++waypointIndex) : nextWaypoint;
            }
            routeModel.addPoint(gpmd);
        }
        routeModel.setLocation(routeModel.getControlPoints().get(0).get());
        routesManager.addRoute(routeModel);
    }
}
