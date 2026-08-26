package gov.takx.core.drifter;

import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.drifter.DrifterConfiguration;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.core.mapobject.MapObjectManager;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.utils.MapObjectTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 5.6
 */
public class DrifterControllerTest extends AIntegrationTest
{
    private MapObjectTestUtils moTestUtils;
    private MapObjectManager mapObjectManager;
    private DrifterController controller;

    @Override
    protected void postProjectOpen()
    {
        mapObjectManager = getBean(MapObjectManager.class);
        moTestUtils = new MapObjectTestUtils(mapObjectManager);
    }

    @BeforeEach
    public void beforeEach()
    {
        controller = Mockito.spy(getBean(DrifterController.class));
    }

    @Test
    public void testUpdateDeleteConfiguration()
    {
        IMapObject mapObject = moTestUtils.createMarkerMapObject(null, null, null);
        DrifterConfiguration config = DrifterConfiguration.builder().speed(11).bearing(11).paused(false).routePoint(-1).routeUid(null).build();
        controller.updateConfiguration(mapObject, config);
        assertThat(controller.getMapObjects()).contains(mapObject);
        controller.togglePaused(mapObject);
        assertThat(mapObject.getDrifterConfiguration().paused()).isTrue();
        controller.deleteConfiguration(mapObject, false);
        assertThat(controller.getMapObjects()).doesNotContain(mapObject);
        assertThat(mapObject.getDrifterConfiguration()).isNull();
    }

    @Test
    public void testSimulateConstant()
    {
        IMapObject mapObject = moTestUtils.createMarkerMapObject(null, null, null);
        mapObject.setLocation(new GeoPoint(20, 20), true);
        GeoPoint location = mapObject.getLocation();
        DrifterConfiguration config = DrifterConfiguration.builder().speed(500).bearing(11).paused(true).routePoint(-1).routeUid(null).build();
        controller.updateConfiguration(mapObject, config);
        assertThat(mapObject.getLocation()).isEqualTo(location);
        controller.simulateTime(mapObject, 50_000);
        assertThat(mapObject.getLocation()).isNotEqualTo(location);
    }

    @Test
    public void testSimulateRoute()
    {
        IMapObject mapObject = moTestUtils.createMarkerMapObject(null, null, null);
        mapObject.setLocation(new GeoPoint(20, 20), true);
        IRouteMapObject routeMapObject = moTestUtils.createRouteMapObject(null, null, null);
        GeoPoint start = new GeoPoint(30, 30);
        routeMapObject.addControlPoint(start, 0);
        routeMapObject.addControlPoint(new GeoPoint(40, 40), 1);
        routeMapObject.addControlPoint(new GeoPoint(50, 50), 2);
        DrifterConfiguration config = DrifterConfiguration.builder().speed(10_000).bearing(11).paused(true).routePoint(0).routeUid(routeMapObject.getUid()).build();
        controller.updateConfiguration(mapObject, config);
        assertThat(mapObject.getLocation()).isEqualTo(start);
        controller.simulateTime(mapObject, 50_000_000);
        assertThat(mapObject.getDrifterConfiguration().routePoint()).isEqualTo(routeMapObject.getControlPoints().size() - 1);
    }

    @Test
    public void testRestart()
    {
        IMapObject mapObject = moTestUtils.createMarkerMapObject(null, null, null);
        IRouteMapObject routeMapObject = moTestUtils.createRouteMapObject(null, null, null);
        routeMapObject.addControlPoint(new GeoPoint(), 0);
        routeMapObject.addControlPoint(new GeoPoint(), 0);
        routeMapObject.addControlPoint(new GeoPoint(), 0);
        DrifterConfiguration config = DrifterConfiguration.builder().speed(11).bearing(11).paused(false).routePoint(1).routeUid(routeMapObject.getUid()).build();
        controller.updateConfiguration(mapObject, config);
        assertThat(mapObject.getDrifterConfiguration().routePoint()).isEqualTo(1);
        controller.restart(mapObject);
        assertThat(mapObject.getDrifterConfiguration().routePoint()).isEqualTo(0);
    }

    /**
     * @since 5.7
     */
    @Test
    public void testRouteRemoved()
    {
        IMapObject mapObject = moTestUtils.createMarkerMapObject(null, null, null);
        mapObject.setLocation(new GeoPoint(20, 20), true);
        IRouteMapObject routeMapObject = moTestUtils.createRouteMapObject(null, null, null);
        GeoPoint start = new GeoPoint(30, 30);
        routeMapObject.addControlPoint(start, 0);
        DrifterConfiguration config = DrifterConfiguration.builder().speed(10_000).bearing(11).paused(true).routePoint(0).routeUid(routeMapObject.getUid()).build();
        controller.updateConfiguration(mapObject, config);
        var route = controller.getRoute(config);
        assertThat(route).isEqualTo(routeMapObject);
        mapObjectManager.deleteMapObject(routeMapObject);
        assertThat(mapObject.getDrifterConfiguration()).isNull();
    }

    /**
     * @since 5.7
     */
    @Test
    public void testRouteStartup()
    {
        IRouteMapObject routeMapObject = moTestUtils.createRouteMapObject(null, null, null);
        GeoPoint start = new GeoPoint(30, 30);
        routeMapObject.addControlPoint(start, 0);
        DrifterConfiguration config = DrifterConfiguration.builder().speed(10_000).bearing(11).paused(true).routePoint(0).routeUid(routeMapObject.getUid()).build();
        IMapObject mapObject = moTestUtils.createMarkerMapObject(mo -> mo.setDrifterConfiguration(config), null, null);
        assertThat(controller.getMapObjects()).contains(mapObject);
        mapObjectManager.deleteMapObject(routeMapObject);
        assertThat(mapObject.getDrifterConfiguration()).isNull();
    }
}
