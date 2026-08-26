package gov.tak.platform.cot.exporter;

import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.test.utils.CotTestConstants;
import gov.tak.test.utils.ExporterTestUtils;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapeArrowMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static gov.tak.api.cot.detail.DetailConstants.ANCHOR_UID;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_VALUE;
import static gov.tak.api.cot.detail.DetailConstants.BEARING;
import static gov.tak.api.cot.detail.DetailConstants.INCLINATION;
import static gov.tak.api.cot.detail.DetailConstants.RANGE;
import static gov.tak.api.cot.detail.DetailConstants.RANGE_UID;
import static gov.tak.test.utils.CotTestConstants.ARROW_KEY;
import static gov.tak.test.utils.CotTestConstants.EPSILON;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validate {@link ArrowExporter} functionality.
 * @since 3.2
 */
public class ArrowExporterTest extends AShapeExporterTest
{
    private final ArrowExporter arrowExporter = new ArrowExporter();

    @Test
    public void test_arrow_export() throws IOException
    {
        final CotEvent sourceEvent = CotEvent.parse(getResourceAsString("/cot/good_arrow.cot"));
        assertThat(importerManager.importEvent(sourceEvent)).isEqualTo(ImportResult.SUCCESS);

        final var arrowMO = (IShapeArrowMapObject) mapObjectManager.getMapObject(ARROW_KEY);
        assertThat(arrowMO).isNotNull();

        // Now convert the Map Object back to a CoT event
        final long now = CoordinatedTime.currentTimeMillis();

        final CotEvent event = arrowExporter.export(arrowMO);
        ExporterTestUtils.verifyCommonEventValues(event, now, arrowMO);

        //noinspection ConstantConditions (verified by ExporterTestUtils)
        assertThat(event.getCallsign()).isEqualTo(CotTestConstants.ARROW_NAME);

        final CotDetail detail = event.getDetail();

        final CotDetail rangeDetail = detail.getFirstChildByName(RANGE);
        assertThat(rangeDetail).isNotNull();
        final String rangeValueString = rangeDetail.getAttribute(ATTR_VALUE);
        assertThat(rangeValueString).isNotNull();
        assertThat(Double.valueOf(rangeValueString)).isCloseTo(CotTestConstants.RANGE, EPSILON);

        final CotDetail bearingDetail = detail.getFirstChildByName(BEARING);
        assertThat(bearingDetail).isNotNull();
        final String bearingValueString = bearingDetail.getAttribute(ATTR_VALUE);
        assertThat(bearingValueString).isNotNull();
        assertThat(Double.valueOf(bearingValueString)).isCloseTo(CotTestConstants.BEARING, EPSILON);

        ExporterTestUtils.checkDetailAttribute(detail, INCLINATION, ATTR_VALUE, "0.0");

        checkCotDetails(event, arrowMO);
    }

    @Test
    public void test_arrow_export_with_anchor_links() throws IOException
    {
        // Create the Map Objects that are the targets of the links
        final GeoPoint startLocation = new GeoPoint(10, 10);
        final IMapObject startAnchor = createAnchorMapObject("start", startLocation);

        final GeoPoint rangeLocation = new GeoPoint(20, 20);
        final IMapObject rangeAnchor = createAnchorMapObject("range", rangeLocation);

        final CotEvent sourceEvent = CotEvent.parse(getResourceAsString("/cot/arrow_with_anchor_links.cot"));
        assertThat(importerManager.importEvent(sourceEvent)).isEqualTo(ImportResult.SUCCESS);

        final var arrowMO = (IShapeArrowMapObject) mapObjectManager.getMapObject(ARROW_KEY);
        assertThat(arrowMO).isNotNull();

        // Now convert the Map Object back to a CoT event
        final long now = CoordinatedTime.currentTimeMillis();

        final CotEvent event = arrowExporter.export(arrowMO);
        ExporterTestUtils.verifyCommonEventValues(event, now, arrowMO);

        //noinspection ConstantConditions (verified by ExporterTestUtils)
        assertThat(event.getCallsign()).isEqualTo(CotTestConstants.ARROW_NAME);

        final CotDetail detail = event.getDetail();

        final CotDetail anchorDetail = detail.getFirstChildByName(ANCHOR_UID);
        assertThat(anchorDetail).isNotNull();
        final String anchorUid = anchorDetail.getAttribute(ATTR_VALUE);
        assertThat(anchorUid).isNotNull().isEqualTo(startAnchor.getUid());

        final CotDetail rangeAnchorDetail = detail.getFirstChildByName(RANGE_UID);
        assertThat(rangeAnchorDetail).isNotNull();
        final String rangeAnchorUid = rangeAnchorDetail.getAttribute(ATTR_VALUE);
        assertThat(rangeAnchorUid).isNotNull().isEqualTo(rangeAnchor.getUid());
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
