package gov.tak.platform.cot.exporter;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.exporter.AShapeExporter;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapeMapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import static gov.tak.api.cot.detail.DetailConstants.ATTR_ANGLE;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_MAJOR;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_MINOR;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_RELATION;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_UID;
import static gov.tak.api.cot.detail.DetailConstants.ELLIPSE;
import static gov.tak.api.cot.detail.DetailConstants.LINK;
import static gov.tak.api.cot.detail.DetailConstants.SHAPE;

/**
 * Abstract base for exporter handling circle/ellipse shapes.
 *
 * @since 3.0
 */
@PseudoBean
public abstract class AEllipseExporter<T extends IShapeMapObject> extends AShapeExporter
{
    /**
     * Export circle/ellipse shapes to CoT events
     *
     * @param mapObject map object to process
     */
    @Nullable
    @Override
    public CotEvent export(@Nonnull IMapObject mapObject)
    {
        //noinspection unchecked
        final T shapeMapObject = (T) mapObject;

        final CotEvent event = createBasicEvent(mapObject, ONE_DAY_SECONDS);
        final CotDetail shapeDetail = event.getDetail().addChild(new CotDetail(SHAPE));

        addEllipseDetail(shapeDetail, shapeMapObject);
        addLinkDetails(shapeDetail, shapeMapObject);

        return event;
    }

    /**
     * Add relevant relation links.  This base implementation handles the center anchor, which is common to both
     * circles and ellipses.
     *
     * @param shapeDetail    Shape detail being constructed
     * @param shapeMapObject Map Object being exported
     */
    @OverridingMethodsMustInvokeSuper
    protected void addLinkDetails(CotDetail shapeDetail, T shapeMapObject)
    {
        final IMapObject centerAnchor = shapeMapObject.getCenterAnchor();
        if (centerAnchor != null)
        {
            addLinkDetail(shapeDetail, DetailConstants.LINK_RELATION_CENTER_ANCHOR, centerAnchor.getUid());
        }
    }

    /**
     * Add the "ellipse" detail using data extracted from the given {@code shapeMapObject}.
     *
     * @param shapeDetail    Shape detail being constructed
     * @param shapeMapObject Map Object being exported
     */
    protected abstract void addEllipseDetail(CotDetail shapeDetail, T shapeMapObject);

    /**
     * Construct and add an "ellipse" detail using the given parameters.
     *
     * @param shapeDetail Shape detail being constructed
     * @param minor       Minor axis length
     * @param major       Major axis length
     * @param orientation orientation
     */
    protected void addEllipseDetail(CotDetail shapeDetail, double minor, double major, double orientation)
    {
        final CotDetail ellipseDetail = new CotDetail(ELLIPSE);

        ellipseDetail.setAttribute(ATTR_MINOR, Double.toString(minor));
        ellipseDetail.setAttribute(ATTR_MAJOR, Double.toString(major));
        ellipseDetail.setAttribute(ATTR_ANGLE, Double.toString(orientation));

        shapeDetail.addChild(ellipseDetail);
    }

    /**
     * Construct and add a link detail element.
     *
     * @param shapeDetail Shape detail being constructed
     * @param relation    Relation type
     * @param uid         UID of linked map object
     */
    protected void addLinkDetail(CotDetail shapeDetail, String relation, String uid)
    {
        final CotDetail link = shapeDetail.addChild(new CotDetail(LINK));

        link.setAttribute(ATTR_RELATION, relation);
        link.setAttribute(ATTR_UID, uid);
    }
}
