package gov.takx.api.playback;

/**
 * An event fired by the playback system to inform interested parties of changes to the time span, the play
 * position, starting and stopping playback, etc.
 *
 * @see ITimeSpan
 * @since 1.47
 */
public enum PlaybackEvent
{
    /**
     * Fired when playback commences.
     */
    Playing,

    /**
     * Fired when playback is paused.
     */
    Paused,

    /**
     * Fires when playback is stopped, resetting to the start position on the time span.
     */
    Stopped,

    /**
     * Fired when the start or end value of the time span has changed.
     */
    TimeSpanChanged,

    /**
     * Fired when the current play position changes.  This only occurs while playing, if the position changes
     * while paused or stopped, a {@link #ScrubPositionChanged} event will be fired instead.
     */
    PlayPositionChanged,

    /**
     * Fired when the user changes the location of the playback cursor.  This only happens while payback is
     * paused or stopped.  If the playback position changes due to normal playback, a {@link #PlayPositionChanged}
     * will be fired instead.
     * <p/>
     * Note that these events will only occur at a maximum frequency of 10 Hz to avoid event flooding, causing
     * possible backlogs in processing by registered listeners.
     */
    ScrubPositionChanged,

    /**
     * Fired after one or more {@link #ScrubPositionChanged} events have been fired followed by a mouse release event.
     *
     * @since 2.26
     */
    ScrubbingStopped,

    /**
     * Fired when the playback speed is changed.
     */
    PlaybackSpeedChanged,

    /**
     * Fired when the playback system has gone into live mode
     */
    Live
}
