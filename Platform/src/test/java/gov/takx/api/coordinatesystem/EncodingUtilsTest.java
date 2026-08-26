package gov.takx.api.coordinatesystem;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class EncodingUtilsTest
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Test
    public void encodingUtilsTest()
    {
        logger.info("Encoding Utils Test");

        double lat = 40.728333;
        double lon = -73.994167;
        double elev = 20;

        ILocation location = new RaptorLocation(lat, lon, elev);

        byte[] bytes = EncodingUtils.encodePoint(location);
        assertThat(bytes).isNotNull();

        List<RaptorLocation> decoded = EncodingUtils.decodePoints(bytes);
        assertThat(decoded).isNotNull();

        assertThat(decoded).hasSize(1);
        assertThat(decoded.get(0)).isEqualTo(location);

        List<ILocation> manyLocations = new ArrayList<ILocation>();
        for (int i = 0; i < 100; i++)
        {
            manyLocations.add(new RaptorLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getElevation()));
        }

        byte[] manyBytes = EncodingUtils.encodePoints(manyLocations);
        assertThat(manyBytes).isNotNull();

        List<RaptorLocation> manyDecoded = EncodingUtils.decodePoints(manyBytes);
        assertThat(manyDecoded).hasSize(manyLocations.size());
        assertThat(manyDecoded).isEqualTo(manyLocations);
    }
}
