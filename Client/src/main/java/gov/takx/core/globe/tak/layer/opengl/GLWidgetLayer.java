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
import gov.takx.core.globe.tak.WidgetLayerManager;
import gov.takx.core.globe.tak.layer.WidgetLayer;
import gov.takx.core.globe.tak.ww.ApplicationSceneController;
import jakarta.annotation.Nonnull;

/**
 * Process widgets served by the {@link WidgetLayerManager}.
 *
 * @since 3.5
 */
public class GLWidgetLayer extends GLAbstractWorldWindLayer
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
            if (object.second instanceof WidgetLayer widgetLayer)
            {
                return new GLWidgetLayer(object.first, widgetLayer);
            }
            return null;
        }
    };
    private final WidgetLayer widgetLayer;

    protected GLWidgetLayer(MapRenderer surface, WidgetLayer subject)
    {
        super(surface, subject, GLMapView.RENDER_PASS_UI);
        widgetLayer = subject;
    }

    @Override
    protected void drawImpl(GLMapView view, @Nonnull IApplicationGlobe globe, TakWorldWindow wwd, ApplicationSceneController sc, int renderPass)
    {
        if (MathUtils.hasBits(renderPass, GLMapView.RENDER_PASS_UI))
        {
            widgetLayer.getWidgetLayerManager().render(globe);
        }
    }
}
