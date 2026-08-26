package gov.takx.platform.globe;

import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.render.markers.Marker;

/**
 * A WorldWind {@link gov.nasa.worldwind.render.Renderable} that wraps a {@link Marker} and will be processed by TAK.
 * @since 5.1
 */
public class MarkerRenderable extends AVListImpl implements OrderedRenderable
{
    private Marker marker;

    /**
     * @param marker the initial marker
     */
    public MarkerRenderable(Marker marker)
    {
        this.marker = marker;
    }

    /**
     * Get the {@link Marker} associated with this renderable.
     *
     * @return the marker
     */
    public Marker getMarker()
    {
        return marker;
    }

    /**
     * Set the {@link Marker} associated with this renderable.
     *
     * @param marker the marker
     */
    public void setMarker(Marker marker)
    {
        this.marker = marker;
    }

    @Override
    public double getDistanceFromEye()
    {
        return 0;
    }

    @Override
    public void pick(DrawContext drawContext, java.awt.Point point)
    {
        // no-op
    }

    @Override
    public void render(DrawContext drawContext)
    {
        // no-op
    }
}