package gov.tak.api.maps.coords;

import gov.takx.api.annotation.SuppressLogging;
import org.junit.jupiter.api.Test;

import static gov.tak.api.engine.map.coords.IGeoPoint.AltitudeReference;
import static gov.tak.api.maps.coords.GeoPoint.UNKNOWN;
import static gov.tak.api.maps.coords.GeoPoint.UNKNOWN_POINT;
import static gov.tak.api.maps.coords.GeoPoint.ZERO_POINT;
import static gov.tak.api.maps.coords.GeoPoint.createMutable;
import static gov.tak.api.maps.coords.GeoPoint.isAltitudeValid;
import static gov.tak.api.maps.coords.GeoPoint.parseGeoPoint;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

/**
 * Validate GeoPoint functionality.
 *
 * @since 3.0
 */
public class GeoPointTest
{
    @Test
    public void test_construction_values()
    {
        final GeoPoint p = new GeoPoint(90, 90);

        assertThat(p.getLatitude()).isEqualTo(90);
        assertThat(p.getLongitude()).isEqualTo(90);
        assertThat(p.getCE()).isNaN();
        assertThat(p.getLE()).isNaN();
        assertThat(p.getAltitudeReference()).isEqualTo(AltitudeReference.HAE);
        assertThat(p.getAltitude()).isNaN();

        assertThat(new GeoPoint(90, 90, UNKNOWN).getAltitude()).isNaN();

        final GeoPoint p2 = new GeoPoint(90, 90, 10, AltitudeReference.AGL);
        assertThat(p2.getAltitudeReference()).isEqualTo(AltitudeReference.AGL);
        assertThat(p2.getAltitude()).isEqualTo(10);

        // Test copy constructor
        final GeoPoint p4 = new GeoPoint(p2);
        assertThat(p4.getLatitude()).isEqualTo(p2.getLatitude());
        assertThat(p4.getLongitude()).isEqualTo(p2.getLongitude());
        assertThat(p4.getAltitude()).isEqualTo(p2.getAltitude());
        assertThat(p4.getAltitudeReference()).isEqualTo(p2.getAltitudeReference());
        assertThat(p4.getCE()).isNaN();
        assertThat(p4.getLE()).isNaN();

        final GeoPoint p3 = new GeoPoint(1, 2, 3, 4, 5);
        assertThat(p3.getAltitudeReference()).isEqualTo(AltitudeReference.HAE);
        assertThat(p3.getAltitude()).isEqualTo(3);
        assertThat(p3.getCE()).isEqualTo(4);
        assertThat(p3.getLE()).isEqualTo(5);
    }

    @Test
    public void test_is_valid()
    {
        assertThat(ZERO_POINT.isValid()).isTrue();
        assertThat(new GeoPoint(90, 180).isValid()).isTrue();
        assertThat(new GeoPoint(-90, 180).isValid()).isTrue();
        assertThat(new GeoPoint(-90, -180).isValid()).isTrue();
        assertThat(new GeoPoint(90, -180).isValid()).isTrue();

        assertThat(UNKNOWN_POINT.isValid()).isFalse();
        assertThat(new GeoPoint(900, 0).isValid()).isFalse();
        assertThat(new GeoPoint(0, -1800).isValid()).isFalse();
    }

    @Test
    public void test_is_altitude_valid()
    {
        assertThat(isAltitudeValid(0)).isTrue();
        assertThat(isAltitudeValid(UNKNOWN)).isFalse();

        assertThat(ZERO_POINT.isAltitudeValid()).isFalse();
        assertThat(UNKNOWN_POINT.isAltitudeValid()).isFalse();
        assertThat(new GeoPoint(0, 0, 0).isAltitudeValid()).isTrue();
    }

    @Test
    public void test_is_mutable()
    {
        assertThat(ZERO_POINT.isMutable()).isFalse();
        assertThat(createMutable().isMutable()).isTrue();
    }

    @Test
    public void test_distance_to()
    {
        assertThat(ZERO_POINT.distanceTo(ZERO_POINT)).isEqualTo(0.0);
        assertThat(new GeoPoint(90, 90).distanceTo(new GeoPoint(90, 0))).isLessThan(1e-8);
        assertThat(new GeoPoint(0, -180).distanceTo(new GeoPoint(0, 180))).isLessThan(1e-8);

        // 111,319 meters == one degree longitude at the equator
        assertThat(ZERO_POINT.distanceTo(new GeoPoint(0, 1))).isEqualTo(111_319, offset(1d));

        // This test used to compare against 157425 (the haversine distance), but the GeoCalculations.distance() method
        // uses a more accurate ellipsoidal calculation, which yields a distance of 156,899
        assertThat(ZERO_POINT.distanceTo(new GeoPoint(1, 1))).isEqualTo(156_899, offset(1d));
        assertThat(ZERO_POINT.distanceTo(new GeoPoint(-1, -1))).isEqualTo(156_899, offset(1d));
    }

    @Test
    public void test_bearing_to()
    {
        // TODO re-enable this assertion when ENGINE-722 is finished and TAKX moves up to the new kernel version
        // The bearingTo calculation in the kernel doesn't special case being given the same points, and is currently
        // returning 180 instead of 0 for the bearing.

//        assertThat(ZERO_POINT.bearingTo(ZERO_POINT)).isEqualTo(0);

        assertThat(ZERO_POINT.bearingTo(new GeoPoint(1, 0))).isEqualTo(0);
        assertThat(ZERO_POINT.bearingTo(new GeoPoint(-1, 0))).isEqualTo(180);
        assertThat(ZERO_POINT.bearingTo(new GeoPoint(0, 1))).isEqualTo(90, offset(.01));
        assertThat(ZERO_POINT.bearingTo(new GeoPoint(0, -1))).isEqualTo(270, offset(.01));
        assertThat(ZERO_POINT.bearingTo(new GeoPoint(1, 1))).isEqualTo(45, offset(.2));
        assertThat(ZERO_POINT.bearingTo(new GeoPoint(-1, -1))).isEqualTo(225, offset(.2));
    }

    @Test
    public void test_set()
    {
        final GeoPoint gp = createMutable();
        gp.set(new GeoPoint(45, 46, 47));

        assertThat(gp.getLatitude()).isEqualTo(45);
        assertThat(gp.getLongitude()).isEqualTo(46);
        assertThat(gp.getAltitude()).isEqualTo(47);

        gp.set(100d);
        assertThat(gp.getAltitude()).isEqualTo(100);

        gp.set(50, 51);
        assertThat(gp.getLatitude()).isEqualTo(50);
        assertThat(gp.getLongitude()).isEqualTo(51);

        gp.set(60, 61, 62);
        assertThat(gp.getLatitude()).isEqualTo(60);
        assertThat(gp.getLongitude()).isEqualTo(61);
        assertThat(gp.getAltitude()).isEqualTo(62);

        // Verify set doesn't alter an immutable GeoPoint
        final GeoPoint gp2 = new GeoPoint(10, 11, 12);
        gp2.set(60, 61, 62);
        assertThat(gp2.getLatitude()).isEqualTo(10);
        assertThat(gp2.getLongitude()).isEqualTo(11);
        assertThat(gp2.getAltitude()).isEqualTo(12);
    }

    @SuppressLogging(GeoPoint.class)
    @Test
    public void test_parse_geopoint()
    {
        assertThat(parseGeoPoint("")).isNull();
        assertThat(parseGeoPoint("nope")).isNull();
        assertThat(parseGeoPoint("nope,nope")).isNull();

        final GeoPoint p1 = parseGeoPoint("10, 20");
        assertThat(p1.getLatitude()).isEqualTo(10);
        assertThat(p1.getLongitude()).isEqualTo(20);
        assertThat(p1.getAltitude()).isNaN();
        assertThat(p1.getCE()).isNaN();
        assertThat(p1.getLE()).isNaN();

        final GeoPoint p2 = parseGeoPoint("-10, -20, 100");
        assertThat(p2.getLatitude()).isEqualTo(-10);
        assertThat(p2.getLongitude()).isEqualTo(-20);
        assertThat(p2.getAltitude()).isEqualTo(100);
        assertThat(p2.getAltitudeReference()).isEqualTo(AltitudeReference.HAE);
        assertThat(p2.getCE()).isNaN();
        assertThat(p2.getLE()).isNaN();

        final GeoPoint p3 = parseGeoPoint("-10, -20, 100, AGL, 1, 2");
        assertThat(p3.getLatitude()).isEqualTo(-10);
        assertThat(p3.getLongitude()).isEqualTo(-20);
        assertThat(p3.getAltitude()).isEqualTo(100);
        assertThat(p3.getAltitudeReference()).isEqualTo(AltitudeReference.AGL);
        assertThat(p3.getCE()).isEqualTo(1);
        assertThat(p3.getLE()).isEqualTo(2);
    }
}
