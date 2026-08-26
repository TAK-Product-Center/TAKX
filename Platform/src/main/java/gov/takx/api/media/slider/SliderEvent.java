package gov.takx.api.media.slider;

import gov.takx.api.annotation.Experimental;

/**
 * Represents a slider event. Construct the event using one of the values from {@link SliderEventType}.
 *
 * @since 2.37
 */
@Experimental
public class SliderEvent
{
    /**
     * The allowable slider event types
     */
    public enum SliderEventType
    {
        /**
         * The slider has moved.
         */
        SLIDER_MOVED,
        /**
         * The slider drag button is clicked down and moving has begun.
         */
        SLIDER_DRAG_BEGIN,
        /**
         * The slider button has been released
         */
        SLIDER_DRAG_END;
    }

    private final SliderEventType eventType;
    private final double thumbPercent;

    public SliderEvent(SliderEventType eventType, double thumbPercent)
    {
        this.eventType = eventType;
        this.thumbPercent = thumbPercent;
    }

    public double getThumbPercent()
    {
        return thumbPercent;
    }
}
