package gov.takx.core.ping;

import gov.takx.api.annotation.Internal;
import gov.takx.api.ping.IPingEvent;
import gov.takx.commons.swing.GraphicsUtils;

import java.awt.Color;

/**
 */
@Internal
public class PingEvent implements IPingEvent
{
    private final double lat;
    private final double lon;
    private final Color color;
    private final int duration;
    private final boolean playAudio;
    private final GraphicsUtils.Shape shape;
    private final boolean verticalFlip;

    /**
     * Was the ping created by a plugin?
     */
    private final boolean pluginEvent;

    /**
     * Was this ping originated on a different/remote system.
     */
    private final boolean remoteEvent;

    public PingEvent(double lat, double lon, Color color, int duration)
    {
        this(lat, lon, color, duration, true, GraphicsUtils.Shape.CIRCLE, false);
    }

    public PingEvent(double lat, double lon, Color color, int duration, boolean pluginEvent)
    {
        this(lat, lon, color, duration, true, GraphicsUtils.Shape.CIRCLE, false, pluginEvent, false);
    }

    public PingEvent(double lat, double lon, Color color, int duration, boolean playAudio, GraphicsUtils.Shape shape, boolean verticalFlip)
    {
        this(lat, lon, color, duration, playAudio, shape, verticalFlip, false, false);
    }

    public PingEvent(double lat, double lon, Color color, int duration, boolean playAudio, GraphicsUtils.Shape shape, boolean verticalFlip, boolean pluginEvent, boolean remoteEvent)
    {
        this.lat = lat;
        this.lon = lon;
        this.color = color;
        this.duration = duration;
        this.playAudio = playAudio;
        this.shape = shape;
        this.verticalFlip = verticalFlip;
        this.pluginEvent = pluginEvent;
        this.remoteEvent = remoteEvent;
    }

    @Override
    public double getLatitude()
    {
        return lat;
    }

    @Override
    public double getLongitude()
    {
        return lon;
    }

    @Override
    public Color getColor()
    {
        return color;
    }

    @Override
    public int getDuration()
    {
        return duration;
    }

    @Override
    public boolean isPluginEvent()
    {
        return pluginEvent;
    }

    @Override
    public boolean isRemoteEvent()
    {
        return remoteEvent;
    }

    @Override
    public boolean isPlayAudio()
    {
        return playAudio;
    }

    @Override
    public GraphicsUtils.Shape getShape()
    {
        return shape;
    }

    @Override
    public boolean isVerticalFlip()
    {
        return verticalFlip;
    }

    @Override
    public String toString()
    {
        return "PingEvent{" +
                "color=" + color +
                ", lat=" + lat +
                ", lon=" + lon +
                ", duration=" + duration +
                ", playAudio=" + playAudio +
                ", shape=" + shape +
                ", verticalFlip=" + verticalFlip +
                ", pluginEvent=" + pluginEvent +
                ", remoteEvent=" + remoteEvent +
                '}';
    }
}
