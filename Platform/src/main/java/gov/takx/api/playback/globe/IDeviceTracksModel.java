package gov.takx.api.playback.globe;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.playback.IDisposable;
import gov.takx.api.playback.ITimeLocation;
import gov.takx.platform.playback.TimeBoundary;

import java.util.NavigableSet;
import java.util.concurrent.TimeUnit;

/**
 * Contract for a device tracks model.  Each device that is being viewed in a playback lane will have an
 * instance of this model.
 * <p/>
 * The data in this model is loaded by a background thread.  If you need to know if that initialization has
 * completed, see {@link #isInitialized()} and {@link #waitForInitialization(long, java.util.concurrent.TimeUnit)}
 *
 * @since 1.47
 */
@NoPluginImplementations
public interface IDeviceTracksModel extends IDisposable
{
    /**
     * @return The time boundary for the tracks contained in this model
     */
    TimeBoundary getTimeBoundary();

    /**
     * @return The first track point in this model, {@code null} if the model is empty
     */
    ITimeLocation getFirstTrackPoint();

    /**
     * @return The last track point in this model, {@code null} if the model is empty
     */
    ITimeLocation getLastTrackPoint();

    /**
     * Get the track point that has a time stamp closest to the given time.  The selected track point will have a
     * time &lt;= the given time.
     *
     * @param time Time of interest
     * @return Closest track point with a time &lt;= the given time
     */
    ITimeLocation getClosestTrackPointInPlayback(long time);

    /**
     * Get the least track point with a time greater than the given time, i.e., the ceiling of the key+1 in the set.
     *
     * @param time Time of interest
     * @return Least track point with a time greater than the given time, or {@code null} if there is no such entry
     */
    ITimeLocation getNextTrackPointInPlayback(long time);

    /**
     * Get the greatest track point with a time less than the given time, i.e., the floor of the key-1 in the set
     *
     * @param time Time of interest
     * @return Greatest track point with a time less than the given time, or {@code null} if there is no such entry
     */
    ITimeLocation getPreviousTrackPointInPlayback(long time);

    /**
     * Returns the track points between start/end times of the current TimeSpan (inclusive). Note that the
     * returned collection is modifiable and directly backed by the full set of track points.  This is for
     * performance reasons, you should NEVER directly modify this collection in any way.
     *
     * @return the track points between the start and end times of the TimeSpan (inclusive)
     */
    NavigableSet<ITimeLocation> getTrackPointsInPlayback();

    /**
     * Returns the track points between to the two given times (inclusive). Note that the returned collection is
     * modifiable and directly backed by the full set of track points.  This is for performance reasons, you
     * should NEVER directly modify this collection in any way.
     *
     * @param startTime lower bound of the track points
     * @param endTime   upper bound of the track points
     * @return the track points between the two times (inclusive)
     */
    NavigableSet<ITimeLocation> getTrackPoints(long startTime, long endTime);

    /**
     * Returns the collection of all track points in this model.  Note that the returned collection is
     * modifiable.  This is for performance reasons, you should NEVER directly modify this collection in any way.
     *
     * @return the track points
     */
    NavigableSet<ITimeLocation> getTrackPoints();

    /**
     * Add a listener.  If a listener is added more than once, it will receive notifications an equal number of times.
     *
     * @param listener Listener to add
     */
    void addListener(IDeviceTracksModelListener listener);

    /**
     * Remove a listener.  If a listener was added more than once, it must be removed an equal number of times in
     * order to stop receiving event notifications.  If the listener is not registered, this request is ignored.
     *
     * @param listener Listener to remove
     */
    void removeListener(IDeviceTracksModelListener listener);

    /**
     * @return {@code true} if the track data has been initialized, {@code false} if not
     */
    boolean isInitialized();

    /**
     * Wait for the model to be initialized (all track points loaded from persistence).
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     */
    void waitForInitialization(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Get the track point that has a time stamp closest to the given time.  The selected track point will have a
     * time &lt;= the given time. Note that this method looks at the global timeline as opposed to only the visible
     * timespan.
     *
     * @param time Time of interest
     * @return Closest track point with a time &lt;= the given time
     * @since 2.18
     */
    ITimeLocation getPreviousTrackPoint(long time);
}
