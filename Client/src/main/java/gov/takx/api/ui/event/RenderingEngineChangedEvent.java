package gov.takx.api.ui.event;

import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IRenderingEngine;

/**
 * Event class used when the rendering engine has been changed (switched).
 *
 * @see IRenderingEngine
 * @since 1.14
 */
public class RenderingEngineChangedEvent
{
    private final IRenderingEngine renderingEngine;
    private final IApplicationGlobe globe;

    /**
     * Construct the event indicating the new rendering engine.
     *
     * @param renderingEngine New rendering engine in use
     * @param globe The globe on which the renderer has changed
     */
    public RenderingEngineChangedEvent(IRenderingEngine renderingEngine, IApplicationGlobe globe)
    {
        this.renderingEngine = renderingEngine;
        this.globe = globe;
    }

    /**
     * @return The rendering engine now in use
     */
    public IRenderingEngine getRenderingEngine()
    {
        return renderingEngine;
    }

    /**
     * @return The WorldWind component on which the event is being fired
     */
    public IApplicationGlobe getGlobe()
    {
        return globe;
    }

    @Override
    public String toString()
    {
        return "RenderingEngineChangedEvent[renderingEngine=" + renderingEngine + "]";
    }
}
