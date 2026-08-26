package gov.tak.platform.cot.importer;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.cot.route.RouteDirection;
import gov.tak.api.cot.route.RouteMethod;
import gov.tak.api.cot.route.RouteOrder;
import gov.tak.api.cot.route.RouteType;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.core.comms.cot.CotImporterManager;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.api.shapes.IRoute;
import gov.takx.dataservices.shapes.Route;
import gov.takx.platform.KeyUtils;
import gov.takx.test.AIntegrationTest;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static gov.takx.api.mapobject.IRouteMapObject.ROUTE_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validate {@link RouteImporter} functionality.
 *
 * @since 3.0
 */
public class RouteImporterTest extends AIntegrationTest
{
    public static final String ROUTE_UID = "a05a0a32-fa3c-4c1b-a746-92e0ec37de4b";
    public static final String ROUTE_KEY = KeyUtils.getUniqueKey(KeyUtils.TOOL_FAMILY, ROUTE_TYPE, ROUTE_UID);
    public static final String ROUTE_NAME = "Route 1";
    public static final int ROUTE_COLOR_RGBA = -16776961;
    private static final String ROUTE_NAME_UPDATED = "Route 1 Updated";

    public static final String WAYPOINT_1_UID = "baa78d0e-85d0-4831-8dc3-6adeeb6987af";
    public static final String WAYPOINT_1_NAME = "Route 1 TGT";
    public static final String WAYPOINT_1_REMARKS = "abc123";

    public static final String WAYPOINT_2_UID = "c09e3a07-c416-4364-96fd-15e8a4dd5912";
    public static final String WAYPOINT_2_NAME = "CP1";

    public static final String WAYPOINT_3_UID = "2dbe9e49-ca89-44f8-a242-d157a983d033";
    public static final String WAYPOINT_3_NAME = "EP";
    private static final String WAYPOINT_3_NAME_UPDATED = "Home";

    public static final String WAYPOINT_4_UID = "90bf3d63-722b-4867-b468-5bf119916042";
    private static final String WAYPOINT_4_NAME = "New WP";

    private static final List<ILocation> CONTROL_POINTS = Arrays.asList(
            new RaptorLocation(34, -87),
            new RaptorLocation(35, -87),
            new RaptorLocation(36, -86),
            new RaptorLocation(35, -85),
            new RaptorLocation(34, -85));

    private static final List<ILocation> CONTROL_POINTS_UPDATED = Arrays.asList(
            new RaptorLocation(34, -88), // New location
            new RaptorLocation(36, -86), // Same UID, but no longer a waypoint
            new RaptorLocation(35, -86), // New CP
            new RaptorLocation(35, -85),
            new RaptorLocation(34, -85));

    private final CotImporterManager importerManager = getBean(CotImporterManager.class);
    private final IMapObjectManager mapObjectManager = getBean(IMapObjectManager.class);

    @AfterEach
    public void cleanupAfterMethod()
    {
        deleteAllMapObjects();
    }

    @SuppressLogging({RouteImporter.class, CotImporterManager.class})
    @Test
    public void test_invalid_route() throws IOException
    {
        final CotEvent event = CotEvent.parse(getResourceAsString("/cot/bad_route.cot"));
        assertThat(event.isValid()).isTrue();

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.FAILURE);
    }

    @Test
    public void test_valid_route() throws IOException
    {
        final CotEvent event = CotEvent.parse(getResourceAsString("/cot/good_route.cot"));
        assertThat(event.isValid()).isTrue();

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(0);

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);

        // One MO for the route,  and three for waypoints
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);

        final var mapObject = mapObjectManager.getMapObject(ROUTE_KEY);
        assertThat(mapObject).isNotNull().isInstanceOf(IRouteMapObject.class);

        IRouteMapObject routeMO = (IRouteMapObject) mapObject;

        Route route = routeMO.getAssociatedShape();
        assertThat(route).isNotNull();
        assertThat(route.getName()).isEqualTo(ROUTE_NAME);
        assertThat(route.getLineColor()).isEqualTo(new Color(ROUTE_COLOR_RGBA, true));
        assertThat(route.getLineWidth()).isEqualTo(1.0);
        assertThat(route.getVertices()).containsExactlyElementsOf(CONTROL_POINTS);

        assertThat(routeMO.getLatestCotEvent()).isEqualTo(event);
        assertThat(routeMO.getCotType()).isEqualTo(CotTypes.ROUTE);
        assertThat(routeMO.getName()).isEqualTo(ROUTE_NAME);
        assertThat(routeMO.getDirection()).isEqualTo(RouteDirection.EXFILTRATION);
        assertThat(routeMO.getMethod()).isEqualTo(RouteMethod.WALKING);
        assertThat(routeMO.getRouteOrder()).isEqualTo(RouteOrder.DESCENDING);
        assertThat(routeMO.getPrefix()).isEqualTo("WP");
        assertThat(routeMO.getRouteType()).isEqualTo(RouteType.SECONDARY);

        assertThat(routeMO.getWaypoints()).hasSize(3);
        verifyWaypoint(WAYPOINT_1_UID, WAYPOINT_1_NAME, CONTROL_POINTS.get(0), routeMO.getWaypoints().get(0), WAYPOINT_1_REMARKS);
        verifyWaypoint(WAYPOINT_2_UID, WAYPOINT_2_NAME, CONTROL_POINTS.get(2), routeMO.getWaypoints().get(1), null);
        verifyWaypoint(WAYPOINT_3_UID, WAYPOINT_3_NAME, CONTROL_POINTS.get(4), routeMO.getWaypoints().get(2), null);
    }

    @Test
    public void test_route_update() throws IOException
    {
        final CotEvent routeEvent = CotEvent.parse(getResourceAsString("/cot/good_route.cot"));
        final CotEvent updateEvent = CotEvent.parse(getResourceAsString("/cot/route_update.cot"));

        assertThat(updateEvent.isValid()).isTrue();
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(0);

        assertThat(importerManager.importEvent(routeEvent)).isEqualTo(ImportResult.SUCCESS);
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);

        // The updated route has:
        //  - Waypoint added
        //  - Waypoint moved
        //  - Waypoint removed
        //  - Waypoint renamed
        //  - Control point added
        //  - Control point removed
        assertThat(importerManager.importEvent(updateEvent)).isEqualTo(ImportResult.SUCCESS);

        // No new map objects are created on update
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);

        final var mapObject = mapObjectManager.getMapObject(ROUTE_KEY);
        assertThat(mapObject).isNotNull().isInstanceOf(IRouteMapObject.class);

        IRouteMapObject routeMO = (IRouteMapObject) mapObject;
        assertThat(routeMO.getCotType()).isEqualTo(CotTypes.ROUTE);
        assertThat(routeMO.getName()).isEqualTo(ROUTE_NAME_UPDATED);

        Route path = routeMO.getAssociatedShape();
        assertThat(path).isNotNull();
        assertThat(path.getName()).isEqualTo(ROUTE_NAME_UPDATED);
        assertThat(path.getLineColor()).isEqualTo(new Color(-16711936, true));
        assertThat(path.getLineWidth()).isEqualTo(2.0);
        assertThat(path.getVertices()).containsExactlyElementsOf(CONTROL_POINTS_UPDATED);

        assertThat(routeMO.getDirection()).isEqualTo(RouteDirection.INFILTRATION);
        assertThat(routeMO.getMethod()).isEqualTo(RouteMethod.DRIVING);
        assertThat(routeMO.getRouteOrder()).isEqualTo(RouteOrder.ASCENDING);
        assertThat(routeMO.getPrefix()).isEqualTo("ABC");
        assertThat(routeMO.getRouteType()).isEqualTo(RouteType.PRIMARY);

        assertThat(routeMO.getWaypoints()).hasSize(3);
        verifyWaypoint(WAYPOINT_1_UID, WAYPOINT_1_NAME, CONTROL_POINTS_UPDATED.get(0), routeMO.getWaypoints().get(0), null);
        verifyWaypoint(WAYPOINT_4_UID, WAYPOINT_4_NAME, CONTROL_POINTS_UPDATED.get(3), routeMO.getWaypoints().get(1), null);
        verifyWaypoint(WAYPOINT_3_UID, WAYPOINT_3_NAME_UPDATED, CONTROL_POINTS_UPDATED.get(4), routeMO.getWaypoints().get(2), null);
    }

    /**
     * Verifies that the waypoint was created correctly and that it is associated with the route correctly.
     *
     * @param waypointUID            The waypoint's UID
     * @param waypointName           The name of the waypoint
     * @param associatedControlPoint The control point associated with the waypoint
     * @param waypoint        The waypoint the route has at the index where this waypoint should be
     * @param remarks                The remarks from the CoT message
     */
    private void verifyWaypoint(@Nonnull String waypointUID, @Nonnull String waypointName,
                                @Nonnull ILocation associatedControlPoint, @Nonnull GeoPointMetaData waypoint,
                                @Nullable String remarks)
    {
        AttributeSet attributes = waypoint.getMetaData();

        assertThat(waypointUID).isEqualTo(attributes.getStringAttribute(StandardAttributeNames.UID));
        assertThat(attributes.getBooleanAttribute(IRoute.IS_A_WAYPOINT)).isTrue();
        assertThat(attributes.getStringAttribute(StandardAttributeNames.CALLSIGN)).isEqualTo(waypointName);
        if (remarks != null) assertThat(attributes.getStringAttribute(StandardAttributeNames.REMARKS)).isEqualTo(remarks);
        assertThat((ILocation) waypoint.get()).isEqualTo(associatedControlPoint);
    }
}
