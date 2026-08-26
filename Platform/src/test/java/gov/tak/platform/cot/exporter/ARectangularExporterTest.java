package gov.tak.platform.cot.exporter;

import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.test.utils.ExporterTestUtils;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IShapeMapObject;

import java.util.List;

import static gov.tak.api.cot.detail.DetailConstants.ATTR_POINT;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_RELATION;
import static gov.tak.api.cot.detail.DetailConstants.LINK;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base for rectangular map object exporters.
 *
 * @since 3.0
 */
public abstract class ARectangularExporterTest extends AShapeExporterTest
{
    private ARectangularExporter exporter;

    /**
     * Assigns the exporter to use in child classes for testing.
     * This is done as a setter to prevent load-order issues with {@code PostInitInjector}.
     * When we do it <i>after</i> construction the CDI container will be available to the injector.
     *
     * @param exporter Exporter to test.
     */
    protected void setExporter(ARectangularExporter exporter)
    {
        this.exporter = exporter;
    }

    /**
     * Export the given map object to CoT and validate that it has the expected corners.
     *
     * @param mapObject Map Object to export
     * @param expectedCorners List of corners to validate against
     * @see #checkCotDetails(CotEvent, IShapeMapObject)
     */
    protected void exportAndValidateEvent(IShapeMapObject<?> mapObject, List<? extends ILocation> expectedCorners)
    {
        final long now = CoordinatedTime.currentTimeMillis();
        final CotEvent event = exporter.export(mapObject);

        ExporterTestUtils.verifyCommonEventValues(event, now, mapObject);

        //noinspection ConstantConditions (verified by ExporterTestUtils)
        checkCotDetails(event, mapObject);

        final CotDetail detail = event.getDetail();
        assertThat(detail.getChildrenByName(LINK)).hasSize(4); // 4 corners

        // Now ensure all the corners are rendered as link details in the correct order, ignore any links with relations
        final List<GeoPoint> corners = detail.getChildrenByName(LINK).stream()
                .filter(link -> link.getAttribute(ATTR_RELATION) == null)
                .map(link -> GeoPoint.parseGeoPoint(link.getAttribute(ATTR_POINT)))
                .toList();

        for (int i = 0; i < expectedCorners.size(); i++)
        {
            final var expected = expectedCorners.get(i);
            final var actual = corners.get(i);

            assertThat(actual.getLatitude()).isEqualTo(expected.getLatitude());
            assertThat(actual.getLongitude()).isEqualTo(expected.getLongitude());
        }
    }
}
