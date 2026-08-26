package gov.takx.core.globe.render;

import gov.nasa.worldwind.render.DrawContext;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.platform.globe.renderer.ARenderingEngine;
import jakarta.annotation.Nonnull;

/**
 * {@code null} implementation of a rendering engine, which can be used as a placeholder if no secondary rendering
 * engine is specified.  Secondary rendering engine defaults to using this.
 *
 * @since 1.49
 */
public class NullRenderingEngine extends ARenderingEngine
{
    public NullRenderingEngine(IApplicationGlobe globe)
    {
        super(globe);
    }

    @Override
    public void preRender(DrawContext dc)
    {
    }

    @Override
    public void render(DrawContext dc)
    {
    }

    @Override
    public boolean supportsElevation()
    {
        return false;
    }

    @Override
    public void updateView(DrawContext dc)
    {
    }

    @Override
    public void setZOrder(@Nonnull IMapObject mapObject, int zOrder)
    {
    }

    @Override
    protected void processSingleMessage(IRaptorDataMessage rdm)
    {
    }

    @Override
    protected void handleMapObjectAdded(IMapObject mapObject)
    {
    }

    @Override
    protected void handleMapObjectDeleted(IMapObject mapObject)
    {
    }

    @Override
    protected void handleMapObjectPartialDelete(@Nonnull IMapObject mapObject, long timeOfDeletion)
    {
    }

    @Override
    protected void handleDeactivate()
    {
    }

    @Override
    protected void clearRenderableState()
    {
    }
}
