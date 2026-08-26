package gov.takx.core.geofence.monitor;

import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.maps.PointMapItem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.geofence.GeoFence;
import gov.takx.api.geofence.Trigger;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.api.mapobject.MapObjectOptions;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.shapes.IShape;
import gov.takx.api.shapes.IShapeService;
import gov.takx.api.shapes.ShapeAlreadyExistsException;
import gov.takx.core.geofence.AlertManager;
import gov.takx.core.shapes.ShapeServiceFactory;
import gov.takx.dataservices.shapes.Circle;
import gov.takx.dataservices.shapes.Ellipse;
import gov.takx.dataservices.shapes.Polygon;
import gov.takx.dataservices.shapes.Rectangle;
import gov.takx.dataservices.shapes.Square;
import gov.takx.platform.utils.ShapeUtils;
import gov.takx.test.AIntegrationTest;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * @since 5.3
 */
public class GeoFenceMonitorTest extends AIntegrationTest
{
    private static final ILocation LOCATION = new RaptorLocation(1, 1, 10_000);
    private static final ILocation FAR_AWAY_LOCATION = new RaptorLocation(30, 30, 10_000);
    private static final String POINT_NAME = "TestPoint";
    private final double POLYGON_VERTEX_DISTANCE = 1000;

    private GeoFenceSpatialCalculator calculator;
    private IShapeService shapeService;
    private IMapObjectManager mapObjectManager;
    private AlertManager alertManager;

    @Override
    protected void postProjectOpen()
    {
        shapeService = getBean(ShapeServiceFactory.class).getOrCreateCoreShapeService();
        mapObjectManager = getBean(IMapObjectManager.class);
        calculator = getBean(GeoFenceSpatialCalculator.class);

        alertManager = Mockito.spy(getBean(AlertManager.class));
    }

    @BeforeEach
    public void setup()
    {
        deleteAllMapObjects();
        reset(alertManager);
    }

    @AfterEach
    public void afterEach()
    {
        calculator.stopService(null);
    }

    @Test
    void testCircleBoundaryCrossings() throws ShapeAlreadyExistsException
    {
        doWithCircle(Trigger.Both, (circle, monitor) -> {
            final var extent = monitor.getExtent();
            assertThat(extent.center()).isEqualTo(LOCATION);
            assertThat(extent.furthestPoint()).isEqualTo(1000);

            final var point = createPoint(LOCATION);
            monitor.setMapItemState(point);

            movePointAndCheck(point, 0, 500, false, monitor, false, null, null);
            movePointAndCheck(point, 0, 1500, false, monitor, true, circle, Trigger.Exit);
            movePointAndCheck(point, 0, 500, false, monitor, true, circle, Trigger.Entry);
        });
    }

    @Test
    void testEllipseBoundaryCrossings() throws ShapeAlreadyExistsException
    {
        doWithEllipse((ellipse, monitor) -> {
            final var extent = monitor.getExtent();
            assertThat(extent.center()).isEqualTo(LOCATION);
            assertThat(extent.furthestPoint()).isEqualTo(2000);

            final var point = createPoint(LOCATION);
            monitor.setMapItemState(point);

            // 0 Orientation has the major axis horizontal

            // Stay within the boundary on both minor/major axes
            movePointAndCheck(point, 0, 750, false, monitor, false, null, null);
            movePointAndCheck(point, 90, 1500, false, monitor, false, null, null);

            // Test exit on different axes
            movePointAndCheck(point, 0, 1250, true, monitor, true, ellipse, Trigger.Exit);
            movePointAndCheck(point, 90, 2050, true, monitor, true, ellipse, Trigger.Exit);

            // Test entry
            resetPoint(point, FAR_AWAY_LOCATION, monitor);
            movePointAndCheck(point, 0, 0, false, monitor, true, ellipse, Trigger.Entry);
        });
    }

    @Test
    void testSquareBoundaryCrossings() throws ShapeAlreadyExistsException
    {
        doWithSquare((square, monitor) -> verifyRectangularMonitor(monitor, square, square.getHeight(), square.getWidth()));
    }

    @Test
    void testRectangleBoundaryCrossings() throws ShapeAlreadyExistsException
    {
        doWithRectangle((rectangle, monitor) -> verifyRectangularMonitor(monitor, rectangle, rectangle.getHeight(), rectangle.getWidth()));
    }

    @Test
    void testPolygonBoundaryCrossings() throws ShapeAlreadyExistsException
    {
        doWithPolygon((polygon, monitor) -> {
            final var extent = monitor.getExtent();
            final var center = extent.center();
            assertThat(center.getLatitude()).isCloseTo(LOCATION.getLatitude(), within(0.000001));
            assertThat(center.getLongitude()).isCloseTo(LOCATION.getLongitude(), within(0.000001));
            assertThat(extent.furthestPoint()).isCloseTo(POLYGON_VERTEX_DISTANCE, within(0.001));

            final var point = createPoint(LOCATION);
            monitor.setMapItemState(point);

            // Generate a series of angle/distance pairs that align to the exact boundary
            final double edgeDistance = POLYGON_VERTEX_DISTANCE * Math.sin(Math.toRadians(60));
            final List<Pair<Double, Double>> angleAndDistance = new ArrayList<>();
            for (int i = 0; i < 6; i++)
            {
                final double angle = 60d * i;
                angleAndDistance.add(Pair.of(angle - 30, edgeDistance));
                angleAndDistance.add(Pair.of(angle, POLYGON_VERTEX_DISTANCE));
            }

            // Test staying within bounds
            angleAndDistance.forEach(pair -> movePointAndCheck(point, pair.getLeft(), pair.getRight() - 5,
                    false, monitor, false, null, null));

            // Test exit
            angleAndDistance.forEach(pair -> movePointAndCheck(point, pair.getLeft(), pair.getRight() + 5,
                    true, monitor, true, polygon, Trigger.Exit));

            // Test entry
            resetPoint(point, FAR_AWAY_LOCATION, monitor);
            movePointAndCheck(point, 0, 0, false, monitor, true, polygon, Trigger.Entry);
        });
    }

    /**
     * Verify boundary crossings for a rectangular geoFence.
     * 
     * @param monitor Monitor to use
     * @param shape Shape for alert verification
     * @param height Height of rectangle
     * @param width Width of rectangle
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void verifyRectangularMonitor(AGeoFenceMonitor<?> monitor, IShape shape, double height, double width)
    {
        final var point = createPoint(LOCATION);

        ILocation corner = ShapeUtils.rhumbEndPositionMeters(shape.getLocation(), 0, height / 2);
        corner = ShapeUtils.rhumbEndPositionMeters(corner, 90, width / 2);
        final double cornerDistance = GeoCalculations.distance(shape.getLocation(), corner);
        final double edgeDistanceX = width / 2d;
        final double edgeDistanceY = height / 2d;

        // This calculation relies on height < width
        final double cornerAngle = Math.toDegrees(Math.atan(edgeDistanceY / edgeDistanceX));

        final var extent = monitor.getExtent();
        assertThat(extent).isNotNull();
        assertThat(extent.center()).isEqualTo(LOCATION);
        assertThat(extent.furthestPoint()).isCloseTo(cornerDistance, within(0.0001));

        monitor.setMapItemState(point); // Start at center

        // Generate a series of angle/distance pairs that align to the exact boundary
        final List<Pair<Double, Double>> angleAndDistance = List.of(
                Pair.of(0d, edgeDistanceY), Pair.of(90d, edgeDistanceX), Pair.of(180d, edgeDistanceY), Pair.of(270d, edgeDistanceX),
                Pair.of(90 - cornerAngle, cornerDistance), Pair.of(90 + cornerAngle, cornerDistance), Pair.of(270 - cornerAngle, cornerDistance), Pair.of(270 + cornerAngle, cornerDistance));

        // Test staying within bounds
        angleAndDistance.forEach(pair -> movePointAndCheck(point, pair.getLeft(), pair.getRight() - 5,
                false, monitor, false, null, null));

        // Test exit
        angleAndDistance.forEach(pair -> movePointAndCheck(point, pair.getLeft(), pair.getRight() + 5,
                true, monitor, true, shape, Trigger.Exit));

        // Test entry
        resetPoint(point, FAR_AWAY_LOCATION, monitor);
        movePointAndCheck(point, 0, 0, false, monitor, true, shape, Trigger.Entry);
    }

    @Test
    void testCircleShapeChange() throws ShapeAlreadyExistsException
    {
        doWithCircle(Trigger.Both, (circle, monitor) -> {
            final var point = createPoint(locationAtDistance(0, 500));
            monitor.setMapItemState(point);
            verify(alertManager).alert(circle, point, Trigger.Entry); // Test alert on initial creation

            reset(alertManager);
            circle.setRadius(750); // Point still contained, so no alert
            verify(alertManager, never()).alert(any(), any(), any());

            circle.setRadius(250); // Point now not contained
            verify(alertManager).alert(circle, point, Trigger.Exit);

            circle.setRadius(1000); // Point now contained
            verify(alertManager).alert(circle, point, Trigger.Entry);

            // Now move the shape
            reset(alertManager);
            circle.setLocation(FAR_AWAY_LOCATION); // Point now not contained
            verify(alertManager).alert(circle, point, Trigger.Exit);
        });
    }

    @Test
    void testEllipseShapeChange() throws ShapeAlreadyExistsException
    {
        doWithEllipse((ellipse, monitor) -> {
            final var point = createPoint(locationAtDistance(90, 1250));
            monitor.setMapItemState(point);
            verify(alertManager).alert(ellipse, point, Trigger.Entry); // Test alert on initial creation

            reset(alertManager);
            ellipse.setAxes(800, 500); // Point still contained, so no alert
            verify(alertManager, never()).alert(any(), any(), any());

            ellipse.setOrientation(90); // Point now not contained
            verify(alertManager).alert(ellipse, point, Trigger.Exit);

            ellipse.setOrientation(180); // Point now contained
            verify(alertManager).alert(ellipse, point, Trigger.Entry);

            reset(alertManager);
            ellipse.setRingCount(1); // Point now not contained
            verify(alertManager).alert(ellipse, point, Trigger.Exit);
            ellipse.setRingCount(2); // Point now contained
            verify(alertManager).alert(ellipse, point, Trigger.Entry);

            // Now move the shape
            reset(alertManager);
            ellipse.setLocation(FAR_AWAY_LOCATION); // Point now not contained
            verify(alertManager).alert(ellipse, point, Trigger.Exit);
        });
    }

    @Test
    void testSquareShapeChange() throws ShapeAlreadyExistsException
    {
        doWithSquare((square, monitor) -> {
            final var point = createPoint(locationAtDistance(0, 350));
            monitor.setMapItemState(point);
            verify(alertManager).alert(square, point, Trigger.Entry); // Test alert on initial creation

            reset(alertManager);
            square.setSideLength(800); // Point still contained, so no alert
            verify(alertManager, never()).alert(any(), any(), any());

            square.setSideLength(500); // Point now not contained
            verify(alertManager).alert(square, point, Trigger.Exit);

            square.setSideLength(1000); // Point now contained
            verify(alertManager).alert(square, point, Trigger.Entry);

            // Now move the shape
            reset(alertManager);
            square.setLocation(FAR_AWAY_LOCATION); // Point now not contained
            verify(alertManager).alert(square, point, Trigger.Exit);
        });
    }

    @Test
    void testRectangleShapeChange() throws ShapeAlreadyExistsException
    {
        doWithRectangle((rectangle, monitor) -> {
            final var point = createPoint(locationAtDistance(0, 125));
            monitor.setMapItemState(point);
            verify(alertManager).alert(rectangle, point, Trigger.Entry); // Test alert on initial creation

            reset(alertManager);
            rectangle.setHeight(400); // Point still contained, so no alert
            rectangle.setWidth(500);
            verify(alertManager, never()).alert(any(), any(), any());

            rectangle.setHeight(200); // Point now not contained
            verify(alertManager).alert(rectangle, point, Trigger.Exit);

            rectangle.setHeight(500); // Point now contained
            verify(alertManager).alert(rectangle, point, Trigger.Entry);

            // Now move the shape
            reset(alertManager);
            rectangle.setLocation(FAR_AWAY_LOCATION); // Point now not contained
            verify(alertManager).alert(rectangle, point, Trigger.Exit);
        });
    }

    @Test
    void testPolygonShapeChange() throws ShapeAlreadyExistsException
    {
        doWithPolygon((polygon, monitor) -> {
            final List<ILocation> vertices = polygon.getVertices();
            final var point = createPoint(locationAtDistance(0, 750));
            monitor.setMapItemState(point);
            verify(alertManager).alert(polygon, point, Trigger.Entry); // Test alert on initial creation

            reset(alertManager);
            vertices.set(0, locationAtDistance(0, 800)); // Point still contained, so no alert
            polygon.setVertices(vertices);
            verify(alertManager, never()).alert(any(), any(), any());

            vertices.set(0, locationAtDistance(0, 500)); // Point now not contained
            polygon.setVertices(vertices);
            verify(alertManager).alert(polygon, point, Trigger.Exit);

            vertices.set(0, locationAtDistance(0, 1000)); // Point now contained
            polygon.setVertices(vertices);
            verify(alertManager).alert(polygon, point, Trigger.Entry);

            // Now move the shape
            reset(alertManager);
            polygon.setVertices(makeHexagon(FAR_AWAY_LOCATION));
            verify(alertManager).alert(polygon, point, Trigger.Exit);
        });
    }

    @Test
    void testAltitudeConstraints() throws ShapeAlreadyExistsException
    {
        doWithCircle(Trigger.Both, b -> b.elevationRange(10_000, 15_000), (circle, monitor) -> {
            final var point = createPoint(new RaptorLocation(1, 1, 5_000));
            monitor.setMapItemState(point); // Not contained due to elevation

            point.setLocation(new RaptorLocation(1, 1, 7_500), false);
            monitor.mapItemMoved(point); // Still not in
            verify(alertManager, never()).alert(any(), any(), any());

            point.setLocation(new RaptorLocation(1, 1, 12_000), false);
            monitor.mapItemMoved(point); // Now contained
            verify(alertManager).alert(circle, point, Trigger.Entry);

            point.setLocation(new RaptorLocation(1, 1, 20_000), false);
            monitor.mapItemMoved(point); // Now contained
            verify(alertManager).alert(circle, point, Trigger.Exit);
        });
    }

    @Test
    void testEntryOnlyGeoFence() throws ShapeAlreadyExistsException
    {
        doWithCircle(Trigger.Entry, (circle, monitor) -> {
            final var point = createPoint(LOCATION);
            monitor.setMapItemState(point); // Initially contained
            verify(alertManager).alert(circle, point, Trigger.Entry); // Test alert on initial creation

            reset(alertManager);
            movePointAndUpdateMonitor(point, 0, 500, monitor); // Still in
            verify(alertManager, never()).alert(any(), any(), any());

            movePointAndUpdateMonitor(point, 0, 5_000, monitor); // Now out, but no alert
            verify(alertManager, never()).alert(any(), any(), any());

            movePointAndUpdateMonitor(point, 0, 500, monitor); // Back in, should alert
            verify(alertManager).alert(circle, point, Trigger.Entry);
        });
    }

    @Test
    void testExitOnlyGeoFence() throws ShapeAlreadyExistsException
    {
        doWithCircle(Trigger.Exit, (circle, monitor) -> {
            final var point = createPoint(FAR_AWAY_LOCATION);
            monitor.setMapItemState(point); // Initially not contained

            movePointAndUpdateMonitor(point, 0, 500, monitor); // Now in, but no alert
            verify(alertManager, never()).alert(any(), any(), any());

            movePointAndUpdateMonitor(point, 0, 5_000, monitor); // Back out, should alert
            verify(alertManager).alert(circle, point, Trigger.Exit);
        });
    }

    @Test
    void newMapItem_triggersOnEntry() throws ShapeAlreadyExistsException
    {
        doWithCircle(Trigger.Entry, (circle, monitor) -> {
            final var point = createPoint(LOCATION);
            monitor.mapItemMoved(point);
            verify(alertManager).alert(circle, point, Trigger.Entry);
        });
    }

    /**
     * Perform test operations on a circle and associated monitor.
     *
     * @param trigger Trigger type for GeoFence
     * @param builderInit additional builder initializations
     * @param testOps operations to perform
     */
    private void doWithCircle(Trigger trigger, Function<GeoFence.Builder, GeoFence.Builder> builderInit, BiConsumer<Circle, AGeoFenceMonitor<?>> testOps) throws ShapeAlreadyExistsException
    {
        final String name = "circle";
        final var circle = new Circle(name, LOCATION, 1000);
        final var circleMapObject = shapeService.createCircleMapObject(circle);
        final GeoFence.Builder builder = GeoFence.builder().shapeUid(circleMapObject.getUid()).trigger(trigger);
        builderInit.apply(builder);
        final var geoFence = builder.build();
        final var monitor = Mockito.spy(new CircleGeoFenceMonitor(circle, geoFence, calculator, alertManager));
        circleMapObject.setGeoFence(geoFence);

        testOps.accept(circle, monitor);
    }

    /**
     * Perform test operations on a circle and associated monitor.
     *
     * @param trigger Trigger type for GeoFence
     * @param testOps operations to perform
     */
    private void doWithCircle(Trigger trigger, BiConsumer<Circle, AGeoFenceMonitor<?>> testOps) throws ShapeAlreadyExistsException
    {
        doWithCircle(trigger, Function.identity(), testOps);
    }

    /**
     * Perform test operations on an ellipse and associated monitor.  The GeoFence will have a trigger of {@link Trigger#Both}.
     *
     * @param testOps operations to perform
     */
    private void doWithEllipse(BiConsumer<Ellipse, AGeoFenceMonitor<?>> testOps) throws ShapeAlreadyExistsException
    {
        final String name = "ellipse";
        final var ellipse = new Ellipse(name, LOCATION, 0, 1000, 500);
        ellipse.setRingCount(2);
        final var ellipseMapObject = shapeService.createEllipseMapObject(ellipse);
        final var geoFence = GeoFence.builder().shapeUid(ellipseMapObject.getUid()).trigger(Trigger.Both).build();
        final var monitor = Mockito.spy(new EllipseGeoFenceMonitor(ellipse, geoFence, calculator, alertManager));
        ellipseMapObject.setGeoFence(geoFence);

        testOps.accept(ellipse, monitor);
    }

    /**
     * Perform test operations on a square and associated monitor.  The GeoFence will have a trigger of {@link Trigger#Both}.
     *
     * @param testOps operations to perform
     */
    private void doWithSquare(BiConsumer<Square, AGeoFenceMonitor<?>> testOps) throws ShapeAlreadyExistsException
    {
        final String name = "square";
        final var square = new Square(name, LOCATION, 0, 1000);
        final var squareMapObject = shapeService.createSquareMapObject(square);
        final var geoFence = GeoFence.builder().shapeUid(squareMapObject.getUid()).trigger(Trigger.Both).build();
        final var monitor = Mockito.spy(new RectangleGeoFenceMonitor(square, geoFence, calculator, alertManager));
        squareMapObject.setGeoFence(geoFence);

        testOps.accept(square, monitor);
    }

    /**
     * Perform test operations on a rectangle and associated monitor.  The GeoFence will have a trigger of {@link Trigger#Both}.
     *
     * @param testOps operations to perform
     */
    private void doWithRectangle(BiConsumer<Rectangle, AGeoFenceMonitor<?>> testOps) throws ShapeAlreadyExistsException
    {
        final String name = "rectangle";
        final var rectangle = new Rectangle(name, LOCATION, 0, 500, 750);
        final var rectangleMapObject = shapeService.createRectangleMapObject(rectangle);
        final var geoFence = GeoFence.builder().shapeUid(rectangleMapObject.getUid()).trigger(Trigger.Both).build();
        final var monitor = Mockito.spy(new RectangleGeoFenceMonitor(rectangle, geoFence, calculator, alertManager));
        rectangleMapObject.setGeoFence(geoFence);

        testOps.accept(rectangle, monitor);
    }

    /**
     * Perform test operations on a polygon and associated monitor.  The GeoFence will have a trigger of {@link Trigger#Both}.
     *
     * @param testOps operations to perform
     */
    private void doWithPolygon(BiConsumer<Polygon, AGeoFenceMonitor<?>> testOps) throws ShapeAlreadyExistsException
    {
        final String name = "polygon";
        final var polygon = new Polygon(name, makeHexagon(LOCATION));
        final var polygonMapObject = shapeService.createPolygonMapObject(polygon);
        final var geoFence = GeoFence.builder().shapeUid(polygonMapObject.getUid()).trigger(Trigger.Both).build();
        final var monitor = Mockito.spy(new PolygonGeoFenceMonitor(polygon, geoFence, calculator, alertManager));
        polygonMapObject.setGeoFence(geoFence);

        testOps.accept(polygon, monitor);
    }

    /**
     * Move a point to a location calculated from {@link #LOCATION} at the given {@code azimuth} and {@code distance}.
     * Then update the monitor with that new point location.
     *
     * @param point Point to update
     * @param azimuth Azimuth to new location
     * @param distance Distance to new location
     * @param reset Pass {@code true} to reset the monitor's state to think the point is starting at {@link #LOCATION}
     * @param monitor The monitor to operate upon
     * @param shouldAlert Pass {@code true} the monitor should produce an alert based on the point's movement
     * @param shape The shape to expect if the monitor alerts
     * @param trigger The trigger type to expect if the monitor alerts
     */
    private void movePointAndCheck(PointMapItem point, double azimuth, double distance, boolean reset, AGeoFenceMonitor<?> monitor,
                                   boolean shouldAlert, IShape shape, Trigger trigger)
    {
        reset(alertManager);

        if (reset) resetPoint(point, LOCATION, monitor);

        movePointAndUpdateMonitor(point, azimuth, distance, monitor);

        if (shouldAlert)
        {
            verify(alertManager).alert(shape, point, trigger);
        } else
        {
            verify(alertManager, never()).alert(any(), any(), any());
        }
    }

    /**
     * Move a point to a location calculated from {@link #LOCATION} at the given {@code azimuth} and {@code distance}.
     * Call {@link AGeoFenceMonitor#mapItemMoved(PointMapItem)} with the updated point.
     */
    private void movePointAndUpdateMonitor(PointMapItem point, double azimuth, double distance, AGeoFenceMonitor<?> monitor)
    {
        point.setLocation(locationAtDistance(azimuth, distance), false);
        calculator.addOrUpdatePoint(point);

        monitor.mapItemMoved(point);
    }

    /**
     * Create a new {@link PointMapItem} at the given location and add it to the calculator.
     */
    private PointMapItem createPoint(ILocation location)
    {
        final MapObjectSpec spec = MapObjectSpec.builder(IMarkerMapObject.MARKER_FAMILY, IMarkerMapObject.MARKER_TYPE)
                .name(POINT_NAME)
                .cotType("a-f-A")
                .mapObjectOptions(MapObjectOptions.STANDARD_RESOURCE_OPTIONS)
                .location(location)
                .build();

        final PointMapItem point = (PointMapItem) mapObjectManager.getOrCreateMapObject(spec);
        calculator.addOrUpdatePoint(point);
        return point;
    }

    /**
     * Change the point's location and reset the monitor's state.
     */
    private void resetPoint(PointMapItem point, ILocation location, IGeoFenceMonitor monitor)
    {
        point.setLocation(location, false);
        calculator.addOrUpdatePoint(point);
        monitor.setMapItemState(point);
    }

    /**
     * @return a collection of vertices that define an equiangular hexagon at a given origin.
     */
    private List<ILocation> makeHexagon(ILocation origin)
    {
        final List<ILocation> vertices = new ArrayList<>();
        for (int i = 0; i < 6; i++) vertices.add(locationAtDistance(origin, 60 * i, POLYGON_VERTEX_DISTANCE));
        return vertices;
    }

    private ILocation locationAtDistance(double azimuth, double distance)
    {
        return locationAtDistance(LOCATION, azimuth, distance);
    }

    private ILocation locationAtDistance(ILocation start, double azimuth, double distance)
    {
        final var geoPoint = GeoCalculations.pointAtDistance(start, azimuth, distance);
        return new RaptorLocation(geoPoint.getLatitude(), geoPoint.getLongitude(), geoPoint.getAltitude());
    }
}
