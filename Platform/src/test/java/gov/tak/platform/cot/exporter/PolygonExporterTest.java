package gov.tak.platform.cot.exporter;

import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.test.utils.ExporterTestUtils;
import gov.takx.api.mapobject.IShapePathMapObject;
import gov.takx.api.shapes.IMutablePolygon;
import gov.takx.core.shapes.ShapeService;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static gov.tak.test.utils.CotTestConstants.POLYGON_POINTS;
import static gov.tak.test.utils.CotTestConstants.POLYGON_UID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validate {@link PolygonExporter} functionality.
 *
 * @since 3.0
 */
public class PolygonExporterTest extends APolyshapeExporterTest
{
    private final PolygonExporter polygonExporter = new PolygonExporter();

    @Test
    public void test_polygon_export() throws IOException
    {
        final CotEvent sourceEvent = CotEvent.parse(getResourceAsString("/cot/good_polygon.cot"));

        assertThat(importerManager.importEvent(sourceEvent)).isEqualTo(ImportResult.SUCCESS);

        final ShapeService coreShapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final IMutablePolygon polygon = coreShapeService.getPolygon(POLYGON_UID);
        final IShapePathMapObject<?> polygonMapObject = polygon.getShapeMapObject();

        assertThat(polygon).isNotNull();
        assertThat(polygonMapObject).isNotNull();

        // Now convert the Map Object back to a CoT event
        final long now = CoordinatedTime.currentTimeMillis();

        final CotEvent event = polygonExporter.export(polygonMapObject);
        ExporterTestUtils.verifyCommonEventValues(event, now, polygonMapObject);

        // Verify stroke/fill style
        //noinspection ConstantConditions (verified by ExporterTestUtils)
        checkCotDetails(event, polygonMapObject);

        // Now ensure all the vertices are rendered as link details
        //   <link point='40,-70'/>
        //   <link point='50,-80'/>
        //   <link point='60,-90'/>
        //   <link point='70,-90'/>
        //   <link point='40,-70'/>

        checkPolyshape(event.getDetail(), POLYGON_POINTS);
    }
}
