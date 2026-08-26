package gov.takx.core.playback.ui;

import gov.takx.api.playback.ITimeSpan;
import gov.takx.platform.playback.TimelineUtils;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * @since 1.47
 */
public class TickModel
{
    private static final double MINIMUM_TIME_SPACING = 65;
    private static final double MINIMUM_TICK_SPACING = 20;
    private static final int MINOR_TICK_COUNT = 4;

    private TimeUnit majorTickUnit = TimeUnit.MINUTES;
    private final ITimeSpan timeSpan;
    private final List<Integer> majorTicks = new CopyOnWriteArrayList<>();
    private final List<Integer> minorTicks = new CopyOnWriteArrayList<>();

    public TickModel(@Nonnull ITimeSpan delegate)
    {
        timeSpan = delegate;
    }

    /**
     * Set the time unit for major ticks.
     *
     * @param majorTickUnit time unit for major ticks.
     */
    public synchronized void setMajorTickUnit(TimeUnit majorTickUnit)
    {
        this.majorTickUnit = majorTickUnit;
        createTicks();
    }

    public void updateSpacings()
    {
        createTicks();
    }

    private double tickToSpacing(long totalTime, TimeUnit tick)
    {
        double tickMillis = tick.toMillis(1);
        double numTicks = totalTime / tickMillis;
        return timeSpan.getEffectiveWidth() / numTicks;
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    public synchronized List<Integer> getMajorTicks()
    {
        return majorTicks;
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    public synchronized List<Integer> getMinorTicks()
    {
        return minorTicks;
    }

    /**
     * Calculates the lists of major and minor ticks, based upon the time span's current status.  The idea is to spread
     * major ticks far enough apart so that their labelled values are legible, while subdividing the spaces in between
     * with minor ticks that make visual sense.  This is the most computationally expensive method in the class, so it
     * is only called when necessary (i.e. when the time span's values change).  The calculated tick lists are then
     * stored for fast retrieval by calling classes.
     */
    private synchronized void createTicks()
    {
        // Determine how many major ticks should be skipped due to the timeline having a very large time span
        // (i.e. if minimum time spacing is 1 minute, but the timeline spans 2 days, many of the minute tick marks
        // must be skipped so that the timeline ruler is not completely filled in with ticks, making it illegible).
        final double majorTickSpacing = Math.max(tickToSpacing(timeSpan.getTotalTime(), majorTickUnit), MINIMUM_TICK_SPACING);
        final int spacingCount = (int) Math.ceil(MINIMUM_TIME_SPACING / majorTickSpacing);
        final double spacedMajorTickSpacing = majorTickSpacing * spacingCount;
        final double minorTickSpacing = spacedMajorTickSpacing / MINOR_TICK_COUNT;

        final List<Integer> newMajorTicks = new ArrayList<>();
        final List<Integer> newMinorTicks = new ArrayList<>();

        for (double i = getMajorTickOffset(), width = timeSpan.getEffectiveWidth(); i < width; i += spacedMajorTickSpacing)
        {
            newMajorTicks.add((int) Math.round(i));

            for (int j = 1; j < MINOR_TICK_COUNT; j++) newMinorTicks.add((int) Math.round(i + j * minorTickSpacing));
        }

        majorTicks.clear();
        majorTicks.addAll(newMajorTicks);
        minorTicks.clear();
        minorTicks.addAll(newMinorTicks);
    }

    /**
     * Get the major tick offset (for time ruler labels).  This is a negative value, created from the nearest lower
     * minute from the start time.
     *
     * @return the major tick offset created from the nearest lower minute from the start time.
     */
    private double getMajorTickOffset()
    {
        long newTime = TimelineUtils.floorMinute(timeSpan.getStartTime());
        return timeSpan.timeToX(newTime);
    }
}
