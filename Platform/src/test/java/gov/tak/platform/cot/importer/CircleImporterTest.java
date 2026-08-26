package gov.tak.platform.cot.importer;

import com.google.common.io.Resources;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.core.comms.cot.CotImporterManager;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapeCircleMapObject;
import gov.takx.api.shapes.IMutableCircle;
import gov.takx.core.shapes.ShapeService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static gov.tak.test.utils.CotTestConstants.CIRCLE_KEY;
import static gov.tak.test.utils.CotTestConstants.CIRCLE_NAME;
import static gov.tak.test.utils.CotTestConstants.CIRCLE_RADIUS;
import static gov.tak.test.utils.CotTestConstants.CIRCLE_UID;
import static gov.tak.test.utils.CotTestConstants.EPSILON;
import static gov.tak.test.utils.CotTestConstants.LARGE_CIRCLE_RADIUS;
import static gov.tak.test.utils.CotTestConstants.SHAPE_CENTER_LAT;
import static gov.tak.test.utils.CotTestConstants.SHAPE_CENTER_LON;
import static gov.tak.test.utils.CotTestConstants.SHAPE_FILL_COLOR;
import static gov.tak.test.utils.CotTestConstants.SHAPE_LINE_COLOR;
import static gov.tak.test.utils.CotTestConstants.SHAPE_LINE_WIDTH;
import static gov.tak.test.utils.CotTestConstants.UPDATED_SHAPE_CENTER_LAT;
import static gov.tak.test.utils.CotTestConstants.UPDATED_SHAPE_CENTER_LON;
import static gov.tak.test.utils.CotTestConstants.UPDATED_SHAPE_FILL_COLOR;
import static gov.tak.test.utils.CotTestConstants.UPDATED_SHAPE_LINE_COLOR;
import static gov.tak.test.utils.CotTestConstants.UPDATED_SHAPE_LINE_WIDTH;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validate {@link CircleImporter} functionality.
 *
 * @since 3.0
 */
public class CircleImporterTest extends AShapeImporterTest
{
    @SuppressLogging({CircleImporter.class, AEllipseImporter.class, CotImporterManager.class})
    @Test
    public void test_invalid_circle() throws IOException
    {
        final CotEvent event = getCotEvent("/cot/bad_circle.cot");
        assertThat(event.isValid()).isTrue();

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.FAILURE);
    }

    @Test
    public void test_valid_circle() throws IOException
    {
        final CotEvent event = getCotEvent("/cot/good_circle.cot");
        assertThat(event.isValid()).isTrue();

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(0);

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);
        assertThat(mapObjectManager.getMapObjectController(CIRCLE_KEY)).isNotNull();

        final ShapeService coreShapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final IMutableCircle circle = coreShapeService.getCircle(CIRCLE_UID);

        assertThat(circle).isNotNull();
        assertThat(circle.getName()).isEqualTo(CIRCLE_NAME);

        assertThat(circle.getLineColor().getRGB()).isEqualTo(SHAPE_LINE_COLOR.getRGB());
        assertThat(circle.getLineWidth()).isEqualTo(SHAPE_LINE_WIDTH);
        assertThat(circle.getFillColor().getRGB()).isEqualTo(SHAPE_FILL_COLOR.getRGB());

        final var shapeMapObject = circle.getShapeMapObject();
        assertThat(shapeMapObject.getLatestCotEvent()).isEqualTo(event);
        assertThat(shapeMapObject.getTitle()).isEqualTo(CIRCLE_NAME);
        assertThat(shapeMapObject.getCotType()).isEqualTo(CotTypes.SHAPE_CIRCLE);

        final ILocation center = circle.getLocation();
        assertThat(center.getLatitude()).isCloseTo(SHAPE_CENTER_LAT, EPSILON);
        assertThat(center.getLongitude()).isCloseTo(SHAPE_CENTER_LON, EPSILON);

        assertThat(circle.getRadius()).isCloseTo(LARGE_CIRCLE_RADIUS, EPSILON);

        assertThat(circle.isPublished()).isFalse();
    }

    @Test
    public void test_style_override() throws IOException
    {
        checkEventWithStyleOverride("/cot/good_circle.cot", CIRCLE_UID);
    }

    @Test
    public void test_circle_update() throws IOException
    {
        final CotEvent circleEvent = getCotEvent("/cot/good_circle.cot");
        final CotEvent updateEvent = getCotEvent("/cot/circle_update.cot");

        assertThat(updateEvent.isValid()).isTrue();
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(0);

        assertThat(importerManager.importEvent(circleEvent)).isEqualTo(ImportResult.SUCCESS);
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);

        assertThat(importerManager.importEvent(updateEvent)).isEqualTo(ImportResult.SUCCESS);
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);
        assertThat(mapObjectManager.getMapObjectController(CIRCLE_KEY)).isNotNull();

        final ShapeService coreShapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final IMutableCircle circle = coreShapeService.getCircle(CIRCLE_UID);

        assertThat(circle).isNotNull();
        assertThat(circle.getName()).isEqualTo(CIRCLE_NAME);

        assertThat(circle.getLineColor().getRGB()).isEqualTo(UPDATED_SHAPE_LINE_COLOR.getRGB());
        assertThat(circle.getLineWidth()).isEqualTo(UPDATED_SHAPE_LINE_WIDTH);
        assertThat(circle.getFillColor().getRGB()).isEqualTo(UPDATED_SHAPE_FILL_COLOR.getRGB());

        final var shapeMapObject = circle.getShapeMapObject();
        assertThat(shapeMapObject.getTitle()).isEqualTo(CIRCLE_NAME);
        assertThat(shapeMapObject.getCotType()).isEqualTo(CotTypes.SHAPE_CIRCLE);

        final ILocation center = circle.getLocation();
        assertThat(center.getLatitude()).isCloseTo(UPDATED_SHAPE_CENTER_LAT, EPSILON);
        assertThat(center.getLongitude()).isCloseTo(UPDATED_SHAPE_CENTER_LON, EPSILON);
        assertThat(circle.getRadius()).isCloseTo(CIRCLE_RADIUS, EPSILON);
    }

    @Test
    public void test_circle_with_anchor_links() throws IOException
    {
        final CotEvent event = getCotEvent("/cot/circle_with_anchor_links.cot");
        assertThat(event.isValid()).isTrue();

        // Create the Map Objects that are the targets of the links
        final GeoPoint centerLocation = new GeoPoint(10, 10);
        final IMapObject centerAnchor = createAnchorMapObject("center", centerLocation);

        final GeoPoint radiusLocation = new GeoPoint(20, 20);
        final IMapObject radiusAnchor = createAnchorMapObject("radius", radiusLocation);
        final double expectedRadius = centerLocation.distanceTo(radiusLocation);

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(3);
        final var circleMapObject = (IShapeCircleMapObject) mapObjectManager.getMapObject(CIRCLE_KEY);

        assertThat(circleMapObject).isNotNull();
        assertThat(circleMapObject.getLocation()).isEqualTo(centerLocation);
        assertThat(circleMapObject.getCenterAnchor()).isSameAs(centerAnchor);
        assertThat(circleMapObject.getRadiusAnchor()).isSameAs(radiusAnchor);

        final IMutableCircle circle = circleMapObject.getAssociatedShape();

        assertThat(circle).isNotNull();
        assertThat(circle.getName()).isEqualTo(CIRCLE_NAME);
        assertThat(circle.getRadius()).isEqualTo(expectedRadius);
    }

    @Test
    public void test_circle_with_rings() throws IOException
    {
        final CotEvent event = getCotEvent("/cot/circle_with_rings.cot");
        assertThat(event.isValid()).isTrue();

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);
        final var circleMapObject = (IShapeCircleMapObject) mapObjectManager.getMapObject(CIRCLE_KEY);

        assertThat(circleMapObject).isNotNull();
        assertThat(circleMapObject.getRingCount()).isEqualTo(3);
        assertThat(circleMapObject.getRadius()).isEqualTo(37); // Should be value from the smallest ring
    }

    @Test
    public void test_range_and_bearing_circle() throws IOException
    {
        // This is just another CoT type (u-r-b-c-c) that is exactly the same as a circle
        final CotEvent event = getCotEvent("/cot/range_ring.cot");
        assertThat(event.isValid()).isTrue();

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);
        final var circleMapObject = (IShapeCircleMapObject) mapObjectManager.getMapObject(CIRCLE_KEY);

        assertThat(circleMapObject).isNotNull();
        assertThat(circleMapObject.getRingCount()).isEqualTo(1);
        assertThat(circleMapObject.getRadius()).isEqualTo(3411);
    }

    /**
     * @since 3.5
     */
    @SuppressWarnings("UnstableApiUsage")
    @Test
    public void test_circle_with_vertical_height() throws IOException
    {
        final URL url = Objects.requireNonNull(getClass().getResource("/cot/good_circle.cot"));
        final String eventXml = Resources.toString(url, StandardCharsets.UTF_8);

        final String override = "    <height value=\"200.0\"/>\n    </detail>";

        final String newEventXml = eventXml.replaceFirst("</detail>", override);
        final CotEvent event = CotEvent.parse(newEventXml);

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);

        final ShapeService coreShapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final IMutableCircle circle = coreShapeService.getCircle(CIRCLE_UID);

        assertThat(circle).isNotNull();
        assertThat(circle.isVerticalHeightEnabled()).isTrue();
        assertThat(circle.getVerticalHeight()).isEqualTo(200);
    }

    /**
     * @since 3.5
     */
    @SuppressWarnings("UnstableApiUsage")
    @Test
    public void test_circle_as_geofence() throws IOException
    {
        final URL url = Objects.requireNonNull(getClass().getResource("/cot/good_circle.cot"));
        final String eventXml = Resources.toString(url, StandardCharsets.UTF_8);

        final String override = "    <__geofence tracking=\"False\" trigger=\"Entry\" monitor=\"All\" boundingSphere=\"75000\" elevationMonitored=\"false\"/>\n    </detail>";

        final String newEventXml = eventXml.replaceFirst("</detail>", override);
        final CotEvent event = CotEvent.parse(newEventXml);

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);

        final ShapeService coreShapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final IMutableCircle circle = coreShapeService.getCircle(CIRCLE_UID);

        assertThat(circle).isNotNull();
        assertThat(circle.isPublished()).isTrue();
    }
}
