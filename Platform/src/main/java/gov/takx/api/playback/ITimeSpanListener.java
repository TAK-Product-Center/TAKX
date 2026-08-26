package gov.takx.api.playback;

/**
 * @since 1.47
 */
public interface ITimeSpanListener
{
    /**
     * Called by the time span whenever either the start or end points change, or the X mapping changes (due to a
     * visual component size change).
     * <p/>
     * Note that in some cases this call may subsume a change in the time position,
     * as when the new time span no longer contains the old position, and a separate call to
     * {@link #timePositionChanged()} will not be made.
     */
    void timeSpanChanged();

    /**
     * Called whenever the time position changes.
     */
    void timePositionChanged();

    /**
     * Notification of a new marker added to the time span.
     *
     * @param marker newly added marker.
     */
    void markerAdded(IMarker marker);

    /**
     * Notification of a marker being removed from the time span.
     *
     * @param marker removed marker.
     */
    void markerRemoved(IMarker marker);
}
