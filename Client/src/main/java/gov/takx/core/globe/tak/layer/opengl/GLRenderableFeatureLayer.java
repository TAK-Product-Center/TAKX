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
import gov.takx.core.globe.tak.RenderableFeatureManager;
import gov.takx.core.globe.tak.layer.RenderableFeatureLayer;
import gov.takx.core.globe.tak.ww.ApplicationSceneController;
import jakarta.annotation.Nonnull;

/**
 * Process renderables served by the {@link RenderableFeatureManager}.
 *
 * @since 3.5
 */
public class GLRenderableFeatureLayer extends GLAbstractWorldWindLayer
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
            if (object.second instanceof RenderableFeatureLayer renderableFeatureLayer)
            {
                return new GLRenderableFeatureLayer(object.first, renderableFeatureLayer);
            }
            return null;
        }
    };
    private final RenderableFeatureLayer renderableFeatureLayer;

    protected GLRenderableFeatureLayer(MapRenderer surface, RenderableFeatureLayer subject)
    {
        super(surface, subject, GLMapView.RENDER_PASS_SPRITES);
        renderableFeatureLayer = subject;
    }

    @Override
    protected void drawImpl(GLMapView view, @Nonnull IApplicationGlobe globe, TakWorldWindow wwd, ApplicationSceneController sc, int renderPass)
    {
        if (MathUtils.hasBits(renderPass, GLMapView.RENDER_PASS_SPRITES))
        {
            renderableFeatureLayer.getRenderableFeatureManager().render(globe);
        }
    }
}
