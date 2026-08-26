package gov.tak.api.icons;

import gov.tak.api.maps.Marker;
import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.core.mapobject.MapObjectManager;
import gov.takx.core.persist.MapObjectPersist;
import gov.takx.dataservices.persist.MarkerMapObject;
import gov.takx.test.AContainerTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validate {@link PredefinedCotIconAdapter} functionality.
 *
 * @since 3.0
 */
public class PredefinedCotIconAdapterTest extends AContainerTest
{
    private final PredefinedCotIconAdapter iconAdapter = new PredefinedCotIconAdapter();

    @SuppressWarnings("unused")
    @Mocked(parentTypes = IMapObjectPersist.class)
    private MapObjectPersist mapObjectPersist;

    @SuppressWarnings("unused")
    @Mocked(parentTypes = IMapObjectManager.class)
    private MapObjectManager mapObjectManager;

    @Test
    public void test_no_match_returns_false()
    {
        assertThat(iconAdapter.adapt(makeMarker("no-match"))).isFalse();
    }

    @Test
    public void test_spot_check_several_types()
    {
        verifyAdapted("a-f-G-E-S-t", "ttl-icon-f.png");
        verifyAdapted("b-i-x-i", "camera.png");
        verifyAdapted("b-a-o-tbl", "alarm-trouble.png");
        verifyAdapted("b-m-p-s-p-op", "binos.png");
        verifyAdapted("stack", "cas_stack.png");
    }

    @Test
    public void test_fallback_to_shorter_prefix()
    {
        verifyAdapted("b-not-present", "b.png");
        verifyAdapted("b-i-not-present", "document.png");
    }

    /**
     * Verify that a marker with the given cotType is adapted with the given iconName.
     */
    private void verifyAdapted(String cotType, String iconName)
    {
        Marker marker = makeMarker(cotType);
        assertThat(iconAdapter.adapt(marker)).isTrue();
        assertThat(marker.getIconImageURI().toString()).endsWith(iconName);
    }

    /**
     * Create a Marker with the given CoT type.
     */
    private Marker makeMarker(String cotType)
    {
        final MapObjectSpec spec = MapObjectSpec.builder("test", IMarkerMapObject.MARKER_TYPE)
                .cotType(cotType).build();

        return new MarkerMapObject(spec, null);
    }
}
