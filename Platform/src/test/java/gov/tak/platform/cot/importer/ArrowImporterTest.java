package gov.tak.platform.cot.importer;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.core.comms.cot.CotImporterManager;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapeArrowMapObject;
import gov.takx.api.shapes.IMutableArrow;
import gov.takx.core.shapes.ShapeService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static gov.tak.test.utils.CotTestConstants.ARROW_KEY;
import static gov.tak.test.utils.CotTestConstants.ARROW_NAME;
import static gov.tak.test.utils.CotTestConstants.ARROW_UID;
import static gov.tak.test.utils.CotTestConstants.BEARING;
import static gov.tak.test.utils.CotTestConstants.EPSILON;
import static gov.tak.test.utils.CotTestConstants.RANGE;
import static gov.tak.test.utils.CotTestConstants.SHAPE_CENTER_LAT;
import static gov.tak.test.utils.CotTestConstants.SHAPE_CENTER_LON;
import static gov.tak.test.utils.CotTestConstants.SHAPE_LINE_COLOR;
import static gov.tak.test.utils.CotTestConstants.SHAPE_LINE_WIDTH;
import static gov.tak.test.utils.CotTestConstants.UPDATED_BEARING;
import static gov.tak.test.utils.CotTestConstants.UPDATED_RANGE;
import static gov.tak.test.utils.CotTestConstants.UPDATED_SHAPE_CENTER_LAT;
import static gov.tak.test.utils.CotTestConstants.UPDATED_SHAPE_CENTER_LON;
import static gov.tak.test.utils.CotTestConstants.UPDATED_SHAPE_LINE_COLOR;
import static gov.tak.test.utils.CotTestConstants.UPDATED_SHAPE_LINE_WIDTH;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validate {@link ArrowImporter} functionality.
 *
 * @since 3.0
 */
public class ArrowImporterTest extends AShapeImporterTest
{
    @SuppressLogging({ArrowImporter.class, CotImporterManager.class})
    @Test
    public void test_invalid_arrow() throws IOException
    {
        final CotEvent event = getCotEvent("/cot/bad_arrow.cot");
        assertThat(event.isValid()).isTrue();

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.FAILURE);
    }

    @Test
    public void test_valid_arrow() throws IOException
    {
        final CotEvent event = getCotEvent("/cot/good_arrow.cot");
        assertThat(event.isValid()).isTrue();

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(0);

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);
        assertThat(mapObjectManager.getMapObjectController(ARROW_KEY)).isNotNull();

        final ShapeService coreShapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final IMutableArrow arrow = coreShapeService.getArrow(ARROW_UID);

        assertThat(arrow).isNotNull();
        assertThat(arrow.getName()).isEqualTo(ARROW_NAME);

        assertThat(arrow.getLineColor().getRGB()).isEqualTo(SHAPE_LINE_COLOR.getRGB());
        assertThat(arrow.getLineWidth()).isEqualTo(SHAPE_LINE_WIDTH);

        final IShapeArrowMapObject shapeMapObject = arrow.getShapeMapObject();
        assertThat(shapeMapObject.getLatestCotEvent()).isEqualTo(event);
        assertThat(shapeMapObject.getTitle()).isEqualTo(ARROW_NAME);
        assertThat(shapeMapObject.getCotType()).isEqualTo(CotTypes.SHAPE_ARROW);

        final List<ILocation> vertices = arrow.getVertices();
        assertThat(vertices).hasSize(2);
        assertThat(vertices.get(0).getLatitude()).isCloseTo(SHAPE_CENTER_LAT, EPSILON);
        assertThat(vertices.get(0).getLongitude()).isCloseTo(SHAPE_CENTER_LON, EPSILON);

        assertThat(arrow.getBearing()).isCloseTo(BEARING, EPSILON);
        assertThat(arrow.getRange()).isCloseTo(RANGE, EPSILON);
    }

    @Test
    public void test_arrow_update() throws IOException
    {
        final CotEvent arrowEvent = getCotEvent("/cot/good_arrow.cot");
        final CotEvent updateEvent = getCotEvent("/cot/arrow_update.cot");

        assertThat(updateEvent.isValid()).isTrue();
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(0);

        assertThat(importerManager.importEvent(arrowEvent)).isEqualTo(ImportResult.SUCCESS);
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);

        assertThat(importerManager.importEvent(updateEvent)).isEqualTo(ImportResult.SUCCESS);
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);
        assertThat(mapObjectManager.getMapObjectController(ARROW_KEY)).isNotNull();

        final ShapeService coreShapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final IMutableArrow arrow = coreShapeService.getArrow(ARROW_UID);

        assertThat(arrow).isNotNull();
        assertThat(arrow.getName()).isEqualTo(ARROW_NAME);

        assertThat(arrow.getLineColor().getRGB()).isEqualTo(UPDATED_SHAPE_LINE_COLOR.getRGB());
        assertThat(arrow.getLineWidth()).isEqualTo(UPDATED_SHAPE_LINE_WIDTH);

        final IShapeArrowMapObject shapeMapObject = arrow.getShapeMapObject();
        assertThat(shapeMapObject.getTitle()).isEqualTo(ARROW_NAME);
        assertThat(shapeMapObject.getCotType()).isEqualTo(CotTypes.SHAPE_ARROW);

        final List<ILocation> vertices = arrow.getVertices();
        assertThat(vertices).hasSize(2);
        assertThat(vertices.get(0).getLatitude()).isCloseTo(UPDATED_SHAPE_CENTER_LAT, EPSILON);
        assertThat(vertices.get(0).getLongitude()).isCloseTo(UPDATED_SHAPE_CENTER_LON, EPSILON);

        assertThat(arrow.getBearing()).isCloseTo(UPDATED_BEARING, EPSILON);
        assertThat(arrow.getRange()).isCloseTo(UPDATED_RANGE, EPSILON);
    }

    @Test
    public void test_arrow_with_anchor_links() throws IOException
    {
        final CotEvent event = getCotEvent("/cot/arrow_with_anchor_links.cot");
        assertThat(event.isValid()).isTrue();

        // Create the Map Objects that are the targets of the links
        final GeoPoint startLocation = new GeoPoint(10, 10);
        final IMapObject startAnchor = createAnchorMapObject("start", startLocation);

        final GeoPoint rangeLocation = new GeoPoint(20, 20);
        final IMapObject rangeAnchor = createAnchorMapObject("range", rangeLocation);
        final double expectedRange = startLocation.distanceTo(rangeLocation);

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(3);
        final var arrowMapObject = (IShapeArrowMapObject) mapObjectManager.getMapObject(ARROW_KEY);

        assertThat(arrowMapObject).isNotNull();

        final List<ILocation> vertices = arrowMapObject.getVertices();
        assertThat(vertices).hasSize(2);
        assertThat(vertices.get(0)).isEqualTo(startLocation);
        assertThat(arrowMapObject.getStartAnchor()).isSameAs(startAnchor);
        assertThat(arrowMapObject.getRangeAnchor()).isSameAs(rangeAnchor);

        final IMutableArrow arrow = arrowMapObject.getAssociatedShape();

        assertThat(arrow).isNotNull();
        assertThat(arrow.getName()).isEqualTo(ARROW_NAME);
        assertThat(arrow.getRange()).isEqualTo(expectedRange);
    }
}
