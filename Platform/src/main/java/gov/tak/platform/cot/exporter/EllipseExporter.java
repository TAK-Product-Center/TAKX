package gov.tak.platform.cot.exporter;

import gov.tak.api.cot.event.CotDetail;
import gov.takx.api.mapobject.IShapeEllipseMapObject;
import gov.takx.dataservices.persist.MapObject;
import gov.takx.dataservices.persist.ShapeEllipseMapObject;

/**
 * Exporter handling {@link ShapeEllipseMapObject}.
 *
 * @since 3.0
 */
public class EllipseExporter extends AEllipseExporter<IShapeEllipseMapObject>
{
    @Override
    public Class<? extends MapObject> getType()
    {
        return ShapeEllipseMapObject.class;
    }

    @Override
    protected void addEllipseDetail(CotDetail shapeDetail, IShapeEllipseMapObject shapeMapObject)
    {
        final int ringCount = shapeMapObject.getRingCount();
        double orientation = shapeMapObject.getOrientation();
        double semiMinorAxis = shapeMapObject.getSemiMinorAxis();
        double semiMajorAxis = shapeMapObject.getSemiMajorAxis();

        if (Double.compare(semiMinorAxis, semiMajorAxis) > 0)
        {
            final double originalSemiMinorAxis = semiMinorAxis;

            // ATAK requires the major axis be larger than the minor axis while WorldWind uses them interchangeably
            // Because WorldWind measures orientation from the minor axis, we do not need to adjust it here
            semiMinorAxis = semiMajorAxis;
            semiMajorAxis = originalSemiMinorAxis;
        } else
        {
            // Adjust the orientation because WorldWind measures orientation from the minor axis
            orientation = (90 + orientation) % 360;
        }

        for (int i = 0; i < ringCount; i++)
        {
            final double ringMinorAxis = (semiMinorAxis * (i + 1)) * 2;
            final double ringMajorAxis = (semiMajorAxis * (i + 1)) * 2;

            addEllipseDetail(shapeDetail, ringMinorAxis, ringMajorAxis, orientation);
        }
    }
}
