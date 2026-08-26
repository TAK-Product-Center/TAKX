package gov.takx.api.event.geo;

import gov.takx.api.event.PlatformEvent;
import gov.takx.api.ping.IGeoPingService;
import gov.takx.api.ping.IPingEvent;
import gov.takx.api.ping.IPingListener;
import gov.takx.commons.swing.GraphicsUtils;

import java.awt.Color;

/**
 * Platform event fired when a GeoPing is initiated.  This event can be used instead of registering as a listener
 * via {@link IGeoPingService#addListener(IPingListener)}.
 * <p>
 * This event implements {@link IPingEvent} for code simplicity, and delegates all method calls to the source
 * ping event.
 *
 * @since 2.0.0
 */
public class GeoPing implements IPingEvent, PlatformEvent
{
    private final IPingEvent sourceEvent;

    public GeoPing(IPingEvent sourceEvent)
    {
        this.sourceEvent = sourceEvent;
    }

    public IPingEvent getSourceEvent()
    {
        return sourceEvent;
    }

    @Override
    public Color getColor()
    {
        return sourceEvent.getColor();
    }

    @Override
    public int getDuration()
    {
        return sourceEvent.getDuration();
    }

    @Override
    public double getLatitude()
    {
        return sourceEvent.getLatitude();
    }

    @Override
    public double getLongitude()
    {
        return sourceEvent.getLongitude();
    }

    @Override
    public boolean isPluginEvent()
    {
        return sourceEvent.isPluginEvent();
    }

    @Override
    public boolean isRemoteEvent()
    {
        return sourceEvent.isRemoteEvent();
    }

    @Override
    public boolean isVerticalFlip()
    {
        return sourceEvent.isVerticalFlip();
    }

    @Override
    public GraphicsUtils.Shape getShape()
    {
        return sourceEvent.getShape();
    }

    @Override
    public boolean isPlayAudio()
    {
        return sourceEvent.isPlayAudio();
    }
}
