package gov.takx.platform.playback;

import gov.takx.api.playback.IMarker;
import gov.takx.api.playback.IPlaybackLane;
import gov.takx.api.playback.ITimeSpan;
import gov.takx.api.playback.ITimeSpanListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Concrete implementation of a {@link ITimeSpan}.
 *
 * @since 1.47
 */
public class TimeSpan extends TimeBoundary implements ITimeSpan
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected long position = -1;
    protected int xPosition = -1;
    protected final List<ITimeSpanListener> listeners = new CopyOnWriteArrayList<>();
    protected Component visualComponent;
    protected final List<IMarker> markers = new CopyOnWriteArrayList<>();

    public TimeSpan()
    {
        super(-1, -1);
    }

    @Override
    public TimeBoundary getTimeBoundary()
    {
        return this;
    }

    @Override
    public void setVisualComponent(Component component)
    {
        this.visualComponent = component;
        this.visualComponent.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                updateTimeValues();
            }
        });
    }

    @Override
    public Component getVisualComponent()
    {
        return visualComponent;
    }

    @Override
    public int getEffectiveWidth()
    {
        return visualComponent != null ? visualComponent.getWidth() : 0;
    }

    @Override
    public void addListener(ITimeSpanListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeListener(ITimeSpanListener listener)
    {
        listeners.remove(listener);
    }

    @Override
    public void setTimes(TimeBoundary timeBoundary)
    {
        setTimes(timeBoundary.getStartTime(), timeBoundary.getEndTime());
    }

    @Override
    public void setTimes(long startTime, long endTime)
    {
        if (getStartTime() == startTime && getEndTime() == endTime) return; // Nothing changed

        long newDuration = endTime - startTime;

        if (logger.isDebugEnabled())
        {
            logger.debug("setTimes({},{}) st={} et={} cdur={} ndur={}",
                    startTime, endTime, getStartTime(), getEndTime(), getEndTime() - getStartTime(), newDuration);
        }

        if (newDuration < 0)
        {
            return;
        }

        // If we're already initialized, then avoid tweaking the timeline if the new duration is too big/small
        if (getStartTime() != -1 && getEndTime() != -1)
        {
            final long currentDuration = getEndTime() - getStartTime();

            // If this time span already has a duration of (greater than or equal to) five years, just return.
            // but only return if we are attempting to zoom out
            if (newDuration >= TimelineUtils.FIVE_YEARS && currentDuration >= TimelineUtils.FIVE_YEARS && currentDuration != newDuration)
            {
                return;
            }
            // If this time span already has a duration of less than one second, just return.
            // but only return if we are attempting to zoom in
            else if (newDuration <= TimelineUtils.SECOND && currentDuration <= TimelineUtils.SECOND && currentDuration != newDuration)
            {
                return;
            }
        }

        boundTimes(startTime, endTime, newDuration);
    }

    private void boundTimes(long startTime, long endTime, long duration)
    {
        if (duration > TimelineUtils.FIVE_YEARS)
        {
            long newEndTime = startTime + TimelineUtils.FIVE_YEARS;
            if (getStartTime() != startTime || getEndTime() != newEndTime)
            {
                super.setStartTime(startTime);
                super.setEndTime(newEndTime);
                updateTimeValues();
            }
            return;
        }

        if (duration < TimelineUtils.SECOND)
        {
            long difference = TimelineUtils.SECOND - duration;
            long newStartTime = startTime - difference / 2;
            long newEndTime = endTime + difference / 2;
            if (getStartTime() != newStartTime || getEndTime() != newEndTime)
            {
                super.setStartTime(newStartTime);
                super.setEndTime(newEndTime);
                updateTimeValues();
            }
            return;
        }

        if (getStartTime() != startTime || getEndTime() != endTime)
        {
            super.setStartTime(startTime);
            super.setEndTime(endTime);
            updateTimeValues();
        }
    }

    @Override
    public void setStartTime(long startTime)
    {
        if (this.startTime != startTime)
        {
            setTimes(startTime, getEndTime());
        }
    }

    @Override
    public void setEndTime(long endTime)
    {
        if (this.endTime != endTime)
        {
            setTimes(getStartTime(), endTime);
        }
    }

    @Override
    public void setPosition(long position)
    {
        position = Math.max(startTime, Math.min(endTime, position)); // Bound to our time range

        if (position == this.position) return; // No change

        this.position = position;
        setXFromPosition();

        fireTimePositionChanged();
    }

    @Override
    public void setXPosition(int xPosition)
    {
        xPosition = Math.max(0, Math.min(getEffectiveWidth(), xPosition)); // Bound to our width

        if (xPosition == this.xPosition) return; // No change

        this.xPosition = xPosition;
        setPositionFromX();

        fireTimePositionChanged();
    }

    /**
     * Using the current X position, set the time position accordingly.
     */
    protected void setPositionFromX()
    {
        position = (long) ((((double) xPosition) / getEffectiveWidth()) * getTotalTime()) + startTime;
    }

    /**
     * Using the current time position, set the X position accordingly.
     */
    protected void setXFromPosition()
    {
        final long tt = getTotalTime();
        final long duration = tt <= 0 ? 1 : tt; // Avoid division by 0

        xPosition = (int) ((((double) getDeltaPosition()) / duration) * getEffectiveWidth());
    }

    /**
     * Update internal data and notify listeners that our bounds (start/end) or size (which affects the position to
     * X mapping) have changed.
     */
    protected void updateTimeValues()
    {
        // If the current position isn't within the new time end points, then reset to start
        if (position < startTime || position > endTime)
        {
            position = startTime;
        }

        setXFromPosition();

        updateMarkerCursorLocations();

        fireTimeSpanChanged();
    }

    @Override
    public long getDeltaPosition()
    {
        return position - startTime;
    }

    @Override
    public long getPosition()
    {
        return position;
    }

    @Override
    public int getXPosition()
    {
        return xPosition;
    }

    @Override
    public void zoomTime(long newTotalTime)
    {
        setTimes(getStartTime(), getEndTime() + newTotalTime);
    }

    @Override
    public long xToTime(int x)
    {
        return (long) ((((double) x) / getEffectiveWidth()) * getTotalTime()) + getStartTime();
    }

    public double timeToX(long time, int width)
    {
        final long tt = getTotalTime();
        final long duration = tt <= 0 ? 1 : tt; // Avoid division by 0

        final double timeDiff = time - getStartTime();
        return (timeDiff / duration) * width;
    }

    @Override
    public double timeToX(long time)
    {
        return timeToX(time, getEffectiveWidth());
    }

    public int timeToXRounded(long time)
    {
        return (int) Math.round(timeToX(time, getEffectiveWidth()));
    }

    @Override
    public void panTimeByXOffset(int xOffset)
    {
        if (xOffset == 0)
        {
            return;
        }

        long diff = xToTime(xOffset) - xToTime(0);
        setTimes(getStartTime() + diff, getEndTime() + diff);
    }

    @Override
    public void scaleTimeOut(double factor, int x)
    {
        updateTimeSpanDuration(x, factor);
    }

    @Override
    public void scaleTimeIn(double factor, int x)
    {
        updateTimeSpanDuration(x, 1.0 / factor);
    }

    /**
     * Inform all listeners that the time position has changed.
     */
    protected void fireTimePositionChanged()
    {
        for (ITimeSpanListener listener : listeners)
        {
            listener.timePositionChanged();
        }
    }

    /**
     * Inform all listeners that the time span end points have changed.
     */
    protected void fireTimeSpanChanged()
    {
        for (ITimeSpanListener listener : listeners)
        {
            listener.timeSpanChanged();
        }
    }

    @Override
    public IMarker createMomentMarker(String label, long time, IPlaybackLane... targetLanes)
    {
        DefaultMarker marker = label != null ? new DefaultMarker(label) : new DefaultMarker();

        marker.setMoment(time);

        for (IPlaybackLane lane : targetLanes)
        {
            marker.addTargetLane(lane);
        }

        addMarker(marker);

        return marker;
    }

    @Override
    public void addMarker(IMarker marker)
    {
        markers.add(marker);
        updateMarkerCursorLocation(marker);

        for (ITimeSpanListener listener : listeners)
        {
            listener.markerAdded(marker);
        }
    }

    @Override
    public void removeMarker(IMarker marker)
    {
        if (markers.remove(marker))
        {
            for (ITimeSpanListener listener : listeners)
            {
                listener.markerRemoved(marker);
            }
        }
    }

    @Override
    public List<IMarker> getMarkers()
    {
        return Collections.unmodifiableList(markers);
    }

    @Override
    public boolean containsTime(long time)
    {
        return isContained(time, getStartTime(), getEndTime());
    }

    @Override
    public boolean intersects(long start, long end)
    {
        return isContained(start, startTime, endTime) || isContained(end, startTime, endTime)
                || isContained(startTime, start, end) || isContained(endTime, start, end);
    }

    /**
     * Update the cursor positions for all our markers.  This only has to occur when the time span end points change
     * or the visual component changes size.
     */
    protected void updateMarkerCursorLocations()
    {
        for (IMarker marker : markers)
        {
            updateMarkerCursorLocation(marker);
        }
    }

    /**
     * If the given marker has a time moment, then update the cursor X position for that time moment.
     *
     * @param marker Marker to update
     */
    protected void updateMarkerCursorLocation(IMarker marker)
    {
        if (marker.hasMoment())
        {
            marker.updateCursorPosition((int) timeToX(marker.getMoment()));
        }
    }

    /**
     * Change the time span's duration based upon a factor value.  Given an x-coordinate, the start and end times of
     * the time span will be adjusted so that the old time value nearest the x will remain centered on the x.
     * <p/>
     * When using a mouse to zoom on the time span, the mouse pointer's x-coordinate is presumably the user's visual
     * focal point, so the zooming toward the time value under the x-coordinate makes the most sense for the user (i.e.
     * they will be able to easily track where they are zooming).
     *
     * @param x          x-coordinate to zoom toward.
     * @param realFactor factor to zoom in/out on the time span.
     */
    protected void updateTimeSpanDuration(int x, double realFactor)
    {
        double oldDuration = getTotalTime();
        double newDuration = oldDuration * realFactor;
        double centerTime = xToTime(x);
        double startRatio = (centerTime - getStartTime()) / oldDuration;
        double endRatio = 1.0 - startRatio;

        // Ensure that start time never goes negative
        long newStartTime = (long) (centerTime - (newDuration * startRatio));

        if (newStartTime < 0)
        {
            newStartTime = 0;
            endRatio = 1;
        }

        setTimes(newStartTime, (long) (centerTime + (newDuration * endRatio)));
    }

    /**
     * Determine if a given time is contained within the given start/end points.
     *
     * @param time  Time to test
     * @param start Start time
     * @param end   End time
     * @return {@code true} if the time is between start and end, inclusive
     */
    private boolean isContained(long time, long start, long end)
    {
        return time >= start && time <= end;
    }

    @Override
    public String toString()
    {
        return String.format("TS[%d - %d, w=%d d=%d]", startTime, endTime, getEffectiveWidth(), getDeltaPosition());
    }
}
