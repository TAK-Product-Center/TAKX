package gov.tak.platform.cot.importer;

import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.takx.api.annotation.PseudoBean;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base for CoT Event importers that produce shapes with corners.
 *
 * @since 3.0
 */
@PseudoBean
public abstract class ACorneredShapeImporter extends AShapeImporter
{
    /**
     * Construct on the given type.
     *
     * @param type Type code for shape
     */
    protected ACorneredShapeImporter(String type)
    {
        super(type);
    }

    /**
     * Extract the points from the cornered shape link detail elements.
     *
     * @param detail                Top-level detail from the CoT event
     * @param skipLinksWithRelation Pass {@code true} to skip any links that have a 'relation' attribute set
     * @return List of GeoPointMetaData objects created from links
     */
    protected List<GeoPointMetaData> getPoints(@Nonnull CotDetail detail, boolean skipLinksWithRelation)
    {
        final List<GeoPointMetaData> points = new ArrayList<>();

        for (CotDetail link : detail.getChildrenByName(DetailConstants.LINK))
        {
            // Skip links that have a "relation" if instructed
            if (!skipLinksWithRelation || link.getAttribute(DetailConstants.ATTR_RELATION) == null)
            {
                final GeoPoint geoPoint = GeoPoint.parseGeoPoint(link.getAttribute(DetailConstants.ATTR_POINT));
                points.add(GeoPointMetaData.wrap(geoPoint));
            }
        }

        return points;
    }
}
