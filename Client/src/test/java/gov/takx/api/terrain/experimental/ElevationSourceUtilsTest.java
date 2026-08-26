package gov.takx.api.terrain.experimental;

import com.atakmap.map.elevation.ElevationChunk;
import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.map.layer.feature.geometry.Envelope;
import gov.tak.blacksmith.mock.Mocked;
import gov.takx.test.AIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ElevationSourceUtils}.
 *
 * @since 5.2
 */
public class ElevationSourceUtilsTest extends AIntegrationTest
{
    @Mocked
    private ElevationSource elevationSource;

    @Mocked
    private ElevationSource.Cursor cursor;

    @Mocked
    private ElevationChunk elevationChunk;

    @BeforeEach
    public void beforeEach()
    {
        // This elevationSource will return a cursor that returns a single
        // elevationChunk that always returns an elevation result.
        when(elevationSource.getName()).thenReturn("Mock Elevation Source");
        when(elevationSource.query(any())).thenReturn(cursor);
        when(cursor.moveToNext()).thenReturn(true).thenReturn(false);
        when(cursor.get()).thenReturn(elevationChunk);
        when(elevationChunk.sample(any(), anyInt(), anyInt())).thenAnswer(invocation -> {
            double[] llaBuffer = invocation.getArgument(0);
            int off = invocation.getArgument(1);
            int len = invocation.getArgument(2);
            for (int a = off; a < (len - off); a++)
            {
                int index = a * 3;
                double lon = llaBuffer[index];
                double lat = llaBuffer[index + 1];
                llaBuffer[index + 2] = lat - lon;
            }
            return null;
        });
    }

    @Test
    public void elevationSourceUtils_createHeightmapLLA_altitudeFilled()
    {
        ElevationManager.HeightmapParams params = new ElevationManager.HeightmapParams();
        params.bounds = new Envelope(45.0, -74.0, 0, 46.0, -73.0, 0);
        params.numPostsLat = 50;
        params.numPostsLng = 100;
        double[] llaBuffer = new double[params.numPostsLng * params.numPostsLat * 3];
        Arrays.fill(llaBuffer, Double.NaN);
        ElevationSourceUtils.createHeightmapLLA(elevationSource, 500, params, llaBuffer);
        for (int index = 2; index < params.numPostsLng * params.numPostsLat * 3; index += 3)
        {
            assertThat(llaBuffer[index]).isNotNaN();
        }
    }

    @Test
    public void elevationSourceUtils_createHeightmapLLA_testBounds()
    {
        ElevationManager.HeightmapParams params = new ElevationManager.HeightmapParams();
        params.bounds = new Envelope(20, 25, 0, 40.0, 45.0, 0);
        params.numPostsLat = 5;
        params.numPostsLng = 2;
        double[] llaBuffer = new double[params.numPostsLat * params.numPostsLng * 3];
        ElevationSourceUtils.createHeightmapLLA(elevationSource, 500, params, llaBuffer);
        double[] expectedLatPosts = {45, 40, 35, 30, 25};
        double[] expectedLngPosts = {20, 40};
        assertLatLngPosts(llaBuffer, expectedLatPosts, expectedLngPosts);
    }

    @Test
    public void elevationSourceUtils_createHeightmapLLA_invertYAxis()
    {
        ElevationManager.HeightmapParams params = new ElevationManager.HeightmapParams();
        params.bounds = new Envelope(20, 25, 0, 40.0, 45.0, 0);
        params.numPostsLat = 5;
        params.numPostsLng = 2;
        params.invertYAxis = true;
        double[] llaBuffer = new double[params.numPostsLat * params.numPostsLng * 3];
        ElevationSourceUtils.createHeightmapLLA(elevationSource, 500, params, llaBuffer);
        double[] expectedLatPosts = {25, 30, 35, 40, 45};
        double[] expectedLngPosts = {20, 40};
        assertLatLngPosts(llaBuffer, expectedLatPosts, expectedLngPosts);
    }

    private void assertLatLngPosts(double[] llaBuffer, double[] expectedLatPosts, double[] expectedLngPosts)
    {
        for (int postLat = 0; postLat < expectedLatPosts.length; postLat++)
        {
            for (int postLng = 0; postLng < expectedLngPosts.length; postLng++)
            {
                int index = ((postLat * expectedLngPosts.length) + postLng) * 3;
                assertThat(llaBuffer[index]).isEqualTo(expectedLatPosts[postLat]);
                assertThat(llaBuffer[index + 1]).isEqualTo(expectedLngPosts[postLng]);
                assertThat(llaBuffer[index + 2]).isEqualTo(expectedLatPosts[postLat] - expectedLngPosts[postLng]);
            }
        }
    }
}
