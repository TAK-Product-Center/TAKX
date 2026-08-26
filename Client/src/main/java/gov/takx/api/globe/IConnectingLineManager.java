package gov.takx.api.globe;

import gov.nasa.worldwind.render.DrawContext;

/**
 * Class contract for the ConnectingLineManager, responsible for creating and rendering the connecting lines.
 *
 */
public interface IConnectingLineManager
{
    /**
     * Render the connecting lines that are visible.
     *
     * @param dc Draw context
     */
    void render(DrawContext dc);

    /**
     * Set the override snap to surface.  This is typically used when the current rendering engine doesn't support
     * full 3D elevations).
     *
     * @param forceSnapToSurface Pass {@code true} to force the connecting lines to render on the surface
     */
    void setForceSnapToSurface(boolean forceSnapToSurface);
}
