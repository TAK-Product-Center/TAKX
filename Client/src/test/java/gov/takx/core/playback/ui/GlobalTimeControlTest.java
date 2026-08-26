package gov.takx.core.playback.ui;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectPartialDelete;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.core.playback.APlaybackIntegrationTest;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 2.18
 */
@PseudoBean
public class GlobalTimeControlTest extends APlaybackIntegrationTest
{
    @Inject
    private Event<MapObjectAdded> mapObjectAddedEvent;

    @Inject
    private Event<MapObjectPartialDelete> mapObjectPartialDeleteEvent;

    @Test
    @Disabled // TODO KMB: Fix test (TAKX-1333)
    public void testMapObjectPartialDelete()
    {
        GTCTest gtc = new GTCTest();
        gtc.init();
        mapObjectAddedEvent.fire(new MapObjectAdded(mapObject1));
        int beforeCount = gtc.getTrackCount(mapObject1);
        mapObjectPartialDeleteEvent.fire(new MapObjectPartialDelete(mapObject1, 3000));
        int afterCount = gtc.getTrackCount(mapObject1);
        assertThat(afterCount).isEqualTo(beforeCount - 2);

        mapObjectPartialDeleteEvent.fire(new MapObjectPartialDelete(mapObject1, 5500));
        afterCount = gtc.getTrackCount(mapObject1);
        assertThat(afterCount).isEqualTo(beforeCount - 5);
    }

    private static class GTCTest extends GlobalTimeControl
    {
        public int getTrackCount(IMapObject mapObject)
        {
            return trackTimes.get(mapObject.getKey()).size();
        }

        public void init()
        {
            hasBeenShown = true;
        }
    }
}
