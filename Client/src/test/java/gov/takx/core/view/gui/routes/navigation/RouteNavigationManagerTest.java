
package gov.takx.core.view.gui.routes.navigation;

import gov.tak.api.location.ILocationProviderManager;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.core.location.LocationTracker;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.navigation.IRouteNavigationManager;
import gov.takx.api.routes.IRoutesManager;
import gov.takx.api.shapes.IRoute;
import gov.takx.core.navigation.RouteNavigationManager;
import gov.takx.core.view.gui.proxy.HeadlessUserProxy;
import gov.takx.dataservices.shapes.Route;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.mock.TestLocationProvider;
import gov.takx.test.utils.SwingTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static gov.takx.test.mock.TestLocationProvider.PROVIDER_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Set of tests for the {@link IRouteNavigationManager} capability for navigating to/along routes.
 *
 * @since 3.4
 */
public class RouteNavigationManagerTest extends AIntegrationTest
{

    private static final GeoPoint GP00 = GeoPoint.ZERO_POINT;
    private static final GeoPoint GP11 = new GeoPoint(1, 1);
    private static final GeoPoint GP22 = new GeoPoint(2, 2);

    private IRouteNavigationManager navigationManager;
    private ILocationProviderManager locationProviderManager;
    private IRoutesManager routesManager;
    private TestLocationProvider testLocationProvider;
    private IMapObjectManager mapObjectManager;
    private LocationTracker locationTracker;

    private IRoute testRoute;

    @BeforeEach
    public void initTestState()
    {
        mapObjectManager = getBean(IMapObjectManager.class);
        locationTracker = getBean(LocationTracker.class);
        locationTracker.createMarker();
        navigationManager = getBean(IRouteNavigationManager.class);
        locationProviderManager = getBean(ILocationProviderManager.class);
        routesManager = getBean(IRoutesManager.class);
        PreferenceEntry.BLOODHOUND_AUTO_TARGET_BREADCRUMBS.put(false);
        testRoute = makeBasicRouteModel();
        routesManager.addRoute(testRoute);
        configureLocationProvider();
    }

    @SuppressLogging(HeadlessUserProxy.class) // Quiet warning from unregistering active location provider
    @AfterEach
    public void cleanupAfterMethod()
    {
        navigationManager.endNavigation();
        locationProviderManager.unregisterLocationProvider(testLocationProvider);
        deleteAllMapObjects();
    }

    private void configureLocationProvider()
    {
        testLocationProvider = new TestLocationProvider();
        locationProviderManager.registerLocationProvider(testLocationProvider);
        testLocationProvider.configure(new TestLocationProvider.TestConfiguration("1, 1"));

        locationProviderManager.setActiveLocationProvider(PROVIDER_KEY);
        SwingTestUtils.waitForEdtEvents();
        assertThat(testLocationProvider.isActive()).isTrue();
        assertThat(locationProviderManager.getActiveLocationProvider()).isSameAs(testLocationProvider);
    }

    @Test
    public void navigate_to_route_valid_waypoint_succeeds()
    {
        navigationManager.navigateToRoute(testRoute, null);
        assertThat(navigationManager.getRouteBeingNavigated()).isEqualTo(testRoute);
        assertThat(navigationManager.getCurrentNavigationWaypoint().get()).isEqualTo(testRoute.getWaypoints().get(0).get());
    }

    @Test
    public void navigate_to_null_route_throws_exception()
    {
        assertThatNullPointerException().isThrownBy(() -> navigationManager.navigateToRoute(null, null));
        assertThat(navigationManager.getRouteBeingNavigated()).isNull();
        assertThat(navigationManager.getCurrentNavigationWaypoint()).isNull();
    }

    @SuppressLogging(RouteNavigationManager.class)
    @Test
    public void navigate_to_route_invalid_waypoint_throws_exception()
    {
        assertThatIllegalArgumentException().isThrownBy(() -> navigationManager.navigateToRoute(testRoute, "abc"));
        assertThat(navigationManager.getCurrentNavigationWaypoint()).isNull();
        assertThat(navigationManager.getRouteBeingNavigated()).isNull();
    }

    @Test
    public void end_navigation_succeeds()
    {
        navigate_to_route_valid_waypoint_succeeds();

        navigationManager.endNavigation();
        assertThat(navigationManager.getRouteBeingNavigated()).isNull();
        assertThat(navigationManager.getCurrentNavigationWaypoint()).isNull();
    }

    @Test
    public void navigate_to_specific_waypoint_succeeds()
    {
        GeoPointMetaData secondWaypoint = testRoute.getWaypoints().get(1);
        navigationManager.navigateToRoute(testRoute, secondWaypoint.getMetaData().getStringAttribute(StandardAttributeNames.UID));
        assertThat(navigationManager.getRouteBeingNavigated()).isEqualTo(testRoute);
        assertThat(navigationManager.getCurrentNavigationWaypoint().get()).isEqualTo(secondWaypoint.get());
    }

    //test auto moving to next point when location is closer both first and last points
    @Test
    public void current_waypoint_changed_after_location_update()
    {
        // After this initial set up we are navigating towards waypoint 1 at 0,0
        navigate_to_route_valid_waypoint_succeeds();

        // We change our location to 0,0
        testLocationProvider.configure(new TestLocationProvider.TestConfiguration("0,0"));
        SwingTestUtils.waitForEdtEvents();
        assertThat(navigationManager.getRouteBeingNavigated()).isEqualTo(testRoute);
        // And verify that we are now navigating to waypoint 2 at 1,1
        assertThat(navigationManager.getCurrentNavigationWaypoint().get()).isEqualTo(testRoute.getWaypoints().get(1).get());

        // We change our location to 1,1
        testLocationProvider.configure(new TestLocationProvider.TestConfiguration("1,1"));
        SwingTestUtils.waitForEdtEvents();
        assertThat(navigationManager.getRouteBeingNavigated()).isEqualTo(testRoute);
        // And verify we are still navigating towards it, since it is the last waypoint so there isn't another to transition to.
        assertThat(navigationManager.getCurrentNavigationWaypoint().get()).isEqualTo(testRoute.getWaypoints().get(1).get());
    }

    @Test
    public void current_waypoint_not_changed_after_location_update_far_away()
    {
        navigate_to_route_valid_waypoint_succeeds();

        TestLocationProvider testLocationProvider = ((TestLocationProvider) locationProviderManager.getActiveLocationProvider());
        testLocationProvider.configure(new TestLocationProvider.TestConfiguration("10,10"));
        SwingTestUtils.waitForEdtEvents();
        assertThat(navigationManager.getRouteBeingNavigated()).isEqualTo(testRoute);
        assertThat(navigationManager.getCurrentNavigationWaypoint().get()).isEqualTo(testRoute.getWaypoints().get(0).get());
    }

    @Test
    public void current_waypoint_changed_after_location_update_exfil()
    {
        GeoPointMetaData firstWaypoint = testRoute.getWaypoints().get(0);
        GeoPointMetaData secondWaypoint = testRoute.getWaypoints().get(1);
        navigationManager.navigateToRoute(testRoute, null);
        navigationManager.setReversedNavigation(true);
        assertThat(navigationManager.getRouteBeingNavigated()).isEqualTo(testRoute);
        assertThat(navigationManager.getCurrentNavigationWaypoint().get()).isEqualTo(firstWaypoint.get());

        TestLocationProvider testLocationProvider = ((TestLocationProvider) locationProviderManager.getActiveLocationProvider());
        testLocationProvider.configure(new TestLocationProvider.TestConfiguration("0,0"));
        SwingTestUtils.waitForEdtEvents();
        assertThat(navigationManager.getRouteBeingNavigated()).isEqualTo(testRoute);
        assertThat(navigationManager.getCurrentNavigationWaypoint().get()).isEqualTo(firstWaypoint.get());

        navigationManager.navigateToRoute(testRoute, secondWaypoint.getMetaData().getStringAttribute(StandardAttributeNames.UID));
        navigationManager.setReversedNavigation(true);
        assertThat(navigationManager.getRouteBeingNavigated()).isEqualTo(testRoute);
        assertThat(navigationManager.getCurrentNavigationWaypoint().get()).isEqualTo(secondWaypoint.get());

        testLocationProvider.configure(new TestLocationProvider.TestConfiguration("2,2"));
        SwingTestUtils.waitForEdtEvents();
        assertThat(navigationManager.getRouteBeingNavigated()).isEqualTo(testRoute);
        assertThat(navigationManager.getCurrentNavigationWaypoint().get()).isEqualTo(firstWaypoint.get());
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
}
