package gov.takx.core.playback;

import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.playback.ITimeLocation;

/**
 * Simple holder class to represent a {@link RaptorLocation} at a point in time.  For all intents and purposes, the time
 * stamp is the determiner of equality.  It drives the hashcode, compareTo, and equals methods.  It follows, then,
 * that any set of these objects can contain no more than a single entry for a given time.
 * <p/>
 * To simplify using instances as keys for set operations (like floor, ceil, subset, etc.), the static factory method
 * {@link #makeTimeKey(long)} is provided.
 *
 * @since 1.47
 */
public class TimeLocation extends RaptorLocation implements ITimeLocation
{
    private static final String UNSUPPORTED = TimeLocation.class.getSimpleName() + " instances are immutable";

    private final long time;
    private final int hashCode;

    /**
     * Create an instance with just a timestamp and no location.  Instances of this form should only be used as
     * "keys" for locating iterator/subset entries within a {@link java.util.NavigableSet Set} of these objects.
     * This works since comparison/equality is strictly driven by time, location data is ignored.
     *
     * @param time Relevant time of location
     */
    public static TimeLocation makeTimeKey(long time)
    {
        return new TimeLocation(time, 0, 0, false, 0);
    }

    /**
     * Construct with the given data.
     *
     * @param time         Relevant time of location
     * @param latitude     Latitude of location
     * @param longitude    Longitude of location
     * @param hasElevation Pass {@code true} if this location has a valid elevation, {@code false} otherwise
     * @param elevation    Elevation, ignored if {@code hasElevation} is {@code false}
     */
    public TimeLocation(long time, double latitude, double longitude, boolean hasElevation, double elevation)
    {
        super(latitude, longitude, hasElevation, elevation);

        this.time = time;
        hashCode = Long.hashCode(time);
    }

    /**
     * Construct from the data extracted from a given {@link IRaptorDataMessage}.  The time will be taken from the
     * {@code relevant time} of the RDM.
     *
     * @param rdm RDM from which to extract time/location data
     */
    public TimeLocation(IRaptorDataMessage rdm)
    {
        this(rdm.getRelevantTime(), rdm.getLatitude(), rdm.getLongitude(), rdm.hasElevation(), rdm.getElevation());
    }

    @Override
    public long getTime()
    {
        return time;
    }

    /**
     * Disallow this operation since instances of this class are immutable.
     */
    @Override
    public void setElevation(double elevation)
    {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    /**
     * Disallow this operation since instances of this class are immutable.
     */
    @Override
    public void setLatitude(double latitude) throws IllegalArgumentException
    {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    /**
     * Disallow this operation since instances of this class are immutable.
     */
    @Override
    public void setLongitude(double longitude) throws IllegalArgumentException
    {
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    @Override
    public int compareTo(ITimeLocation o)
    {
        return Long.compare(time, o.getTime());
    }

    @Override
    public boolean equals(Object o)
    {
        return this == o || !(o == null || getClass() != o.getClass()) && time == ((ITimeLocation) o).getTime();
    }

    @Override
    public int hashCode()
    {
        return hashCode;
    }
}
