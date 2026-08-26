package gov.takx.shapes;

import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IShapeCircleMapObject;
import gov.takx.api.mapobject.IShapeEllipseMapObject;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.shapes.IMutableCircle;
import gov.takx.api.shapes.IMutableEllipse;
import gov.takx.api.shapes.IShape;
import gov.takx.api.shapes.NoSuchShapeException;
import gov.takx.api.shapes.ShapeAlreadyExistsException;
import gov.takx.core.shapes.ShapeService;
import gov.takx.core.shapes.ShapeServiceFactory;
import gov.takx.dataservices.shapes.Circle;
import gov.takx.dataservices.shapes.Ellipse;
import gov.takx.platform.utils.ShapeUtils;
import gov.takx.test.AIntegrationTest;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verify shape anchors for center and radius.
 *
 * @since 3.0
 */
public class ShapeAnchorTest extends AIntegrationTest
{
    private static final ILocation START_CENTER = new RaptorLocation(10, 20);
    private static final ILocation START_ANCHOR_CENTER = new RaptorLocation(20, 30);
    private static final ILocation NEW_LOCATION = new GeoPoint(0, 0);
    private static final double START_RADIUS = 100;

    // Variance due to geographic distance/path calculations
    private static final Offset<Double> EPSILON = Offset.offset(1e-6);

    private final IMapObjectManager mapObjectManager = getBean(IMapObjectManager.class);
    private final ShapeServiceFactory shapeServiceFactory = getBean(ShapeServiceFactory.class);

    @AfterEach
    public void cleanupAfterMethod() throws NoSuchShapeException
    {
        final ShapeService shapeService = shapeServiceFactory.getOrCreateCoreShapeService();

        for (IShape shape : shapeService.getShapes()) shapeService.deleteShape(shape.getName());

        deleteAllMapObjects();
    }

    @Test
    public void test_shape_moves_with_center_anchor() throws ShapeAlreadyExistsException
    {
        final IMapObject anchor = createAnchor("center", START_ANCHOR_CENTER);
        final var ellipseMapObject = createEllipseWithCenterAnchor(anchor);

        anchor.setLocation(NEW_LOCATION, true);

        assertThat(ellipseMapObject.getLocation()).isEqualTo(NEW_LOCATION); // Shape should have moved on update
    }

    @Test
    public void test_center_anchor_moves_when_shape_updated() throws ShapeAlreadyExistsException
    {
        final IMapObject anchor = createAnchor("center", START_ANCHOR_CENTER);
        final IMutableEllipse ellipse = createEllipseWithCenterAnchor(anchor);

        ellipse.setLocation(NEW_LOCATION);

        assertThat(anchor.getLocation()).isEqualTo(NEW_LOCATION); // Anchor should have moved on update
    }

    @Test
    public void test_removed_center_anchor_is_not_updated() throws ShapeAlreadyExistsException
    {
        final IMapObject anchor = createAnchor("center", START_ANCHOR_CENTER);
        final var ellipseMapObject = createEllipseWithCenterAnchor(anchor);

        ellipseMapObject.setCenterAnchor(null); // Clear the center anchor

        ellipseMapObject.setLocation(NEW_LOCATION);
        assertThat(anchor.getLocation()).isEqualTo(START_ANCHOR_CENTER); // Anchor should not have moved on update

        // And, moving the anchor should not affect the shape
        anchor.setLocation(START_CENTER, true);
        assertThat(ellipseMapObject.getLocation()).isEqualTo(NEW_LOCATION);
    }

    @Test
    public void test_radius_anchor_moves_when_shape_moved() throws ShapeAlreadyExistsException
    {
        validateRadiusAnchorChange(circle -> {
            final double radius = circle.getRadius();
            circle.setLocation(NEW_LOCATION);
            return radius;
        });
    }

    @Test
    public void test_radius_anchor_moves_when_radius_updated() throws ShapeAlreadyExistsException
    {
        validateRadiusAnchorChange(circle -> {
            final double newRadius = circle.getRadius() * 2;
            circle.setRadius(newRadius);
            return newRadius;
        });
    }

    @Test
    public void test_radius_updates_when_anchor_moved() throws ShapeAlreadyExistsException
    {
        final IMapObject anchor = createAnchor("radius", START_ANCHOR_CENTER);
        final IMutableCircle circle = createCircleWithRadiusAnchor(anchor);

        anchor.setLocation(NEW_LOCATION, true);

        assertThat(circle.getRadius()).isEqualTo(ShapeUtils.distanceTo(START_CENTER, NEW_LOCATION));
    }

    @Test
    public void test_removed_radius_anchor_is_not_updated() throws ShapeAlreadyExistsException
    {
        final IMapObject anchor = createAnchor("radius", START_ANCHOR_CENTER);
        final IMutableCircle circle = createCircleWithRadiusAnchor(anchor);
        final IShapeCircleMapObject shapeMapObject = circle.getShapeMapObject();

        final GeoPoint anchorLocation = anchor.getLocation();

        shapeMapObject.setRadiusAnchor(null); // Clear the anchor

        circle.setRadius(circle.getRadius() * 2);
        assertThat(anchor.getLocation()).isEqualTo(anchorLocation); // Shouldn't move

        circle.setLocation(NEW_LOCATION);
        assertThat(anchor.getLocation()).isEqualTo(anchorLocation); // Shouldn't move

        anchor.setLocation(START_CENTER, true);
        assertThat(circle.getLocation()).isEqualTo(NEW_LOCATION); // Shouldn't move
    }

    /**
     * Construct a circle with a radius anchor, apply the given update function (which should return the expected
     * radius), and validate that the anchor marker has been properly updated.
     *
     * @param radiusUpdater Update function
     */
    private void validateRadiusAnchorChange(Function<IMutableCircle, Double> radiusUpdater) throws ShapeAlreadyExistsException
    {
        final IMapObject anchor = createAnchor("radius", START_ANCHOR_CENTER);
        final IMutableCircle circle = createCircleWithRadiusAnchor(anchor);
        final IShapeMapObject<?> shapeMapObject = circle.getShapeMapObject();

        final double startBearing = ShapeUtils.bearingTo(START_CENTER, START_ANCHOR_CENTER);

        final Double expectedRadius = radiusUpdater.apply(circle);

        final GeoPoint mapObjectLocation = shapeMapObject.getLocation();
        final GeoPoint newAnchorLocation = anchor.getLocation();
        final double newDistance = mapObjectLocation.distanceTo(newAnchorLocation);
        final double newBearing = mapObjectLocation.bearingTo(newAnchorLocation);

        assertThat(newDistance).isCloseTo(expectedRadius, EPSILON);
        assertThat(newBearing).isCloseTo(startBearing, EPSILON); // Bearing should not have changed
    }

    /**
     * Create a Map Object to be used as an anchor.
     *
     * @param type     MO Type
     * @param location Initial location for MO
     * @return New MO
     */
    private IMapObject createAnchor(String type, ILocation location)
    {
        final MapObjectSpec spec = MapObjectSpec.builder("test", "test_" + type, "1")
                .location(location)
                .uid(type + "Anchor")
                .build();
        return mapObjectManager.getOrCreateMapObject(spec);
    }

    /**
     * Create an ellipse with the given anchor as its center anchor.
     *
     * @param anchor Center anchor to install
     * @return Created ellipse
     */
    private IShapeEllipseMapObject createEllipseWithCenterAnchor(IMapObject anchor) throws ShapeAlreadyExistsException
    {
        final ShapeService shapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final var ellipseMapObject = shapeService.createEllipseMapObject(new Ellipse("test", START_CENTER, 0, 5, 10));

        assertThat(ellipseMapObject.getLocation()).isEqualTo(START_CENTER);

        ellipseMapObject.setCenterAnchor(anchor);

        assertThat(ellipseMapObject.getCenterAnchor()).isSameAs(anchor);
        assertThat(ellipseMapObject.getLocation()).isEqualTo(START_ANCHOR_CENTER); // Should have moved on set

        return ellipseMapObject;
    }

    /**
     * Create a circle with the given anchor as its radius anchor.
     *
     * @param anchor Radius anchor to install
     * @return Created circle
     */
    private IMutableCircle createCircleWithRadiusAnchor(IMapObject anchor) throws ShapeAlreadyExistsException
    {
        final ShapeService shapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final var circleMapObject = shapeService.createCircleMapObject(new Circle("test", START_CENTER, START_RADIUS));

        assertThat(circleMapObject.getLocation()).isEqualTo(START_CENTER);
        assertThat(circleMapObject.getRadius()).isEqualTo(START_RADIUS);

        circleMapObject.setRadiusAnchor(anchor);

        assertThat(circleMapObject.getRadiusAnchor()).isSameAs(anchor);
        assertThat(circleMapObject.getCenterAnchor()).isNull();

        // Setting the radius anchor should update the radius.
        // Calculate the distance between the original center and the radius anchor location,
        // that should be the new radius.

        final double distance = ShapeUtils.distanceTo(circleMapObject.getLocation(), anchor.getLocation());
        assertThat(circleMapObject.getRadius()).isEqualTo(distance); // Should have been updated on set

        return circleMapObject;
    }
}
