package gov.tak.platform.cot.exporter;

import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.test.utils.CotTestConstants;
import gov.tak.test.utils.ExporterTestUtils;
import gov.takx.api.shapes.ShapeAlreadyExistsException;
import gov.takx.core.shapes.ShapeService;
import gov.takx.dataservices.shapes.Ellipse;
import org.junit.jupiter.api.Test;

import static gov.tak.test.utils.CotTestConstants.ELLIPSE_NAME;
import static gov.tak.test.utils.CotTestConstants.ELLIPSE_ORIENTATION;
import static gov.tak.test.utils.CotTestConstants.ELLIPSE_ORIENTATION_WITH_OFFSET;
import static gov.tak.test.utils.CotTestConstants.SEMI_MAJOR_AXIS;
import static gov.tak.test.utils.CotTestConstants.SEMI_MINOR_AXIS;
import static gov.tak.test.utils.CotTestConstants.SHAPE_FILL_COLOR;
import static gov.tak.test.utils.CotTestConstants.SHAPE_LINE_COLOR;
import static gov.tak.test.utils.CotTestConstants.SHAPE_LINE_WIDTH;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validate {@link EllipseExporter} functionality.
 *
 * @since 3.0
 */
public class EllipseExporterTest extends AEllipseExporterTest
{
    private final EllipseExporter ellipseExporter = new EllipseExporter();

    @Test
    public void test_ellipse_export() throws ShapeAlreadyExistsException
    {
        final ShapeService shapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final var ellipseMapObject = shapeService.createEllipseMapObject(
                new Ellipse("testEllipse", CotTestConstants.ZERO_LOC, ELLIPSE_ORIENTATION, SEMI_MAJOR_AXIS / 2, SEMI_MINOR_AXIS / 2));

        ellipseMapObject.setLineWidth(SHAPE_LINE_WIDTH);
        ellipseMapObject.setLineColor(SHAPE_LINE_COLOR);
        ellipseMapObject.setFillColor(SHAPE_FILL_COLOR);
        ellipseMapObject.setTitle(ELLIPSE_NAME);

        // Now convert the Map Object back to a CoT event
        final long now = CoordinatedTime.currentTimeMillis();

        final CotEvent event = ellipseExporter.export(ellipseMapObject);
        ExporterTestUtils.verifyCommonEventValues(event, now, ellipseMapObject);

        //noinspection ConstantConditions (verified by ExporterTestUtils)
        checkCotDetails(event, ellipseMapObject);

        // Now ensure the ellipse parameters are rendered as ellipse attributes
        //   <ellipse minor="35000.0" major="25000.0" angle="320"/>
        checkEllipseParameters(event, SEMI_MINOR_AXIS, SEMI_MAJOR_AXIS, ELLIPSE_ORIENTATION_WITH_OFFSET);
    }

    @Test
    public void test_ellipse_with_anchors() throws ShapeAlreadyExistsException
    {
        final ShapeService shapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final var ellipseMapObject = shapeService.createEllipseMapObject(
                new Ellipse("testEllipse", CotTestConstants.ZERO_LOC, ELLIPSE_ORIENTATION, SEMI_MAJOR_AXIS / 2, SEMI_MINOR_AXIS / 2));

        // Create and set the center anchor
        ellipseMapObject.setCenterAnchor(createAnchorMapObject("center", new GeoPoint(10, 10)));

        final CotEvent event = ellipseExporter.export(ellipseMapObject);
        assertThat(event).isNotNull();
        assertThat(event.isValid()).isTrue();

        checkEllipseParameters(event, SEMI_MINOR_AXIS, SEMI_MAJOR_AXIS, ELLIPSE_ORIENTATION_WITH_OFFSET);
        checkRelationLinkExists(event, DetailConstants.LINK_RELATION_CENTER_ANCHOR, "centerAnchor");
    }
}
