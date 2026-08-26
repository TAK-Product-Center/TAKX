package gov.tak.platform.cot.exporter;

import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapeCircleMapObject;
import gov.takx.dataservices.persist.MapObject;
import gov.takx.dataservices.persist.ShapeCircleMapObject;

/**
 * Exporter handling {@link ShapeCircleMapObject}.
 *
 * @since 3.0
 */
public class CircleExporter extends AEllipseExporter<IShapeCircleMapObject>
{
    @Override
    public Class<? extends MapObject> getType()
    {
        return ShapeCircleMapObject.class;
    }

    @Override
    protected void addEllipseDetail(CotDetail shapeDetail, IShapeCircleMapObject shapeMapObject)
    {
        final int ringCount = shapeMapObject.getRingCount();
        final double radius = shapeMapObject.getRadius();

        for (int i = 0; i < ringCount; i++)
        {
            final double ringRadius = radius * (i + 1);
            addEllipseDetail(shapeDetail, ringRadius, ringRadius, 0);
        }
    }

    /**
     * Handle radius anchor.
     */
    @Override
    protected void addLinkDetails(CotDetail shapeDetail, IShapeCircleMapObject shapeMapObject)
    {
        super.addLinkDetails(shapeDetail, shapeMapObject);

        final IMapObject radiusAnchor = shapeMapObject.getRadiusAnchor();
        if (radiusAnchor != null)
        {
            addLinkDetail(shapeDetail, DetailConstants.LINK_RELATION_RADIUS_ANCHOR, radiusAnchor.getUid());
        }
    }
}
