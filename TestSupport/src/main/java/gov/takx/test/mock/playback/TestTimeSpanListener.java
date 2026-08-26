package gov.takx.test.mock.playback;

import gov.takx.api.playback.IMarker;
import gov.takx.api.playback.ITimeSpanListener;

/**
 * Simple counting listener.
 *
 * @since 1.48
 */
public class TestTimeSpanListener implements ITimeSpanListener
{
    private int spanChangeCount = 0;
    private int posChangeCount = 0;
    private int addedCount = 0;
    private int removedCount = 0;

    public void reset()
    {
        spanChangeCount = 0;
        posChangeCount = 0;
        addedCount = 0;
        removedCount = 0;
    }

    @Override
    public void timeSpanChanged()
    {
        spanChangeCount += 1;
    }

    @Override
    public void timePositionChanged()
    {
        posChangeCount += 1;
    }

    @Override
    public void markerAdded(IMarker marker)
    {
        addedCount += 1;
    }

    @Override
    public void markerRemoved(IMarker marker)
    {
        removedCount += 1;
    }

    public int getSpanChangeCount()
    {
        return spanChangeCount;
    }

    public int getPosChangeCount()
    {
        return posChangeCount;
    }

    public int getAddedCount()
    {
        return addedCount;
    }

    public int getRemovedCount()
    {
        return removedCount;
    }
}
