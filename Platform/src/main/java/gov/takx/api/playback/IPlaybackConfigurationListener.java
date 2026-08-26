package gov.takx.api.playback;

/**
 * Contract for receiving notifications about changes in the playback configuration.  The configuration
 * is different than the low-level {@link PlaybackEvent} in that it covers elements
 * like the playback lanes added/removed from the system, whether dynamic time span end-points are enabled,
 * whether to show future tracks or icons, etc.
 *
 * @since 1.47
 */
public interface IPlaybackConfigurationListener
{
    /**
     * The playback speed has been changed, either via the UI or API.
     *
     * @param speed The new playback speed
     */
    void playbackSpeedChanged(int speed);

    /**
     * A set of playback lanes have been added to the configuration.
     *
     * @param playbackLanes Lanes added
     */
    void playbackLanesAdded(IPlaybackLane... playbackLanes);

    /**
     * A set of playback lanes have been removed from the configuration.
     *
     * @param playbackLanes Lanes removed
     */
    void playbackLanesRemoved(IPlaybackLane... playbackLanes);

    /**
     * One of the display options has changed.  The display options include the show/hide setting for future
     * tracks and MO icons during playback.
     */
    void displayOptionsChanged();
}
