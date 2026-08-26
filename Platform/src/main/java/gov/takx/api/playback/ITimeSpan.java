package gov.takx.api.playback;

import gov.takx.platform.playback.TimeBoundary;

import java.awt.Component;
import java.util.List;

/**
 * This class is used to keep track of a span of time, which is used as a timeline model.  Keeps track of start and
 * end times, cursor position, and notifies listeners of changes within.
 * <p/>
 * A visual component is also tracked, allowing for conversions between time positions and X locations within that
 * visual component.
 * <p/>
 * Currently, the time span implementation has a max duration of 5 years.
 *
 * @since 1.47
 */
@SuppressWarnings("UnusedDeclaration")
public interface ITimeSpan
{
    /**
     * The default factor by which to zoom/scale the time span.
     */
    double DEFAULT_ZOOM_FACTOR = 1.5;

    /**
     * The default amount to pan the timespan.
     */
    int DEFAULT_PAN_OFFSET = 50;

    /**
     * @return A TimeBoundary representing the start/end points of this time span
     */
    TimeBoundary getTimeBoundary();

    /**
     * Set the visual component for this time span.  The visual component's width affects calculations for utility
     * methods such as {@link #xToTime(int)} or {@link #setXPosition(int)}.
     *
     * @param owner Component to which this time span will use for width-based calculations
     */
    void setVisualComponent(Component owner);

    /**
     * @return The visual component specified on this time span
     */
    Component getVisualComponent();

    /**
     * Get the effective width of the timeline.  This is the actual width, in pixels, of the timeline, even if it
     * is not fully visible in the main panel (i.e. the timeline is scrollable).
     *
     * @return the effective width of the timeline in pixels.
     */
    int getEffectiveWidth();

    /**
     * Set the time span with a start and end time.
     *
     * @param startTime start time, in milliseconds
     * @param endTime   end time in milliseconds
     */
    void setTimes(long startTime, long endTime);

    /**
     * Set the time span to match the given time boundary.
     *
     * @param timeBoundary Time boundary from which to extract start/end times
     */
    void setTimes(TimeBoundary timeBoundary);

    /**
     * Set the start time.
     *
     * @param startTime start time, in milliseconds
     */
    void setStartTime(long startTime);

    /**
     * Set the end time.
     *
     * @param endTime end time, in milliseconds
     */
    void setEndTime(long endTime);

    /**
     * Get the start time.
     *
     * @return the start time, in milliseconds
     */
    long getStartTime();

    /**
     * Get the end time.
     *
     * @return the end time, in milliseconds
     */
    long getEndTime();

    /**
     * Get the total time (difference between end time and start time).
     *
     * @return the total time, in milliseconds
     */
    long getTotalTime();

    /**
     * Set the playback position.
     *
     * @param position the playback position, in milliseconds.
     */
    void setPosition(long position);

    /**
     * Set the playback position with an x-coordinate (x position in the timeline).
     *
     * @param xPosition the playback position as an x-coordinate
     */
    void setXPosition(int xPosition);

    /**
     * Get the playback position delta (difference between position and start time).
     *
     * @return the playback position delta, in milliseconds
     */
    long getDeltaPosition();

    /**
     * Get the playback position.
     *
     * @return the playback position, in milliseconds
     */
    long getPosition();

    /**
     * Get the playback position x-coordinate (x position in the timeline).
     *
     * @return the playback position as an x-coordinate
     */
    int getXPosition();

    /**
     * Zoom the time range with a new total time.  This value is offset from the start time and sets the end time.
     *
     * @param newTotalTime the new total time, in milliseconds
     */
    void zoomTime(long newTotalTime);

    /**
     * Convert an x-coordinate (from inside the timeline) to a time.
     *
     * @param x an x-coordinate within the timeline.
     * @return the respective timeline time, in milliseconds
     */
    long xToTime(int x);

    /**
     * Convert a time to an x-coordinate in the timeline.
     *
     * @param time time, in milliseconds
     * @return the respective x-coordinate in the timeline, in double precision.
     */
    double timeToX(long time);

    /**
     * Convert a time to an x-coordinate in the timeline, rounded to the nearest integer.
     *
     * @param time time, in milliseconds
     * @return the respective x-coordinate in the timeline, rounded to the nearest integer.
     */
    int timeToXRounded(long time);

    /**
     * Shift the time span to an earlier or later point in time, based upon the xOffset.  This shift/pan does not
     * change the total time duration of the time span.
     *
     * @param xOffset x-coordinate offset by which to shift the time span.
     */
    void panTimeByXOffset(int xOffset);

    /**
     * Scale the time span out - that is, extend the time span - by a factor.  The scale will position itself with
     * respect to the x-coordinate.
     *
     * @param factor factor by which to scale out.
     * @param x      x-coordinate to scale toward.
     */
    void scaleTimeOut(double factor, int x);

    /**
     * Scale the time span in - that is, decrease the time span - by a factor.  The scale will position itself with
     * respect to the x-coordinate.
     *
     * @param factor factor by which to scale in.
     * @param x      x-coordinate to scale toward.
     */
    void scaleTimeIn(double factor, int x);

    /**
     * Add a time delegate listener.
     *
     * @param listener a time delegate listener to be added
     */
    void addListener(ITimeSpanListener listener);

    /**
     * Remove a time delegate listener
     *
     * @param listener a time delegate listener to be removed
     */
    void removeListener(ITimeSpanListener listener);

    /**
     * Create a marker with a moment timestamp.  Optionally associate the new marker with any number of playback lanes.
     *
     * @param label       label to associate with this marker.
     * @param time        timestamp moment to associate with this marker (this is where the marker will be displayed
     *                    on the timeline).
     * @param targetLanes optional playback lanes to associate with this marker.  The marker will display a shape or
     *                    icon on the track(s) associated with itself.
     * @return the newly created moment marker.
     */
    IMarker createMomentMarker(String label, long time, IPlaybackLane... targetLanes);

    /**
     * Manually add a marker to this time span.
     *
     * @param marker a marker to add to this time span.
     */
    void addMarker(IMarker marker);

    /**
     * Remove a marker from this time span.  If the marker is not currently added to the time span, then the request
     * is ignored and no events are fired.
     *
     * @param marker a marker to remove from this time span.
     */
    void removeMarker(IMarker marker);

    /**
     * Get a list of the markers in this time span.
     *
     * @return list of the markers in this time span.
     */
    List<IMarker> getMarkers();

    /**
     * Determine whether or not a timestamp is contained within this time span.
     *
     * @param time timestamp to test for containment inside this time span.
     * @return {@code true} if this time span contains this timestamp, or {@code false} otherwise.
     */
    boolean containsTime(long time);

    boolean intersects(long start, long end);
}
