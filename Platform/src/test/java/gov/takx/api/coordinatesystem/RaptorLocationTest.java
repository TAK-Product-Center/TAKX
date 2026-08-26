package gov.takx.api.coordinatesystem;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 */
public class RaptorLocationTest
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Test
    public void raptorLocationTest()
    {
        logger.info("RaptorLocation Test");

        double lat = 51.30995;
        double lon = 1.28836;
        double elev = 1000;

        RaptorLocation location = new RaptorLocation(lat, lon);
        assertThat(location.hasElevation()).isFalse();

        location.setElevation(elev);
        assertThat(location.hasElevation()).isTrue();

        assertThat(location).isEqualTo(new RaptorLocation(lat, lon, elev));

        location.setLatitude(lat + 1);
        assertThat(location.getLatitude()).isEqualTo(lat + 1);

        location.setLongitude(lon + 1);
        assertThat(location.getLongitude()).isEqualTo(lon + 1);

        String locationString = location.toString();
        assertThat(locationString).isNotNull();

        ILocation location2 = CoordinateSystem.computeLatLonFromString(locationString);
        assertThat(location2).isNotNull();

        assertThat(location2.getLatitude()).isCloseTo(location.getLatitude(), within(0.01));
        assertThat(location2.getLongitude()).isCloseTo(location.getLongitude(), within(0.01));
        assertThat(location2.getElevation()).isCloseTo(location.getElevation(), within(0.01));

        String locationString2 = location.toString(CoordinateSystem.DECIMAL_DEGREES);
        assertThat(locationString2).isNotNull();

        ILocation location3 = CoordinateSystem.computeLatLonFromString(locationString2);
        assertThat(location3).isNotNull();

        assertThat(location3.getLatitude()).isCloseTo(location.getLatitude(), within(0.01));
        assertThat(location3.getLongitude()).isCloseTo(location.getLongitude(), within(0.01));
        assertThat(location3.getElevation()).isCloseTo(location.getElevation(), within(0.01));
    }
}
