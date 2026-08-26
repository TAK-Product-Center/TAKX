package gov.takx.core.globe.utils;

import gov.tak.api.maps.coords.GeoPoint;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link GeoBoundsUtils}.
 *
 * @since 5.3
 */
public class GeoBoundsUtilsTest
{
    @Test
    public void fromGeoPoints_whenSinglePointProvided_returnsCorrectGeoBounds()
    {
        final var geoPoint = new GeoPoint(1, 1);
        final var geoBounds = GeoBoundsUtils.fromGeoPoints(geoPoint);

        assertThat(geoBounds.getNorth()).isEqualTo(1);
        assertThat(geoBounds.getSouth()).isEqualTo(1);
        assertThat(geoBounds.getEast()).isEqualTo(1);
        assertThat(geoBounds.getWest()).isEqualTo(1);
    }

    @Test
    public void fromGeoPoints_whenMultiplePointsProvided_returnsCorrectGeoBounds()
    {
        final var geoPoint1 = new GeoPoint(1, 1);
        final var geoPoint2 = new GeoPoint(2, 2);
        final var geoBounds = GeoBoundsUtils.fromGeoPoints(geoPoint1, geoPoint2);

        assertThat(geoBounds.getNorth()).isEqualTo(2);
        assertThat(geoBounds.getSouth()).isEqualTo(1);
        assertThat(geoBounds.getEast()).isEqualTo(2);
        assertThat(geoBounds.getWest()).isEqualTo(1);
    }

    @Test
    public void toDegrees_thenFromDegrees_restoresCorrectGeoBounds()
    {
        final var geoPoint1 = new GeoPoint(1, 1, 1); // SW
        final var geoPoint2 = new GeoPoint(2, 2, 2); // NE
        final var geoBounds = GeoBoundsUtils.fromGeoPoints(geoPoint1, geoPoint2);

        final double[] degreesAndAlt = GeoBoundsUtils.toDegreesAndAlt(geoBounds);
        final var geoBoundsRestored = GeoBoundsUtils.fromDegreesAndAlt(degreesAndAlt);

        assertThat(geoBoundsRestored).isEqualTo(geoBounds);
    }

    @Test
    public void contains_returnsCorrectResults()
    {
        final var geoPoint1 = new GeoPoint(1, 1);
        final var geoPoint2 = new GeoPoint(3, 3);
        final var geoBounds = GeoBoundsUtils.fromGeoPoints(geoPoint1, geoPoint2);

        final var includedPoint = new GeoPoint(2, 2);
        final var excludedPoint = new GeoPoint(4, 4);

        assertThat(GeoBoundsUtils.contains(geoBounds, includedPoint)).isTrue();
        assertThat(GeoBoundsUtils.contains(geoBounds, excludedPoint)).isFalse();
    }

    @Test
    public void union_returnsCorrectResults()
    {
        final var geoPoint1 = new GeoPoint(1, 1);
        final var geoPoint2 = new GeoPoint(2, 2);
        final var geoBounds = GeoBoundsUtils.fromGeoPoints(geoPoint1, geoPoint2);

        final var geoPoint3 = new GeoPoint(3, 3);
        final var geoBoundsUnion = GeoBoundsUtils.union(geoBounds, geoPoint3);

        assertThat(geoBoundsUnion.getNorth()).isEqualTo(3);
        assertThat(geoBoundsUnion.getSouth()).isEqualTo(1);
        assertThat(geoBoundsUnion.getEast()).isEqualTo(3);
        assertThat(geoBoundsUnion.getWest()).isEqualTo(1);
    }
}
