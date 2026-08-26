package gov.takx.core.globe.render;

import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.platform.IRaptorShutdownListener;

/**
 * This class holds the components that make up the rendering context used by the rendering engine.  It holds the
 * {@link RenderingModel}, {@link RenderingStyleContextManager}, and {@link MarkerAttributeTracker}.  This collection
 * of context data lasts across rendering engine boundaries.  It lives as long as the open project does.
 *
 * @see RenderingModel
 * @see RenderingStyleContextManager
 * @see MarkerAttributeTracker
 * @since 1.14
 */
public class RenderingContext implements IRaptorShutdownListener
{
    private RenderingModel renderingModel;
    private RenderingStyleContextManager scManager;
    private MarkerAttributeTracker markerAttrTracker;

    /**
     * Constructor. Initialize the components of the context.
     */
    public RenderingContext()
    {
        // Setup the rendering model
        renderingModel = new RenderingModel();
        scManager = new RenderingStyleContextManager(renderingModel);
        markerAttrTracker = new MarkerAttributeTracker(scManager);

        // Slightly tricky construction since these two classes need to refer to each other.  The scManager is
        // ready for initial use by the model before its initialize() method is called, so do them in that order.

        renderingModel.initialize(scManager);
    }

    /**
     * @return the rendering style context manager
     */
    public RenderingStyleContextManager getStyleContextManager()
    {
        return scManager;
    }

    /**
     * @return the MarkerAttributeTracker
     */
    public MarkerAttributeTracker getMarkerAttrTracker()
    {
        return markerAttrTracker;
    }

    /**
     * @return The RenderingModel
     */
    public RenderingModel getRenderingModel()
    {
        return renderingModel;
    }

    @Override
    public void onShutdown(IProgressMonitor progressMonitor)
    {
        scManager.shutdown();
        markerAttrTracker.onShutdown(progressMonitor);
        renderingModel.onShutdown(progressMonitor);

        scManager = null;
        markerAttrTracker = null;
        renderingModel = null;
    }
}
