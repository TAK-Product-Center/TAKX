package gov.takx.core.view.gui.maps.exporters.dataset;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.PreRenderable;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfacePolyline;

import java.util.List;

/**
 * Renderable that draws a set of cross hairs on the surface of the globe.
 *
 * @since 1.43
 */
public class SurfaceCrossHairs implements Renderable, PreRenderable
{
    private static final ShapeAttributes DEFAULT_SHAPE_ATTRIBUTES = new BasicShapeAttributes();

    private LatLon location;
    private Angle width = Angle.ZERO;

    private SurfacePolyline verticalBar;
    private SurfacePolyline horizontalBar;

    /**
     * Create a new cross hairs object with no location.
     */
    public SurfaceCrossHairs()
    {
    }

    /**
     * Indicates the location of the cross hairs target point (where the lines cross).
     *
     * @return The location of the target point. Returns {@code null} if there is no location set.
     */
    public LatLon getLocation()
    {
        return location;
    }

    /**
     * Indicates the location of the cross hairs target point (the point where the lines cross).
     *
     * @param location New target location. May be {@code null}.
     */
    public void setLocation(LatLon location)
    {
        this.location = location;
        invalidate();
    }

    /**
     * Indicates the width of the cross hairs icon.
     *
     * @return The width of the cross hairs (i.e. the length of the horizontal line in the cross). The width is returned
     * as an angle on the globe.
     */
    public Angle getWidth()
    {
        return width;
    }

    /**
     * Specifies the width of the cross hairs icon.
     *
     * @param width New width as an angle on the globe. May not be negative.
     */
    public void setWidth(Angle width)
    {
        if (width.degrees < 0) throw new IllegalArgumentException("Parameter width must be non-negative angle");
        this.width = width;
        invalidate();
    }

    @Override
    public void preRender(DrawContext dc)
    {
        if (location == null) return;

        // If the line shapes are not available, generate them now.
        if (verticalBar == null) createShapes();

        verticalBar.preRender(dc);
        horizontalBar.preRender(dc);
    }

    @Override
    public void render(DrawContext dc)
    {
        if (verticalBar == null) return;

        verticalBar.render(dc);
        horizontalBar.render(dc);
    }

    /**
     * Invalidate this shape, indicating that the geometry must be regenerated on the next frame.
     */
    private void invalidate()
    {
        // Need to regenerate shapes next frame
        verticalBar = null;
        horizontalBar = null;
    }

    private void createShapes()
    {
        final Angle halfWidth = width.divide(2.0);

        verticalBar = new SurfacePolyline(DEFAULT_SHAPE_ATTRIBUTES);
        verticalBar.setLocations(List.of(
                LatLon.greatCircleEndPosition(location, Angle.ZERO, halfWidth),     // North end
                LatLon.greatCircleEndPosition(location, Angle.POS180, halfWidth))); // South end

        horizontalBar = new SurfacePolyline(DEFAULT_SHAPE_ATTRIBUTES);
        horizontalBar.setLocations(List.of(
                LatLon.greatCircleEndPosition(location, Angle.NEG90, halfWidth),   // West end
                LatLon.greatCircleEndPosition(location, Angle.POS90, halfWidth))); // East end
    }
}
