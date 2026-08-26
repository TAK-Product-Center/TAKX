package gov.tak.platform.cot.exporter;

import gov.takx.dataservices.persist.MapObject;
import gov.takx.dataservices.persist.ShapePolygonMapObject;

/**
 * Exporter handling {@link ShapePolygonMapObject}.
 *
 * @since 3.0
 */
public class PolygonExporter extends APolyshapeExporter
{
    @Override
    public Class<? extends MapObject> getType()
    {
        return ShapePolygonMapObject.class;
    }
}
