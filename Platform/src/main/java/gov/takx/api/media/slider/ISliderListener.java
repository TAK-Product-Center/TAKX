package gov.takx.api.media.slider;

import gov.takx.api.annotation.Experimental;

/**
 * Listener for slider events
 *
 * @since 2.37
 */
@Experimental
public interface ISliderListener
{
    /**
     * The slider has moved
     *
     * @param e the event
     */
    void sliderMoved(SliderEvent e);

    /**
     * The slider drag has begun
     *
     * @param e the event
     */
    void sliderDragBegin(SliderEvent e);

    /**
     * The slider drag has ended
     *
     * @param e the event
     */
    void sliderDragEnd(SliderEvent e);
}
