package gov.takx.platform.playback;

import java.util.List;

/**
 * A representation of a range of time.  Basically just a simple start/end tuple.  Utility methods
 * are available for detecting overlap ({@link #intersects(TimeBoundary)} and to determine the maximum boundaries
 * given a list of boundaries ({@link #union(java.util.List)}.
 *
 * @since 1.47
 */
@SuppressWarnings("UnusedDeclaration") // Some methods only used by plugins
public class TimeBoundary
{
    public long startTime;
    public long endTime;

    /**
     * Construct using the given boundary values.
     *
     * @param startTime Start time boundary
     * @param endTime   End time boundary
     */
    public TimeBoundary(long startTime, long endTime)
    {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Copy constructor.
     *
     * @param boundary Boundary to copy
     */
    public TimeBoundary(TimeBoundary boundary)
    {
        startTime = boundary.startTime;
        endTime = boundary.endTime;
    }

    /**
     * @return The start time of the boundary
     */
    public long getStartTime()
    {
        return startTime;
    }

    /**
     * Set the start time of this boundary.
     *
     * @param startTime New start time
     */
    public void setStartTime(long startTime)
    {
        this.startTime = startTime;
    }

    /**
     * @return The end time of the boundary
     */
    public long getEndTime()
    {
        return endTime;
    }

    /**
     * Set the end time of this boundary.
     *
     * @param endTime New end time
     */
    public void setEndTime(long endTime)
    {
        this.endTime = endTime;
    }

    /**
     * Determine whether another time boundary intersects this time boundary.
     *
     * @param timeBoundary another time boundary
     * @return {@code true} if an intersection exists, {@code false} otherwise
     */
    public boolean intersects(TimeBoundary timeBoundary)
    {
        return isContained(timeBoundary.startTime) || isContained(timeBoundary.endTime) ||
                timeBoundary.isContained(startTime) || timeBoundary.isContained(endTime);
    }

    /**
     * Determine whether a moment in time is contained by this time boundary.
     *
     * @param time moment in time
     * @return {@code true} if the moment is contained in this time boundary, {@code false} otherwise.
     */
    public boolean isContained(long time)
    {
        return time >= startTime && time <= endTime;
    }

    /**
     * Update this boundary so that its bounds are the extremes of this instance and another.
     *
     * @param boundary The other instance to union with
     */
    public void union(TimeBoundary boundary)
    {
        if (boundary.startTime < startTime) startTime = boundary.startTime;
        if (boundary.endTime > endTime) endTime = boundary.endTime;
    }

    /**
     * Given a list of time boundaries, compute the time boundary defined by the least start time of any boundary
     * and the greatest end time of any boundary.
     *
     * @param boundaryList List of boundaries, {@code null} entries are legal and simply ignored
     * @return {@link TimeBoundary} created using min start time and max end time, {@code null} if the boundaryList was empty
     */
    public static TimeBoundary union(List<TimeBoundary> boundaryList)
    {
        TimeBoundary union = null;

        for (TimeBoundary boundary : boundaryList)
        {
            if (boundary == null) continue; // Ignore null entries

            if (union == null)
            {
                union = new TimeBoundary(boundary); // Start with a clone of the first boundary
            } else
            {
                union.union(boundary);
            }
        }

        return union;
    }

    /**
     * @return The span of time represented by this time boundary (end - start)
     */
    public long getTotalTime()
    {
        return endTime - startTime;
    }

    /**
     * @return The point in time halfway between the start and end times
     */
    public long getCenterTime()
    {
        return startTime + getTotalTime() / 2;
    }

    /**
     * Move the start time of this boundary to the given new start time, adjusting the end time so the duration
     * is unchanged.
     *
     * @param newStartTime New start time
     */
    public void move(long newStartTime)
    {
        final long newEndTime = newStartTime + getTotalTime();

        startTime = newStartTime;
        endTime = newEndTime;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TimeBoundary that = (TimeBoundary) o;

        return endTime == that.endTime && startTime == that.startTime;
    }

    @Override
    public int hashCode()
    {
        int result = Long.hashCode(startTime);
        result = 31 * result + Long.hashCode(endTime);
        return result;
    }

    @Override
    public String toString()
    {
        return "[" + startTime + ", " + endTime + "]";
    }
}
