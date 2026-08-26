package gov.tak.platform.cot.exporter;

import gov.tak.test.utils.CotTestConstants;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.shapes.ShapeAlreadyExistsException;
import gov.takx.dataservices.shapes.Square;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validate {@link SquareExporter} functionality.
 *
 * @since 3.0
 */
public class SquareExporterTest extends ARectangularExporterTest
{
    private static final double HEIGHT = 1_000;

    protected SquareExporterTest()
    {
        setExporter(new SquareExporter());
    }

    @Test
    public void test_square_export() throws ShapeAlreadyExistsException
    {
        // CoT doesn't really have a concept pf square - it's all rectangles.
        // So, manufacture a Square and ensure that it exports as a rectangle.

        final var coreShapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final var squareMapObject = coreShapeService.createSquareMapObject(new Square("CoT Square", CotTestConstants.ZERO_LOC, 0, HEIGHT));
        assertThat(squareMapObject).isNotNull();

        squareMapObject.setTitle("callsign");

        // Rectangle vertices are ordered SW, SE, NE, NW
        List<ILocation> vertices = squareMapObject.getVertices();
        // CoT vertices are ordered NW, SW, SE, NE
        final List<ILocation> corners = List.of(vertices.get(3), vertices.get(0), vertices.get(1), vertices.get(2));

        exportAndValidateEvent(squareMapObject, corners);
    }
}
