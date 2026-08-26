package gov.takx.dataservices.persist;

import gov.tak.api.cot.route.RouteDirection;
import gov.tak.api.cot.route.RouteMethod;
import gov.tak.api.cot.route.RouteOrder;
import gov.tak.api.cot.route.RouteType;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.commons.utils.UUIDUtils;
import gov.takx.core.mapobject.MapObjectManager;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.mock.mapobject.TestMapObjectPropertyChangeListener;
import gov.takx.test.utils.MapObjectTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static gov.takx.api.shapes.IRoute.IS_A_WAYPOINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIndexOutOfBoundsException;

/**
 * Tests the functionality of {@link RouteMapObject}. Persistence verification is
 * covered in MapObjectPersistTest#test_route_path_and_waypoints().
 *
 * @since 3.0
 */
public class RouteMapObjectTest extends AIntegrationTest
{
    private MapObjectTestUtils moTestUtils;

    @Override
    protected void postProjectOpen()
    {
        moTestUtils = new MapObjectTestUtils(getBean(MapObjectManager.class));
    }

    @AfterEach
    public void cleanupAfterMethod()
    {
        deleteAllMapObjects();
    }

    @Test
    public void test_get_waypoints_when_none_set()
    {
        // The waypoints should be empty and nothing bad should happen
        assertThat(moTestUtils.createRouteMapObject(null, null, null).getWaypoints()).isNotNull().isEmpty();
    }

    @Test
    public void test_get_waypoint_with_bad_index()
    {
        assertThatIndexOutOfBoundsException()
                .isThrownBy(() -> moTestUtils.createRouteMapObject(null, null, null).getPointByIndex(0));
    }

    @Test
    public void test_add_waypoint()
    {
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        IRouteMapObject routeMO = moTestUtils.createRouteMapObject(null, null, listener);

        listener.clearEvents(); // Clear events after setup

        GeoPointMetaData metaData = createWayPoint();

        routeMO.addPoint(metaData, 0);
        assertThat(routeMO.getLocation()).isEqualTo(metaData.get());
        assertThat(routeMO.getWaypoints()).containsExactly(metaData);
        assertThat(routeMO.getPointByIndex(0)).isEqualTo(metaData);
        assertThat(listener.getCountForEventType(IRouteMapObject.WAYPOINT_ADDED)).isEqualTo(1);

        GeoPointMetaData controlPoint = routeMO.getPointByIndex(0);
        assertThat((ILocation) controlPoint.get()).isEqualTo(metaData.get());

        assertThat(listener.getCountForEventType(ShapeProperties.LOCATIONS)).isEqualTo(1);
    }

    @Test
    public void test_add_waypoint_at_last_index()
    {
        IRouteMapObject routeMO = moTestUtils.createRouteMapObject(null, null, null);
        createDefaultPoints(routeMO);
        routeMO.addWaypoint(createGeoPoint(), routeMO.getControlPoints().size());
        assertThat(routeMO.getWaypoints()).isNotNull().isNotEmpty();
    }

    @Test
    public void test_add_waypoint_past_last_index()
    {
        IRouteMapObject routeMO = moTestUtils.createRouteMapObject(null, null, null);
        createDefaultPoints(routeMO);
        assertThatIndexOutOfBoundsException()
                .isThrownBy(() -> routeMO.addWaypoint(createGeoPoint(), routeMO.getControlPoints().size() + 1));
    }

    @SuppressLogging(RouteMapObject.class)
    @Test
    public void test_add_duplicate_waypoint()
    {
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        IRouteMapObject routeMO = moTestUtils.createRouteMapObject(null, null, listener);

        GeoPointMetaData waypointMO = createWayPoint();
        routeMO.addPoint(waypointMO);

        listener.clearEvents(); // Clear events after setup

        routeMO.addPoint(waypointMO);

        assertThat(listener.getCountForEventType(IRouteMapObject.WAYPOINT_ADDED)).isEqualTo(0);
        assertThat(listener.getCountForEventType(ShapeProperties.LOCATIONS)).isEqualTo(0);
    }

    @Test
    public void test_move_waypoint()
    {
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        IRouteMapObject routeMO = moTestUtils.createRouteMapObject(null, null, listener);
        createDefaultPoints(routeMO);

        listener.clearEvents(); // Clear events after setup

        GeoPoint newLocation = new GeoPoint(10, 10);
        routeMO.updatePointLocationByIndex(0, newLocation);
        assertThat(routeMO.getLocation()).isEqualTo(newLocation); // Changing first point changes location

        // The control point location should be updated to match the waypoint MO, but no event since the UID is the same
        GeoPointMetaData firstPoint = routeMO.getPointByIndex(0);
        assertThat((ILocation) newLocation).isEqualTo(firstPoint.get());

        // The path should also be updated
        assertThat(listener.getCountForEventType(ShapeProperties.LOCATIONS)).isEqualTo(1);
    }

    @Test
    public void test_move_removed_waypoint()
    {
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        IRouteMapObject routeMO = moTestUtils.createRouteMapObject(null, null, listener);
        createDefaultPoints(routeMO);

        GeoPointMetaData waypoint = createWayPoint();
        String waypointUid = getGeoPointUid(waypoint);
        routeMO.addPoint(waypoint);

        listener.clearEvents();
        GeoPoint newWaypointLocation = new GeoPoint(6, 6);
        routeMO.updatePointLocationByUid(waypointUid, newWaypointLocation);
        // Check that we can move the waypoint initially
        assertThat(listener.getCountForEventType(ShapeProperties.LOCATIONS)).isEqualTo(1);

        routeMO.removePointByUid(waypointUid, true);
        // This should throw an IllegalArgumentException
        assertThatIllegalArgumentException()
                .isThrownBy(() -> routeMO.updatePointLocationByUid(waypointUid, newWaypointLocation));
    }

    @Test
    public void test_control_point_promotion()
    {
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        IRouteMapObject routeMO = moTestUtils.createRouteMapObject(null, null, listener);
        createDefaultPoints(routeMO);

        int oldWaypointCount = routeMO.getWaypoints().size();
        assertThat(routeMO.getControlPoints().get(3).getMetaData().getBooleanAttribute(IS_A_WAYPOINT, false)).isFalse();

        routeMO.promoteToWaypoint(3);
        int newWaypointCount = routeMO.getWaypoints().size();

        assertThat(oldWaypointCount).isNotEqualTo(newWaypointCount);
        assertThat(routeMO.getControlPoints().get(3).getMetaData().getBooleanAttribute(IS_A_WAYPOINT, false)).isTrue();
    }

    @Test
    public void test_control_point_demotion()
    {
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        IRouteMapObject routeMO = moTestUtils.createRouteMapObject(null, null, listener);
        List<GeoPointMetaData> points = new ArrayList<>();
        points.add(createControlPoint());
        points.add(createControlPoint());
        points.add(createWayPoint());
        points.add(createControlPoint());
        points.add(createControlPoint());
        routeMO.setPoints(points);
        listener.clearEvents();

        int oldWaypointCount = routeMO.getWaypoints().size();
        assertThat(routeMO.getControlPoints().get(2).getMetaData().getBooleanAttribute(IS_A_WAYPOINT)).isTrue();

        routeMO.demoteToControlPoint(2);
        int newWaypointCount = routeMO.getWaypoints().size();

        assertThat(oldWaypointCount).isNotEqualTo(newWaypointCount);
        assertThat(routeMO.getControlPoints().get(2).getMetaData().getBooleanAttribute(IS_A_WAYPOINT)).isFalse();
        assertThat(listener.getCountForEventType(IRouteMapObject.WAYPOINT_REMOVED)).isEqualTo(1);
    }

    @Test
    public void test_remove_waypoint()
    {
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        IRouteMapObject routeMO = moTestUtils.createRouteMapObject(null, null, listener);
        createDefaultPoints(routeMO);
        routeMO.promoteToWaypoint(2);

        int waypointCount = routeMO.getWaypoints().size();
        int controlPointCount = routeMO.getControlPoints().size();
        GeoPointMetaData firstWaypoint = routeMO.getWaypoints().get(1);

        String waypointUid = getGeoPointUid(firstWaypoint);

        listener.clearEvents(); // Clear events after setup

        routeMO.removePointByUid(waypointUid, false);

        // Removed the waypoint, but not the control point
        assertThat(waypointCount).isNotEqualTo(routeMO.getWaypoints().size());
        assertThat(controlPointCount).isEqualTo(routeMO.getControlPoints().size());
        assertThat(listener.getCountForEventType(IRouteMapObject.WAYPOINT_REMOVED)).isEqualTo(1);
    }

    @Test
    public void test_remove_waypoint_and_control_point()
    {
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        IRouteMapObject routeMO = moTestUtils.createRouteMapObject(null, null, listener);
        createDefaultPoints(routeMO);
        routeMO.promoteToWaypoint(2);

        int waypointCount = routeMO.getWaypoints().size();
        int controlPointCount = routeMO.getControlPoints().size();
        GeoPointMetaData firstWaypoint = routeMO.getWaypoints().get(1);
        String waypointUid = getGeoPointUid(firstWaypoint);

        listener.clearEvents(); // Clear events after setup

        routeMO.removePointByUid(waypointUid, true);

        // Removed both the waypoint and control point
        assertThat(waypointCount).isNotEqualTo(routeMO.getWaypoints().size());
        assertThat(controlPointCount).isNotEqualTo(routeMO.getControlPoints().size());
        assertThat(listener.getCountForEventType(IRouteMapObject.WAYPOINT_REMOVED)).isEqualTo(1);
    }

    @Test
    public void test_remove_waypoint_when_none_set()
    {
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        moTestUtils.createRouteMapObject(null, null, listener).removePointByUid(UUIDUtils.generateType1UUID().toString(), false);

        // Nothing should happen
        assertThat(listener.getCountForEventType(IRouteMapObject.WAYPOINT_REMOVED)).isEqualTo(0);
    }

    @Test
    public void test_get_control_point_with_bad_index()
    {
        assertThatIndexOutOfBoundsException()
                .isThrownBy(() -> moTestUtils.createRouteMapObject(null, null, null).getPointByIndex(0));
    }

    @Test
    public void test_set_control_points()
    {
        // The initial route has a path with 3 control points and a waypoint at each end.
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        IRouteMapObject routeMO = moTestUtils.createRouteMapObject(null, null, listener);
        createDefaultPoints(routeMO);
        routeMO.promoteToWaypoint(2);

        listener.clearEvents(); // Clear events after setup

        List<GeoPointMetaData> newControlPoints = new ArrayList<>();
        newControlPoints.add(createWayPoint());
        newControlPoints.add(createControlPoint());
        newControlPoints.add(createWayPoint());
        newControlPoints.add(createControlPoint());
        newControlPoints.add(createControlPoint());
        newControlPoints.add(createWayPoint());

        routeMO.setPoints(newControlPoints);
        assertThat(routeMO.getLocation()).isEqualTo(newControlPoints.get(0).get());

        verifyLocationsEqual(routeMO.getControlPoints(), newControlPoints);
        assertThat(listener.getCountForEventType(ShapeProperties.LOCATIONS)).isEqualTo(1);

        // One waypoint is still valid but should have a new location
        List<GeoPointMetaData> waypoints = routeMO.getWaypoints();
        assertThat(waypoints.size()).isEqualTo(3);
        assertThat((ILocation) waypoints.get(0).get()).isEqualTo(newControlPoints.get(0).get());

        // The other waypoint is no longer valid and should be removed
        assertThat(listener.getCountForEventType(IRouteMapObject.WAYPOINT_REMOVED)).isEqualTo(0);
        assertThat(listener.getCountForEventType(IRouteMapObject.WAYPOINT_ADDED)).isEqualTo(0);
    }

    @Test
    public void test_set_direction()
    {
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        IRouteMapObject routeMO = moTestUtils.createRouteMapObject(null, null, listener);

        RouteDirection direction = RouteDirection.EXFILTRATION;
        routeMO.setDirection(direction);

        assertThat(routeMO.getDirection()).isEqualTo(direction);
        assertThat(listener.getCountForEventType(IRouteMapObject.DIRECTION)).isEqualTo(1);
    }

    @Test
    public void test_set_method()
    {
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        IRouteMapObject routeMO = moTestUtils.createRouteMapObject(null, null, listener);

        RouteMethod method = RouteMethod.FLYING;
        routeMO.setMethod(method);

        assertThat(routeMO.getMethod()).isEqualTo(method);
        assertThat(listener.getCountForEventType(IRouteMapObject.METHOD)).isEqualTo(1);
    }

    @Test
    public void test_set_prefix()
    {
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        IRouteMapObject routeMO = moTestUtils.createRouteMapObject(null, null, listener);

        String prefix = "";
        routeMO.setPrefix(prefix);

        assertThat(routeMO.getPrefix()).isEqualTo(prefix);
        assertThat(listener.getCountForEventType(IRouteMapObject.PREFIX)).isEqualTo(1);
    }

    @Test
    public void test_set_precedence()
    {
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        IRouteMapObject routeMO = moTestUtils.createRouteMapObject(null, null, listener);

        RouteType routeType = RouteType.SECONDARY;
        routeMO.setRouteType(routeType);

        assertThat(routeMO.getRouteType()).isEqualTo(routeType);
        assertThat(listener.getCountForEventType(IRouteMapObject.ROUTE_PRECEDENCE_TYPE)).isEqualTo(1);
    }

    @Test
    public void test_set_waypoint_order()
    {
        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        IRouteMapObject routeMO = moTestUtils.createRouteMapObject(null, null, listener);

        RouteOrder order = RouteOrder.DESCENDING;
        routeMO.setRouteOrder(order);

        assertThat(routeMO.getRouteOrder()).isEqualTo(order);
        assertThat(listener.getCountForEventType(IRouteMapObject.ROUTE_ORDER)).isEqualTo(1);
    }

    private void createDefaultPoints(IRouteMapObject mapObject)
    {
        List<GeoPointMetaData> points = new ArrayList<>();
        points.add(createControlPoint());
        points.add(createControlPoint());
        points.add(createControlPoint());
        points.add(createControlPoint());
        points.add(createControlPoint());
        mapObject.setPoints(points);
    }

    private String getGeoPointUid(GeoPointMetaData metaData)
    {
        return metaData.getMetaData().getStringAttribute(StandardAttributeNames.UID);
    }

    private GeoPoint createGeoPoint()
    {
        Random random = new Random();
        return new GeoPoint(random.nextInt(50), random.nextInt(50));
    }

    private GeoPointMetaData createControlPoint()
    {
        GeoPointMetaData metaData = new GeoPointMetaData(createGeoPoint());
        metaData.getMetaData().setAttribute(StandardAttributeNames.UID, UUIDUtils.generateType1UUID().toString());
        metaData.getMetaData().setAttribute(IS_A_WAYPOINT, false);
        return metaData;
    }

    private GeoPointMetaData createWayPoint()
    {
        GeoPointMetaData metaData = new GeoPointMetaData(createGeoPoint());
        metaData.getMetaData().setAttribute(StandardAttributeNames.UID, UUIDUtils.generateType1UUID().toString());
        metaData.getMetaData().setAttribute(IS_A_WAYPOINT, true);
        return metaData;
    }

    /**
     * Convenience method for comparing two GeoPointMetaData lists with potentially different concrete instantiations.
     *
     * @param testLocations  The location list being tested
     * @param validLocations The valid location list against which to test
     */
    private void verifyLocationsEqual(List<? extends GeoPointMetaData> testLocations, List<? extends GeoPointMetaData> validLocations)
    {
        assertThat(testLocations.size()).isEqualTo(validLocations.size());
        for (int i = 0; i < testLocations.size(); i++)
        {
            assertThat((ILocation) testLocations.get(i).get()).isEqualTo(validLocations.get(i).get());
        }
    }
}
