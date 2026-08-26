package gov.takx.test.utils;

import gov.takx.api.playback.ITimeSpan;
import gov.takx.platform.playback.TimeSpan;

import java.awt.Component;

/**
 * @since 1.48
 */
public class PlaybackTestUtils
{
    /**
     * Construct a new TimeSpan with a visual component of the given initial width.
     *
     * @param width Width of visual component
     * @return new TimeSpan
     */
    public static ITimeSpan makeTimeSpanWithComponent(int width, long startTime, long endTime)
    {
        final ITimeSpan timeSpan = new TimeSpan();
        timeSpan.setTimes(startTime, endTime);

        addComponentToTimeSpan(timeSpan, width);

        return timeSpan;
    }

    /**
     * Add a visual component with the given width to the given time span.
     *
     * @param timeSpan Time span to update
     * @param width    initial width of component to create
     * @return Created component
     */
    public static Component addComponentToTimeSpan(ITimeSpan timeSpan, int width)
    {
        final Component c = new Component()
        {
        };

        c.setBounds(0, 0, width, 10);
        timeSpan.setVisualComponent(c);

        return c;
    }
}
