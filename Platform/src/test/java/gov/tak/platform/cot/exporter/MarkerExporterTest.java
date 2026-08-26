package gov.tak.platform.cot.exporter;

import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.cot.CotConstants;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.core.comms.cot.CotImporterManager;
import gov.tak.platform.symbology.milstd2525.MilSymStandardAttributes;
import gov.tak.test.utils.ExporterTestUtils;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.core.mapobject.MapObjectManager;
import gov.takx.core.persist.MapObjectPersist;
import gov.takx.dataservices.persist.MarkerMapObject;
import gov.takx.test.AContainerTest;
import gov.takx.test.utils.CdiTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.concurrent.TimeUnit;

import static gov.tak.test.utils.ExporterTestUtils.checkDetailAttribute;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Validate {@link MarkerExporter} functionality.
 *
 * @since 3.0
 */
public class MarkerExporterTest extends AContainerTest
{
    private static final String MARKER_UID = "21c57f66-d135-4180-a771-f2ede16629dc";
    private static final String MARKER_NAME = "Unit 1";
    private static final String MARKER_TYPE = CotTypes.PREFIX_NEUTRAL + CotTypes.SUFFIX_GROUND_COMBAT_UNIT;
    private static final String SYMBOL_CODE = "INSPSCP-----**-";
    public static final GeoPoint POINT = new GeoPoint(10, 20, 30);
    public static final String CALL_SIGN = "bravo zulu";
    public static final int PINK_ARGB = Color.pink.getRGB();
    public static final String RANDOM_REMARKS = "random remarks";

    @SuppressWarnings("unused")
    @Mocked(parentTypes = IMapObjectPersist.class)
    private MapObjectPersist mapObjectPersist;

    private final MarkerExporter markerExporter = new MarkerExporter();

    protected MarkerExporterTest()
    {
        BlacksmithExtension.setBeansToUpdate(CdiTestUtils.getInstance(getBean(MapObjectManager.class)));
    }

    @AfterEach
    public void stopClean()
    {
        stopService(CotImporterManager.class);
    }

    @Test
    public void test_marker_exporter()
    {
        final MapObjectSpec spec = MapObjectSpec.builder(IMarkerMapObject.MARKER_FAMILY, IMarkerMapObject.MARKER_TYPE, "123")
                .uid(MARKER_UID)
                .cotType(MARKER_TYPE)
                .geoPoint(POINT)
                .name(MARKER_NAME)
                .build();

        final MarkerMapObject marker = new MarkerMapObject(spec, null);

        marker.setMetaString(StandardAttributeNames.ACCESS, "denied");
        marker.setMetaString(StandardAttributeNames.QOS, "bad");
        marker.setMetaString(StandardAttributeNames.OPEX, "E");

        marker.setMetaString(StandardAttributeNames.HOW, CotConstants.HOW_MACHINE_GENERATED_GARBAGE);
        marker.setRemarks(RANDOM_REMARKS);
        marker.setMetaString(StandardAttributeNames.CALLSIGN, CALL_SIGN);
        marker.setMetaInteger(StandardAttributeNames.COLOR, PINK_ARGB);
        marker.setMetaString(StandardAttributeNames.ARCHIVE, "");
        marker.setMetaBoolean(StandardAttributeNames.READINESS, true);

        marker.setMetaString(StandardAttributeNames.MIL_SYM, SYMBOL_CODE);
        marker.setMetaString(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + 'X', "X marks the spot");
        marker.setMetaString(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + 'Y', "Y marks another spot");

        startService(CotImporterManager.class); // Must be started to get detail handlers registered

        final long now = CoordinatedTime.currentTimeMillis();

        final CotEvent event = markerExporter.export(marker);
        ExporterTestUtils.verifyCommonEventValues(event, now, marker, TimeUnit.SECONDS.toMillis(MarkerExporter.DEFAULT_STALE_TIME_SECONDS),
                CotConstants.HOW_MACHINE_GENERATED_GARBAGE);

        //noinspection ConstantConditions (verified by ExporterTestUtils)
        final CotDetail detail = event.getDetail();

        assertThat(event.getUID()).isEqualTo(MARKER_UID);
        assertThat(event.getType()).isEqualTo(MARKER_TYPE);
        assertThat(new GeoPoint(event.getPoint())).isEqualTo(POINT);
        assertThat(event.getCallsign()).isEqualTo(CALL_SIGN);
        assertThat(event.getOpex()).isEqualTo("E");
        assertThat(event.getQos()).isEqualTo("bad");
        assertThat(event.getAccess()).isEqualTo("denied");

        // Check details
        assertThat(detail).isNotNull();

        // Since we used a 2525C SIDC, the details will contain both __milicon and __milsym
        // See ATAK-20119
        assertThat(detail.getChildren()).extracting(CotDetail::getElementName)
                .contains("color", "archive", "remarks", "contact", "status", "__milicon", "__milsym");

        assertThat(detail.getFirstChildByName(DetailConstants.ARCHIVE)).isNotNull();

        checkDetailAttribute(detail, DetailConstants.STATUS, DetailConstants.ATTR_READINESS, "true");
        checkDetailAttribute(detail, DetailConstants.CONTACT, DetailConstants.ATTR_CALLSIGN, CALL_SIGN);
        checkDetailAttribute(detail, DetailConstants.COLOR, DetailConstants.ATTR_ARGB, Integer.toString(PINK_ARGB));

        final CotDetail milIcon = detail.getFirstChildByName("__milicon");
        assertThat(milIcon.getAttribute(DetailConstants.ATTR_ID)).isEqualTo(SYMBOL_CODE);
        assertThat(milIcon).isNotNull();

        final var unitModifiers = milIcon.getChildrenByName(DetailConstants.UNIT_MODIFIER);
        assertThat(unitModifiers).hasSize(2);

        for (CotDetail unitModifier : unitModifiers)
        {
            final String code = unitModifier.getAttribute(DetailConstants.ATTR_CODE);
            if ("X".equals(code))
            {
                assertThat(unitModifier.getInnerText()).isEqualTo("X marks the spot");
            } else if ("Y".equals(code))
            {
                assertThat(unitModifier.getInnerText()).isEqualTo("Y marks another spot");
            } else
            {
                fail("Unknown modifier code: '%s'", code);
            }
        }

        final CotDetail remarks = detail.getFirstChildByName(DetailConstants.REMARKS);
        assertThat(remarks).isNotNull();
        assertThat(remarks.getInnerText()).isEqualTo(RANDOM_REMARKS);
    }
}
