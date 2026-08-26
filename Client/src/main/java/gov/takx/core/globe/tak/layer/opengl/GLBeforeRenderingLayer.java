package gov.takx.core.globe.tak.layer.opengl;

import android.util.Pair;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.layer.opengl.GLLayerSpi2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.math.MathUtils;
import com.jogamp.opengl.GLAutoDrawable;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IWorldWindowGL;
import gov.takx.core.globe.TakWorldWindow;
import gov.takx.core.globe.tak.layer.BeforeRenderingLayer;
import gov.takx.core.globe.tak.ww.ApplicationSceneController;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Handle pre-rendering WW events.
 *
 * @since 3.5
 */
public class GLBeforeRenderingLayer extends GLAbstractWorldWindLayer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public final static GLLayerSpi2 SPI = new GLLayerSpi2()
    {
        @Override
        public int getPriority()
        {
            return 1;
        }

        @Override
        public GLLayer2 create(Pair<MapRenderer, Layer> object)
        {
            if (object.second instanceof BeforeRenderingLayer)
            {
                return new GLBeforeRenderingLayer(object.first, object.second);
            }
            return null;
        }
    };

    GLBeforeRenderingLayer(MapRenderer surface, Layer subject)
    {
        super(surface, subject, GLMapView.RENDER_PASS_SPRITES);
    }

    @Override
    protected void drawImpl(GLMapView view, @Nonnull IApplicationGlobe globe, TakWorldWindow wwd, ApplicationSceneController sc, int renderPass)
    {
        if (MathUtils.hasBits(renderPass, GLMapView.RENDER_PASS_SPRITES))
        {
            sc.setValue(ApplicationSceneController.RENDER_PASS, renderPass);
            doBeforeRendering(view, wwd);
        }
    }

    private void doBeforeRendering(GLMapView view, TakWorldWindow wwd)
    {
        GLAutoDrawable drawable = (IWorldWindowGL) view.getRenderSurface();

        try
        {
            try
            {
                wwd.callRenderingListeners(new RenderingEvent(drawable, "gov.nasa.worldwind.RenderingEvent.BeforeRendering"));
            } catch (Exception exception)
            {
                logger.error("Exception during call to GL event display listener", exception);
            }

            try
            {
                wwd.callRenderingListeners(new RenderingEvent(drawable, "gov.nasa.worldwind.RenderingEvent.BeforeBufferSwap"));
            } catch (Exception exception)
            {
                logger.error("Exception during call to GL event display listener", exception);
            }
        } catch (Exception exception)
        {
            logger.error("Exception while attempting to handle before-rendering events", exception);
        }
    }
}
