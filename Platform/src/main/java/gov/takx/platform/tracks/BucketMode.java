package gov.takx.platform.tracks;

/**
 */
public enum BucketMode
{
    None("None"),
    HourOfDay("Hour of the Day"),
    DayOfWeek("Day of the Week"),
    MonthOfYear("Month of the Year");
//    Year("Year"),  // Not supported yet, if ever
//    MotionNoMotion("Motion/No motion");

    private String displayName;

    /**
     * Constructor.
     *
     * @param displayName The display name (for use in UI components) of the constant
     */
    private BucketMode(String displayName)
    {
        this.displayName = displayName;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
