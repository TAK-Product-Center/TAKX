package gov.tak.platform.cot.exporter;

import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.test.utils.ExporterTestUtils;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapeCircleMapObject;
import gov.takx.api.shapes.ShapeAlreadyExistsException;
import gov.takx.core.shapes.ShapeService;
import gov.takx.dataservices.shapes.Circle;
import org.junit.jupiter.api.Test;

import static gov.tak.test.utils.CotTestConstants.CIRCLE_NAME;
import static gov.tak.test.utils.CotTestConstants.SEMI_MAJOR_AXIS;
import static gov.tak.test.utils.CotTestConstants.SHAPE_FILL_COLOR;
import static gov.tak.test.utils.CotTestConstants.SHAPE_LINE_COLOR;
import static gov.tak.test.utils.CotTestConstants.SHAPE_LINE_WIDTH;
import static gov.tak.test.utils.CotTestConstants.ZERO_LOC;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validate {@link CircleExporter} functionality.
 *
 * @since 3.0
 */
public class CircleExporterTest extends AEllipseExporterTest
{
    private final CircleExporter circleExporter = new CircleExporter();

    @Test
    public void test_circle_export() throws ShapeAlreadyExistsException
    {
        final ShapeService shapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final var circleMapObject = shapeService.createCircleMapObject(new Circle("testCircle", ZERO_LOC, SEMI_MAJOR_AXIS));

        circleMapObject.setLineWidth(SHAPE_LINE_WIDTH);
        circleMapObject.setLineColor(SHAPE_LINE_COLOR);
        circleMapObject.setFillColor(SHAPE_FILL_COLOR);

        circleMapObject.setTitle(CIRCLE_NAME);

        // Now convert the Map Object into a CoT event
        final long now = CoordinatedTime.currentTimeMillis();

        final CotEvent event = circleExporter.export(circleMapObject);
        ExporterTestUtils.verifyCommonEventValues(event, now, circleMapObject);

        //noinspection ConstantConditions (verified by ExporterTestUtils)
        checkCotDetails(event, circleMapObject);

        // Now ensure the ellipse parameters are rendered as ellipse attributes
        //   <ellipse minor="70000.0" major="70000.0" angle="0"/>

        checkEllipseParameters(event, SEMI_MAJOR_AXIS, SEMI_MAJOR_AXIS, 0);
    }

    @Test
    public void test_circle_with_anchors() throws ShapeAlreadyExistsException
    {
        final ShapeService shapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final IShapeCircleMapObject circleMapObject = shapeService.createCircleMapObject(new Circle("testCircle", ZERO_LOC, 10));

        // Create and set the anchors
        final GeoPoint centerLocation = new GeoPoint(10, 10);
        final IMapObject centerAnchor = createAnchorMapObject("center", centerLocation);

        final GeoPoint radiusLocation = new GeoPoint(20, 20);
        final IMapObject radiusAnchor = createAnchorMapObject("radius", radiusLocation);
        final double expectedRadius = centerLocation.distanceTo(radiusLocation);

        circleMapObject.setCenterAnchor(centerAnchor);
        circleMapObject.setRadiusAnchor(radiusAnchor);

        // Export it to CoT
        final CotEvent event = circleExporter.export(circleMapObject);
        assertThat(event).isNotNull();
        assertThat(event.isValid()).isTrue();

        checkEllipseParameters(event, expectedRadius, expectedRadius, 0);
        checkRelationLinkExists(event, DetailConstants.LINK_RELATION_CENTER_ANCHOR, "centerAnchor");
        checkRelationLinkExists(event, DetailConstants.LINK_RELATION_RADIUS_ANCHOR, "radiusAnchor");
    }
}
