package gov.tak.platform.cot.importer;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.core.comms.cot.CotImporterManager;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.shapes.IMutablePolygon;
import gov.takx.api.shapes.IMutablePolyline;
import gov.takx.api.shapes.NoSuchShapeException;
import gov.takx.core.shapes.ShapeService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static gov.tak.test.utils.CotTestConstants.POLYGON_KEY;
import static gov.tak.test.utils.CotTestConstants.POLYGON_NAME;
import static gov.tak.test.utils.CotTestConstants.POLYGON_POINTS;
import static gov.tak.test.utils.CotTestConstants.POLYGON_UID;
import static gov.tak.test.utils.CotTestConstants.POLYLINE_KEY;
import static gov.tak.test.utils.CotTestConstants.POLYLINE_NAME;
import static gov.tak.test.utils.CotTestConstants.POLYLINE_POINTS;
import static gov.tak.test.utils.CotTestConstants.POLYLINE_UID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validate {@link PolyShapeImporter} functionality.
 *
 * @since 3.0
 */
public class PolyShapeImporterTest extends AShapeImporterTest
{
    @SuppressLogging({PolyShapeImporter.class, CotImporterManager.class})
    @Test
    public void test_invalid_polyshape() throws IOException
    {
        CotEvent event = getCotEvent("/cot/bad_polyshape.cot");
        assertThat(event.isValid()).isTrue();

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.FAILURE);

        event = getCotEvent("/cot/bad_polyshape_no_points.cot");
        assertThat(event.isValid()).isTrue();

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.FAILURE);
    }

    @Test
    public void test_valid_polyline() throws IOException, NoSuchShapeException
    {
        final CotEvent event = getCotEvent("/cot/good_polyline.cot");
        assertThat(event.isValid()).isTrue();

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(0);

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);
        assertThat(mapObjectManager.getMapObjectController(POLYLINE_KEY)).isNotNull();

        final ShapeService coreShapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final IMutablePolyline polyline = coreShapeService.getPolyline(POLYLINE_UID);

        assertThat(polyline).isNotNull();
        assertThat(polyline.getName()).isEqualTo(POLYLINE_NAME);
        assertThat(polyline.getLocation()).isEqualTo(POLYGON_POINTS.get(0));

        final IShapeMapObject<?> shapeMapObject = polyline.getShapeMapObject();
        assertThat(shapeMapObject.getLatestCotEvent()).isEqualTo(event);
        assertThat(shapeMapObject.getTitle()).isEqualTo(POLYLINE_NAME);
        assertThat(shapeMapObject.getCotType()).isEqualTo(CotTypes.SHAPE_POLYSHAPE);

        final List<ILocation> vertices = polyline.getVertices();
        assertThat(vertices).containsExactly(POLYLINE_POINTS.toArray(new GeoPoint[0]));

        coreShapeService.deleteShape(POLYLINE_UID);
    }

    @Test
    public void test_valid_polygon() throws IOException, NoSuchShapeException
    {
        final CotEvent event = getCotEvent("/cot/good_polygon.cot");
        assertThat(event.isValid()).isTrue();

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(0);

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);
        assertThat(mapObjectManager.getMapObjectController(POLYGON_KEY)).isNotNull();

        final ShapeService coreShapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final IMutablePolygon polygon = coreShapeService.getPolygon(POLYGON_UID);

        assertThat(polygon).isNotNull();
        assertThat(polygon.getName()).isEqualTo(POLYGON_NAME);

        final IShapeMapObject<?> shapeMapObject = polygon.getShapeMapObject();
        assertThat(shapeMapObject.getTitle()).isEqualTo(POLYGON_NAME);
        assertThat(shapeMapObject.getCotType()).isEqualTo(CotTypes.SHAPE_POLYSHAPE);

        assertThat(polygon.getVertices()).containsExactly(POLYGON_POINTS.toArray(new GeoPoint[0]));

        coreShapeService.deleteShape(POLYGON_UID);
    }

    @Test
    public void test_polyline_update() throws IOException, NoSuchShapeException
    {
        final CotEvent polylineEvent = getCotEvent("/cot/good_polyline.cot");
        final CotEvent updateEvent = getCotEvent("/cot/polyline_update.cot");

        assertThat(updateEvent.isValid()).isTrue();
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(0);

        assertThat(importerManager.importEvent(polylineEvent)).isEqualTo(ImportResult.SUCCESS);
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);

        assertThat(importerManager.importEvent(updateEvent)).isEqualTo(ImportResult.SUCCESS);
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);
        assertThat(mapObjectManager.getMapObjectController(POLYLINE_KEY)).isNotNull();

        final ShapeService coreShapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final IMutablePolyline polyline = coreShapeService.getPolyline(POLYLINE_UID);

        assertThat(polyline).isNotNull();
        assertThat(polyline.getName()).isEqualTo(POLYLINE_NAME);

        final IShapeMapObject<?> shapeMapObject = polyline.getShapeMapObject();
        assertThat(shapeMapObject.getTitle()).isEqualTo(POLYLINE_NAME);
        assertThat(shapeMapObject.getCotType()).isEqualTo(CotTypes.SHAPE_POLYSHAPE);

        final List<ILocation> vertices = polyline.getVertices();
        assertThat(vertices).containsExactly(
                new GeoPoint(80, -110),
                new GeoPoint(90, -120));

        coreShapeService.deleteShape(POLYLINE_UID);
    }

    @Test
    public void test_polygon_update() throws IOException, NoSuchShapeException
    {
        final CotEvent polygonEvent = getCotEvent("/cot/good_polygon.cot");
        final CotEvent updateEvent = getCotEvent("/cot/polygon_update.cot");

        assertThat(updateEvent.isValid()).isTrue();
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(0);

        assertThat(importerManager.importEvent(polygonEvent)).isEqualTo(ImportResult.SUCCESS);
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);

        assertThat(importerManager.importEvent(updateEvent)).isEqualTo(ImportResult.SUCCESS);
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);
        assertThat(mapObjectManager.getMapObjectController(POLYGON_KEY)).isNotNull();

        final ShapeService coreShapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final IMutablePolygon polygon = coreShapeService.getPolygon(POLYGON_UID);

        assertThat(polygon).isNotNull();
        assertThat(polygon.getName()).isEqualTo(POLYGON_NAME);

        final IShapeMapObject<?> shapeMapObject = polygon.getShapeMapObject();
        assertThat(shapeMapObject.getTitle()).isEqualTo(POLYGON_NAME);
        assertThat(shapeMapObject.getCotType()).isEqualTo(CotTypes.SHAPE_POLYSHAPE);

        final List<ILocation> vertices = polygon.getVertices();
        assertThat(vertices).containsExactly(
                new GeoPoint(30, -70),
                new GeoPoint(40, -80),
                new GeoPoint(40, -70),
                new GeoPoint(30, -70));

        coreShapeService.deleteShape(POLYGON_UID);
    }
}
