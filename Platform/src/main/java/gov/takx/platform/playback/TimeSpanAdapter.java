package gov.takx.platform.playback;

import gov.takx.api.playback.IMarker;
import gov.takx.api.playback.ITimeSpanListener;

/**
 * Adapter for creating simple implementations of {@link ITimeSpanListener}.  Just override
 * the methods you actually need.
 *
 * @since 1.47
 */
public class TimeSpanAdapter implements ITimeSpanListener
{
    @Override
    public void timeSpanChanged()
    {
    }

    @Override
    public void timePositionChanged()
    {
    }

    @Override
    public void markerAdded(IMarker marker)
    {
    }

    @Override
    public void markerRemoved(IMarker marker)
    {
    }
}
