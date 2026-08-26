package gov.tak.platform.cot.exporter;

import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.maps.coords.GeoPoint;

import java.util.ArrayList;
import java.util.List;

import static gov.tak.api.cot.detail.DetailConstants.ATTR_POINT;
import static gov.tak.api.cot.detail.DetailConstants.LINK;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base for polyshape exporter tests.
 *
 * @since 3.0
 */
public class APolyshapeExporterTest extends AShapeExporterTest
{
    /**
     * Assert that the polyshape vertices are equal to the given vertices
     */
    protected void checkPolyshape(CotDetail detail, List<GeoPoint> expectedVertices)
    {
        List<GeoPoint> actualVertices = new ArrayList<>();

        for (CotDetail link : detail.getChildrenByName(LINK))
        {
            actualVertices.add(GeoPoint.parseGeoPoint(link.getAttribute(ATTR_POINT)));
        }

        assertThat(actualVertices).isEqualTo(expectedVertices);
    }
}
