package gov.takx.platform.playback;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.playback.IMarkerBoundary;

/**
 * Concrete implementation of {@link IMarkerBoundary}.
 *
 * @since 1.47
 */
public class MarkerBoundary implements IMarkerBoundary
{
    protected Long startTime;
    protected Long endTime;
    protected ILocation[] geography;
    protected Long moment;
    protected ILocation point;

    @Override
    public boolean hasChronologicalBoundary()
    {
        return startTime != null && endTime != null;
    }

    @Override
    public void setChronologicalBoundary(long startTime, long endTime)
    {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public void clearChronologicalBoundary()
    {
        startTime = null;
        endTime = null;
    }

    @Override
    public Long getStartTime()
    {
        return startTime;
    }

    @Override
    public Long getEndTime()
    {
        return endTime;
    }

    @Override
    public boolean hasGeographicBoundary()
    {
        return geography != null;
    }

    @Override
    public ILocation[] getGeographicBoundary()
    {
        return geography;
    }

    @Override
    public void setGeographicBoundary(ILocation[] geography)
    {
        this.geography = geography;
    }

    @Override
    public boolean hasPoint()
    {
        return point != null;
    }

    @Override
    public ILocation getPoint()
    {
        return point;
    }

    @Override
    public void setPoint(ILocation point)
    {
        this.point = point;
    }

    @Override
    public boolean hasMoment()
    {
        return moment != null;
    }

    @Override
    public Long getMoment()
    {
        return moment;
    }

    @Override
    public void setMoment(long moment)
    {
        this.moment = moment;
    }

    @Override
    public void clearMoment()
    {
        moment = null;
    }
}
