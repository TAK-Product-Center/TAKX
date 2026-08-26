package gov.takx.core.globe.layers;

import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.render.DrawContext;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IConnectingLineManager;
import gov.takx.api.globe.IRenderingEngine;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.platform.IRaptorShutdownListener;
import gov.takx.api.ui.event.RenderingEngineChangedEvent;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.lang.invoke.MethodHandles;

/**
 * This layer is responsible for displaying all the track points and any connecting lines active for the
 * various map objects.
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class TracksLayer extends AbstractLayer implements IGlobeLayer, IRaptorShutdownListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IConnectingLineManager connectingLineManager;

    @Inject
    private EventBus eventBus;

    private final IRaptorLayer raptorLayer;
    private final IApplicationGlobe globe;

    /**
     * Construct a tracks layer working with the given globe.
     *
     * @param globe The globe we are rendering for
     * @param raptorLayer The raptor layer for which we are the globe layer
     */
    public TracksLayer(IApplicationGlobe globe, IRaptorLayer raptorLayer)
    {
        this.globe = globe;
        this.raptorLayer = raptorLayer;
    }

    @PostConstruct
    private void postConstruct()
    {
        configureConnectingLines();

        eventBus.register(this);  // Wire up event processing
    }

    /**
     * @return Always false
     */
    @Override
    public boolean isSharable()
    {
        return false;
    }

    @Override
    public void setSharable(boolean sharable)
    {
        // Ignored
    }

    /**
     * The rendering engine has changed.
     *
     * @param evt Event indicating the new rendering engine
     */
    @SuppressWarnings("unused")
    @Subscribe
    public void onRenderingEngineChanged(RenderingEngineChangedEvent evt)
    {
        if (evt.getGlobe() != globe) return; // Not directed at us, ignore it

        if (logger.isDebugEnabled()) logger.debug("Rendering Engine changed to: {}", evt.getRenderingEngine());

        configureConnectingLines();
    }

    @Override
    public void onShutdown(IProgressMonitor progressMonitor)
    {
        if (logger.isDebugEnabled()) logger.debug("Unregistering event listener");

        eventBus.unregister(this);  // Un-Wire up event processing
    }

    /**
     * Inform the connecting line manager if it is allowed to render at elevation or not.
     */
    private void configureConnectingLines()
    {
        final IRenderingEngine renderer = globe.getPrimaryRenderingEngine();
        final boolean snapToSurface = renderer == null || !renderer.supportsElevation();

        connectingLineManager.setForceSnapToSurface(snapToSurface);
    }

    @Override
    protected void doPreRender(DrawContext dc)
    {
        globe.preRenderRenderers(dc); // Process pre-renderable elements in the rendering engines
    }

    /**
     * Render the connecting lines.
     *
     * @param dc Drawing context
     */
    @Override
    protected void doRender(DrawContext dc)
    {
        draw(dc);
    }

    /**
     * Render the scene in pick mode.
     *
     * @param dc    the current draw context for rendering.
     * @param point the screen coordinate point
     */
    @Override
    protected void doPick(DrawContext dc, Point point)
    {
        draw(dc);
    }

    protected void draw(final DrawContext dc)
    {
        if (dc.getVisibleSector() == null || dc.getSurfaceGeometry() == null) return;

        connectingLineManager.render(dc); // Process all the connecting lines

        globe.renderRenderers(dc); // Process renderable elements in the rendering engines
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
        throw new UnsupportedOperationException("Layer can not be (re)-set");
    }

    @Override
    public void setEditable(boolean editable)
    {
        throw new UnsupportedOperationException("Layer not editable");
    }
}
