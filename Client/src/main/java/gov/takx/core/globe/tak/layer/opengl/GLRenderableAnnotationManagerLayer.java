package gov.takx.core.globe.tak.layer.opengl;

import android.util.Pair;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.layer.opengl.GLLayerSpi2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.math.MathUtils;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.core.globe.TakWorldWindow;
import gov.takx.core.globe.tak.RenderableAnnotationManager;
import gov.takx.core.globe.tak.layer.RenderableAnnotationManagerLayer;
import gov.takx.core.globe.tak.ww.ApplicationSceneController;
import jakarta.annotation.Nonnull;

/**
 * Process annotations served by the {@link RenderableAnnotationManager}.
 *
 * @since 5.1
 */
public class GLRenderableAnnotationManagerLayer extends GLAbstractWorldWindLayer
{
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
            if (object.second instanceof RenderableAnnotationManagerLayer renderableAnnotationManagerLayer)
            {
                return new GLRenderableAnnotationManagerLayer(object.first, renderableAnnotationManagerLayer);
            }
            return null;
        }
    };
    private final RenderableAnnotationManager renderableAnnotationManager;

    protected GLRenderableAnnotationManagerLayer(MapRenderer surface, RenderableAnnotationManagerLayer subject)
    {
        super(surface, subject, GLMapView.RENDER_PASS_SPRITES);
        // This requires getRenderableAnnotationManager() to return a singleton
        renderableAnnotationManager = subject.getRenderableAnnotationManager();
    }

    @Override
    protected void drawImpl(GLMapView view, @Nonnull IApplicationGlobe globe, TakWorldWindow wwd, ApplicationSceneController sc, int renderPass)
    {
        if (MathUtils.hasBits(renderPass, GLMapView.RENDER_PASS_SPRITES))
        {
            renderableAnnotationManager.render(globe);
        }
    }
}
