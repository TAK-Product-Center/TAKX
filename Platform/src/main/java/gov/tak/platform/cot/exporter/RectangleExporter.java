package gov.tak.platform.cot.exporter;

import gov.takx.dataservices.persist.ShapeRectangleMapObject;

/**
 * Exporter handling {@link ShapeRectangleMapObject}.
 *
 * @since 3.0
 */
public class RectangleExporter extends ARectangularExporter
{
    public RectangleExporter()
    {
        super(ShapeRectangleMapObject.class);
    }
}
