package gov.takx.core.globe.utils;

import gov.nasa.worldwind.event.SelectEvent;
import gov.tak.platform.ui.IMotionEventHandler;
import gov.tak.platform.ui.MotionEvent;

import java.awt.Point;

/**
 * Adapter for translating NASA WW SelectEvents to TAK MotionEvents for use with AbstractLayer
 * implementations (such as the WidgetLayer)
 *
 * @since 3.0
 */
public class MotionEventAdapter
{

    private IMotionEventHandler handler;
    private long lastDownTime;
    private Point hoverPickPoint;
    private Point dragPickPoint;
    private boolean down;

    /**
     * Handle the select event and raise any resulting MotionEvents
     *
     * @param selectEvent
     */
    public void handleSelectEvent(SelectEvent selectEvent)
    {
        if (selectEvent.getEventAction().equals(SelectEvent.DRAG))
        {
            dragPickPoint = selectEvent.getPickPoint();
        }

        int action = getMotionAction(selectEvent.getEventAction());
        if (action < 0)
        {
            return;
        }

        // hover (from ROLLOVER) seem to spam like made from NWW, so filter duplicates
        if (action == MotionEvent.ACTION_HOVER_MOVE)
        {

            // filter out duplicates
            if (hoverPickPoint != null && hoverPickPoint.equals(selectEvent.getPickPoint()))
            {
                return;
            }

            // filter out hovers during down state
            if (down)
            {
                return;
            }

            hoverPickPoint = selectEvent.getPickPoint();
        } else
        {
            hoverPickPoint = null;
        }

        Point pickPoint = selectEvent.getPickPoint();

        // DRAG_END does not have a pick point
        if (selectEvent.getEventAction().equals(SelectEvent.DRAG_END) && pickPoint == null)
        {
            pickPoint = dragPickPoint;
        }

        long currentTime = System.currentTimeMillis();

        // track the new down time
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN)
        {
            lastDownTime = currentTime;
            down = true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL ||
                action == MotionEvent.ACTION_POINTER_UP)
        {
            down = false;
        }

        if (this.handler != null && pickPoint != null)
        {

            // Obtain simple motion event for primary pointer
            MotionEvent motionEvent = MotionEvent.obtain(
                    lastDownTime,
                    currentTime,
                    action,
                    (float) pickPoint.getX(), (float) pickPoint.getY(),
                    0);

            // consume the select event if handled
            if (handler.handleMotionEvent(motionEvent))
            {
                selectEvent.consume();
            }

            // recycle the motion event
            motionEvent.recycle();
        }
    }

    /**
     * Set the IMotionEventHandler to raise motion events on
     *
     * @param handler
     */
    public void setMotionEventHandler(IMotionEventHandler handler)
    {
        this.handler = handler;
    }

    private int getMotionAction(String eventAction)
    {

        // at the moment, only really concerned with mouse events that neatly translate to
        // primary-pointer MotionEvents that are meaningful to the Widget framework

        if (eventAction.equals(SelectEvent.LEFT_CLICK) || eventAction.equals(SelectEvent.LEFT_DOUBLE_CLICK))
        {
            return MotionEvent.ACTION_UP;
        } else if (eventAction.equals(SelectEvent.LEFT_PRESS))
        {
            return MotionEvent.ACTION_DOWN;
        } else if (eventAction.equals(SelectEvent.DRAG))
        {
            return MotionEvent.ACTION_MOVE;
        } else if (eventAction.equals(SelectEvent.HOVER))
        {
            return MotionEvent.ACTION_HOVER_ENTER;
        } else if (eventAction.equals(SelectEvent.ROLLOVER))
        {
            return MotionEvent.ACTION_HOVER_MOVE;
        } else if (eventAction.equals(SelectEvent.DRAG_END))
        {
            return MotionEvent.ACTION_UP;
        }

        return -1;
    }
}
