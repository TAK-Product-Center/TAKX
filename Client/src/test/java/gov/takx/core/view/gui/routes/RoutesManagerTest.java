package gov.takx.core.view.gui.routes;

import gov.tak.api.cot.route.RouteDirection;
import gov.tak.api.cot.route.RouteMethod;
import gov.tak.api.cot.route.RouteOrder;
import gov.tak.api.cot.route.RouteType;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.event.InitializeDrawToolEvent;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.api.routes.IRouteEditListener;
import gov.takx.api.routes.IRouteListener;
import gov.takx.api.routes.IRoutesManager;
import gov.takx.api.shapes.IRoute;
import gov.takx.dataservices.shapes.Route;
import gov.takx.platform.KeyUtils;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.utils.SwingTestUtils;
import jakarta.annotation.Nonnull;
import org.greenrobot.eventbus.EventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Functional tests for the Routes API, including {@link IRoutesManager}, {@link IRouteListener}, and {@link IRouteEditListener}.
 * @since 3.4
 */
public class RoutesManagerTest extends AIntegrationTest
{
    private static final GeoPoint GP00 = GeoPoint.ZERO_POINT;
    private static final GeoPoint GP11 = new GeoPoint(1, 1);
    private static final GeoPoint GP22 = new GeoPoint(2, 2);

    private IRoutesManager routesManager;
    private IMapObjectManager mapObjectManager;

    private RouteListener routeListener;
    private RouteEditListener routeEditListener;

    @BeforeEach
    public void initTestState()
    {
        routesManager = getBean(IRoutesManager.class);
        mapObjectManager = getBean(IMapObjectManager.class);
    }

    @AfterEach
    public void cleanupAfterMethod()
    {
        if (routeListener != null) routesManager.removeRouteListener(routeListener);
        routeListener = null;

        if (routeEditListener != null) routesManager.removeRouteEditListener(routeEditListener);
        routeEditListener = null;

        deleteAllMapObjects();
    }

    @Test
    public void findRouteByKey_returnsNull_whenNoRoutes()
    {
        assertThat(routesManager.findRouteByKey("anything")).isNull();
    }

    @Test
    public void getRoutes_returnsEmptyList_whenNoRoutes()
    {
        assertThat(routesManager.getRoutes()).isNotNull().isEmpty();
    }

    @Test
    public void getCurrentlyEditingRoute_returnsNull_whenNotEditing()
    {
        assertThat(routesManager.getCurrentlyEditingRoute()).isNull();
    }

    @Test
    public void addRoute_succeeds_whenValidRouteGiven()
    {
        final var route = makeBasicRouteModel();
        final var routeMapObject = routesManager.addRoute(route);
        assertThat(routeMapObject).isNotNull();
        assertThat(routeMapObject.getAssociatedShape()).isSameAs(route);
        assertThat(getBean(IMapObjectPersist.class).findAllMapObjects()).hasSize(1);

        final var controlPoints = routeMapObject.getControlPoints();
        assertThat(controlPoints).hasSize(3);

        final var geoPoints = controlPoints.stream().map(GeoPointMetaData::get).toList();
        assertThat(geoPoints).containsExactly(GP00, GP11, GP22);

        for (int i = 0; i < 3; i++)
        {
            assertThat(controlPoints.get(i).getMetaData().getBooleanAttribute(IRoute.IS_A_WAYPOINT, false))
                    .isEqualTo(i != 1); // 0 and 2 are waypoints
        }
    }

    @Test
    public void addRoute_throws_whenRouteHasKey()
    {
        final Route route = new Route();
        route.setKey("theKey");

        assertThatThrownBy(() -> routesManager.addRoute(route))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Route must not have a pre-assigned unique key")
                .hasMessageContaining("theKey");

    }

    @Test
    public void addRoute_throws_whenRouteHasMapObject()
    {
        final Route route = new Route();
        route.addControlPoint(GeoPoint.ZERO_POINT, 0);
        route.addControlPoint(GeoPoint.ZERO_POINT, 1);

        final var spec = MapObjectSpec.builder(KeyUtils.TOOL_FAMILY, IRouteMapObject.ROUTE_TYPE, "foo").build();
        final var mapObject = (IShapeMapObject<?>) mapObjectManager.getOrCreateMapObject(spec);
        route.setShapeMapObject(mapObject);

        assertThatThrownBy(() -> routesManager.addRoute(route))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Route must not already be associated with a Map Object")
                .hasMessageContaining("foo");
    }

    @Test
    public void addRoute_throws_whenRouteHasNotEnoughControlPoints()
    {
        final Route route = new Route();
        route.addControlPoint(GeoPoint.ZERO_POINT, 0);

        assertThatThrownBy(() -> routesManager.addRoute(route))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Routes must have at least 2 control points");
    }

    @Test
    public void deleteRoute_succeeds_whenRouteExists()
    {
        final IRoute route = makeBasicRouteModel();
        final IRouteMapObject routeMapObject = routesManager.addRoute(route);
        assertThat(routeMapObject).isNotNull();

        final String key = route.getKey();
        assertThat(routesManager.findRouteByKey(key)).isNotNull();

        routesManager.deleteRoute(key);
        assertThat(routesManager.findRouteByKey(key)).isNull();
    }

    @Test
    public void deleteRoute_throws_whenKeyNotFound()
    {
        assertThatThrownBy(() -> routesManager.deleteRoute("nope"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No route with key");
    }

    @Test
    public void deleteRoute_throws_whenKeyNotRoute()
    {
        final String key = "foo/bar/1";
        final MapObjectSpec spec = MapObjectSpec.builder("foo", "bar", "1").build();
        mapObjectManager.getOrCreateMapObject(spec);

        assertThatThrownBy(() -> routesManager.deleteRoute(key))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Map Object with that key is not a route")
                .hasMessageContaining(key);
    }

    @Test
    public void getRoutes_returnsProperList()
    {
        final IRouteMapObject routeMapObject1 = routesManager.addRoute(makeBasicRouteModel());
        final IRouteMapObject routeMapObject2 = routesManager.addRoute(makeBasicRouteModel());
        final IRouteMapObject routeMapObject3 = routesManager.addRoute(makeBasicRouteModel());

        assertThat(routesManager.getRoutes())
                .hasSize(3)
                .containsExactlyInAnyOrder(routeMapObject1, routeMapObject2, routeMapObject3);
    }

    @Test
    public void routeListeners_areCalled_onProperOperations()
    {
        routeListener = new RouteListener();
        routesManager.addRouteListener(routeListener);

        final IRoute route = makeBasicRouteModel();
        routesManager.addRoute(route);
        routeListener.assertCounts(1, 0);

        routesManager.deleteRoute(route);
        routeListener.assertCounts(1, 1);
    }

    @Test
    public void routeListeners_areNotCalled_afterRemoval()
    {
        routeListener = new RouteListener();
        routesManager.addRouteListener(routeListener);

        final IRoute route = makeBasicRouteModel();
        routesManager.addRoute(route);
        routeListener.assertCounts(1, 0);

        routesManager.removeRouteListener(routeListener);

        routesManager.deleteRoute(route);
        routeListener.assertCounts(1, 0);

        routesManager.addRoute(makeBasicRouteModel());
        routeListener.assertCounts(1, 0);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void routeListener_throws_whenNull()
    {
        assertThatThrownBy(() -> routesManager.addRouteListener(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> routesManager.removeRouteListener(null)).isInstanceOf(NullPointerException.class);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void routeEditListener_throws_whenNull()
    {
        assertThatThrownBy(() -> routesManager.addRouteEditListener(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> routesManager.removeRouteEditListener(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void startRouteEditing_succeeds_onExistingRoute()
    {
        final IRoute route = makeBasicRouteModel();
        routesManager.addRoute(route);

        routesManager.startRouteEditing(route);
        SwingTestUtils.waitForEdtEvents();

        assertThat(routesManager.getCurrentlyEditingRoute()).isEqualTo(route);

        routesManager.finishRouteEditing();
        SwingTestUtils.waitForEdtEvents();

        assertThat(routesManager.getCurrentlyEditingRoute()).isNull();
    }

    @Test
    public void startRouteEditing_succeeds_onNewRoute()
    {
        // Create a route with all non-default properties
        final IRoute initialRouteProperties = new Route();
        initialRouteProperties.setDirection(RouteDirection.EXFILTRATION);
        initialRouteProperties.setMethod(RouteMethod.FLYING);
        initialRouteProperties.setRouteType(RouteType.SECONDARY);
        initialRouteProperties.setRouteOrder(RouteOrder.DESCENDING);
        initialRouteProperties.setLineColor(Color.orange);
        initialRouteProperties.setLineWidth(15);
        initialRouteProperties.setPrefix("Test Prefix");
        initialRouteProperties.setName("Test Name");

        routesManager.startNewRouteEditing(initialRouteProperties);
        SwingTestUtils.waitForEdtEvents();

        // Fire the event used to initialize a newly created route
        final IRoute createdRoute = new Route();
        getBean(EventBus.class, UiEventBus.Literal.INSTANCE).post(new InitializeDrawToolEvent(createdRoute));

        assertThat(routesManager.getCurrentlyEditingRoute()).isEqualTo(createdRoute);

        routesManager.finishRouteEditing();
        SwingTestUtils.waitForEdtEvents();

        // Verify that our initial properties were copied over
        assertThat(createdRoute.getDirection()).isEqualTo(RouteDirection.EXFILTRATION);
        assertThat(createdRoute.getMethod()).isEqualTo(RouteMethod.FLYING);
        assertThat(createdRoute.getRouteType()).isEqualTo(RouteType.SECONDARY);
        assertThat(createdRoute.getRouteOrder()).isEqualTo(RouteOrder.DESCENDING);
        assertThat(createdRoute.getLineColor()).isEqualTo(Color.orange);
        assertThat(createdRoute.getLineWidth()).isEqualTo(15);
        assertThat(createdRoute.getPrefix()).isEqualTo("Test Prefix");
        assertThat(createdRoute.getName()).isEqualTo("Test Name");

        assertThat(routesManager.getCurrentlyEditingRoute()).isNull();

        // Note: due to current architecture, we can't verify that a MO was created since that happens within
        // AGlobeShapeEditor, and it is never instantiated while tests are running.
    }

    @Test
    public void routeEditListeners_areCalled_onExistingRouteEdit()
    {
        final IRoute route = makeBasicRouteModel();
        routesManager.addRoute(route);

        routeEditListener = new RouteEditListener();
        routesManager.addRouteEditListener(routeEditListener);

        routesManager.startRouteEditing(route);
        SwingTestUtils.waitForEdtEvents();
        routeEditListener.assertCounts(0, 1, 0);

        routesManager.finishRouteEditing();
        SwingTestUtils.waitForEdtEvents();
        routeEditListener.assertCounts(0, 1, 1);
        assertThat(routeEditListener.lastCompleteCanceled).isFalse();

        // Now test cancel
        routesManager.startRouteEditing(route);
        SwingTestUtils.waitForEdtEvents();
        routeEditListener.assertCounts(0, 2, 1);

        routesManager.cancelRouteEditing();
        SwingTestUtils.waitForEdtEvents();
        routeEditListener.assertCounts(0, 2, 2);
        assertThat(routeEditListener.lastCompleteCanceled).isTrue();
    }

    @Test
    public void routeEditListeners_areCalled_onNewRouteEdit()
    {
        routeEditListener = new RouteEditListener();
        routesManager.addRouteEditListener(routeEditListener);

        routesManager.startNewRouteEditing();
        SwingTestUtils.waitForEdtEvents();
        routeEditListener.assertCounts(1, 0, 0);

        routesManager.finishRouteEditing();
        SwingTestUtils.waitForEdtEvents();
        routeEditListener.assertCounts(1, 0, 1);
        assertThat(routeEditListener.lastCompleteCanceled).isFalse();

        // Now test cancel
        routesManager.startNewRouteEditing();
        SwingTestUtils.waitForEdtEvents();
        routeEditListener.assertCounts(2, 0, 1);

        routesManager.cancelRouteEditing();
        SwingTestUtils.waitForEdtEvents();
        routeEditListener.assertCounts(2, 0, 2);
        assertThat(routeEditListener.lastCompleteCanceled).isTrue();
    }

    /**
     * @return A route with 3 control points, 1st and 3rd are waypoints
     */
    private IRoute makeBasicRouteModel()
    {
        final Route route = new Route();

        route.addWaypoint(GP00, 0);
        route.addControlPoint(GP11, 1);
        route.addWaypoint(GP22, 2);
        return route;
    }

    private static class RouteListener implements IRouteListener
    {
        private int addCount = 0;
        private int removeCount = 0;

        @Override
        public void onRouteAdded(@Nonnull IRoute route)
        {
            addCount += 1;
        }

        @Override
        public void onRouteRemoved(@Nonnull IRoute route)
        {
            removeCount += 1;
        }

        private void assertCounts(int expectedAddCount, int expectedRemoveCount)
        {
            assertThat(addCount).describedAs("add count wrong").isEqualTo(expectedAddCount);
            assertThat(removeCount).describedAs("remove count wrong").isEqualTo(expectedRemoveCount);
        }
    }

    private static class RouteEditListener implements IRouteEditListener
    {
        private int editNewCount = 0;
        private int editExistingCount = 0;
        private int completeCount = 0;
        private boolean lastCompleteCanceled;

        @Override
        public void onEditNewRoute(@Nonnull IRoute route)
        {
            editNewCount += 1;
        }

        @Override
        public void onEditExistingRoute(@Nonnull IRoute route)
        {
            editExistingCount += 1;
        }

        @Override
        public void onEditComplete(@Nonnull IRoute route, boolean canceled)
        {
            completeCount += 1;
            lastCompleteCanceled = canceled;
        }

        private void assertCounts(int expectedEditNewCount, int expectedEditExistingCount, int expectedCompleteCount)
        {
            assertThat(editNewCount).describedAs("editNew count wrong").isEqualTo(expectedEditNewCount);
            assertThat(editExistingCount).describedAs("editExisting count wrong").isEqualTo(expectedEditExistingCount);
            assertThat(completeCount).describedAs("complete count wrong").isEqualTo(expectedCompleteCount);
        }
    }
}
