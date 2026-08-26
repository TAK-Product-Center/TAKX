package gov.tak.api.comms.takserver;

import jakarta.annotation.Nonnull;

import java.util.Arrays;

/**
 * Time threshold options to be used when specifying a time period before some action should be taken.
 *
 * @since 5.3 (Moved to platform)
 */
public enum TimeThresholdOption
{
    // indicates no threshold, i.e. "keep all"
    none("All", Long.MAX_VALUE),
    today("Today", 1),
    days7("7 days", 7),
    days14("14 days", 14),
    days30("30 days", 30),
    days60("60 days", 60),
    days120("120 days", 120),
    days180("180 days", 180);

    private final String displayText;
    private final long timeInDays;

    TimeThresholdOption(@Nonnull String displayText, long timeInDays)
    {
        this.displayText = displayText;
        this.timeInDays = timeInDays;
    }

    /**
     * @return Display text for the threshold.
     * @since 5.3
     */
    @Nonnull
    public String getDisplayText()
    {
        return displayText;
    }

    /**
     * @return Length in days for the threshold.
     * @since 5.3
     */
    public long getTimeInDays()
    {
        return timeInDays;
    }

    /**
     * @param displayText The threshold's display text
     * @return The {@link TimeThresholdOption} with the matching display text or {@link #none} if there's no match
     */
    @Nonnull
    public static TimeThresholdOption getThreshold(@Nonnull String displayText)
    {
        return Arrays.stream(values())
                .filter(st -> st.displayText.equals(displayText))
                .findFirst()
                .orElse(none);
    }

    @Override
    public String toString()
    {
        return displayText;
    }
}
