package gov.tak.platform.cot.importer;

import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.cot.CotConstants;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.time.SystemClock;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.core.comms.cot.CotImporterManager;
import gov.tak.platform.symbology.milstd2525.MilSymStandardAttributes;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.test.AIntegrationTest;
import jakarta.inject.Inject;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validate {@link MarkerImporter} functionality.
 *
 * @since 3.0
 */
@SuppressWarnings("DataFlowIssue")
@PseudoBean
public class MarkerImporterTest extends AIntegrationTest
{
    private static final String MARKER_UID = "21c57f66-d135-4180-a771-f2ede16629dc";
    private static final String MARKER_NAME = "Unit 1";
    private static final String MARKER_TYPE = CotTypes.PREFIX_FRIENDLY + CotTypes.SUFFIX_GROUND_COMBAT_UNIT;

    private static final String NEW_MARKER_NAME = "New Unit 1";
    private static final String NEW_MARKER_TYPE = CotTypes.PREFIX_HOSTILE + CotTypes.SUFFIX_GROUND_COMBAT_UNIT;
    private static final String SYMBOL_CODE = "INSPSCP-----**-";

    private static final Offset<Double> EPSILON = Offset.offset(1e-6); // Comparison tolerance

    // We need more slop in our clock test because the value is set early in the import of the MapObject,
    // and it takes longer to get to the test that verifies its value.
    private static final Offset<Long> CLOCK_SLOP = Offset.offset(200L);

    private final IMapObjectManager mapObjectManager = getBean(IMapObjectManager.class);
    private final IMapObjectPersist mapObjectPersist = getBean(IMapObjectPersist.class);
    private final CotImporterManager importerManager = getBean(CotImporterManager.class);

    @Inject
    private CotImportHelper cotImportHelper;

    @AfterEach
    public void afterEach()
    {
        deleteAllMapObjects();
    }

    @SuppressLogging(CotImporterManager.class)
    @Test
    public void test_marker_import() throws IOException
    {
        final CotEvent event = CotEvent.parse(getResourceAsString("/cot/marker.cot"));
        assertThat(event.isValid()).isTrue();

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(0);

        final long startTime = SystemClock.elapsedRealtime();
        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);
        final IMapObject mapObject = mapObjectPersist.findMapObjectByUid(MARKER_UID);

        // Test this right away since it is time-sensitive
        assertThat(mapObject.getMetaLong(StandardAttributeNames.OFFSCREEN_INTEREST, 0))
                .isCloseTo(startTime, CLOCK_SLOP);

        assertThat(mapObject.getMetaLong(StandardAttributeNames.LAST_UPDATE_TIME, 0))
                .isCloseTo(CoordinatedTime.currentTimeMillis(), CLOCK_SLOP);

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);

        assertThat(mapObject.getLatestCotEvent()).isEqualTo(event);
        assertThat(mapObject.getName()).isEqualTo(MARKER_NAME);
        assertThat(mapObject.getTitle()).isEqualTo(MARKER_NAME);
        assertThat(mapObject.getCotType()).isEqualTo(MARKER_TYPE);
        assertThat(mapObject.isTransient()).isFalse();

        final ILocation location = mapObject.getLocation();
        assertThat(location.getLatitude()).isCloseTo(50, EPSILON);
        assertThat(location.getLongitude()).isCloseTo(-112, EPSILON);
        assertThat(location.getAltitude()).isCloseTo(100, EPSILON); // Get HAE using getAltitude()

        assertThat(mapObject.getRemarks()).isEqualTo("the remarks");

        final AttributeSet attrs = mapObject.getAttributeSet();

        assertThat(attrs.getStringAttribute(StandardAttributeNames.UID)).isEqualTo(MARKER_UID);
        assertThat(attrs.getStringAttribute(StandardAttributeNames.COT_TYPE)).isEqualTo(MARKER_TYPE);
        assertThat(attrs.getStringAttribute(StandardAttributeNames.CALLSIGN)).isEqualTo(MARKER_NAME);
        assertThat(attrs.getStringAttribute(StandardAttributeNames.TITLE)).isEqualTo(MARKER_NAME);
        assertThat(attrs.getStringAttribute(StandardAttributeNames.HOW)).isEqualTo(CotConstants.HOW_HUMAN_GARBAGE_IN_GARBAGE_OUT);

        assertThat(mapObject.isAllowDrag()).isFalse();
        assertThat(attrs.getBooleanAttribute(StandardAttributeNames.MOVABLE)).isFalse();

        assertThat(attrs.getBooleanAttribute(StandardAttributeNames.EDITABLE)).isTrue();
        assertThat(attrs.getBooleanAttribute(StandardAttributeNames.REMOVABLE)).isTrue();
        assertThat(attrs.getBooleanAttribute(StandardAttributeNames.READINESS)).isTrue();

        assertThat(attrs.getStringAttribute(StandardAttributeNames.OPEX)).isEqualTo("E");
        assertThat(attrs.getStringAttribute(StandardAttributeNames.QOS)).isEqualTo("as if");
        assertThat(attrs.getStringAttribute(StandardAttributeNames.ACCESS)).isEqualTo("denied");

        assertThat(attrs.getStringAttribute(StandardAttributeNames.MIL_SYM)).isEqualTo(SYMBOL_CODE);
        assertThat(attrs.getStringAttribute(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + 'X')).isEqualTo("X marks the spot");
        assertThat(attrs.getStringAttribute(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + 'Y')).isEqualTo("Y marks another spot");

        mapObjectManager.deleteMapObject(mapObject);
    }

    @SuppressLogging(CotImporterManager.class)
    @Test
    public void test_marker_update() throws IOException
    {
        final CotEvent event = CotEvent.parse(getResourceAsString("/cot/marker.cot"));
        assertThat(event.isValid()).isTrue();

        final CotEvent updateEvent = CotEvent.parse(getResourceAsString("/cot/marker_update.cot"));
        assertThat(updateEvent.isValid()).isTrue();

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(0);

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);

        final IMapObject mapObject = mapObjectPersist.findMapObjectByUid(MARKER_UID);
        final long offscreenInterest = mapObject.getMetaLong(StandardAttributeNames.OFFSCREEN_INTEREST, 0);

        // Force the "lastUpdateTime" backwards 100 seconds so the speed calculations have something to work with
        final long newLuTime = CoordinatedTime.currentTimeMillis() - 100_000;
        mapObject.setMetaLong(StandardAttributeNames.LAST_UPDATE_TIME, newLuTime);

        // Now import the update
        assertThat(importerManager.importEvent(updateEvent)).isEqualTo(ImportResult.SUCCESS);

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);

        // Should have been updated
        assertThat(mapObject.getMetaLong(StandardAttributeNames.LAST_UPDATE_TIME, 0))
                .isCloseTo(CoordinatedTime.currentTimeMillis(), CLOCK_SLOP);

        // Should not have changed
        assertThat(mapObject.getMetaLong(StandardAttributeNames.OFFSCREEN_INTEREST, 0))
                .isEqualTo(offscreenInterest);

        assertThat(mapObject.getName()).isEqualTo(NEW_MARKER_NAME);
        assertThat(mapObject.getTitle()).isEqualTo(NEW_MARKER_NAME);
        assertThat(mapObject.getCotType()).isEqualTo(NEW_MARKER_TYPE);

        final ILocation location = mapObject.getLocation();
        assertThat(location.getLatitude()).isCloseTo(51, EPSILON);
        assertThat(location.getLongitude()).isCloseTo(-113, EPSILON);
        assertThat(location.getAltitude()).isCloseTo(100, EPSILON); // Get HAE using getAltitude()

        assertThat(mapObject.getRemarks()).isEqualTo("the new remarks");

        final AttributeSet attrs = mapObject.getAttributeSet();

        assertThat(attrs.getStringAttribute(StandardAttributeNames.UID)).isEqualTo(MARKER_UID);
        assertThat(attrs.getStringAttribute(StandardAttributeNames.COT_TYPE)).isEqualTo(NEW_MARKER_TYPE);
        assertThat(attrs.getStringAttribute(StandardAttributeNames.CALLSIGN)).isEqualTo(NEW_MARKER_NAME);
        assertThat(attrs.getStringAttribute(StandardAttributeNames.TITLE)).isEqualTo(NEW_MARKER_NAME);
        assertThat(attrs.getStringAttribute(StandardAttributeNames.HOW)).isEqualTo(CotConstants.HOW_HUMAN_GARBAGE_IN_GARBAGE_OUT);

        assertThat(mapObject.isAllowDrag()).isFalse();
        assertThat(attrs.getBooleanAttribute(StandardAttributeNames.MOVABLE)).isFalse();

        assertThat(attrs.getBooleanAttribute(StandardAttributeNames.EDITABLE)).isTrue();
        assertThat(attrs.getBooleanAttribute(StandardAttributeNames.REMOVABLE)).isTrue();
        assertThat(attrs.getBooleanAttribute(StandardAttributeNames.READINESS)).isFalse();

        assertThat(attrs.getStringAttribute(StandardAttributeNames.OPEX)).isEqualTo("E");
        assertThat(attrs.getStringAttribute(StandardAttributeNames.QOS)).isEqualTo("better now");
        assertThat(attrs.getStringAttribute(StandardAttributeNames.ACCESS)).isEqualTo("approved");

        // Validate that the importer has calculated the estimates of speed, course, and distance

        final long estTime = attrs.getLongAttribute(StandardAttributeNames.EST_TIME);
        final double estSpeed = attrs.getDoubleAttribute(StandardAttributeNames.EST_SPEED);
        final double estCourse = attrs.getDoubleAttribute(StandardAttributeNames.EST_COURSE);
        final double estDist = attrs.getDoubleAttribute(StandardAttributeNames.EST_DISTANCE);

        assertThat(estTime).isCloseTo(CoordinatedTime.currentTimeMillis(), CLOCK_SLOP);

        // These benchmark values are based on moving the marker from 50,-112 to 51,-113 in 100 seconds
        final GeoPoint previousPoint = new GeoPoint(50, -112);
        final GeoPoint newPoint = new GeoPoint(51, -113);

        final double distance = previousPoint.distanceTo(newPoint);
        final double course = previousPoint.bearingTo(newPoint);
        final double speed = distance / ((estTime - newLuTime) / 1000d);

        assertThat(estDist).isCloseTo(distance, EPSILON);
        assertThat(estSpeed).isCloseTo(speed, EPSILON);
        assertThat(estCourse).isCloseTo(course, EPSILON);

        mapObjectManager.deleteMapObject(mapObject);
    }

    /**
     * Verify that a received CoT message has the UID properly sanitized to prevent path manipulation attacks.
     * @since 5.2
     */
    @Test
    void cotWithAttackUid_whenImported_avoidsPathManipulationAttack() throws IOException
    {
        final String attackUid = "../../../random_place";
        final String cotMessage = getResourceAsString("/cot/marker.cot").replace(MARKER_UID, attackUid);

        assertThat(cotImportHelper.parseAndImportCot(cotMessage).join()).isEqualTo(ImportResult.SUCCESS);

        assertThat(mapObjectPersist.findMapObjectByUid(attackUid)).isNull();
        assertThat(mapObjectPersist.findMapObjectByUid("random_place")).isNotNull();
    }

    @Test
    void cotWithoutArchiveDetail_createsTransientMarker() throws IOException
    {
        final String cotMessage = getResourceAsString("/cot/marker.cot").replace("<archive/>", "");

        assertThat(cotImportHelper.parseAndImportCot(cotMessage).join()).isEqualTo(ImportResult.SUCCESS);

        final IMapObject mapObject = mapObjectPersist.findMapObjectByUid(MARKER_UID);
        assertThat(mapObject.isTransient()).isTrue();
    }

    @Test
    void cotCapabilitiesMessage_doesNotCreateMarker() throws IOException
    {
        final String cotMessage = getResourceAsString("/cot/marker.cot").replace(MARKER_TYPE, "c-x");
        assertThat(cotImportHelper.parseAndImportCot(cotMessage).join()).isEqualTo(ImportResult.FAILURE);
        assertThat(mapObjectPersist.findMapObjectByUid(MARKER_UID)).isNull();
    }
}
