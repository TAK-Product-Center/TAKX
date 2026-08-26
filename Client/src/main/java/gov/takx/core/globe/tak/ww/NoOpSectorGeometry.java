package gov.takx.core.globe.tak.ww;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.Intersection;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.terrain.SectorGeometry;

import java.awt.Point;
import java.nio.DoubleBuffer;
import java.util.List;

/**
 * A no-op implementation of {@link SectorGeometry}.
 * @since 5.2
 */
public class NoOpSectorGeometry implements SectorGeometry
{
    private final Extent extent;
    private final Sector sector;

    public NoOpSectorGeometry(Extent extent, Sector sector)
    {
        this.extent = extent;
        this.sector = sector;
    }

    @Override
    public Extent getExtent()
    {
        return extent;
    }

    @Override
    public Sector getSector()
    {
        return sector;
    }

    @Override
    public void pick(DrawContext dc, Point pickPoint)
    {
    }

    @Override
    public Vec4 getSurfacePoint(Angle latitude, Angle longitude, double metersOffset)
    {
        return null;
    }

    @Override
    public void beginRendering(DrawContext dc, int numTextureUnits)
    {
    }

    @Override
    public void endRendering(DrawContext dc)
    {
    }

    @Override
    public void renderMultiTexture(DrawContext dc, int numTextureUnits)
    {
    }

    @Override
    public void renderWireframe(DrawContext dc, boolean interior, boolean exterior)
    {
    }

    @Override
    public void renderBoundingVolume(DrawContext dc)
    {
    }

    @Override
    public void renderTileID(DrawContext dc)
    {
    }

    @Override
    public PickedObject[] pick(DrawContext dc, List<? extends Point> pickPoints)
    {
        return new PickedObject[0];
    }

    @Override
    public Intersection[] intersect(Line line)
    {
        return new Intersection[0];
    }

    @Override
    public Intersection[] intersect(double elevation)
    {
        return new Intersection[0];
    }

    @Override
    public DoubleBuffer makeTextureCoordinates(GeographicTextureCoordinateComputer computer)
    {
        return null;
    }

    @Override
    public void renderMultiTexture(DrawContext dc, int numTextureUnits, boolean beginRenderingCalled)
    {
    }

    @Override
    public void render(DrawContext dc, boolean beginRenderingCalled)
    {
    }

    @Override
    public void render(DrawContext dc)
    {
    }
}
