package gov.takx.core.globe.tak.ww.layer;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Path;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.globe.tak.RenderableManager;
import jakarta.inject.Inject;

/**
 * Implementation of {@link Path} that directly submits itself to the {@link RenderableManager} to reduce unnecessary calculations.
 *
 * @since 5.2
 */
@PseudoBean
class TakPath extends Path
{
    @Inject
    private RenderableManager renderableManager;

    public TakPath(Iterable<? extends Position> positions)
    {
        super(positions);
    }

    @Override
    protected void makeOrderedRenderable(DrawContext dc)
    {
        renderableManager.offerRenderable(this, dc);
    }
}
