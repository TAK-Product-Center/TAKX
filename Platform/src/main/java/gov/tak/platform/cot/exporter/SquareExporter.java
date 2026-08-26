package gov.tak.platform.cot.exporter;

import gov.takx.dataservices.persist.ShapeSquareMapObject;

/**
 * Exporter handling {@link ShapeSquareMapObject}.
 *
 * @since 3.0
 */
public class SquareExporter extends ARectangularExporter
{
    public SquareExporter()
    {
        super(ShapeSquareMapObject.class);
    }
}
