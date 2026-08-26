package gov.takx.api.playback;

import gov.takx.api.event.PlatformEvent;

/**
 * CDI Event fired when the playback system enters or leaves "live" mode.  This event is not fired during initial
 * system startup since we start in "live" mode.
 *
 * @since 2.25
 */
public class PlaybackLiveEvent implements PlatformEvent
{
    private final boolean live;

    public PlaybackLiveEvent(boolean live)
    {
        this.live = live;
    }

    /**
     * @return {@code true} if the playback system is in "live" mode, {@code false} otherwise
     */
    public boolean isLive()
    {
        return live;
    }
}
