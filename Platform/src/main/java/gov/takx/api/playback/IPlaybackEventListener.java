package gov.takx.api.playback;

/**
 * Interface defining the contract for listeners of playback events.  These events are the "top level" aggregated
 * events from numerous sub-components within the playback system.  If you are interested in specific element
 * events, then you should subscribe to those event channels.
 *
 * @see PlaybackEvent
 * @since 1.47
 */
public interface IPlaybackEventListener
{
    /**
     * Callback for the given playback event.
     *
     * @param playbackEvent The event fired
     */
    void onPlaybackEvent(PlaybackEvent playbackEvent);
}
