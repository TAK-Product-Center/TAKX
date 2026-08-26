package gov.takx.core.playback.globe;

import gov.takx.core.playback.APlaybackIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 2.18
 */
public class GlobePlaybackLaneTest extends APlaybackIntegrationTest
{
    @Test
    @Disabled // TODO KMB: Fix test (TAKX-1333)
    public void testMapObjectAddRemove()
    {
        GlobePlaybackLane gpl = new GlobePlaybackLane();
        gpl.addMapObjects(Collections.singletonList(mapObject1));
        assertThat(gpl.getMapObjects()).hasSize(1);
        gpl.addMapObjects(Collections.singletonList(mapObject2));
        assertThat(gpl.getMapObjects()).hasSize(2);
        gpl.addMapObjects(Collections.singletonList(mapObject2));
        assertThat(gpl.getMapObjects()).hasSize(2);
        gpl.removeMapObjects(Collections.singletonList(mapObject1));
        assertThat(gpl.getMapObjects()).hasSize(1);
    }
}
