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
import gov.takx.core.globe.tak.ScreenAnnotationManager;
import gov.takx.core.globe.tak.layer.ScreenAnnotationLayer;
import gov.takx.core.globe.tak.ww.ApplicationSceneController;
import jakarta.annotation.Nonnull;

/**
 * Process screen annotations served by the {@link ScreenAnnotationManager}.
 *
 * @since 3.5
 */
public class GLScreenAnnotationLayer extends GLAbstractWorldWindLayer
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
            if (object.second instanceof ScreenAnnotationLayer screenAnnotationLayer)
            {
                return new GLScreenAnnotationLayer(object.first, screenAnnotationLayer);
            }
            return null;
        }
    };
    private final ScreenAnnotationLayer screenAnnotationLayer;

    protected GLScreenAnnotationLayer(MapRenderer surface, ScreenAnnotationLayer subject)
    {
        super(surface, subject, GLMapView.RENDER_PASS_UI);
        screenAnnotationLayer = subject;
    }

    @Override
    protected void drawImpl(GLMapView view, @Nonnull IApplicationGlobe globe, TakWorldWindow wwd, ApplicationSceneController sc, int renderPass)
    {
        if (MathUtils.hasBits(renderPass, GLMapView.RENDER_PASS_UI))
        {
            screenAnnotationLayer.getScreenAnnotationManager().render(view, sc.getDrawContext());
        }
    }
}
