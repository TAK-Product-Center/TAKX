package gov.tak.platform.cot.exporter;

import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.test.utils.ExporterTestUtils;
import gov.takx.api.mapobject.IShapePathMapObject;
import gov.takx.api.shapes.IMutablePolyline;
import gov.takx.core.shapes.ShapeService;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static gov.tak.test.utils.CotTestConstants.POLYLINE_POINTS;
import static gov.tak.test.utils.CotTestConstants.POLYLINE_UID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validate {@link PolylineExporter} functionality.
 *
 * @since 3.0
 */
public class PolylineExporterTest extends APolyshapeExporterTest
{
    private final PolylineExporter polylineExporter = new PolylineExporter();

    @Test
    public void test_polyline_export() throws IOException
    {
        final CotEvent sourceEvent = CotEvent.parse(getResourceAsString("/cot/good_polyline.cot"));

        assertThat(importerManager.importEvent(sourceEvent)).isEqualTo(ImportResult.SUCCESS);

        final ShapeService coreShapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final IMutablePolyline polyline = coreShapeService.getPolyline(POLYLINE_UID);
        final IShapePathMapObject polylineMapObject = polyline.getShapeMapObject();

        assertThat(polyline).isNotNull();
        assertThat(polylineMapObject).isNotNull();

        // Now convert the Map Object back to a CoT event
        final long now = CoordinatedTime.currentTimeMillis();

        final CotEvent event = polylineExporter.export(polylineMapObject);
        ExporterTestUtils.verifyCommonEventValues(event, now, polylineMapObject);

        //noinspection ConstantConditions (verified by ExporterTestUtils)
        checkCotDetails(event, polylineMapObject);

        // Now ensure all the vertices are rendered as link details
        //   <link point='40,-70'/>
        //   <link point='50,-80'/>
        //   <link point='60,-90'/>
        //   <link point='70,-90'/>

        checkPolyshape(event.getDetail(), POLYLINE_POINTS);
    }
}
