package gov.takx.api.media.slider;

import gov.takx.api.annotation.Experimental;

import java.awt.Color;
import java.util.Date;

/**
 * Represents a segment along the slider to be painted with a specific color
 *
 * @since 2.37
 */
@Experimental
class SliderColorSegment implements Comparable<SliderColorSegment>
{
    final double start;
    final double end;
    final Color color;
    private final Date date = new Date();

    public SliderColorSegment(double start, double end, Color color)
    {
        this.start = start;
        this.end = end;
        this.color = color;
    }

    @Override
    public int compareTo(SliderColorSegment o)
    {
        int result = Double.compare(start, o.start);
        if (result == 0)
        {
            result = date.compareTo(o.date);
        }
        return result;
    }
}
