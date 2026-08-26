package gov.tak.platform.cot.importer;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.core.comms.cot.CotExporterManager;
import gov.tak.core.comms.cot.CotImporterManager;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.shapes.IMutableRectangle;
import gov.takx.api.shapes.NoSuchShapeException;
import gov.takx.api.shapes.ShapeAlreadyExistsException;
import gov.takx.core.shapes.ShapeService;
import gov.takx.dataservices.shapes.Rectangle;
import gov.takx.dataservices.shapes.Square;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static gov.tak.test.utils.CotTestConstants.RECTANGLE_CORNERS;
import static gov.tak.test.utils.CotTestConstants.RECTANGLE_KEY;
import static gov.tak.test.utils.CotTestConstants.RECTANGLE_NAME;
import static gov.tak.test.utils.CotTestConstants.RECTANGLE_UID;
import static gov.tak.test.utils.CotTestConstants.SHAPE_FILL_COLOR;
import static gov.tak.test.utils.CotTestConstants.SHAPE_LINE_COLOR;
import static gov.tak.test.utils.CotTestConstants.SHAPE_LINE_WIDTH;
import static gov.tak.test.utils.CotTestConstants.ZERO_LOC;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validate {@link RectangleImporter} functionality.
 *
 * @since 3.0
 */
public class RectangleImporterTest extends AShapeImporterTest
{
    @SuppressLogging({RectangleImporter.class, CotImporterManager.class})
    @Test
    public void test_invalid_rectangle() throws IOException
    {
        final CotEvent event = getCotEvent("/cot/bad_rectangle.cot");
        assertThat(event.isValid()).isTrue();

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.FAILURE);
    }

    @Test
    public void test_valid_rectangle() throws IOException, NoSuchShapeException
    {
        final CotEvent event = getCotEvent("/cot/good_rectangle.cot");
        assertThat(event.isValid()).isTrue();

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(0);

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);
        assertThat(mapObjectManager.getMapObjectController(RECTANGLE_KEY)).isNotNull();

        final ShapeService coreShapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final IMutableRectangle rectangle = coreShapeService.getRectangle(RECTANGLE_UID);

        assertThat(rectangle).isNotNull();
        assertThat(rectangle.getName()).isEqualTo(RECTANGLE_NAME);

        final IShapeMapObject<?> shapeMapObject = rectangle.getShapeMapObject();
        assertThat(shapeMapObject.getLatestCotEvent()).isEqualTo(event);
        assertThat(shapeMapObject.getTitle()).isEqualTo(RECTANGLE_NAME);
        assertThat(shapeMapObject.getCotType()).isEqualTo(CotTypes.SHAPE_RECTANGLE);

        final GeoPoint expectedMaxCorner = RECTANGLE_CORNERS.get(3);
        final ILocation maxCorner = rectangle.getMaxCorner();
        assertThat(maxCorner.getLatitude()).isEqualTo(expectedMaxCorner.getLatitude());
        assertThat(maxCorner.getLongitude()).isEqualTo(expectedMaxCorner.getLongitude());

        final GeoPoint expectedMinCorner = RECTANGLE_CORNERS.get(1);
        final ILocation minCorner = rectangle.getMinCorner();
        assertThat(minCorner.getLatitude()).isEqualTo(expectedMinCorner.getLatitude());
        assertThat(minCorner.getLongitude()).isEqualTo(expectedMinCorner.getLongitude());

        assertThat(rectangle.getLineColor()).isEqualTo(SHAPE_LINE_COLOR);
        assertThat(rectangle.getLineWidth()).isEqualTo(SHAPE_LINE_WIDTH);
        assertThat(rectangle.getFillColor()).isEqualTo(SHAPE_FILL_COLOR);

        coreShapeService.deleteShape(RECTANGLE_UID);
    }

    /**
     * @since 3.5
     */
    @Test
    public void test_rotated_rectangle() throws NoSuchShapeException, ShapeAlreadyExistsException
    {
        // Converting from center/width/height/orientation to four-points and back again is subject to
        // loss in our Rectangle calculations and the results that are produced require a larger than normal epsilon.
        final Offset<Double> epsilon = Offset.offset(.15);

        final double rectOrientation = 25;
        final double rectHeight = 20;
        final double rectWidth = 30;

        final ShapeService coreShapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final var rectangleMapObject = coreShapeService.createRectangleMapObject(new Rectangle(RECTANGLE_NAME, ZERO_LOC, rectOrientation, rectHeight, rectWidth));

        final CotExporterManager exporterManager = getBean(CotExporterManager.class);
        final CotEvent event = exporterManager.export(rectangleMapObject);

        assertThat(event.isValid()).isTrue();

        mapObjectManager.deleteMapObject(rectangleMapObject);
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(0);

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);
        assertThat(mapObjectManager.getMapObjectController(rectangleMapObject.getKey())).isNotNull();

        final IMutableRectangle rectangle = coreShapeService.getRectangle(RECTANGLE_NAME);

        assertThat(rectangle).isNotNull();
        assertThat(rectangle.getName()).isEqualTo(RECTANGLE_NAME);

        final IShapeMapObject<?> shapeMapObject = rectangle.getShapeMapObject();
        assertThat(shapeMapObject.getLatestCotEvent()).isEqualTo(event);
        assertThat(shapeMapObject.getTitle()).isEqualTo(RECTANGLE_NAME);
        assertThat(shapeMapObject.getCotType()).isEqualTo(CotTypes.SHAPE_RECTANGLE);

        final GeoPoint location = shapeMapObject.getLocation();

        assertThat(location.getLatitude()).isCloseTo(ZERO_LOC.getLatitude(), epsilon);
        assertThat(location.getLongitude()).isCloseTo(ZERO_LOC.getLongitude(), epsilon);

        assertThat(rectangle.getOrientation()).isCloseTo(rectOrientation, epsilon);
        assertThat(rectangle.getHeight()).isCloseTo(rectHeight, epsilon);
        assertThat(rectangle.getWidth()).isCloseTo(rectWidth, epsilon);

        coreShapeService.deleteShape(RECTANGLE_NAME);
    }

    @Test
    public void test_rectangle_update() throws IOException, NoSuchShapeException
    {
        final CotEvent rectangleEvent = getCotEvent("/cot/good_rectangle.cot");
        final CotEvent updateEvent = getCotEvent("/cot/rectangle_update.cot");

        assertThat(updateEvent.isValid()).isTrue();
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(0);

        assertThat(importerManager.importEvent(rectangleEvent)).isEqualTo(ImportResult.SUCCESS);
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);

        assertThat(importerManager.importEvent(updateEvent)).isEqualTo(ImportResult.SUCCESS);
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);
        assertThat(mapObjectManager.getMapObjectController(RECTANGLE_KEY)).isNotNull();

        final ShapeService coreShapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final IMutableRectangle rectangle = coreShapeService.getRectangle(RECTANGLE_UID);

        assertThat(rectangle).isNotNull();
        assertThat(rectangle.getName()).isEqualTo(RECTANGLE_NAME);

        final IShapeMapObject<?> shapeMapObject = rectangle.getShapeMapObject();
        assertThat(shapeMapObject.getTitle()).isEqualTo(RECTANGLE_NAME);
        assertThat(shapeMapObject.getCotType()).isEqualTo(CotTypes.SHAPE_RECTANGLE);

        final ILocation maxCorner = rectangle.getMaxCorner();
        assertThat(maxCorner.getLatitude()).isEqualTo(21);
        assertThat(maxCorner.getLongitude()).isEqualTo(-90);

        final ILocation minCorner = rectangle.getMinCorner();
        assertThat(minCorner.getLatitude()).isEqualTo(20);
        assertThat(minCorner.getLongitude()).isEqualTo(-92);

        coreShapeService.deleteShape(RECTANGLE_UID);
    }

    @Test
    public void test_square_update() throws ShapeAlreadyExistsException, IOException, NoSuchShapeException
    {
        final int initialSideLength = 1;
        final ShapeService coreShapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final var existingSquare = coreShapeService.createSquareMapObject(new Square(RECTANGLE_UID, ZERO_LOC, 1, initialSideLength));
        final String squareKey = existingSquare.getKey();

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);
        assertThat(mapObjectManager.getMapObjectController(squareKey)).isNotNull();

        final CotEvent updateEvent = getCotEvent("/cot/rectangle_update.cot");
        assertThat(updateEvent.isValid()).isTrue();
        assertThat(importerManager.importEvent(updateEvent)).isEqualTo(ImportResult.SUCCESS);
        assertThat(mapObjectManager.getMapObjectController(squareKey)).isNotNull();
        assertThat(coreShapeService.getRectangle(RECTANGLE_UID)).isEqualTo(existingSquare.getAssociatedShape());
        assertThat(existingSquare.getLocation()).isNotEqualTo(ZERO_LOC);
        assertThat(existingSquare.getSideLength()).isNotEqualTo(initialSideLength);

        coreShapeService.deleteShape(RECTANGLE_UID);
    }
}
