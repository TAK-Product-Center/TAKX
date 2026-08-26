package gov.takx.core.globe.utils;

/**
 * Simple class to hold the data about a time threshold assignment.  This is just a data holder so members are
 * accessed directly.
 *
 * @since 1.25
 */
public class TimeThresholdAssignment
{
    public final StylingContextKey scKey;
    public final long timeThreshold;

    /**
     * Construct an assignment.
     *
     * @param scKey         The context key that has a new time threshold
     * @param timeThreshold The new time threshold
     */
    public TimeThresholdAssignment(StylingContextKey scKey, long timeThreshold)
    {
        this.scKey = scKey;
        this.timeThreshold = timeThreshold;
    }

    @Override
    public String toString()
    {
        return String.format("[TTA: scKeyo=%s tt=%d]", scKey, timeThreshold);
    }
}
