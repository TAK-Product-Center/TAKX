package gov.tak.platform.cot.importer;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.core.comms.cot.CotImporterManager;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapeEllipseMapObject;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.shapes.IMutableEllipse;
import gov.takx.core.shapes.ShapeService;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static gov.tak.test.utils.CotTestConstants.ELLIPSE_KEY;
import static gov.tak.test.utils.CotTestConstants.ELLIPSE_NAME;
import static gov.tak.test.utils.CotTestConstants.ELLIPSE_ORIENTATION_WITH_OFFSET;
import static gov.tak.test.utils.CotTestConstants.ELLIPSE_UID;
import static gov.tak.test.utils.CotTestConstants.EPSILON;
import static gov.tak.test.utils.CotTestConstants.SEMI_MAJOR_AXIS;
import static gov.tak.test.utils.CotTestConstants.SEMI_MINOR_AXIS;
import static gov.tak.test.utils.CotTestConstants.SHAPE_CENTER_LAT;
import static gov.tak.test.utils.CotTestConstants.SHAPE_CENTER_LON;
import static gov.tak.test.utils.CotTestConstants.SHAPE_FILL_COLOR;
import static gov.tak.test.utils.CotTestConstants.SHAPE_LINE_COLOR;
import static gov.tak.test.utils.CotTestConstants.SHAPE_LINE_WIDTH;
import static gov.tak.test.utils.CotTestConstants.UPDATED_ELLIPSE_ORIENTATION;
import static gov.tak.test.utils.CotTestConstants.UPDATED_SEMI_MAJOR_AXIS;
import static gov.tak.test.utils.CotTestConstants.UPDATED_SEMI_MINOR_AXIS;
import static gov.tak.test.utils.CotTestConstants.UPDATED_SHAPE_CENTER_LAT;
import static gov.tak.test.utils.CotTestConstants.UPDATED_SHAPE_CENTER_LON;
import static gov.tak.test.utils.CotTestConstants.UPDATED_SHAPE_FILL_COLOR;
import static gov.tak.test.utils.CotTestConstants.UPDATED_SHAPE_LINE_COLOR;
import static gov.tak.test.utils.CotTestConstants.UPDATED_SHAPE_LINE_WIDTH;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validate {@link EllipseImporter} functionality.
 *
 * @since 3.0
 */
public class EllipseImporterTest extends AShapeImporterTest
{
    @SuppressLogging({EllipseImporter.class, AEllipseImporter.class, CotImporterManager.class})
    @Test
    public void test_invalid_ellipse() throws IOException
    {
        final CotEvent event = getCotEvent("/cot/bad_ellipse.cot");
        assertThat(event.isValid()).isTrue();

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.FAILURE);
    }

    @Test
    public void test_valid_ellipse() throws IOException
    {
        final CotEvent event = getCotEvent("/cot/good_ellipse.cot");
        assertThat(event.isValid()).isTrue();

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(0);

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);
        assertThat(mapObjectManager.getMapObjectController(ELLIPSE_KEY)).isNotNull();

        final ShapeService coreShapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final IMutableEllipse ellipse = coreShapeService.getEllipse(ELLIPSE_UID);

        assertThat(ellipse).isNotNull();
        assertThat(ellipse.getName()).isEqualTo(ELLIPSE_NAME);

        assertThat(ellipse.getLineColor().getRGB()).isEqualTo(SHAPE_LINE_COLOR.getRGB());
        assertThat(ellipse.getLineWidth()).isEqualTo(SHAPE_LINE_WIDTH);
        assertThat(ellipse.getFillColor().getRGB()).isEqualTo(SHAPE_FILL_COLOR.getRGB());

        final IShapeMapObject<?> shapeMapObject = ellipse.getShapeMapObject();
        assertThat(shapeMapObject.getLatestCotEvent()).isEqualTo(event);
        assertThat(shapeMapObject.getTitle()).isEqualTo(ELLIPSE_NAME);
        assertThat(shapeMapObject.getCotType()).isEqualTo(CotTypes.SHAPE_ELLIPSE);

        final ILocation center = ellipse.getLocation();
        assertThat(center.getLatitude()).isCloseTo(SHAPE_CENTER_LAT, EPSILON);
        assertThat(center.getLongitude()).isCloseTo(SHAPE_CENTER_LON, EPSILON);

        assertThat(ellipse.getSemiMajorAxis()).isCloseTo(SEMI_MAJOR_AXIS, EPSILON);
        assertThat(ellipse.getSemiMinorAxis()).isCloseTo(SEMI_MINOR_AXIS, EPSILON);
        assertThat(ellipse.getOrientation()).isCloseTo(ELLIPSE_ORIENTATION_WITH_OFFSET, EPSILON);
    }

    @Test
    public void test_ellipse_with_circle_type() throws IOException
    {
        final CotEvent event = getCotEvent("/cot/ellipse_with_circle_type.cot");
        assertThat(event.isValid()).isTrue();

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(0);

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);
        assertThat(mapObjectManager.getMapObjectController(ELLIPSE_KEY)).isNotNull();

        final ShapeService coreShapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final IMutableEllipse ellipse = coreShapeService.getEllipse(ELLIPSE_UID);

        assertThat(ellipse).isNotNull();
        assertThat(ellipse.getName()).isEqualTo(ELLIPSE_NAME);

        assertThat(ellipse.getLineColor().getRGB()).isEqualTo(SHAPE_LINE_COLOR.getRGB());
        assertThat(ellipse.getLineWidth()).isEqualTo(SHAPE_LINE_WIDTH);
        assertThat(ellipse.getFillColor().getRGB()).isEqualTo(SHAPE_FILL_COLOR.getRGB());

        final IShapeMapObject<?> shapeMapObject = ellipse.getShapeMapObject();
        assertThat(shapeMapObject.getTitle()).isEqualTo(ELLIPSE_NAME);
        assertThat(shapeMapObject.getCotType()).isEqualTo(CotTypes.SHAPE_ELLIPSE);

        final ILocation center = ellipse.getLocation();
        assertThat(center.getLatitude()).isCloseTo(SHAPE_CENTER_LAT, EPSILON);
        assertThat(center.getLongitude()).isCloseTo(SHAPE_CENTER_LON, EPSILON);

        assertThat(ellipse.getSemiMajorAxis()).isCloseTo(SEMI_MAJOR_AXIS, EPSILON);
        assertThat(ellipse.getSemiMinorAxis()).isCloseTo(SEMI_MINOR_AXIS, EPSILON);
        assertThat(ellipse.getOrientation()).isCloseTo(ELLIPSE_ORIENTATION_WITH_OFFSET, EPSILON);
    }

    @Test
    public void test_style_override() throws IOException
    {
        checkEventWithStyleOverride("/cot/good_ellipse.cot", ELLIPSE_UID);
    }

    @Test
    public void test_ellipse_update() throws IOException
    {
        final CotEvent ellipseEvent = getCotEvent("/cot/good_ellipse.cot");
        final CotEvent updateEvent = getCotEvent("/cot/ellipse_update.cot");

        assertThat(updateEvent.isValid()).isTrue();
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(0);

        assertThat(importerManager.importEvent(ellipseEvent)).isEqualTo(ImportResult.SUCCESS);
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);

        assertThat(importerManager.importEvent(updateEvent)).isEqualTo(ImportResult.SUCCESS);
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);
        assertThat(mapObjectManager.getMapObjectController(ELLIPSE_KEY)).isNotNull();

        final ShapeService coreShapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final IMutableEllipse ellipse = coreShapeService.getEllipse(ELLIPSE_UID);

        assertThat(ellipse).isNotNull();
        assertThat(ellipse.getName()).isEqualTo(ELLIPSE_NAME);

        assertThat(ellipse.getLineColor().getRGB()).isEqualTo(UPDATED_SHAPE_LINE_COLOR.getRGB());
        assertThat(ellipse.getLineWidth()).isEqualTo(UPDATED_SHAPE_LINE_WIDTH);
        assertThat(ellipse.getFillColor().getRGB()).isEqualTo(UPDATED_SHAPE_FILL_COLOR.getRGB());

        final IShapeMapObject<?> shapeMapObject = ellipse.getShapeMapObject();
        assertThat(shapeMapObject.getTitle()).isEqualTo(ELLIPSE_NAME);
        assertThat(shapeMapObject.getCotType()).isEqualTo(CotTypes.SHAPE_ELLIPSE);

        final ILocation center = ellipse.getLocation();
        assertThat(center.getLatitude()).isCloseTo(UPDATED_SHAPE_CENTER_LAT, EPSILON);
        assertThat(center.getLongitude()).isCloseTo(UPDATED_SHAPE_CENTER_LON, EPSILON);

        assertThat(ellipse.getSemiMajorAxis()).isCloseTo(UPDATED_SEMI_MAJOR_AXIS, EPSILON);
        assertThat(ellipse.getSemiMinorAxis()).isCloseTo(UPDATED_SEMI_MINOR_AXIS, EPSILON);
        assertThat(ellipse.getOrientation()).isCloseTo(UPDATED_ELLIPSE_ORIENTATION, EPSILON);
    }

    @Test
    public void test_ellipse_with_anchor_links() throws IOException
    {
        final CotEvent event = getCotEvent("/cot/ellipse_with_anchor_links.cot");
        assertThat(event.isValid()).isTrue();

        // Create the Map Object that is the target of the link
        final GeoPoint centerLocation = new GeoPoint(10, 10);
        final IMapObject centerAnchor = createAnchorMapObject("center", centerLocation);

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(2);
        final var shapeMapObject = (IShapeEllipseMapObject) mapObjectManager.getMapObject(ELLIPSE_KEY);

        assertThat(shapeMapObject).isNotNull();
        assertThat(shapeMapObject.getLocation()).isEqualTo(centerLocation);
        assertThat(shapeMapObject.getCenterAnchor()).isSameAs(centerAnchor);

        final IMutableEllipse ellipse = shapeMapObject.getAssociatedShape();

        assertThat(ellipse).isNotNull();
        assertThat(ellipse.getName()).isEqualTo(ELLIPSE_NAME);
        assertThat(ellipse.getSemiMajorAxis()).isCloseTo(SEMI_MAJOR_AXIS, EPSILON);
        assertThat(ellipse.getSemiMinorAxis()).isCloseTo(SEMI_MINOR_AXIS, EPSILON);
        assertThat(ellipse.getOrientation()).isCloseTo(ELLIPSE_ORIENTATION_WITH_OFFSET, EPSILON);
    }

    @Test
    public void test_ellipse_with_rings() throws IOException
    {
        final CotEvent event = getCotEvent("/cot/ellipse_with_rings.cot");
        assertThat(event.isValid()).isTrue();

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);
        final var ellipseMapObject = (IShapeEllipseMapObject) mapObjectManager.getMapObject(ELLIPSE_KEY);

        assertThat(ellipseMapObject).isNotNull();
        assertThat(ellipseMapObject.getRingCount()).isEqualTo(3);
        assertThat(ellipseMapObject.getSemiMajorAxis()).isEqualTo(37); // Should be value from the smallest ring
        assertThat(ellipseMapObject.getSemiMinorAxis()).isEqualTo(18.5); // Should be value from the smallest ring
        assertThat(ellipseMapObject.getOrientation()).isEqualTo(150);
    }
}
