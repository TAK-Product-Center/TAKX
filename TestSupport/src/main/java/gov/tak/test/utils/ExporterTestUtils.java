package gov.tak.test.utils;

import gov.tak.api.cot.CotConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.exporter.ACotEventExporter;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.mapobject.IMapObject;

import java.util.concurrent.TimeUnit;

import static gov.tak.test.utils.CotTestConstants.CLOCK_SLOP;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Common methods for CoT exporter tests.
 *
 * @since 3.0
 */
public class ExporterTestUtils
{
    /**
     * Verifies common values in the exported CoT event, including:
     * <ul>
     *     <li>version</li>
     *     <li>uid</li>
     *     <li>type</li>
     *     <li>time</li>
     *     <li>start</li>
     *     <li>stale</li>
     *     <li>how</li>
     *     <li>point</li>
     * </ul>
     * <p>
     * This method assumes the stale time should be 1 day later than the start time and how should be
     * {@value CotConstants#HOW_HUMAN_ENTERED}. If they should be something else, use the overload
     * {@link #verifyCommonEventValues(CotEvent, long, IMapObject, long, String)} instead and pass the expected values.
     *
     * @param event     The event to verify
     * @param now       The time in millis immediately before exporting the event
     * @param mapObject The map object that was exported
     */
    public static void verifyCommonEventValues(CotEvent event, long now, IMapObject mapObject)
    {
        verifyCommonEventValues(event, now, mapObject, TimeUnit.SECONDS.toMillis(ACotEventExporter.ONE_DAY_SECONDS), CotConstants.HOW_HUMAN_ENTERED);
    }

    /**
     * Verifies common values in the exported CoT event, including:
     * <ul>
     *     <li>version</li>
     *     <li>uid</li>
     *     <li>type</li>
     *     <li>time</li>
     *     <li>start</li>
     *     <li>stale</li>
     *     <li>how</li>
     *     <li>point</li>
     * </ul>
     *
     * @param event         The event to verify
     * @param now           The time in millis immediately before exporting the event
     * @param mapObject     The map object that was exported
     * @param staleOffsetMs The offset to use for checking the stale time in milliseconds (i.e. 1 day, 1 year, etc.)
     * @param how           The value to use for verifying the "how" value
     */
    public static void verifyCommonEventValues(CotEvent event, long now, IMapObject mapObject, long staleOffsetMs, String how)
    {
        assertThat(event).isNotNull();
        assertThat(event.isValid()).isTrue();

        assertThat(event.getVersion()).isEqualTo(CotEvent.VERSION_2_0);
        assertThat(event.getUID()).isEqualTo(mapObject.getUid());
        assertThat(event.getType()).isNotEmpty().isEqualTo(mapObject.getCotType());

        // time and start should be equal, and roughly "now", and stale should be 1 day out
        final long timeMillis = event.getTime().getMilliseconds();
        final long staleMillis = now + staleOffsetMs;

        assertThat(timeMillis).isCloseTo(now, CLOCK_SLOP);
        assertThat(event.getStart().getMilliseconds()).isEqualTo(timeMillis);
        assertThat(event.getStale().getMilliseconds()).isCloseTo(staleMillis, CLOCK_SLOP);

        assertThat(event.getHow()).isEqualTo(how);

        assertThat(new GeoPoint(event.getPoint())).isEqualTo(mapObject.getLocation());
    }

    /**
     * Check that the given child detail exists, and it has an attribute with the given name and value.
     *
     * @param detail     main detail
     * @param detailName Name of child detail to process
     * @param attrName   Name of attribute to check
     * @param attrValue  Expected value of attribute
     * @since 3.2 (moved from MarkerExporterTest)
     */
    public static void checkDetailAttribute(CotDetail detail, String detailName, String attrName, String attrValue)
    {
        final CotDetail child = detail.getFirstChildByName(detailName);
        assertThat(child).isNotNull();
        assertThat(child.getAttribute(attrName)).isEqualTo(attrValue);
    }

    private ExporterTestUtils()
    {
    }
}
