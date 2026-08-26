package gov.tak.platform.cot.exporter;

import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.takx.api.shapes.IMutableRectangle;
import gov.takx.core.shapes.ShapeService;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static gov.tak.test.utils.CotTestConstants.RECTANGLE_CORNERS;
import static gov.tak.test.utils.CotTestConstants.RECTANGLE_UID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validate {@link RectangleExporter} functionality.
 *
 * @since 3.0
 */
public class RectangleExporterTest extends ARectangularExporterTest
{
    protected RectangleExporterTest()
    {
        setExporter(new RectangleExporter());
    }

    @Test
    public void test_rectangle_export() throws IOException
    {
        final CotEvent sourceEvent = CotEvent.parse(getResourceAsString("/cot/good_rectangle.cot"));

        assertThat(importerManager.importEvent(sourceEvent)).isEqualTo(ImportResult.SUCCESS);

        final ShapeService coreShapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final IMutableRectangle rectangle = coreShapeService.getRectangle(RECTANGLE_UID);

        assertThat(rectangle).isNotNull();
        exportAndValidateEvent(rectangle.getShapeMapObject(), RECTANGLE_CORNERS);
    }
}
