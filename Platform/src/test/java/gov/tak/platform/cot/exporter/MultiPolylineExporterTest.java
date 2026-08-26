package gov.tak.platform.cot.exporter;

import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.platform.cot.importer.MultiPolylineImporterTest;
import gov.tak.test.utils.ExporterTestUtils;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMultiPolylineMapObject;
import gov.takx.api.mapobject.IShapePathMapObject;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static gov.tak.api.cot.detail.DetailConstants.ATTR_VALUE;
import static gov.tak.api.cot.detail.DetailConstants.HEIGHT;
import static gov.tak.api.cot.detail.DetailConstants.LINK;
import static gov.tak.api.cot.detail.DetailConstants.STROKE_COLOR;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validate {@link MultiPolylineExporter} functionality.
 *
 * @since 3.0
 */
public class MultiPolylineExporterTest extends APolyshapeExporterTest
{
    private final MultiPolylineExporter multiPolylineExporter = new MultiPolylineExporter();

    @Test
    public void test_multi_polyline_export() throws IOException
    {
        final CotEvent sourceEvent = CotEvent.parse(getResourceAsString("/cot/good_multi_polyline.cot"));
        assertThat(importerManager.importEvent(sourceEvent)).isEqualTo(ImportResult.SUCCESS);

        final var multiPolylineMO = (IMultiPolylineMapObject) mapObjectManager.getMapObject(MultiPolylineImporterTest.MULTI_POLYLINE_KEY);
        assertThat(multiPolylineMO).isNotNull();

        // Now convert the Map Object back to a CoT event
        final long now = CoordinatedTime.currentTimeMillis();

        CotEvent event = multiPolylineExporter.export(multiPolylineMO);
        ExporterTestUtils.verifyCommonEventValues(event, now, multiPolylineMO);

        //noinspection ConstantConditions (verified by ExporterTestUtils)
        assertThat(event.getCallsign()).isEqualTo(MultiPolylineImporterTest.MULTI_POLYLINE_NAME);

        final CotDetail detail = event.getDetail();
        final CotDetail heightDetail = detail.getFirstChildByName(HEIGHT);
        assertThat(heightDetail).isNotNull();
        assertThat(heightDetail.getAttribute(ATTR_VALUE)).isEqualTo("1.23");

        final List<IShapePathMapObject<?>> polylines = multiPolylineMO.getPolylines();
        final List<CotDetail> linkDetails = detail.getChildrenByName(LINK);
        assertThat(linkDetails).hasSize(2);
        verifyLinkDetail(now, linkDetails.get(0), polylines.get(0), MultiPolylineImporterTest.POLYLINE_1_NAME, MultiPolylineImporterTest.POLYLINE_1_POINTS);
        verifyLinkDetail(now, linkDetails.get(1), polylines.get(1), MultiPolylineImporterTest.POLYLINE_2_NAME, MultiPolylineImporterTest.POLYLINE_2_POINTS);
    }

    /**
     * Verify the link detail contains the expected values.
     *
     * @param now      The time in millis immediately before exporting the event
     * @param link     The detail to verify
     * @param polyline The associated polyline
     * @param callsign The expected callsign
     * @param points   The expected points in the polyline
     */
    private void verifyLinkDetail(long now, @Nonnull CotDetail link, @Nonnull IShapePathMapObject<?> polyline,
                                  @Nonnull String callsign, @Nonnull List<ILocation> points)
    {
        final String linkLine = link.getAttribute(DetailConstants.ATTR_LINE);
        final CotEvent event = CotEvent.parse(linkLine);
        ExporterTestUtils.verifyCommonEventValues(event, now, polyline);

        assertThat(event.getCallsign()).isEqualTo(callsign);
        assertThat(new GeoPoint(event.getPoint())).isEqualTo(polyline.getLocation());

        final CotDetail detail = event.getDetail();
        assertThat(detail).isNotNull();

        assertThat(detail.getFirstChildByName(STROKE_COLOR).getAttribute(ATTR_VALUE))
                .isEqualTo(Integer.toString(polyline.getLineColor().getRGB()));

        checkPolyshape(detail, points.stream().map(GeoPoint::new).collect(Collectors.toList()));
    }
}
