package gov.takx.core.view.gui.maps.terraindiscovery.controller.render;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.ITerrainDiscoveryController;

import java.awt.Point;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Layer for rendering elevation layer visualizations.  This class is backed by the Terrain Discovery Controller, so any
 * changes in it will automatically be reflected on the globe.
 *
 */
public class RenderableElevationLayer extends AbstractLayer
{
    private final ITerrainDiscoveryController controller;
    private final AtomicReference<Sector> sector;

    public RenderableElevationLayer(ITerrainDiscoveryController controller)
    {
        this.controller = controller;
        sector = new AtomicReference<>(null);
    }

    public void setSector(Sector sector)
    {
        this.sector.set(sector);
    }

    @Override
    public boolean isLayerInView(DrawContext dc)
    {
        Sector s = sector.get();
        return s == null || dc == null || dc.getVisibleSector() == null || s.intersects(dc.getVisibleSector());
    }

    @Override
    public void pick(DrawContext dc, Point point)
    {
        // ignore
    }

    @Override
    protected void doPreRender(DrawContext dc)
    {
        for (ITerrainRenderable tr : controller.getTerrainRenderables())
        {
            if (tr.isVisible())
            {
                tr.preRender(dc);
            }
        }
    }

    @Override
    protected void doRender(DrawContext dc)
    {
        for (ITerrainRenderable tr : controller.getTerrainRenderables())
        {
            if (tr.isVisible())
            {
                tr.render(dc);
            }
        }
    }
}
