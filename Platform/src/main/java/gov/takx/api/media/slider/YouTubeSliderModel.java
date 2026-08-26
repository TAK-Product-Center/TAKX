package gov.takx.api.media.slider;

import gov.takx.api.annotation.Experimental;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.Observable;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Represents the state of a {@link YouTubeSlider}
 *
 * @since 2.37
 */
@Experimental
class YouTubeSliderModel extends Observable
{
    SortedSet<SliderColorSegment> sliderColorSegments = new TreeSet<>();

    private Color colorBehindThumb = Color.red;
    private boolean drawColorBehindThumb = true;
    private double thumbPosition = 0.0;
    private Color thumbColor = Color.red;

    /**
     * Creates a dynamic slider model with a default color segment.
     */
    YouTubeSliderModel()
    {
        sliderColorSegments.add(new SliderColorSegment(0.0f, 1.0f, Color.lightGray));
    }

    /**
     * Creates a dynamic slider model with specified color segments.
     *
     * @param segments the segments to build into the slider track
     */
    YouTubeSliderModel(SliderColorSegment... segments)
    {
        sliderColorSegments.addAll(Arrays.asList(segments));
    }

    /**
     * Retrieves the color that will be painted to the left of the thumb.
     *
     * @return the color
     * @see #isDrawColorBehindThumbEnabled()
     */
    Color getColorBehindThumb()
    {
        return colorBehindThumb;
    }

    /**
     * Sets the color that will be painted to the left of the thumb.
     *
     * @param color the color
     * @see #isDrawColorBehindThumbEnabled()
     */
    void setColorBehindThumb(Color color)
    {
        setChanged();
        colorBehindThumb = color;
        notifyObservers();
    }

    /**
     * Sets whether a solid color should be painted to the left of the thumb.
     *
     * @param enabled true if the color-behind-thumb should be painted, false otherwise
     */
    void setDrawColorBehindThumb(boolean enabled)
    {
        setChanged();
        drawColorBehindThumb = enabled;
        notifyObservers();
    }

    /**
     * Retrieves whether a solid color should be painted to the left of the thumb.
     *
     * @return true if the color-behind-thumb should be painted, false otherwise
     */
    boolean isDrawColorBehindThumbEnabled()
    {
        return drawColorBehindThumb;
    }

    /**
     * Retrieves the thumb position as a fraction of its maximum value (between 0.0 and 1.0).
     *
     * @return the thumb position
     */
    double getThumbPosition()
    {
        return thumbPosition;
    }

    /**
     * Sets the thumb position to a fraction of its maximum value.
     *
     * @param thumbPosition the position to set. If a value outside the range 0.0 - 1.0 is passed, the function will lock the input to that range.
     */
    void setThumbPosition(double thumbPosition)
    {
        if (thumbPosition < 0.0)
        {
            thumbPosition = 0.0;
        }
        if (thumbPosition > 1.0)
        {
            thumbPosition = 1.0;
        }

        setChanged();
        this.thumbPosition = thumbPosition;
        notifyObservers();
    }

    /**
     * Retrieves the color of the thumb.
     *
     * @return the color
     */
    Color getThumbColor()
    {
        return thumbColor;
    }

    /**
     * Sets the color of the thumb
     *
     * @param color the color
     */
    void setThumbColor(Color color)
    {
        thumbColor = color;
    }

    /**
     * Adds a color segment to the slider track.
     *
     * @param segment the color segment
     */
    void addColorSegment(SliderColorSegment segment)
    {
        setChanged();
        sliderColorSegments.add(segment);
        notifyObservers();
    }

    /**
     * Adds a color segment to the slider track.
     *
     * @param start the fraction of the slider's value where the segment begins
     * @param end   the fraction of the slider's value where the segment ends
     * @param color the color of the segment
     */
    void addColorSegment(double start, double end, Color color)
    {
        addColorSegment(new SliderColorSegment(start, end, color));
    }

    /**
     * Retrieves the set of color segments that have been set on this slider.
     */
    Set<SliderColorSegment> getSliderColorSegments()
    {
        return Collections.unmodifiableSortedSet(sliderColorSegments);
    }
}
