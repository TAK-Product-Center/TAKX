package gov.takx.core.globe.tak.ww;

import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.render.DrawContext;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.core.globe.layers.IGlobeLayer;

/**
 * Implementation of {@link Layer} that is used as a placeholder and does no actual rendering.
 *
 * @since 3.4
 */
public class NoOpGlobeLayer extends AbstractLayer implements IGlobeLayer
{
    private final IRaptorLayer raptorLayer;

    public NoOpGlobeLayer(IRaptorLayer raptorLayer)
    {
        this.raptorLayer = raptorLayer;
    }

    @Override
    public boolean isEnabled()
    {
        return raptorLayer.isVisible();
    }

    @Override
    public boolean isPickEnabled()
    {
        return false;
    }

    @Override
    public IRaptorLayer getRaptorLayer()
    {
        return raptorLayer;
    }

    @Override
    public Layer getLayer()
    {
        return this;
    }

    @Override
    public void setLayer(Layer layer)
    {
        //
    }

    @Override
    public void setEditable(boolean editable)
    {
        //
    }

    @Override
    public boolean isSharable()
    {
        return true;
    }

    @Override
    public void setSharable(boolean sharable)
    {
        //
    }

    @Override
    protected void doRender(DrawContext drawContext)
    {
        // do nothing - rendering happens in TAK, not WW
    }
}
