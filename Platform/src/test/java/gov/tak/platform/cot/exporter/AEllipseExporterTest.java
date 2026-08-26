package gov.tak.platform.cot.exporter;

import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import jakarta.annotation.Nonnull;

import static gov.tak.api.cot.detail.DetailConstants.ATTR_ANGLE;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_MAJOR;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_MINOR;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_RELATION;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_UID;
import static gov.tak.api.cot.detail.DetailConstants.ELLIPSE;
import static gov.tak.api.cot.detail.DetailConstants.LINK;
import static gov.tak.api.cot.detail.DetailConstants.SHAPE;
import static gov.tak.test.utils.CotTestConstants.EPSILON;
import static gov.takx.commons.utils.ParseUtils.parseDouble;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base for ellipse exporter tests.
 *
 * @since 3.0
 */
public class AEllipseExporterTest extends AShapeExporterTest
{
    /**
     * Assert that the ellipse parameters are close to the given minor/major axes and angle (within EPSILON)
     */
    protected void checkEllipseParameters(CotEvent event, double minorAxis, double majorAxis, double angle)
    {
        final CotDetail detail = event.getDetail().getFirstChildByName(SHAPE).getFirstChildByName(ELLIPSE);

        final double detailMinor = parseDouble(detail.getAttribute(ATTR_MINOR));
        final double detailMajor = parseDouble(detail.getAttribute(ATTR_MAJOR));
        final double detailAngle = parseDouble(detail.getAttribute(ATTR_ANGLE));

        assertThat(detailMinor).isNotNegative().isCloseTo(minorAxis, EPSILON);
        assertThat(detailMajor).isNotNegative().isCloseTo(majorAxis, EPSILON);
        assertThat(detailAngle).isNotNegative().isCloseTo(angle, EPSILON);
    }

    /**
     * Determine if a link exists with the required relation and UID.
     *
     * @param event    Event to check
     * @param relation Required relation value
     * @param uid      Required UID
     * @return {@code true} if a link with that relation/uid exists
     */
    protected boolean checkRelationLinkExists(@Nonnull CotEvent event, @Nonnull String relation, @Nonnull String uid)
    {
        final CotDetail shapeDetail = event.getDetail().getFirstChildByName(SHAPE);

        for (CotDetail link : shapeDetail.getChildrenByName(LINK))
        {
            if (relation.equals(link.getAttribute(ATTR_RELATION)) && uid.equals(link.getAttribute(ATTR_UID)))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Create a MO to be used as a given type of anchor.
     */
    protected IMapObject createAnchorMapObject(@Nonnull String type, @Nonnull ILocation location)
    {
        final MapObjectSpec spec = MapObjectSpec.builder("test", "test_" + type, "1")
                .location(location)
                .uid(type + "Anchor")
                .build();
        return mapObjectManager.getOrCreateMapObject(spec);
    }
}
